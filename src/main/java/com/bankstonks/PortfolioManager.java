package com.bankstonks;

import com.bankstonks.model.PortfolioData;
import com.bankstonks.model.PortfolioRow;
import com.bankstonks.model.SlotState;
import com.bankstonks.model.TrackedItem;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;

/**
 * Owns the portfolio state for the currently logged-in account: cost basis per item,
 * last-seen bank quantities and GE slot states. Handles load/save to the RuneLite config
 * and computes the display rows.
 *
 * <p>All reads/writes happen from either the client thread (GE/bank events, row computation)
 * or the config-manager threads; the underlying maps are only touched from the client thread
 * so no extra synchronization is needed for the MVP.</p>
 */
@Slf4j
@Singleton
public class PortfolioManager
{
	static final String CONFIG_GROUP = "bankstonks";
	private static final String DATA_KEY_PREFIX = "data_";

	private final ConfigManager configManager;
	private final Gson gson;

	private long accountHash = -1L;
	private PortfolioData data = new PortfolioData();

	@Inject
	public PortfolioManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Switches to the given account, loading its persisted data. No-op if unchanged. */
	public void setAccount(long hash)
	{
		if (hash == accountHash)
		{
			return;
		}
		accountHash = hash;
		load();
	}

	public boolean hasAccount()
	{
		return accountHash != -1L;
	}

	private void load()
	{
		if (accountHash == -1L)
		{
			data = new PortfolioData();
			return;
		}
		String json = configManager.getConfiguration(CONFIG_GROUP, DATA_KEY_PREFIX + accountHash);
		if (json == null || json.isEmpty())
		{
			data = new PortfolioData();
		}
		else
		{
			try
			{
				data = gson.fromJson(json, PortfolioData.class);
				if (data == null)
				{
					data = new PortfolioData();
				}
				data.ensureInitialized();
			}
			catch (Exception e)
			{
				log.warn("failed to parse stored portfolio for account {}, starting fresh", accountHash, e);
				data = new PortfolioData();
			}
		}
	}

	public void save()
	{
		if (accountHash == -1L)
		{
			return;
		}
		configManager.setConfiguration(CONFIG_GROUP, DATA_KEY_PREFIX + accountHash, gson.toJson(data));
	}

	// ---- GE slot bookkeeping -------------------------------------------------

	public SlotState getSlot(int slot)
	{
		return data.getSlots().get(slot);
	}

	public void setSlot(int slot, SlotState state)
	{
		data.getSlots().put(slot, state);
	}

	public void clearSlot(int slot)
	{
		data.getSlots().remove(slot);
	}

	// ---- cost basis ----------------------------------------------------------

	/** Records a buy (GE fill) against the running cost basis, stamped with the current time. */
	public void recordBuy(int itemId, int quantity, long spent)
	{
		recordBuy(itemId, quantity, spent, 0L);
	}

	/**
	 * Records a buy against the running cost basis.
	 *
	 * @param heldSinceEpochMs an explicit "held since" time (manual entry); if &gt; 0 it sets
	 *                         the held-since date, otherwise the current time is used only when
	 *                         no date has been recorded yet.
	 */
	public void recordBuy(int itemId, int quantity, long spent, long heldSinceEpochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		TrackedItem tracked = data.getItems().computeIfAbsent(itemId, id -> new TrackedItem());
		if (heldSinceEpochMs > 0)
		{
			tracked.setFirstBoughtEpochMs(heldSinceEpochMs);
		}
		else if (tracked.getFirstBoughtEpochMs() == 0)
		{
			tracked.setFirstBoughtEpochMs(System.currentTimeMillis());
		}
		tracked.addBuy(quantity, spent);
	}

	public TrackedItem getTracked(int itemId)
	{
		return data.getItems().get(itemId);
	}

	/**
	 * Deletes an item's tracked data ("untrack"). It disappears from the list and will only
	 * reappear, freshly, if it is bought again.
	 */
	public void removeItem(int itemId)
	{
		data.getItems().remove(itemId);
	}

	/**
	 * Finds the tracked (bought) item id that corresponds to an item sitting in the bank,
	 * matching across variations so a charged/banked item resolves to the uncharged item it
	 * was bought as. Returns -1 if nothing tracked corresponds.
	 */
	public int boughtIdForBankItem(int bankItemId)
	{
		if (data.getItems().containsKey(bankItemId))
		{
			return bankItemId;
		}
		int base = ItemVariationMapping.map(bankItemId);
		for (Integer id : data.getItems().keySet())
		{
			if (ItemVariationMapping.map(id) == base)
			{
				return id;
			}
		}
		return -1;
	}

	/**
	 * Whether an item is on the untracked list and should be excluded. Supports {@code *}
	 * wildcards. Single source of truth used by the panel, buy tracker and bank overlay.
	 */
	public static boolean isBlocked(BankStonksConfig config, String itemName)
	{
		return BlockLists.matches(config.blockList(), itemName);
	}

	// ---- bank ----------------------------------------------------------------

	public void updateBank(Map<Integer, Integer> bankQuantities)
	{
		data.setBank(bankQuantities);
	}

	// ---- display -------------------------------------------------------------

	/** Total profit/loss across all held items from the last {@link #buildRows} call. */
	private volatile long lastTotalProfit;

	public long getLastTotalProfit()
	{
		return lastTotalProfit;
	}

	/**
	 * Net value of one item after the 2% GE sell tax (floored, capped at 5M per item).
	 * Items priced under 50 are tax-exempt. Buys are never taxed, so this only ever adjusts
	 * the current price side of the profit calculation.
	 */
	public static long netAfterTax(int price)
	{
		if (price < 50)
		{
			return price;
		}
		long tax = Math.min((long) Math.floor(price * 0.02), 5_000_000L);
		return price - tax;
	}

	/**
	 * Builds the rows to render. Held quantity is min(totalBought, held) so that items
	 * obtained outside the GE (drops, etc.) are never valued as profit, and sold-off stock
	 * drops out automatically.
	 *
	 * <p>Held counts the bank snapshot plus the live inventory/equipment quantities passed in
	 * (an item is only ever in one container, so there is no double counting).</p>
	 *
	 * <p>Must be called on the client thread (uses {@link ItemManager}).</p>
	 */
	public List<PortfolioRow> buildRows(ItemManager itemManager, BankStonksConfig config, Map<Integer, Integer> liveQuantities)
	{
		// Total held quantity grouped by variation base, so a charged/banked item counts
		// towards the uncharged item it was bought as (and dose variants collapse together).
		// Bank is a cached snapshot; inventory and worn items are live.
		Map<Integer, Integer> heldByBase = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : data.getBank().entrySet())
		{
			heldByBase.merge(ItemVariationMapping.map(e.getKey()), e.getValue(), Integer::sum);
		}
		if (liveQuantities != null)
		{
			for (Map.Entry<Integer, Integer> e : liveQuantities.entrySet())
			{
				heldByBase.merge(ItemVariationMapping.map(e.getKey()), e.getValue(), Integer::sum);
			}
		}

		List<PortfolioRow> rows = new ArrayList<>();
		long total = 0;
		for (Map.Entry<Integer, TrackedItem> entry : data.getItems().entrySet())
		{
			int itemId = entry.getKey();
			TrackedItem tracked = entry.getValue();
			if (tracked.getTotalBought() <= 0)
			{
				continue;
			}

			String name = itemManager.getItemComposition(itemId).getName();
			if (isBlocked(config, name))
			{
				continue;
			}

			int heldQty = heldByBase.getOrDefault(ItemVariationMapping.map(itemId), 0);
			int quantity = Math.min(tracked.getTotalBought(), heldQty);
			if (quantity <= 0 && config.hideEmpty())
			{
				continue;
			}

			long avg = tracked.averageBuyPrice();
			int rawCurrent = itemManager.getItemPrice(itemId);
			long effectiveCurrent = config.applyGeTax() ? netAfterTax(rawCurrent) : rawCurrent;
			long profitEach = effectiveCurrent - avg;
			long profitTotal = profitEach * quantity;

			total += profitTotal;
			rows.add(new PortfolioRow(itemId, name, quantity, avg, rawCurrent, profitEach, profitTotal, tracked.getFirstBoughtEpochMs()));
		}

		sort(rows, config.sortOrder());
		lastTotalProfit = total;
		return rows;
	}

	private static void sort(List<PortfolioRow> rows, SortOrder order)
	{
		final Comparator<PortfolioRow> comparator;
		switch (order)
		{
			case MOST_PROFIT:
				comparator = Comparator.comparingLong(PortfolioRow::getProfitTotal).reversed();
				break;
			case MOST_LOSS:
				comparator = Comparator.comparingLong(PortfolioRow::getProfitTotal);
				break;
			case HIGHEST_VALUE:
				comparator = Comparator.comparingLong((PortfolioRow r) -> (long) r.getCurrentPrice() * r.getQuantity()).reversed();
				break;
			case NAME:
				comparator = Comparator.comparing(r -> r.getName().toLowerCase());
				break;
			case BIGGEST_MOVERS:
			default:
				comparator = Comparator.comparingLong((PortfolioRow r) -> Math.abs(r.getProfitTotal())).reversed();
				break;
		}
		rows.sort(comparator);
	}
}

package com.geportfolio;

import com.geportfolio.model.PortfolioData;
import com.geportfolio.model.PortfolioRow;
import com.geportfolio.model.SlotState;
import com.geportfolio.model.TrackedItem;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

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
	static final String CONFIG_GROUP = "geportfolio";
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

	/** Records a buy (GE fill delta or manual entry) against the running cost basis. */
	public void recordBuy(int itemId, int quantity, long spent)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		data.getItems().computeIfAbsent(itemId, id -> new TrackedItem()).addBuy(quantity, spent);
	}

	// ---- bank ----------------------------------------------------------------

	public void updateBank(Map<Integer, Integer> bankQuantities)
	{
		data.setBank(bankQuantities);
	}

	// ---- display -------------------------------------------------------------

	/**
	 * Builds the rows to render. Held quantity is min(totalBought, bank quantity) so that
	 * items obtained outside the GE (drops, etc.) are never valued as profit, and sold-off
	 * stock drops out automatically.
	 *
	 * <p>Must be called on the client thread (uses {@link ItemManager}).</p>
	 */
	public List<PortfolioRow> buildRows(ItemManager itemManager, boolean hideEmpty)
	{
		List<PortfolioRow> rows = new ArrayList<>();
		for (Map.Entry<Integer, TrackedItem> entry : data.getItems().entrySet())
		{
			int itemId = entry.getKey();
			TrackedItem tracked = entry.getValue();
			if (tracked.getTotalBought() <= 0)
			{
				continue;
			}

			int bankQty = data.getBank().getOrDefault(itemId, 0);
			int quantity = Math.min(tracked.getTotalBought(), bankQty);
			if (quantity <= 0 && hideEmpty)
			{
				continue;
			}

			long avg = tracked.averageBuyPrice();
			int current = itemManager.getItemPrice(itemId);
			long profitEach = current - avg;
			long profitTotal = profitEach * quantity;
			String name = itemManager.getItemComposition(itemId).getName();

			rows.add(new PortfolioRow(itemId, name, quantity, avg, current, profitEach, profitTotal));
		}

		// Biggest absolute movers first.
		rows.sort(Comparator.comparingLong((PortfolioRow r) -> Math.abs(r.getProfitTotal())).reversed());
		return rows;
	}
}

package com.bankstonks;

import com.bankstonks.model.Lot;
import com.bankstonks.model.LotValuation;
import com.bankstonks.model.PortfolioData;
import com.bankstonks.model.PortfolioRow;
import com.bankstonks.model.SlotState;
import com.bankstonks.model.TrackedItem;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemMapping;
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
	static final String DATA_KEY_PREFIX = "data_";

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

	/**
	 * Records a manual buy as a standalone lot.
	 *
	 * @param heldSinceEpochMs the purchase time for this lot; if &le; 0 the current time is used.
	 */
	public void recordBuy(int itemId, int quantity, long spent, long heldSinceEpochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		long epochMs = heldSinceEpochMs > 0 ? heldSinceEpochMs : System.currentTimeMillis();
		data.getItems().computeIfAbsent(itemId, id -> new TrackedItem()).addBuy(quantity, spent, epochMs);
	}

	/** Records a GE fill, grouping the partial fills of one offer into a single lot. */
	public void recordOfferFill(int itemId, int quantity, long spent, long epochMs, long offerId)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		data.getItems().computeIfAbsent(itemId, id -> new TrackedItem()).addOfferFill(offerId, quantity, spent, epochMs);
	}

	/** Returns a fresh lot id for a newly-seen GE offer. */
	public long nextOfferId()
	{
		long id = data.getNextLotId() + 1;
		data.setNextLotId(id);
		return id;
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
	 * Removes a single buy lot (matched by quantity, spent and date). If it was the last lot,
	 * the whole item is dropped.
	 */
	public void removeLot(int itemId, int quantity, long spent, long epochMs)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked == null)
		{
			return;
		}
		List<Lot> lots = tracked.getLots();
		for (int i = 0; i < lots.size(); i++)
		{
			Lot lot = lots.get(i);
			if (lot.getQuantity() == quantity && lot.getSpent() == spent && lot.getEpochMs() == epochMs)
			{
				lots.remove(i);
				break;
			}
		}
		if (lots.isEmpty())
		{
			data.getItems().remove(itemId);
		}
	}

	/** Locks an item to its most recent buy (pinning it, shown regardless of holdings). */
	public void lockItem(int itemId)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.lockToNewest();
		}
	}

	/** Locks an item to a specific buy (matched by quantity, spent and date). */
	public void lockLot(int itemId, int quantity, long spent, long epochMs)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.lockToLot(quantity, spent, epochMs);
		}
	}

	/** Removes the lock from an item. */
	public void unlockItem(int itemId)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.unlock();
		}
	}

	/**
	 * The tradeable "base" item ids a held item counts towards. Uses RuneLite's {@link ItemMapping}
	 * (the same data {@link ItemManager#getItemPrice} sums) so a combination item resolves to the
	 * tradeable parts it was assembled from (e.g. an avernic defender counts as its hilt), then
	 * normalises through {@link ItemVariationMapping} for charged/dose variants.
	 *
	 * <p>Only single-quantity components are followed, so assembled parts (hilts, shards, ornament
	 * kits) count but consumed bulk charges (scales, darts) do not resurrect as "held".</p>
	 */
	/** Adds a held item's quantity to every tradeable base it counts towards. */
	private static void addHeld(Map<Integer, Integer> heldByBase, int itemId, int quantity)
	{
		for (int base : representedBases(itemId))
		{
			heldByBase.merge(base, quantity, Integer::sum);
		}
	}

	/**
	 * Assembled items that RuneLite has no price for and does not model in {@link ItemMapping}
	 * (the finished item is untradeable), mapped to the tradeable component you actually bought.
	 * Lets a held avernic defender count as its hilt, etc. Hand maintained; add entries as needed.
	 */
	private static final Map<Integer, int[]> ASSEMBLED_COMPONENTS = new HashMap<>();

	static
	{
		// Others (amulet of blood fury, ferocious gloves, etc.) are already resolved by ItemMapping.
		ASSEMBLED_COMPONENTS.put(22322, new int[]{22477}); // Avernic defender -> Avernic defender hilt
	}

	private static Set<Integer> representedBases(int itemId)
	{
		Set<Integer> bases = new HashSet<>();
		int[] manual = ASSEMBLED_COMPONENTS.get(itemId);
		if (manual != null)
		{
			for (int component : manual)
			{
				bases.add(ItemVariationMapping.map(component));
			}
			return bases;
		}
		Collection<ItemMapping> mappings = ItemMapping.map(itemId);
		if (mappings != null)
		{
			for (ItemMapping mapping : mappings)
			{
				if (mapping.getQuantity() == 1)
				{
					bases.add(ItemVariationMapping.map(mapping.getTradeableItem()));
				}
			}
		}
		if (bases.isEmpty())
		{
			bases.add(ItemVariationMapping.map(itemId));
		}
		return bases;
	}

	/**
	 * Finds the tracked (bought) item id that corresponds to an item sitting in the bank,
	 * matching across variations and combination items so a charged, banked or assembled item
	 * resolves to the item it was bought as. Returns -1 if nothing tracked corresponds.
	 */
	public int boughtIdForBankItem(int bankItemId)
	{
		if (data.getItems().containsKey(bankItemId))
		{
			return bankItemId;
		}
		Set<Integer> bases = representedBases(bankItemId);
		for (Integer id : data.getItems().keySet())
		{
			if (bases.contains(ItemVariationMapping.map(id)))
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
			addHeld(heldByBase, e.getKey(), e.getValue());
		}
		if (liveQuantities != null)
		{
			for (Map.Entry<Integer, Integer> e : liveQuantities.entrySet())
			{
				addHeld(heldByBase, e.getKey(), e.getValue());
			}
		}

		// Attribute held items to purchases most-recent first, so a newer buy claims the held units
		// before an older one. This resolves the ambiguity when the same items could satisfy either
		// a set purchase or individual-piece purchases: whichever you bought more recently wins
		// (consistent with the LIFO cost basis). A set claims un-unpacked set items plus complete
		// sets assembled from loose pieces, then consumes those pieces so nothing double counts.
		List<Map.Entry<Integer, TrackedItem>> claims = new ArrayList<>();
		for (Map.Entry<Integer, TrackedItem> entry : data.getItems().entrySet())
		{
			if (!entry.getValue().isLocked() && entry.getValue().totalBought() > 0)
			{
				claims.add(entry);
			}
		}
		claims.sort((a, b) ->
		{
			int cmp = Long.compare(newestBuy(b.getValue()), newestBuy(a.getValue()));
			return cmp != 0 ? cmp : Integer.compare(a.getKey(), b.getKey());
		});

		List<PortfolioRow> rows = new ArrayList<>();
		long total = 0;

		// Locked items are pinned to one buy: always shown, valued at that lot's price/date/quantity,
		// and excluded from the held-pool allocation below (their quantity is the locked lot's, not
		// what you currently hold).
		for (Map.Entry<Integer, TrackedItem> entry : data.getItems().entrySet())
		{
			TrackedItem tracked = entry.getValue();
			if (!tracked.isLocked())
			{
				continue;
			}
			int itemId = entry.getKey();
			if (isBlocked(config, itemManager.getItemComposition(itemId).getName()))
			{
				continue;
			}
			PortfolioRow row = buildLockedRow(itemManager, config, itemId, tracked);
			rows.add(row);
			total += row.getProfitTotal();
		}

		for (Map.Entry<Integer, TrackedItem> entry : claims)
		{
			int itemId = entry.getKey();
			TrackedItem tracked = entry.getValue();
			if (isBlocked(config, itemManager.getItemComposition(itemId).getName()))
			{
				continue;
			}

			int quantity;
			int[] pieces = ItemSets.piecesFor(itemId);
			if (pieces != null)
			{
				int unopened = heldByBase.getOrDefault(ItemVariationMapping.map(itemId), 0);
				int completeSets = Integer.MAX_VALUE;
				for (int piece : pieces)
				{
					completeSets = Math.min(completeSets, heldByBase.getOrDefault(ItemVariationMapping.map(piece), 0));
				}
				int heldSets = unopened + (completeSets == Integer.MAX_VALUE ? 0 : completeSets);
				quantity = Math.min(tracked.totalBought(), heldSets);
				if (quantity <= 0)
				{
					continue;
				}
				int fromUnopened = Math.min(quantity, unopened);
				consume(heldByBase, ItemVariationMapping.map(itemId), fromUnopened);
				int fromPieces = quantity - fromUnopened;
				for (int piece : pieces)
				{
					consume(heldByBase, ItemVariationMapping.map(piece), fromPieces);
				}
			}
			else
			{
				int base = ItemVariationMapping.map(itemId);
				// The list only shows what you currently hold (bank + inventory + gear); items you
				// have none of drop off until they come back.
				quantity = Math.min(tracked.totalBought(), heldByBase.getOrDefault(base, 0));
				if (quantity <= 0)
				{
					continue;
				}
				consume(heldByBase, base, quantity);
			}

			PortfolioRow row = buildRow(itemManager, config, itemId, tracked, quantity);
			rows.add(row);
			total += row.getProfitTotal();
		}

		sort(rows, config.sortOrder());
		lastTotalProfit = total;
		return rows;
	}

	/**
	 * A cheap snapshot of the held quantities of just the tracked items (and set pieces), keyed by
	 * variation base. Used to decide whether an inventory change actually affected the panel, so we
	 * can skip a full rebuild when it did not. Does no pricing or row building.
	 */
	public Map<Integer, Integer> trackedHeldSignature(Map<Integer, Integer> liveQuantities)
	{
		if (data.getItems().isEmpty())
		{
			return new HashMap<>();
		}

		Set<Integer> relevant = new HashSet<>();
		for (Integer id : data.getItems().keySet())
		{
			relevant.add(ItemVariationMapping.map(id));
			int[] pieces = ItemSets.piecesFor(id);
			if (pieces != null)
			{
				for (int piece : pieces)
				{
					relevant.add(ItemVariationMapping.map(piece));
				}
			}
		}

		Map<Integer, Integer> held = new HashMap<>();
		for (Map.Entry<Integer, Integer> e : data.getBank().entrySet())
		{
			addHeld(held, e.getKey(), e.getValue());
		}
		if (liveQuantities != null)
		{
			for (Map.Entry<Integer, Integer> e : liveQuantities.entrySet())
			{
				addHeld(held, e.getKey(), e.getValue());
			}
		}

		Map<Integer, Integer> signature = new HashMap<>();
		for (Integer base : relevant)
		{
			int quantity = held.getOrDefault(base, 0);
			if (quantity != 0)
			{
				signature.put(base, quantity);
			}
		}
		return signature;
	}

	/** The most recent buy date across a tracked item's lots (0 if none). */
	private static long newestBuy(TrackedItem tracked)
	{
		long newest = 0;
		for (Lot lot : tracked.getLots())
		{
			if (lot.getEpochMs() > newest)
			{
				newest = lot.getEpochMs();
			}
		}
		return newest;
	}

	/** Removes up to {@code amount} of a base from the held pool. */
	private static void consume(Map<Integer, Integer> heldByBase, int base, int amount)
	{
		if (amount <= 0)
		{
			return;
		}
		int remaining = heldByBase.getOrDefault(base, 0) - amount;
		if (remaining > 0)
		{
			heldByBase.put(base, remaining);
		}
		else
		{
			heldByBase.remove(base);
		}
	}

	/** Builds one display row for {@code quantity} held units of a tracked item, valued LIFO. */
	private PortfolioRow buildRow(ItemManager itemManager, BankStonksConfig config, int itemId, TrackedItem tracked, int quantity)
	{
		LotValuation valuation = tracked.value(quantity);
		long avg = valuation.getAveragePrice();
		int rawCurrent = itemManager.getItemPrice(itemId);
		long effectiveCurrent = config.applyGeTax() ? netAfterTax(rawCurrent) : rawCurrent;
		long profitEach = effectiveCurrent - avg;
		long profitTotal = profitEach * quantity;

		List<Lot> lots = new ArrayList<>(tracked.getLots());
		lots.sort(Comparator.comparingLong(Lot::getEpochMs).reversed());

		String name = itemManager.getItemComposition(itemId).getName();
		return new PortfolioRow(itemId, name, quantity, avg, rawCurrent, profitEach, profitTotal, valuation.getHeldSinceEpochMs(), lots, false);
	}

	/** Builds a pinned row for a locked item, valued at its locked lot's price, date and quantity. */
	private PortfolioRow buildLockedRow(ItemManager itemManager, BankStonksConfig config, int itemId, TrackedItem tracked)
	{
		Lot locked = tracked.getLockedLot();
		int quantity = locked.getQuantity();
		long avg = locked.unitPrice();
		int rawCurrent = itemManager.getItemPrice(itemId);
		long effectiveCurrent = config.applyGeTax() ? netAfterTax(rawCurrent) : rawCurrent;
		long profitEach = effectiveCurrent - avg;
		long profitTotal = profitEach * quantity;

		// A locked item shows no history dropdown (empty lots); the underlying lots are still kept
		// and updated, and are restored when it is unlocked.
		String name = itemManager.getItemComposition(itemId).getName();
		return new PortfolioRow(itemId, name, quantity, avg, rawCurrent, profitEach, profitTotal, locked.getEpochMs(), new ArrayList<>(), true);
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

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

/** Not thread-safe: use it from the client thread only. */
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

	/** Loads the account's saved portfolio, unless it is already the current account. */
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

	/** @param heldSinceEpochMs 0 or less means now */
	public void recordBuy(int itemId, int quantity, long spent, long heldSinceEpochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		long epochMs = heldSinceEpochMs > 0 ? heldSinceEpochMs : System.currentTimeMillis();
		data.getItems().computeIfAbsent(itemId, id -> new TrackedItem()).addBuy(quantity, spent, epochMs);
	}

	public void recordOfferFill(int itemId, int quantity, long spent, long epochMs, long offerId)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		data.getItems().computeIfAbsent(itemId, id -> new TrackedItem()).addOfferFill(offerId, quantity, spent, epochMs);
	}

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

	/** Deletes the item's buy history, so it only comes back with a new buy. */
	public void removeItem(int itemId)
	{
		data.getItems().remove(itemId);
	}

	/** Removing the last lot untracks the item. */
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
			if (lots.get(i).matches(quantity, spent, epochMs))
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

	public void lockItem(int itemId)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.lockToNewest();
		}
	}

	public void lockLot(int itemId, int quantity, long spent, long epochMs)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.lockToLot(quantity, spent, epochMs);
		}
	}

	public void unlockItem(int itemId)
	{
		TrackedItem tracked = data.getItems().get(itemId);
		if (tracked != null)
		{
			tracked.unlock();
		}
	}

	private static void addHeld(Map<Integer, Integer> heldByBase, int itemId, int quantity)
	{
		for (int base : tradeableBasesOf(itemId))
		{
			heldByBase.merge(base, quantity, Integer::sum);
		}
	}

	private static final int AVERNIC_DEFENDER = 22322;
	private static final int AVERNIC_DEFENDER_HILT = 22477;

	/**
	 * Untradeable assembled items that {@link ItemMapping} does not cover, mapped to the tradeable
	 * parts they are made from.
	 */
	private static final Map<Integer, int[]> ASSEMBLED_COMPONENTS = new HashMap<>();

	static
	{
		ASSEMBLED_COMPONENTS.put(AVERNIC_DEFENDER, new int[]{AVERNIC_DEFENDER_HILT});
	}

	private static Set<Integer> tradeableBasesOf(int itemId)
	{
		Set<Integer> bases = new HashSet<>();
		int[] components = ASSEMBLED_COMPONENTS.get(itemId);
		if (components != null)
		{
			for (int component : components)
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
				if (isAssembledPart(mapping))
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

	/** Excludes bulk charges (scales, darts): they get used up, so they must not count as held. */
	private static boolean isAssembledPart(ItemMapping mapping)
	{
		return mapping.getQuantity() == 1;
	}

	/** Matches across variants and assembled items; -1 when nothing tracked matches. */
	public int boughtIdForBankItem(int bankItemId)
	{
		if (data.getItems().containsKey(bankItemId))
		{
			return bankItemId;
		}
		Set<Integer> bases = tradeableBasesOf(bankItemId);
		for (Integer id : data.getItems().keySet())
		{
			if (bases.contains(ItemVariationMapping.map(id)))
			{
				return id;
			}
		}
		return -1;
	}

	public static boolean isBlocked(BankStonksConfig config, String itemName)
	{
		return BlockLists.matches(config.blockList(), itemName);
	}

	public void updateBank(Map<Integer, Integer> bankQuantities)
	{
		data.setBank(bankQuantities);
	}

	private volatile long lastTotalProfit;

	public long getLastTotalProfit()
	{
		return lastTotalProfit;
	}

	private static final double GE_TAX_RATE = 0.02;
	private static final long GE_TAX_CAP_PER_ITEM = 5_000_000L;
	private static final long GE_TAX_FREE_BELOW = 50;

	/** Buys are never taxed, so only the selling side of a profit goes through this. */
	public static long netAfterTax(long price)
	{
		if (price < GE_TAX_FREE_BELOW)
		{
			return price;
		}
		long tax = Math.min((long) Math.floor(price * GE_TAX_RATE), GE_TAX_CAP_PER_ITEM);
		return price - tax;
	}

	/**
	 * Values at most the amount bought, so items got outside the GE (drops etc.) never count as
	 * profit, and sold stock drops out. Held is the bank snapshot plus {@code liveQuantities}: an
	 * item is only ever in one of them, so nothing counts twice.
	 */
	public List<PortfolioRow> buildRows(ItemManager itemManager, BankStonksConfig config, Map<Integer, Integer> liveQuantities)
	{
		Map<Integer, Integer> heldByBase = heldQuantitiesByBase(liveQuantities);
		List<PortfolioRow> rows = lockedRows(itemManager, config);
		rows.addAll(heldRows(itemManager, config, heldByBase));
		sort(rows, config.sortOrder());
		lastTotalProfit = totalProfit(rows);
		return rows;
	}

	/** Cheap (no pricing), so it can be compared every tick to tell whether tracked holdings changed. */
	public Map<Integer, Integer> trackedHeldSignature(Map<Integer, Integer> liveQuantities)
	{
		if (data.getItems().isEmpty())
		{
			return new HashMap<>();
		}
		Map<Integer, Integer> held = heldQuantitiesByBase(liveQuantities);
		Map<Integer, Integer> signature = new HashMap<>();
		for (Integer base : trackedBasesWithSetPieces())
		{
			int quantity = held.getOrDefault(base, 0);
			if (quantity != 0)
			{
				signature.put(base, quantity);
			}
		}
		return signature;
	}

	private Map<Integer, Integer> heldQuantitiesByBase(Map<Integer, Integer> liveQuantities)
	{
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
		return heldByBase;
	}

	private Set<Integer> trackedBasesWithSetPieces()
	{
		Set<Integer> bases = new HashSet<>();
		for (Integer id : data.getItems().keySet())
		{
			bases.add(ItemVariationMapping.map(id));
			int[] pieces = ItemSets.piecesFor(id);
			if (pieces != null)
			{
				for (int piece : pieces)
				{
					bases.add(ItemVariationMapping.map(piece));
				}
			}
		}
		return bases;
	}

	private List<PortfolioRow> lockedRows(ItemManager itemManager, BankStonksConfig config)
	{
		List<PortfolioRow> rows = new ArrayList<>();
		for (Map.Entry<Integer, TrackedItem> entry : data.getItems().entrySet())
		{
			TrackedItem tracked = entry.getValue();
			if (!tracked.isLocked())
			{
				continue;
			}
			int itemId = entry.getKey();
			if (!isBlocked(config, itemManager.getItemComposition(itemId).getName()))
			{
				rows.add(buildLockedRow(itemManager, config, itemId, tracked));
			}
		}
		return rows;
	}

	private List<PortfolioRow> heldRows(ItemManager itemManager, BankStonksConfig config, Map<Integer, Integer> heldByBase)
	{
		List<PortfolioRow> rows = new ArrayList<>();
		for (Map.Entry<Integer, TrackedItem> entry : unlockedNewestBuyFirst())
		{
			int itemId = entry.getKey();
			TrackedItem tracked = entry.getValue();
			if (isBlocked(config, itemManager.getItemComposition(itemId).getName()))
			{
				continue;
			}
			int quantity = claimHeld(heldByBase, itemId, tracked.totalBought());
			if (quantity > 0)
			{
				rows.add(buildRow(itemManager, config, itemId, tracked, quantity));
			}
		}
		return rows;
	}

	/**
	 * Newest buy first, so when held items could belong to either a set or its loose pieces, the
	 * more recent purchase claims them, as with the LIFO cost basis.
	 */
	private List<Map.Entry<Integer, TrackedItem>> unlockedNewestBuyFirst()
	{
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
			int cmp = Long.compare(newestBuyEpochMs(b.getValue()), newestBuyEpochMs(a.getValue()));
			return cmp != 0 ? cmp : Integer.compare(a.getKey(), b.getKey());
		});
		return claims;
	}

	private static int claimHeld(Map<Integer, Integer> heldByBase, int itemId, int totalBought)
	{
		int[] pieces = ItemSets.piecesFor(itemId);
		if (pieces != null)
		{
			return claimSet(heldByBase, itemId, pieces, totalBought);
		}
		int base = ItemVariationMapping.map(itemId);
		int quantity = Math.min(totalBought, heldByBase.getOrDefault(base, 0));
		consume(heldByBase, base, quantity);
		return quantity;
	}

	private static int claimSet(Map<Integer, Integer> heldByBase, int setItemId, int[] pieces, int totalBought)
	{
		int setBase = ItemVariationMapping.map(setItemId);
		int unopened = heldByBase.getOrDefault(setBase, 0);
		int quantity = Math.min(totalBought, unopened + completeSets(heldByBase, pieces));
		if (quantity <= 0)
		{
			return quantity;
		}
		int fromUnopened = Math.min(quantity, unopened);
		consume(heldByBase, setBase, fromUnopened);
		int fromPieces = quantity - fromUnopened;
		for (int piece : pieces)
		{
			consume(heldByBase, ItemVariationMapping.map(piece), fromPieces);
		}
		return quantity;
	}

	private static int completeSets(Map<Integer, Integer> heldByBase, int[] pieces)
	{
		int completeSets = Integer.MAX_VALUE;
		for (int piece : pieces)
		{
			completeSets = Math.min(completeSets, heldByBase.getOrDefault(ItemVariationMapping.map(piece), 0));
		}
		return completeSets == Integer.MAX_VALUE ? 0 : completeSets;
	}

	private static long totalProfit(List<PortfolioRow> rows)
	{
		long total = 0;
		for (PortfolioRow row : rows)
		{
			total += row.getProfitTotal();
		}
		return total;
	}

	private static long newestBuyEpochMs(TrackedItem tracked)
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

	private PortfolioRow buildRow(ItemManager itemManager, BankStonksConfig config, int itemId, TrackedItem tracked, int quantity)
	{
		LotValuation valuation = tracked.value(quantity);
		long avg = valuation.getAveragePrice();
		long rawCurrent = itemManager.getItemPrice(itemId);
		long effectiveCurrent = config.applyGeTax() ? netAfterTax(rawCurrent) : rawCurrent;
		long profitEach = effectiveCurrent - avg;
		long profitTotal = profitEach * quantity;

		List<Lot> lots = new ArrayList<>(tracked.getLots());
		lots.sort(Comparator.comparingLong(Lot::getEpochMs).reversed());

		String name = itemManager.getItemComposition(itemId).getName();
		return new PortfolioRow(itemId, name, quantity, avg, rawCurrent, profitEach, profitTotal, valuation.getHeldSinceEpochMs(), lots, false);
	}

	private PortfolioRow buildLockedRow(ItemManager itemManager, BankStonksConfig config, int itemId, TrackedItem tracked)
	{
		Lot locked = tracked.getLockedLot();
		int quantity = locked.getQuantity();
		long avg = locked.unitPrice();
		long rawCurrent = itemManager.getItemPrice(itemId);
		long effectiveCurrent = config.applyGeTax() ? netAfterTax(rawCurrent) : rawCurrent;
		long profitEach = effectiveCurrent - avg;
		long profitTotal = profitEach * quantity;

		List<Lot> hiddenHistory = new ArrayList<>();
		String name = itemManager.getItemComposition(itemId).getName();
		return new PortfolioRow(itemId, name, quantity, avg, rawCurrent, profitEach, profitTotal, locked.getEpochMs(), hiddenHistory, true);
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
				comparator = Comparator.comparingLong((PortfolioRow r) -> r.getCurrentPrice() * r.getQuantity()).reversed();
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

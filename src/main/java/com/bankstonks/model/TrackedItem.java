package com.bankstonks.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The persisted cost-basis record for a single item id: the list of buy lots (GE fills and
 * manual entries).
 *
 * <p>The current holding is always valued at the <b>most recent</b> purchases (LIFO): to value
 * {@code n} held units we walk the lots newest-first and take {@code n} units, ignoring older
 * lots beyond that. This means selling then rebuying shows the latest buy price (not a blended
 * average), and items that leave and come back (bank/inventory/death/storage) keep their data
 * because lots are never deleted.</p>
 */
@Data
@NoArgsConstructor
public class TrackedItem
{
	/** Keep this many lots individually; older ones are merged so storage stays bounded. */
	private static final int MAX_LOTS = 40;

	private List<Lot> lots = new ArrayList<>();

	public void ensureInitialized()
	{
		if (lots == null)
		{
			lots = new ArrayList<>();
		}
	}

	/** Records a standalone buy (e.g. a manual entry) as a new lot. */
	public void addBuy(int quantity, long spent, long epochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		lots.add(new Lot(quantity, spent, epochMs, 0));
		compact();
	}

	/**
	 * Adds a GE fill to the lot for {@code offerId}, so partial fills of one offer accumulate
	 * into a single lot (showing the offer's overall average price). Creates the lot on the
	 * first fill.
	 */
	public void addOfferFill(long offerId, int quantity, long spent, long epochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		if (offerId != 0)
		{
			for (Lot lot : lots)
			{
				if (lot.getOfferId() == offerId)
				{
					lot.setQuantity(lot.getQuantity() + quantity);
					lot.setSpent(lot.getSpent() + spent);
					return;
				}
			}
		}
		lots.add(new Lot(quantity, spent, epochMs, offerId));
		compact();
	}

	/** Total quantity ever bought (sum of all lots). */
	public int totalBought()
	{
		int total = 0;
		for (Lot lot : lots)
		{
			total += lot.getQuantity();
		}
		return total;
	}

	/**
	 * Values the most recent {@code quantity} held units (LIFO): walks lots newest-first,
	 * taking units until {@code quantity} is covered. Returns the average price of those units
	 * and the most recent buy date among them (the "held since").
	 */
	public LotValuation value(int quantity)
	{
		if (quantity <= 0 || lots.isEmpty())
		{
			return new LotValuation(0, 0);
		}
		List<Lot> newestFirst = new ArrayList<>(lots);
		newestFirst.sort(Comparator.comparingLong(Lot::getEpochMs).reversed());

		long cost = 0;
		int taken = 0;
		long lastBuyEpochMs = 0;
		for (Lot lot : newestFirst)
		{
			if (taken >= quantity)
			{
				break;
			}
			if (lastBuyEpochMs == 0)
			{
				// "Held since" is the most recent buy that makes up the current holding.
				lastBuyEpochMs = lot.getEpochMs();
			}
			int take = Math.min(quantity - taken, lot.getQuantity());
			cost += lot.unitPrice() * take;
			taken += take;
		}
		long average = taken > 0 ? cost / taken : 0;
		return new LotValuation(average, lastBuyEpochMs);
	}

	/** Merges the oldest lots together until at most {@link #MAX_LOTS} remain. */
	private void compact()
	{
		if (lots.size() <= MAX_LOTS)
		{
			return;
		}
		lots.sort(Comparator.comparingLong(Lot::getEpochMs));
		while (lots.size() > MAX_LOTS)
		{
			Lot a = lots.remove(0);
			Lot b = lots.remove(0);
			// Merged history loses its offer identity (0) so it is never appended to again.
			lots.add(0, new Lot(a.getQuantity() + b.getQuantity(), a.getSpent() + b.getSpent(),
				Math.min(a.getEpochMs(), b.getEpochMs()), 0));
		}
	}
}

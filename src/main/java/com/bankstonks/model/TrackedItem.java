package com.bankstonks.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holdings are valued LIFO, against the newest buys, so selling and rebuying shows the new price
 * rather than a blended average. Lots stay when the item leaves your holdings, so it keeps its
 * history when it comes back.
 */
@Data
@NoArgsConstructor
public class TrackedItem
{
	/** Bounds the persisted JSON per item. */
	private static final int MAX_LOTS = 40;

	private List<Lot> lots = new ArrayList<>();

	/**
	 * A copy of the pinned buy, so it survives its lot being merged or removed. While set, the item
	 * is shown whatever you hold, valued at this lot.
	 */
	private Lot lockedLot;

	public void ensureInitialized()
	{
		if (lots == null)
		{
			lots = new ArrayList<>();
		}
	}

	public boolean isLocked()
	{
		return lockedLot != null;
	}

	public void lockToNewest()
	{
		Lot newest = null;
		for (Lot lot : lots)
		{
			if (newest == null || lot.getEpochMs() > newest.getEpochMs())
			{
				newest = lot;
			}
		}
		if (newest != null)
		{
			lockedLot = copy(newest);
		}
	}

	public void lockToLot(int quantity, long spent, long epochMs)
	{
		for (Lot lot : lots)
		{
			if (lot.matches(quantity, spent, epochMs))
			{
				lockedLot = copy(lot);
				return;
			}
		}
	}

	public void unlock()
	{
		lockedLot = null;
	}

	private static Lot copy(Lot lot)
	{
		return new Lot(lot.getQuantity(), lot.getSpent(), lot.getEpochMs(), lot.getOfferId());
	}

	public void addBuy(int quantity, long spent, long epochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		lots.add(new Lot(quantity, spent, epochMs, Lot.NO_OFFER));
		compact();
	}

	public void addOfferFill(long offerId, int quantity, long spent, long epochMs)
	{
		if (quantity <= 0 || spent <= 0)
		{
			return;
		}
		if (offerId != Lot.NO_OFFER)
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

	public int totalBought()
	{
		int total = 0;
		for (Lot lot : lots)
		{
			total += lot.getQuantity();
		}
		return total;
	}

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
		long heldSinceEpochMs = 0;
		for (Lot lot : newestFirst)
		{
			if (taken >= quantity)
			{
				break;
			}
			if (heldSinceEpochMs == 0)
			{
				heldSinceEpochMs = lot.getEpochMs();
			}
			int take = Math.min(quantity - taken, lot.getQuantity());
			cost += lot.unitPrice() * take;
			taken += take;
		}
		long average = taken > 0 ? cost / taken : 0;
		return new LotValuation(average, heldSinceEpochMs);
	}

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
			lots.add(0, new Lot(a.getQuantity() + b.getQuantity(), a.getSpent() + b.getSpent(),
				Math.min(a.getEpochMs(), b.getEpochMs()), Lot.NO_OFFER));
		}
	}
}

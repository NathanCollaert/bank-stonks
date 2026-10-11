package com.bankstonks;

import com.bankstonks.model.Lot;
import com.bankstonks.model.SlotState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * Offer events carry the offer's running totals, so each fill is the difference from the slot's
 * last-seen state. {@code getSpent()} is what was actually paid across fills at different prices,
 * so spend is accumulated rather than derived from a price.
 */
@Slf4j
@Singleton
public class GrandExchangeBuyTracker
{
	private final PortfolioManager manager;

	/**
	 * True while the client replays the existing offers on login/hop. An offer with no baseline then
	 * was recorded in an earlier session, so it becomes the baseline instead of being recorded again.
	 */
	private volatile boolean loginSync;

	@Inject
	public GrandExchangeBuyTracker(PortfolioManager manager)
	{
		this.manager = manager;
	}

	public void setLoginSync(boolean syncing)
	{
		this.loginSync = syncing;
	}

	/** @return whether a buy fill was recorded */
	public boolean handle(GrandExchangeOfferChanged event)
	{
		if (!manager.hasAccount())
		{
			return false;
		}

		final int slot = event.getSlot();
		final GrandExchangeOffer offer = event.getOffer();
		final GrandExchangeOfferState state = offer.getState();

		if (!isBuy(state))
		{
			forgetSlot(slot);
			return false;
		}

		final int itemId = offer.getItemId();
		final int quantitySold = offer.getQuantitySold();
		final long spent = offer.getSpent();

		SlotState prev = manager.getSlot(slot);
		boolean continuing = prev != null && prev.getItemId() == itemId && quantitySold >= prev.getQuantitySold();
		int prevQty = continuing ? prev.getQuantitySold() : 0;
		long prevSpent = continuing ? prev.getSpent() : 0;

		int deltaQty = quantitySold - prevQty;
		long deltaSpent = spent - prevSpent;

		long offerId = continuing ? prev.getLotId() : Lot.NO_OFFER;

		boolean recorded = false;
		if (deltaQty > 0 && deltaSpent > 0)
		{
			boolean adoptAsBaseline = loginSync && !continuing;
			if (adoptAsBaseline)
			{
				log.debug("login sync: adopting baseline item={} qty={} spent={} (not recorded)", itemId, quantitySold, spent);
			}
			else
			{
				if (offerId == Lot.NO_OFFER)
				{
					offerId = manager.nextOfferId();
				}
				manager.recordOfferFill(itemId, deltaQty, deltaSpent, System.currentTimeMillis(), offerId);
				recorded = true;
				log.debug("recorded buy fill: item={} qty={} spent={} offer={}", itemId, deltaQty, deltaSpent, offerId);
			}
		}

		manager.setSlot(slot, new SlotState(itemId, quantitySold, spent, offerId));
		manager.save();
		return recorded;
	}

	/**
	 * Not during the login/hop replay: the client empties every slot before replaying the real
	 * offers, and clearing then would lose the baseline and split the offer's fills into a new lot.
	 */
	private void forgetSlot(int slot)
	{
		if (loginSync)
		{
			return;
		}
		manager.clearSlot(slot);
		manager.save();
	}

	private static boolean isBuy(GrandExchangeOfferState state)
	{
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}
}

package com.geportfolio;

import com.geportfolio.model.SlotState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * Watches Grand Exchange offers and records the cost basis of every <b>buy</b> that fills.
 *
 * <p>A single buy is reported incrementally across many {@code GrandExchangeOfferChanged}
 * events. We diff each event against the last-seen state of that slot to extract only the
 * newly-filled quantity/spend. Because the previous state is persisted, offers that the
 * client re-broadcasts on login produce a zero delta and are not double-counted.</p>
 *
 * <p>{@code offer.getSpent() / offer.getQuantitySold()} gives the true average price paid,
 * including partial fills at different prices, so we accumulate spend directly.</p>
 */
@Slf4j
@Singleton
public class GrandExchangeBuyTracker
{
	private final PortfolioManager manager;

	@Inject
	public GrandExchangeBuyTracker(PortfolioManager manager)
	{
		this.manager = manager;
	}

	/** @return true if a buy was recorded (so the caller can refresh the UI). */
	public boolean handle(GrandExchangeOfferChanged event)
	{
		if (!manager.hasAccount())
		{
			return false;
		}

		final int slot = event.getSlot();
		final GrandExchangeOffer offer = event.getOffer();
		final GrandExchangeOfferState state = offer.getState();

		if (state == GrandExchangeOfferState.EMPTY)
		{
			manager.clearSlot(slot);
			manager.save();
			return false;
		}

		if (!isBuy(state))
		{
			// Sell offers do not affect cost basis; forget the slot so a later buy in the
			// same slot starts from a clean delta.
			manager.clearSlot(slot);
			manager.save();
			return false;
		}

		final int itemId = offer.getItemId();
		final int quantitySold = offer.getQuantitySold();
		final long spent = offer.getSpent();

		int prevQty = 0;
		long prevSpent = 0;
		SlotState prev = manager.getSlot(slot);
		if (prev != null && prev.getItemId() == itemId && quantitySold >= prev.getQuantitySold())
		{
			prevQty = prev.getQuantitySold();
			prevSpent = prev.getSpent();
		}

		int deltaQty = quantitySold - prevQty;
		long deltaSpent = spent - prevSpent;

		boolean recorded = false;
		if (deltaQty > 0 && deltaSpent > 0)
		{
			manager.recordBuy(itemId, deltaQty, deltaSpent);
			recorded = true;
			log.debug("recorded buy: item={} qty={} spent={}", itemId, deltaQty, deltaSpent);
		}

		manager.setSlot(slot, new SlotState(itemId, quantitySold, spent));
		manager.save();
		return recorded;
	}

	private static boolean isBuy(GrandExchangeOfferState state)
	{
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}
}

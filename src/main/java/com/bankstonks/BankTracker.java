package com.bankstonks;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;

/**
 * The game only sends the bank's contents while it is open, so the last snapshot is kept for when
 * it is closed.
 */
@Slf4j
@Singleton
public class BankTracker
{
	private final PortfolioManager manager;

	@Inject
	public BankTracker(PortfolioManager manager)
	{
		this.manager = manager;
	}

	/** @return whether the bank snapshot was updated */
	public boolean handle(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK || !manager.hasAccount())
		{
			return false;
		}

		ItemContainer container = event.getItemContainer();
		if (container == null)
		{
			return false;
		}

		Map<Integer, Integer> quantities = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
		}

		manager.updateBank(quantities);
		manager.save();
		return true;
	}
}

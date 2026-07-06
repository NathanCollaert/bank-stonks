package com.bankstonks.ui;

/**
 * Callbacks the panel uses to ask the plugin to mutate portfolio state. The plugin performs
 * the work on the client thread and refreshes the panel afterwards.
 */
public interface PortfolioActions
{
	/**
	 * Manually records a buy for the item best matching {@code query}.
	 *
	 * @param query             item name to resolve (fuzzy)
	 * @param quantity          quantity acquired
	 * @param priceEach         price paid per item
	 * @param heldSinceEpochMs  when it was bought (epoch millis), or 0 to use now
	 */
	void addManual(String query, int quantity, long priceEach, long heldSinceEpochMs);

	/**
	 * Deletes an item's tracked data. It disappears from the list and reappears (fresh) only
	 * if bought again. This is not the block list — it is a one-time removal.
	 */
	void untrackItem(int itemId);

	/** Removes a single buy lot from an item's history (matched by quantity, spent and date). */
	void untrackLot(int itemId, int quantity, long spent, long epochMs);

	/**
	 * Adds an item to the block list so it is never shown (it is still tracked in the
	 * background). Reversible only by editing the block list in settings.
	 */
	void blockItem(String itemName);
}

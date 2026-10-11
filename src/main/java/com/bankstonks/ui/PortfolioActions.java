package com.bankstonks.ui;

import com.bankstonks.SortOrder;

/** The plugin does the work on the client thread and refreshes the panel afterwards. */
public interface PortfolioActions
{
	/**
	 * Records a buy for the item whose name best matches {@code query}.
	 *
	 * @param heldSinceEpochMs when it was bought, or 0 for now
	 */
	void addManual(String query, int quantity, long priceEach, long heldSinceEpochMs);

	/** Deletes the item's whole history; it reappears, fresh, only if bought again. */
	void untrackItem(int itemId);

	void untrackLot(int itemId, int quantity, long spent, long epochMs);

	/** Hides the item for good but keeps tracking it. Undone only by editing the block list in settings. */
	void blockItem(String itemName);

	/** Locks the item to its most recent buy: it stays listed and is valued at that buy. */
	void lockItem(int itemId);

	void lockLot(int itemId, int quantity, long spent, long epochMs);

	void unlockItem(int itemId);

	/** Changes the same setting as the config panel. */
	void setSortOrder(SortOrder order);
}

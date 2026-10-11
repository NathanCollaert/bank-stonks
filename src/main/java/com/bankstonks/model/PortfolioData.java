package com.bankstonks.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * Saved as JSON with Gson: the field names here and in the classes it holds are the stored keys,
 * so renaming one loses that data for every existing user.
 */
@Data
public class PortfolioData
{
	private Map<Integer, TrackedItem> items = new HashMap<>();

	/** Item id to quantity, as of the last time the bank was open. */
	private Map<Integer, Integer> bank = new HashMap<>();

	private Map<Integer, SlotState> slots = new HashMap<>();

	/** The last offer id handed out, not the next one. */
	private long nextLotId;

	public void ensureInitialized()
	{
		if (items == null)
		{
			items = new HashMap<>();
		}
		for (TrackedItem item : items.values())
		{
			if (item != null)
			{
				item.ensureInitialized();
			}
		}
		if (bank == null)
		{
			bank = new HashMap<>();
		}
		if (slots == null)
		{
			slots = new HashMap<>();
		}
	}
}

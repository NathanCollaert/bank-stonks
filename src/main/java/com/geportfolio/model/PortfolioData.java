package com.geportfolio.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * The full persisted state for one account. Serialized to the RuneLite config as JSON.
 */
@Data
public class PortfolioData
{
	/** Cost-basis records keyed by item id. */
	private Map<Integer, TrackedItem> items = new HashMap<>();

	/** Last-seen bank quantity keyed by item id. */
	private Map<Integer, Integer> bank = new HashMap<>();

	/** Last-seen GE offer state keyed by slot index (0-7). */
	private Map<Integer, SlotState> slots = new HashMap<>();

	/** Guards against null maps after deserializing older/partial JSON. */
	public void ensureInitialized()
	{
		if (items == null)
		{
			items = new HashMap<>();
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

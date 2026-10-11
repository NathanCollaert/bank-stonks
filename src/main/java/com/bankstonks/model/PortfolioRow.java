package com.bankstonks.model;

import java.util.List;
import lombok.Value;

@Value
public class PortfolioRow
{
	int itemId;
	String name;

	/** Held units capped at the amount bought; the locked lot's quantity for a locked row. */
	int quantity;

	long averageBuyPrice;

	/** Before GE tax, even when the profit fields are after it. */
	long currentPrice;

	long profitEach;
	long profitTotal;

	/** "Held since": despite the name, the newest buy among the valued units; 0 if unknown. */
	long firstBoughtEpochMs;

	/** Newest first; empty for a locked row. */
	List<Lot> lots;

	boolean locked;

	public double profitPercent()
	{
		long invested = averageBuyPrice * quantity;
		return invested <= 0 ? 0 : (profitTotal * 100.0) / invested;
	}
}

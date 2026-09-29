package com.bankstonks.model;

import java.util.List;
import lombok.Value;

/**
 * A single computed row shown in the panel. Immutable snapshot for the UI thread.
 */
@Value
public class PortfolioRow
{
	int itemId;
	String name;

	/** min(totalBought, current bank quantity) — the amount we value. */
	int quantity;

	/** Average price paid for the valued units (most-recent lots). */
	long averageBuyPrice;

	/** Current wiki (GE) price. */
	long currentPrice;

	/** Profit/loss per single item: currentPrice - averageBuyPrice. */
	long profitEach;

	/** Total profit/loss: profitEach * quantity. */
	long profitTotal;

	/** Epoch millis of the first recorded buy ("held since"); 0 if unknown. */
	long firstBoughtEpochMs;

	/** The individual buy lots (newest first), for the expandable history. */
	List<Lot> lots;

	/** Whether this item is locked (pinned to one buy, shown without a history dropdown). */
	boolean locked;

	/** Profit as a percentage of the amount invested, or 0 if nothing invested. */
	public double profitPercent()
	{
		long invested = averageBuyPrice * quantity;
		return invested <= 0 ? 0 : (profitTotal * 100.0) / invested;
	}
}

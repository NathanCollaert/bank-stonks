package com.geportfolio.model;

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

	/** Running average buy price. */
	long averageBuyPrice;

	/** Current wiki (GE) price. */
	int currentPrice;

	/** Profit/loss per single item: currentPrice - averageBuyPrice. */
	long profitEach;

	/** Total profit/loss: profitEach * quantity. */
	long profitTotal;

	/** Profit as a percentage of the amount invested, or 0 if nothing invested. */
	public double profitPercent()
	{
		long invested = averageBuyPrice * quantity;
		return invested <= 0 ? 0 : (profitTotal * 100.0) / invested;
	}
}

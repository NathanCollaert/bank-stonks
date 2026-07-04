package com.stockpile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The persisted cost-basis record for a single item id.
 *
 * <p>We only accumulate what we have actually bought on the Grand Exchange (plus manual
 * entries). The average buy price is a running average across every buy ever recorded:
 * {@code totalSpent / totalBought}. Held quantity is <b>not</b> stored here — it is read
 * live from the bank so that drops, alching, trading and selling are all reflected
 * automatically.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedItem
{
	/** Total quantity ever bought (GE fills + manual adds). */
	private int totalBought;

	/** Total gp ever spent buying this item (GE fills + manual adds). */
	private long totalSpent;

	/** Epoch millis of the first recorded buy ("held since"); 0 if unknown. */
	private long firstBoughtEpochMs;

	/** Running average buy price, or 0 if nothing has been bought yet. */
	public long averageBuyPrice()
	{
		return totalBought <= 0 ? 0 : totalSpent / totalBought;
	}

	/** Records an additional buy, updating the running totals. */
	public void addBuy(int quantity, long spent)
	{
		totalBought += quantity;
		totalSpent += spent;
	}
}

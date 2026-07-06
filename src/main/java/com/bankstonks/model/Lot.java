package com.bankstonks.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single recorded purchase: a quantity bought for a total price at a point in time.
 * Lots are never mutated after creation; valuation walks them newest-first (LIFO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lot
{
	private int quantity;
	private long spent;
	private long epochMs;

	/** The GE offer this lot came from, so partial fills of one offer accumulate into it. 0 for manual entries. */
	private long offerId;

	public long unitPrice()
	{
		return quantity <= 0 ? 0 : spent / quantity;
	}
}

package com.stockpile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The last-seen state of a single Grand Exchange slot, persisted per account.
 *
 * <p>Kept so that we can compute the delta between successive {@code GrandExchangeOfferChanged}
 * events (a single buy fills in increments) and, crucially, so we do not double-count offers
 * that the client re-broadcasts on login.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotState
{
	private int itemId;
	private int quantitySold;
	private long spent;
}

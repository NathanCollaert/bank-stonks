package com.bankstonks.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted so that the offers the client re-sends on login diff to nothing instead of being
 * counted again.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotState
{
	private int itemId;
	private int quantitySold;
	private long spent;

	/** The {@code offerId} of the {@link Lot} this offer's fills accumulate into. */
	private long lotId;
}

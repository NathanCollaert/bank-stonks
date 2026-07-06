package com.bankstonks.model;

import lombok.Value;

/**
 * The result of valuing a held quantity against a {@link TrackedItem}'s lots: the average
 * price paid for those units and the most recent buy date among them ("held since").
 */
@Value
public class LotValuation
{
	long averagePrice;
	long heldSinceEpochMs;
}

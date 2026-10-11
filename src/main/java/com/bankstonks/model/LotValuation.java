package com.bankstonks.model;

import lombok.Value;

@Value
public class LotValuation
{
	long averagePrice;

	/** The newest buy among the valued units, not the oldest. */
	long heldSinceEpochMs;
}

package com.bankstonks.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lot
{
	public static final long NO_OFFER = 0;

	private int quantity;
	private long spent;
	private long epochMs;
	private long offerId;

	public long unitPrice()
	{
		return quantity <= 0 ? 0 : spent / quantity;
	}

	public boolean matches(int quantity, long spent, long epochMs)
	{
		return this.quantity == quantity && this.spent == spent && this.epochMs == epochMs;
	}
}

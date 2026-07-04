package com.stockpile;

/**
 * How the sidebar list is ordered.
 */
public enum SortOrder
{
	BIGGEST_MOVERS("Biggest movers"),
	MOST_PROFIT("Most profit"),
	MOST_LOSS("Most loss"),
	HIGHEST_VALUE("Highest value"),
	NAME("Name (A-Z)");

	private final String label;

	SortOrder(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

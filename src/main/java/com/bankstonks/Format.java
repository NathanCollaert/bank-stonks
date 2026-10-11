package com.bankstonks;

public final class Format
{
	private Format()
	{
	}

	/** Signed and compact, e.g. "+1.23M", "-450.0K". */
	public static String gp(long value)
	{
		long abs = Math.abs(value);
		String sign = value > 0 ? "+" : (value < 0 ? "-" : "");
		if (abs >= 1_000_000_000L)
		{
			return sign + String.format("%.2fB", abs / 1_000_000_000.0);
		}
		if (abs >= 1_000_000L)
		{
			return sign + String.format("%.2fM", abs / 1_000_000.0);
		}
		if (abs >= 1_000L)
		{
			return sign + String.format("%.1fK", abs / 1_000.0);
		}
		return sign + abs;
	}

	public static String exact(long value)
	{
		return String.format("%,d", value);
	}

	/** Unsigned and compact, e.g. "1.23M", "450.0K". */
	public static String plain(long value)
	{
		if (value >= 1_000_000_000L)
		{
			return String.format("%.2fB", value / 1_000_000_000.0);
		}
		if (value >= 1_000_000L)
		{
			return String.format("%.2fM", value / 1_000_000.0);
		}
		if (value >= 1_000L)
		{
			return String.format("%.1fK", value / 1_000.0);
		}
		return Long.toString(value);
	}

	/** Empty when the time is unknown (0 or less). */
	public static String age(long sinceEpochMs)
	{
		if (sinceEpochMs <= 0)
		{
			return "";
		}
		long days = (System.currentTimeMillis() - sinceEpochMs) / 86_400_000L;
		if (days <= 0)
		{
			return "today";
		}
		if (days < 30)
		{
			return days + "d";
		}
		long months = days / 30;
		if (months < 12)
		{
			return months + "mo";
		}
		long years = months / 12;
		long remMonths = months % 12;
		return remMonths > 0 ? years + "y " + remMonths + "mo" : years + "y";
	}
}

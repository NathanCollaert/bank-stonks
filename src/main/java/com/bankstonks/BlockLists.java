package com.bankstonks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers for the comma-separated item-name list stored in config (the "Block list").
 *
 * <p>Matching is case-insensitive. An entry may contain {@code *} as a wildcard matching any
 * run of characters, so {@code prayer potion*} matches every dose ("Prayer potion(1)"…), and
 * {@code *(unf)} matches every unfinished potion.</p>
 *
 * <p>Patterns are compiled once and cached per CSV value, since {@link #matches} is called
 * frequently (per item, and per bank item each render frame).</p>
 */
final class BlockLists
{
	private static volatile String cachedCsv;
	private static volatile List<Pattern> cachedPatterns = Collections.emptyList();

	private BlockLists()
	{
	}

	/** Splits a CSV config value into trimmed, non-empty entries (original case preserved). */
	static List<String> names(String csv)
	{
		List<String> out = new ArrayList<>();
		if (csv == null)
		{
			return out;
		}
		for (String part : csv.split(","))
		{
			String trimmed = part.trim();
			if (!trimmed.isEmpty())
			{
				out.add(trimmed);
			}
		}
		return out;
	}

	/** Whether the item name matches any entry in the list (supporting {@code *} wildcards). */
	static boolean matches(String csv, String itemName)
	{
		if (itemName == null)
		{
			return false;
		}
		List<Pattern> patterns = compiled(csv);
		if (patterns.isEmpty())
		{
			return false;
		}
		String lower = itemName.toLowerCase(Locale.ROOT);
		for (Pattern pattern : patterns)
		{
			if (pattern.matcher(lower).find())
			{
				return true;
			}
		}
		return false;
	}

	private static List<Pattern> compiled(String csv)
	{
		String key = csv == null ? "" : csv;
		if (key.equals(cachedCsv))
		{
			return cachedPatterns;
		}
		List<Pattern> patterns = new ArrayList<>();
		for (String entry : names(csv))
		{
			patterns.add(toPattern(entry.toLowerCase(Locale.ROOT)));
		}
		cachedPatterns = patterns;
		cachedCsv = key;
		return patterns;
	}

	private static Pattern toPattern(String entry)
	{
		// Entries with a wildcard match as "contains" (unanchored), so "* potion" also
		// matches "Divine super combat potion(4)". Entries without a wildcard are anchored,
		// so they only match the exact item name.
		boolean wildcard = entry.indexOf('*') >= 0;
		StringBuilder regex = new StringBuilder();
		if (!wildcard)
		{
			regex.append('^');
		}
		String[] parts = entry.split("\\*", -1);
		for (int i = 0; i < parts.length; i++)
		{
			if (i > 0)
			{
				regex.append(".*");
			}
			if (!parts[i].isEmpty())
			{
				regex.append(Pattern.quote(parts[i]));
			}
		}
		if (!wildcard)
		{
			regex.append('$');
		}
		return Pattern.compile(regex.toString());
	}

	/** Returns the CSV with {@code name} appended, or unchanged if already present. */
	static String add(String csv, String name)
	{
		List<String> names = names(csv);
		for (String existing : names)
		{
			if (existing.equalsIgnoreCase(name))
			{
				return csv == null ? "" : csv;
			}
		}
		names.add(name);
		return String.join(", ", names);
	}

	/** Returns the CSV with {@code name} removed (exact, case-insensitive). */
	static String remove(String csv, String name)
	{
		List<String> names = names(csv);
		names.removeIf(n -> n.equalsIgnoreCase(name));
		return String.join(", ", names);
	}
}

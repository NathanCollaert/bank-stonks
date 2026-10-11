package com.bankstonks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class BlockLists
{
	// Compiled once per CSV value: matches() runs for bank items on every frame.
	private static volatile String cachedCsv;
	private static volatile List<Pattern> cachedPatterns = Collections.emptyList();

	private BlockLists()
	{
	}

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

	/** A wildcard entry matches anywhere in the name, so "* potion" also catches "Prayer potion(4)". */
	private static Pattern toPattern(String entry)
	{
		boolean exactNameOnly = entry.indexOf('*') < 0;
		StringBuilder regex = new StringBuilder();
		if (exactNameOnly)
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
		if (exactNameOnly)
		{
			regex.append('$');
		}
		return Pattern.compile(regex.toString());
	}

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

	static String remove(String csv, String name)
	{
		List<String> names = names(csv);
		names.removeIf(n -> n.equalsIgnoreCase(name));
		return String.join(", ", names);
	}
}

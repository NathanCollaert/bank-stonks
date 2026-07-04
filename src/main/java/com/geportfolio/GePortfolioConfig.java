package com.geportfolio;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GePortfolioConfig.GROUP)
public interface GePortfolioConfig extends Config
{
	String GROUP = "geportfolio";

	@ConfigItem(
		keyName = "hideEmpty",
		name = "Hide items not held",
		description = "Hide tracked items you no longer have any of in your bank.",
		position = 1
	)
	default boolean hideEmpty()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPercent",
		name = "Show profit %",
		description = "Show the profit/loss as a percentage of the amount invested.",
		position = 2
	)
	default boolean showPercent()
	{
		return true;
	}
}

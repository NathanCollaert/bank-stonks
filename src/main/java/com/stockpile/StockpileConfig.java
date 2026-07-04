package com.stockpile;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(StockpileConfig.GROUP)
public interface StockpileConfig extends Config
{
	String GROUP = "stockpile";
	String KEY_BLOCK_LIST = "blockList";

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

	@ConfigItem(
		keyName = "sortOrder",
		name = "Sort by",
		description = "How to order the items in the panel.",
		position = 3
	)
	default SortOrder sortOrder()
	{
		return SortOrder.BIGGEST_MOVERS;
	}

	@ConfigItem(
		keyName = "bankOverlay",
		name = "Show P/L on bank items",
		description = "Draw each tracked item's profit/loss directly on its slot in the bank.",
		position = 4
	)
	default boolean bankOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankTotalOverlay",
		name = "Show total on bank",
		description = "Draw the total portfolio profit/loss as an overlay while the bank is open.",
		position = 5
	)
	default boolean bankTotalOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "applyGeTax",
		name = "Subtract GE tax",
		description = "Value holdings at what you would net after the 2% GE sell tax (capped at 5M per item) instead of the raw wiki price.",
		position = 6
	)
	default boolean applyGeTax()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "profitColor",
		name = "Profit colour",
		description = "Colour used for a gain.",
		position = 7
	)
	default Color profitColor()
	{
		return new Color(76, 175, 80);
	}

	@Alpha
	@ConfigItem(
		keyName = "lossColor",
		name = "Loss colour",
		description = "Colour used for a loss.",
		position = 8
	)
	default Color lossColor()
	{
		return new Color(244, 67, 54);
	}

	@ConfigItem(
		keyName = KEY_BLOCK_LIST,
		name = "Block list",
		description = "Comma-separated item names that are never shown in the list (they are still tracked in the background). Use * as a wildcard, e.g. 'prayer potion*' matches all doses.",
		position = 9
	)
	default String blockList()
	{
		return "";
	}
}

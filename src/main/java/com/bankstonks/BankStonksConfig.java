package com.bankstonks;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(BankStonksConfig.GROUP)
public interface BankStonksConfig extends Config
{
	String GROUP = "bankstonks";
	String KEY_BLOCK_LIST = "blockList";

	@ConfigSection(
		name = "Display",
		description = "How the sidebar list is shown.",
		position = 0
	)
	String SECTION_DISPLAY = "display";

	@ConfigSection(
		name = "Bank overlay",
		description = "Profit/loss drawn on your bank slots.",
		position = 1
	)
	String SECTION_OVERLAY = "overlay";

	@ConfigSection(
		name = "Colours",
		description = "Colours for gains and losses.",
		position = 2
	)
	String SECTION_COLOURS = "colours";

	@ConfigSection(
		name = "Block list",
		description = "Items to hide from the list.",
		position = 3
	)
	String SECTION_BLOCK_LIST = "blockListSection";

	@ConfigItem(
		keyName = "showPercent",
		name = "Show profit %",
		description = "Show the profit/loss as a percentage of the amount invested.",
		section = SECTION_DISPLAY,
		position = 1
	)
	default boolean showPercent()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sortOrder",
		name = "Sort by",
		description = "How to order the items in the panel.",
		section = SECTION_DISPLAY,
		position = 2
	)
	default SortOrder sortOrder()
	{
		return SortOrder.BIGGEST_MOVERS;
	}

	@ConfigItem(
		keyName = "applyGeTax",
		name = "Subtract GE tax",
		description = "Value holdings at what you would net after the 2% GE sell tax (capped at 5M per item) instead of the raw wiki price.",
		section = SECTION_DISPLAY,
		position = 3
	)
	default boolean applyGeTax()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bankOverlay",
		name = "Show bank overlay",
		description = "Draw each tracked item's profit/loss directly on its slot in the bank.",
		section = SECTION_OVERLAY,
		position = 1
	)
	default boolean bankOverlay()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "profitColor",
		name = "Profit colour",
		description = "Colour used for a gain.",
		section = SECTION_COLOURS,
		position = 1
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
		section = SECTION_COLOURS,
		position = 2
	)
	default Color lossColor()
	{
		return new Color(244, 67, 54);
	}

	@ConfigItem(
		keyName = KEY_BLOCK_LIST,
		name = "Block list",
		description = "Comma-separated item names that are never shown in the list (they are still tracked in the background). A name with no * must match exactly; a name with a * matches any item containing that text, e.g. '*potion*' blocks all potions and 'prayer potion*' blocks all doses.",
		section = SECTION_BLOCK_LIST,
		position = 1
	)
	default String blockList()
	{
		return "";
	}
}

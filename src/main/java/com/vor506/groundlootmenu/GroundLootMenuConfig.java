package com.vor506.groundlootmenu;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("groundlootmenu")
public interface GroundLootMenuConfig extends Config
{
	@ConfigItem(
		keyName = "hideLowValue",
		name = "Hide low-value items",
		description = "Hide ground-item stacks below the minimum total GE value.",
		position = 0
	)
	default boolean hideLowValue()
	{
		return true;
	}

	@Range(min = 0, max = 2147483647)
	@ConfigItem(
		keyName = "minimumTotalGeValue",
		name = "Minimum total GE value",
		description = "Minimum GE value of the entire stack, in GP.",
		position = 1
	)
	default int minimumTotalGeValue()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "hideUnpriced",
		name = "Hide unpriced items",
		description = "Hide items with no positive GE price.",
		position = 2
	)
	default boolean hideUnpriced()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ignoreList",
		name = "Ignore list",
		description = "Comma-separated item names to hide, unless listed as valuable.",
		position = 3
	)
	default String ignoreList()
	{
		return "";
	}

	@ConfigItem(
		keyName = "valuableItems",
		name = "Always show items",
		description = "Comma-separated item names that bypass the ignore, minimum-value, and unpriced filters.",
		position = 5
	)
	default String valuableItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sortOrder",
		name = "Sort order",
		description = "Choose value, quantity, or name.",
		position = 6
	)
	default SortOrder sortOrder()
	{
		return SortOrder.VALUE;
	}

	enum SortOrder
	{
		VALUE,
		QUANTITY,
		NAME
	}

	@ConfigItem(
		keyName = "showQuantity",
		name = "Show quantity",
		description = "Display stack quantity on each item icon.",
		position = 7
	)
	default boolean showQuantity()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoCollapse",
		name = "Auto-collapse when empty",
		description = "Collapse the panel when no items pass the filters.",
		position = 8
	)
	default boolean autoCollapse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoExpand",
		name = "Open panel when loot appears",
		description = "Expand the panel when eligible loot appears. Turn this off to leave the panel closed after you collapse it.",
		position = 9
	)
	default boolean autoExpand()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGeTotal",
		name = "Show GE total",
		description = "Display the total Grand Exchange value.",
		position = 10
	)
	default boolean showGeTotal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHighAlchTotal",
		name = "Show high-alch total",
		description = "Display the total high-alchemy value.",
		position = 11
	)
	default boolean showHighAlchTotal()
	{
		return true;
	}

	@Range(min = 0, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "highlightSeconds",
		name = "Clear highlight after",
		description = "Seconds before a selected tile is unmarked. Set to 0 to keep the mark until you click that item again.",
		position = 12
	)
	default int highlightSeconds()
	{
		return 0;
	}
}

package com.vor506.groundlootmenu;

import net.runelite.api.coords.WorldPoint;

final class LootEntry
{
	private final int itemId;
	private final int quantity;
	private final String name;
	private final WorldPoint location;
	private final int sceneX;
	private final int sceneY;
	private final long totalValue;
	private final long highAlchValue;
	private final boolean stackable;
	private final String quantityText;
	private final String valueText;

	LootEntry(
		int itemId,
		int quantity,
		String name,
		WorldPoint location,
		int sceneX,
		int sceneY,
		long totalValue,
		long highAlchValue,
		boolean stackable)
	{
		this.itemId = itemId;
		this.quantity = quantity;
		this.name = name;
		this.location = location;
		this.sceneX = sceneX;
		this.sceneY = sceneY;
		this.totalValue = totalValue;
		this.highAlchValue = highAlchValue;
		this.stackable = stackable;
		this.quantityText = formatAmount(quantity);
		this.valueText = formatAmount(totalValue);
	}

	int getItemId()
	{
		return itemId;
	}

	int getQuantity()
	{
		return quantity;
	}

	String getName()
	{
		return name;
	}

	WorldPoint getLocation()
	{
		return location;
	}

	int getSceneX()
	{
		return sceneX;
	}

	int getSceneY()
	{
		return sceneY;
	}

	long getTotalValue()
	{
		return totalValue;
	}

	long getHighAlchValue()
	{
		return highAlchValue;
	}

	boolean isStackable()
	{
		return stackable;
	}

	String getQuantityText()
	{
		return quantityText;
	}

	String getValueText()
	{
		return valueText;
	}

	boolean samePileItem(LootEntry other)
	{
		return other != null
			&& itemId == other.itemId
			&& sceneX == other.sceneX
			&& sceneY == other.sceneY
			&& location.equals(other.location);
	}

	static String formatAmount(long amount)
	{
		if (amount >= 1_000_000_000L)
		{
			return formatTenths(amount, 1_000_000_000L, "b");
		}

		if (amount >= 1_000_000L)
		{
			return formatTenths(amount, 1_000_000L, "m");
		}

		if (amount >= 10_000L)
		{
			return ((amount + 500L) / 1_000L) + "k";
		}

		return Long.toString(amount);
	}

	private static String formatTenths(long amount, long divisor, String suffix)
	{
		long step = divisor / 10L;
		long tenths = amount / step;
		if (amount % step >= step / 2L)
		{
			tenths++;
		}

		return (tenths / 10L) + "." + (tenths % 10L) + suffix;
	}
}

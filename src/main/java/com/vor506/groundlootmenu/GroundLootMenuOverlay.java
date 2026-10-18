package com.vor506.groundlootmenu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class GroundLootMenuOverlay extends Overlay
{
	static final int VISIBLE_SLOTS = 16;

	private static final int COLUMNS = 4;
	private static final int ROWS = 4;
	private static final int SLOT_WIDTH = 48;
	private static final int SLOT_HEIGHT = 52;
	private static final int GAP = 3;
	private static final int GRID_X = 8;
	private static final int GRID_Y = 40;
	private static final int WIDTH = 217;
	private static final int HEIGHT = 298;
	private static final int COLLAPSED_WIDTH = 48;
	private static final int COLLAPSED_HEIGHT = 40;

	private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 13);
	private static final Font COUNT_FONT = new Font("SansSerif", Font.PLAIN, 10);
	private static final Font TOTAL_FONT = new Font("SansSerif", Font.BOLD, 10);
	private static final Font QUANTITY_FONT = new Font("SansSerif", Font.BOLD, 10);
	private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 9);
	private static final Stroke SELECTED_STROKE = new BasicStroke(2.5f);
	private static final Stroke HOVER_STROKE = new BasicStroke(1.5f);

	private static final Color BACKGROUND = new Color(22, 19, 16, 245);
	private static final Color SLOT_BACKGROUND = new Color(34, 30, 25, 238);
	private static final Color SLOT_BORDER = new Color(71, 59, 43);
	private static final Color GOLD = new Color(177, 145, 83);
	private static final Color QUANTITY_COLOR = new Color(245, 238, 220);
	private static final Color VALUE_COLOR = new Color(166, 202, 137);
	private static final Color BADGE_BACKGROUND = new Color(20, 16, 12, 220);
	private static final Color COLLAPSE_FILL = new Color(65, 52, 36);
	private static final Color SATCHEL_OUTLINE = new Color(12, 8, 7);
	private static final Color SATCHEL_BODY = new Color(105, 20, 25);
	private static final Color SATCHEL_FLAP = new Color(145, 30, 35);
	private static final Color SATCHEL_HIGHLIGHT = new Color(181, 45, 45);
	private static final Color SATCHEL_SHADE = new Color(68, 13, 19);
	private static final Color CLASP_SHADE = new Color(48, 30, 12);
	private static final Color CLASP_GOLD = new Color(184, 133, 42);
	private static final Color CLASP_BRIGHT = new Color(244, 205, 92);
	private static final Color CHEVRON_SHADOW = new Color(10, 8, 6);
	private static final Color CHEVRON = new Color(220, 180, 83);

	private final ItemManager itemManager;
	private final GroundLootMenuConfig config;
	private final TooltipManager tooltipManager;
	private List<LootEntry> entries = Collections.emptyList();
	private LootEntry selectedEntry;
	private String itemCountText = "0 items";
	private String geTotalText = "G.E Value: 0";
	private String highAlchTotalText = "High alch value: 0";
	private boolean collapsed;
	private volatile int hoverX = -1;
	private volatile int hoverY = -1;

	@Inject
	public GroundLootMenuOverlay(ItemManager itemManager, GroundLootMenuConfig config, TooltipManager tooltipManager)
	{
		this.itemManager = itemManager;
		this.config = config;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPreferredLocation(new Point(30, 120));
	}

	void showLoot(
		List<LootEntry> entries,
		int matchCount,
		String geTotal,
		String highAlchTotal,
		LootEntry selectedEntry)
	{
		this.entries = entries;
		this.selectedEntry = selectedEntry;
		this.itemCountText = matchCount + " items";
		this.geTotalText = "G.E Value: " + geTotal;
		this.highAlchTotalText = "High alch value: " + highAlchTotal;
	}

	void setSelectedEntry(LootEntry selectedEntry)
	{
		this.selectedEntry = selectedEntry;
	}

	void reset()
	{
		collapsed = false;
		hoverX = -1;
		hoverY = -1;
		showLoot(Collections.emptyList(), 0, "0", "0", null);
	}

	void setHover(int x, int y)
	{
		hoverX = x;
		hoverY = y;
	}

	LootEntry getHoveredEntry()
	{
		int x = hoverX;
		int y = hoverY;
		if (x < 0 || y < 0)
		{
			return null;
		}

		return getEntryAt(x, y);
	}

	public boolean isCollapsed()
	{
		return collapsed;
	}

	public void toggleCollapsed()
	{
		collapsed = !collapsed;
	}

	public boolean isCollapseButtonAt(int x, int y)
	{
		if (collapsed)
		{
			return x >= 0 && x < COLLAPSED_WIDTH
				&& y >= 0 && y < COLLAPSED_HEIGHT;
		}

		return x >= WIDTH - 28 && x <= WIDTH - 8
			&& y >= 5 && y <= 25;
	}

	public LootEntry getEntryAt(int x, int y)
	{
		if (collapsed || x < GRID_X || y < GRID_Y)
		{
			return null;
		}

		int relativeX = x - GRID_X;
		int relativeY = y - GRID_Y;
		int cellWidth = SLOT_WIDTH + GAP;
		int cellHeight = SLOT_HEIGHT + GAP;
		int column = relativeX / cellWidth;
		int row = relativeY / cellHeight;

		if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS)
		{
			return null;
		}

		if (relativeX % cellWidth >= SLOT_WIDTH
			|| relativeY % cellHeight >= SLOT_HEIGHT)
		{
			return null;
		}

		int index = row * COLUMNS + column;
		if (index >= entries.size())
		{
			return null;
		}

		return entries.get(index);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (collapsed)
		{
			renderCollapsed(graphics);
			return new Dimension(COLLAPSED_WIDTH, COLLAPSED_HEIGHT);
		}

		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, WIDTH, HEIGHT, 8, 8);

		graphics.setColor(GOLD);
		graphics.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 8, 8);

		graphics.setFont(TITLE_FONT);
		graphics.drawString("Ground Loot", 10, 19);

		graphics.setFont(COUNT_FONT);
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.drawString(itemCountText, 10, 29);

		renderCollapseButton(graphics);

		for (int index = 0; index < VISIBLE_SLOTS; index++)
		{
			int column = index % COLUMNS;
			int row = index / COLUMNS;
			int x = GRID_X + column * (SLOT_WIDTH + GAP);
			int y = GRID_Y + row * (SLOT_HEIGHT + GAP);
			renderSlot(graphics, x, y, index);
		}

		renderTotals(graphics);
		renderTooltip();
		return new Dimension(WIDTH, HEIGHT);
	}

	private void renderTooltip()
	{
		int x = hoverX;
		int y = hoverY;
		if (x < 0 || y < 0)
		{
			return;
		}

		LootEntry entry = getHoveredEntry();
		if (entry == null)
		{
			return;
		}

		tooltipManager.add(new Tooltip(entry.getName()));
	}

	private void renderTotals(Graphics2D graphics)
	{
		boolean showGeTotal = config.showGeTotal();
		boolean showHighAlchTotal = config.showHighAlchTotal();
		if (!showGeTotal && !showHighAlchTotal)
		{
			return;
		}

		int gridBottom = GRID_Y + ROWS * SLOT_HEIGHT + (ROWS - 1) * GAP;
		int dividerY = gridBottom + 4;

		graphics.setColor(SLOT_BORDER);
		graphics.drawLine(8, dividerY, WIDTH - 8, dividerY);
		graphics.setFont(TOTAL_FONT);
		graphics.setColor(GOLD);

		int lineY = dividerY + 14;
		if (showGeTotal)
		{
			graphics.drawString(geTotalText, 10, lineY);
			lineY += 14;
		}

		if (showHighAlchTotal)
		{
			graphics.drawString(highAlchTotalText, 10, lineY);
		}
	}

	private void renderCollapsed(Graphics2D graphics)
	{
		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, COLLAPSED_WIDTH, COLLAPSED_HEIGHT, 6, 6);
		graphics.setColor(GOLD);
		graphics.drawRoundRect(0, 0, COLLAPSED_WIDTH - 1, COLLAPSED_HEIGHT - 1, 6, 6);

		graphics.setColor(SATCHEL_OUTLINE);
		graphics.fillRect(9, 4, 17, 4);
		graphics.fillRect(6, 8, 25, 6);
		graphics.fillRect(4, 14, 29, 14);
		graphics.fillRect(7, 28, 24, 4);
		graphics.fillRect(11, 32, 17, 3);
		graphics.fillRect(31, 17, 2, 9);

		graphics.setColor(SATCHEL_BODY);
		graphics.fillRect(8, 11, 22, 17);
		graphics.fillRect(6, 15, 25, 12);
		graphics.fillRect(10, 28, 19, 4);

		graphics.setColor(SATCHEL_FLAP);
		graphics.fillRect(10, 9, 18, 6);
		graphics.fillRect(8, 15, 5, 11);
		graphics.fillRect(13, 16, 15, 10);
		graphics.fillRect(13, 27, 12, 3);

		graphics.setColor(SATCHEL_HIGHLIGHT);
		graphics.fillRect(11, 10, 13, 3);
		graphics.fillRect(9, 16, 3, 7);
		graphics.fillRect(14, 19, 10, 4);

		graphics.setColor(SATCHEL_SHADE);
		graphics.fillRect(28, 15, 3, 12);
		graphics.fillRect(25, 27, 4, 4);
		graphics.fillRect(24, 31, 4, 1);

		graphics.setColor(CLASP_SHADE);
		graphics.fillRect(16, 14, 7, 13);
		graphics.setColor(CLASP_GOLD);
		graphics.fillRect(17, 15, 5, 11);
		graphics.setColor(CLASP_BRIGHT);
		graphics.fillRect(18, 17, 3, 7);
		graphics.fillRect(18, 14, 4, 2);

		graphics.setColor(CHEVRON_SHADOW);
		graphics.drawLine(37, 18, 41, 21);
		graphics.drawLine(41, 21, 37, 24);
		graphics.setColor(CHEVRON);
		graphics.drawLine(37, 17, 40, 20);
		graphics.drawLine(40, 20, 37, 23);
	}

	private void renderCollapseButton(Graphics2D graphics)
	{
		graphics.setColor(COLLAPSE_FILL);
		graphics.fillRoundRect(WIDTH - 28, 5, 20, 20, 4, 4);
		graphics.setColor(GOLD);
		graphics.drawRoundRect(WIDTH - 28, 5, 20, 20, 4, 4);
		graphics.drawLine(WIDTH - 23, 15, WIDTH - 13, 15);
	}

	private void renderSlot(Graphics2D graphics, int x, int y, int index)
	{
		graphics.setColor(SLOT_BACKGROUND);
		graphics.fillRoundRect(x, y, SLOT_WIDTH, SLOT_HEIGHT, 5, 5);
		graphics.setColor(SLOT_BORDER);
		graphics.drawRoundRect(x, y, SLOT_WIDTH - 1, SLOT_HEIGHT - 1, 5, 5);

		if (index >= entries.size())
		{
			return;
		}

		LootEntry entry = entries.get(index);
		if (entry.samePileItem(selectedEntry))
		{
			double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 140.0);
			Stroke previousStroke = graphics.getStroke();
			graphics.setColor(new Color(255, 190, 40, 35 + (int) (75 * pulse)));
			graphics.fillRoundRect(x, y, SLOT_WIDTH, SLOT_HEIGHT, 5, 5);
			graphics.setColor(new Color(255, 225, 135, 115 + (int) (140 * pulse)));
			graphics.setStroke(SELECTED_STROKE);
			graphics.drawRoundRect(x + 1, y + 1, SLOT_WIDTH - 2, SLOT_HEIGHT - 2, 5, 5);
			graphics.setStroke(previousStroke);
		}
		else if (config.highlightOnHover() && entry.samePileItem(getHoveredEntry()))
		{
			Stroke previousStroke = graphics.getStroke();
			graphics.setColor(new Color(255, 225, 135, 170));
			graphics.setStroke(HOVER_STROKE);
			graphics.drawRoundRect(x + 1, y + 1, SLOT_WIDTH - 2, SLOT_HEIGHT - 2, 5, 5);
			graphics.setStroke(previousStroke);
		}

		graphics.drawImage(
			itemManager.getImage(entry.getItemId(), entry.getQuantity(), entry.isStackable()),
			x + 8,
			y + 5,
			32,
			32,
			null);

		if (config.showQuantity() && entry.getQuantity() > 1)
		{
			String quantity = entry.getQuantityText();
			graphics.setFont(QUANTITY_FONT);
			int badgeWidth = graphics.getFontMetrics().stringWidth(quantity) + 6;
			int badgeX = x + SLOT_WIDTH - badgeWidth - 3;
			int badgeY = y + 3;

			graphics.setColor(BADGE_BACKGROUND);
			graphics.fillRoundRect(badgeX, badgeY, badgeWidth, 13, 4, 4);
			graphics.setColor(QUANTITY_COLOR);
			graphics.drawString(quantity, badgeX + 3, badgeY + 10);
		}

		graphics.setFont(VALUE_FONT);
		graphics.setColor(VALUE_COLOR);
		String value = entry.getValueText();
		int textWidth = graphics.getFontMetrics().stringWidth(value);
		graphics.drawString(
			value,
			x + Math.max(2, (SLOT_WIDTH - textWidth) / 2),
			y + SLOT_HEIGHT - 6);
	}
}

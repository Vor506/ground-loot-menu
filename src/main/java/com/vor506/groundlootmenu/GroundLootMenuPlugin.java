package com.vor506.groundlootmenu;

import com.google.inject.Provides;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
		name = "Ground Loot Menu",
		description = "Shows nearby ground items and highlights the selected pile"
)
public class GroundLootMenuPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "groundlootmenu";
	private static final int RANGE = 15;
	private static final Pattern COLOR_TAG = Pattern.compile("(?i)</?col(?:=[^>]*)?>");
	private static final Comparator<LootEntry> BY_VALUE = Comparator
			.comparingLong(LootEntry::getTotalValue)
			.reversed()
			.thenComparing(LootEntry::getName);
	private static final Comparator<LootEntry> BY_QUANTITY = Comparator
			.comparingInt(LootEntry::getQuantity)
			.reversed()
			.thenComparing(LootEntry::getName);
	private static final Comparator<LootEntry> BY_NAME = Comparator
			.comparing(LootEntry::getName, String.CASE_INSENSITIVE_ORDER);

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private GroundLootMenuOverlay overlay;

	@Inject
	private GroundLootTargetOverlay targetOverlay;

	@Inject
	private GroundLootMenuConfig config;

	@Inject
	private ConfigManager configManager;

	private volatile LootEntry selectedEntry;
	private long selectedAtMillis;
	private boolean dragging;
	private boolean hadEligibleLoot;
	private Point dragOffset;
	private JPopupMenu activeMenu;
	private String cachedIgnoreList;
	private String cachedValuableList;
	private Set<String> ignoredNames = Collections.emptySet();
	private Set<String> valuableNames = Collections.emptySet();

	private final MouseAdapter mouseAdapter = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			Rectangle bounds = overlay.getBounds();

			if (bounds == null || !bounds.contains(event.getPoint()))
			{
				return event;
			}

			if (SwingUtilities.isLeftMouseButton(event) && event.isAltDown())
			{
				dragging = true;
				dragOffset = new Point(
						event.getX() - bounds.x,
						event.getY() - bounds.y);
				event.consume();
				return event;
			}

			if (SwingUtilities.isRightMouseButton(event))
			{
				int x = event.getX() - bounds.x;
				int y = event.getY() - bounds.y;
				LootEntry entry = overlay.isCollapsed() ? null : overlay.getEntryAt(x, y);

				if (entry != null)
				{
					showQuickAddMenu(event, entry);
				}

				event.consume();
				return event;
			}

			if (SwingUtilities.isLeftMouseButton(event))
			{
				int x = event.getX() - bounds.x;
				int y = event.getY() - bounds.y;

				if (overlay.isCollapseButtonAt(x, y) || overlay.isCollapsed())
				{
					overlay.toggleCollapsed();
				}
				else
				{
					LootEntry entry = overlay.getEntryAt(x, y);
					if (entry != null)
					{
						selectEntry(entry);
					}
				}
			}

			event.consume();
			return event;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent event)
		{
			updateHover(event);
			return event;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent event)
		{
			updateHover(event);

			if (dragging && dragOffset != null)
			{
				overlay.setPreferredLocation(new Point(
						event.getX() - dragOffset.x,
						event.getY() - dragOffset.y));
				event.consume();
			}

			return event;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent event)
		{
			overlay.setHover(-1, -1);
			return event;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event)
		{
			if (dragging)
			{
				dragging = false;
				dragOffset = null;
				event.consume();
			}

			return event;
		}
	};

	@Provides
	GroundLootMenuConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GroundLootMenuConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(targetOverlay);
		mouseManager.registerMouseListener(mouseAdapter);
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(mouseAdapter);
		overlayManager.remove(overlay);
		overlayManager.remove(targetOverlay);
		hideQuickAddMenu();
		selectedEntry = null;
		selectedAtMillis = 0L;
		hadEligibleLoot = false;
		dragging = false;
		dragOffset = null;
		cachedIgnoreList = null;
		cachedValuableList = null;
		ignoredNames = Collections.emptySet();
		valuableNames = Collections.emptySet();
		overlay.reset();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		expireSelection();
		LootEntry selected = selectedEntry;
		if (selected == null)
		{
			return;
		}

		MenuAction type = MenuAction.of(event.getType());
		if (type == null || !isPileOption(type))
		{
			return;
		}

		if (event.getActionParam0() != selected.getSceneX()
				|| event.getActionParam1() != selected.getSceneY())
		{
			return;
		}

		MenuEntry menuEntry = event.getMenuEntry();
		if (menuEntry == null)
		{
			return;
		}

		if (event.getIdentifier() != selected.getItemId())
		{
			menuEntry.setDeprioritized(true);
			return;
		}

		String target = menuEntry.getTarget();
		if (target == null || target.isEmpty())
		{
			return;
		}

		menuEntry.setTarget("<col=FFD700>" + COLOR_TAG.matcher(target).replaceAll("") + "</col>");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		Scene scene = client.getScene();
		LocalPoint local = player == null ? null : player.getLocalLocation();

		if (player == null || scene == null || local == null)
		{
			clearLoot(true);
			return;
		}

		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();
		if (plane < 0 || plane >= tiles.length || tiles[plane] == null)
		{
			clearLoot(true);
			return;
		}

		refreshNameLists();
		long[] totals = new long[2];
		List<LootEntry> found = collectLoot(player.getWorldLocation(), local, tiles[plane], totals);
		sortLoot(found);
		expireSelection();
		updateSelectedEntry(found);

		int visibleCount = Math.min(GroundLootMenuOverlay.VISIBLE_SLOTS, found.size());
		List<LootEntry> visible = visibleCount == found.size()
				? found
				: new ArrayList<>(found.subList(0, visibleCount));
		overlay.showLoot(
				visible,
				found.size(),
				LootEntry.formatAmount(totals[0]),
				LootEntry.formatAmount(totals[1]),
				selectedEntry);

		boolean hasEligibleLoot = !found.isEmpty();
		if (hasEligibleLoot != hadEligibleLoot)
		{
			boolean expand = hasEligibleLoot && config.autoExpand() && overlay.isCollapsed();
			boolean collapse = !hasEligibleLoot && config.autoCollapse() && !overlay.isCollapsed();
			if (expand || collapse)
			{
				overlay.toggleCollapsed();
			}
		}

		hadEligibleLoot = hasEligibleLoot;
	}

	LootEntry getSelectedEntry()
	{
		expireSelection();
		return selectedEntry;
	}

	private void updateHover(MouseEvent event)
	{
		Rectangle bounds = overlay.getBounds();
		if (bounds == null || !bounds.contains(event.getPoint()))
		{
			overlay.setHover(-1, -1);
			return;
		}

		overlay.setHover(event.getX() - bounds.x, event.getY() - bounds.y);
	}

	private void selectEntry(LootEntry entry)
	{
		if (entry.samePileItem(selectedEntry))
		{
			clearSelection();
			return;
		}

		selectedEntry = entry;
		selectedAtMillis = System.currentTimeMillis();
		overlay.setSelectedEntry(entry);
	}

	private void clearSelection()
	{
		selectedEntry = null;
		selectedAtMillis = 0L;
		overlay.setSelectedEntry(null);
	}

	private void expireSelection()
	{
		if (selectedEntry == null)
		{
			return;
		}

		int seconds = config.highlightSeconds();
		if (seconds <= 0)
		{
			return;
		}

		if (System.currentTimeMillis() - selectedAtMillis >= seconds * 1000L)
		{
			clearSelection();
		}
	}

	private void clearLoot(boolean collapseWhenEmpty)
	{
		selectedEntry = null;
		selectedAtMillis = 0L;
		overlay.showLoot(Collections.emptyList(), 0, "0", "0", null);
		if (collapseWhenEmpty && config.autoCollapse() && !overlay.isCollapsed())
		{
			overlay.toggleCollapsed();
		}
		hadEligibleLoot = false;
	}

	private List<LootEntry> collectLoot(WorldPoint playerLocation, LocalPoint local, Tile[][] planeTiles, long[] totals)
	{
		int minX = Math.max(0, local.getSceneX() - RANGE);
		int maxX = Math.min(planeTiles.length - 1, local.getSceneX() + RANGE);
		List<LootEntry> found = new ArrayList<>();

		for (int x = minX; x <= maxX; x++)
		{
			Tile[] column = planeTiles[x];
			if (column == null)
			{
				continue;
			}

			int minY = Math.max(0, local.getSceneY() - RANGE);
			int maxY = Math.min(column.length - 1, local.getSceneY() + RANGE);
			for (int y = minY; y <= maxY; y++)
			{
				collectTile(column[y], playerLocation, found, totals);
			}
		}

		return found;
	}

	private void collectTile(Tile tile, WorldPoint playerLocation, List<LootEntry> found, long[] totals)
	{
		if (tile == null || tile.getGroundItems() == null)
		{
			return;
		}

		WorldPoint location = tile.getWorldLocation();
		LocalPoint localLocation = tile.getLocalLocation();
		if (location == null || localLocation == null
				|| playerLocation.distanceTo(location) > RANGE)
		{
			return;
		}

		for (TileItem item : tile.getGroundItems())
		{
			LootEntry entry = toEntry(item, location, localLocation);
			if (entry != null)
			{
				found.add(entry);
				totals[0] += entry.getTotalValue();
				totals[1] += entry.getHighAlchValue();
			}
		}
	}

	private LootEntry toEntry(TileItem item, WorldPoint location, LocalPoint localLocation)
	{
		if (item.getOwnership() == TileItem.OWNERSHIP_OTHER || item.getQuantity() <= 0)
		{
			return null;
		}

		int id = item.getId();
		ItemComposition composition = itemManager.getItemComposition(id);
		String name = composition.getName();
		if (name == null || name.isEmpty())
		{
			return null;
		}

		int quantity = item.getQuantity();
		String normalizedName = name.trim().toLowerCase(Locale.ROOT);
		long unitPrice = itemManager.getItemPrice(id);
		if (!valuableNames.contains(normalizedName))
		{
			if (ignoredNames.contains(normalizedName))
			{
				return null;
			}

			if (config.hideUnpriced() && unitPrice <= 0)
			{
				return null;
			}

			if (config.hideLowValue() && unitPrice * (long) quantity < config.minimumTotalGeValue())
			{
				return null;
			}
		}

		return new LootEntry(
				id,
				quantity,
				name,
				location,
				localLocation.getSceneX(),
				localLocation.getSceneY(),
				unitPrice * (long) quantity,
				(long) composition.getHaPrice() * quantity,
				composition.isStackable());
	}

	private void sortLoot(List<LootEntry> found)
	{
		switch (config.sortOrder())
		{
			case QUANTITY:
				found.sort(BY_QUANTITY);
				break;
			case NAME:
				found.sort(BY_NAME);
				break;
			default:
				found.sort(BY_VALUE);
				break;
		}
	}

	private void updateSelectedEntry(List<LootEntry> found)
	{
		LootEntry selected = selectedEntry;
		if (selected == null)
		{
			return;
		}

		for (LootEntry entry : found)
		{
			if (entry.samePileItem(selected))
			{
				selectedEntry = entry;
				return;
			}
		}

		selectedEntry = null;
	}

	private void refreshNameLists()
	{
		String ignoreList = config.ignoreList() == null ? "" : config.ignoreList();
		if (!ignoreList.equals(cachedIgnoreList))
		{
			cachedIgnoreList = ignoreList;
			ignoredNames = parseItemNames(ignoreList);
		}

		String valuableList = config.valuableItems() == null ? "" : config.valuableItems();
		if (!valuableList.equals(cachedValuableList))
		{
			cachedValuableList = valuableList;
			valuableNames = parseItemNames(valuableList);
		}
	}

	private static Set<String> parseItemNames(String input)
	{
		if (input == null || input.trim().isEmpty())
		{
			return Collections.emptySet();
		}

		Set<String> names = new HashSet<>();
		for (String part : input.split("[,\\r\\n]+"))
		{
			String normalized = part.trim().toLowerCase(Locale.ROOT);
			if (!normalized.isEmpty())
			{
				names.add(normalized);
			}
		}

		return names;
	}

	private static boolean isPileOption(MenuAction type)
	{
		switch (type)
		{
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case EXAMINE_ITEM_GROUND:
				return true;
			default:
				return false;
		}
	}

	private void showQuickAddMenu(MouseEvent event, LootEntry entry)
	{
		String itemName = entry.getName();
		java.awt.Component component = event.getComponent();
		int x = event.getX();
		int y = event.getY();

		SwingUtilities.invokeLater(() ->
		{
			hideQuickAddMenu();
			JPopupMenu menu = new JPopupMenu();
			addListMenuItem(menu, "ignoreList", "Ignore", itemName);
			addListMenuItem(menu, "valuableItems", "Always show", itemName);
			activeMenu = menu;
			menu.show(component, x, y);
		});
	}

	private void hideQuickAddMenu()
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			dismissQuickAddMenu();
		}
		else
		{
			SwingUtilities.invokeLater(this::dismissQuickAddMenu);
		}
	}

	private void dismissQuickAddMenu()
	{
		JPopupMenu menu = activeMenu;
		activeMenu = null;
		if (menu != null)
		{
			menu.setVisible(false);
		}
	}

	private void addListMenuItem(JPopupMenu menu, String key, String label, String itemName)
	{
		boolean alreadyListed = parseItemNames(listValue(key))
				.contains(itemName.trim().toLowerCase(Locale.ROOT));
		JMenuItem item = new JMenuItem((alreadyListed ? "Remove from " : "Add to ") + label);
		item.addActionListener(e -> updateItemList(key, itemName, !alreadyListed));
		menu.add(item);
	}

	private void updateItemList(String key, String itemName, boolean add)
	{
		List<String> names = new ArrayList<>();
		String currentValue = listValue(key);

		if (!currentValue.trim().isEmpty())
		{
			for (String part : currentValue.split("[,\\r\\n]+"))
			{
				String trimmed = part.trim();
				if (!trimmed.isEmpty())
				{
					names.add(trimmed);
				}
			}
		}

		String normalizedName = itemName.trim().toLowerCase(Locale.ROOT);
		boolean exists = names.stream().anyMatch(name ->
				name.toLowerCase(Locale.ROOT).equals(normalizedName));

		if (add && !exists)
		{
			names.add(itemName.trim());
		}
		else if (!add)
		{
			names.removeIf(name -> name.toLowerCase(Locale.ROOT).equals(normalizedName));
		}
		else
		{
			return;
		}

		String updated = String.join(", ", names);
		if (updated.isEmpty())
		{
			configManager.unsetConfiguration(CONFIG_GROUP, key);
		}
		else
		{
			configManager.setConfiguration(CONFIG_GROUP, key, updated);
		}

		if ("ignoreList".equals(key))
		{
			cachedIgnoreList = updated;
			ignoredNames = parseItemNames(updated);
		}
		else if ("valuableItems".equals(key))
		{
			cachedValuableList = updated;
			valuableNames = parseItemNames(updated);
		}

		// The settings text area is filled once when the page opens and is not
		// subscribed to config changes. Leaving it stale also writes the old
		// blank text back over this update when the field loses focus.
		syncOpenConfigField(configFieldName(key), updated);
	}

	private String listValue(String key)
	{
		String value = "ignoreList".equals(key) ? config.ignoreList() : config.valuableItems();
		return value == null ? "" : value;
	}

	private static String configFieldName(String key)
	{
		return "ignoreList".equals(key) ? "Ignore list" : "Always show items";
	}

	private static void syncOpenConfigField(String label, String value)
	{
		String shown = value == null ? "" : value;
		for (Window window : Window.getWindows())
		{
			syncOpenConfigField(window, label, shown);
		}
	}

	private static void syncOpenConfigField(Component component, String label, String value)
	{
		if (component instanceof JLabel && label.equals(((JLabel) component).getText()))
		{
			Container parent = component.getParent();
			if (parent != null)
			{
				setConfigText(parent, value);
			}
		}

		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				syncOpenConfigField(child, label, value);
			}
		}
	}

	private static void setConfigText(Container parent, String value)
	{
		for (Component child : parent.getComponents())
		{
			if (child instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent) child;
				if (!value.equals(text.getText()))
				{
					text.setText(value);
				}
			}
			else if (child instanceof Container)
			{
				setConfigText((Container) child, value);
			}
		}
	}
}
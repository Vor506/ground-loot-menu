package com.vor506.groundlootmenu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class GroundLootTargetOverlay extends Overlay
{
	private static final Stroke TILE_STROKE = new BasicStroke(2.0f);
	private static final Stroke ARROW_STROKE = new BasicStroke(1.5f);
	private static final Color ARROW_SHADOW = new Color(0, 0, 0, 180);
	private static final Color ARROW_OUTLINE = new Color(74, 43, 13, 255);

	private final Client client;
	private final GroundLootMenuPlugin plugin;

	@Inject
	public GroundLootTargetOverlay(Client client, GroundLootMenuPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		LootEntry selected = plugin.getSelectedEntry();
		if (selected == null || client.getLocalPlayer() == null
			|| selected.getLocation().getPlane() != client.getPlane())
		{
			return null;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(client, selected.getLocation());
		if (localPoint == null)
		{
			return null;
		}

		Polygon tile = Perspective.getCanvasTilePoly(client, localPoint);
		Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getPlane(), 0);
		if (tile == null || canvasPoint == null)
		{
			return null;
		}

		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 140.0);
		int alpha = 35 + (int) (150 * pulse);
		int arrowAlpha = 90 + (int) (165 * pulse);
		Stroke oldStroke = graphics.getStroke();

		graphics.setColor(new Color(255, 190, 40, alpha));
		graphics.fillPolygon(tile);
		graphics.setColor(new Color(255, 225, 135, alpha));
		graphics.setStroke(TILE_STROKE);
		graphics.drawPolygon(tile);
		graphics.setStroke(oldStroke);

		int x = canvasPoint.getX();
		int y = canvasPoint.getY() - 8;
		Polygon shadow = new Polygon(
			new int[]{x - 6, x + 10, x + 10, x + 16, x + 2, x - 12, x - 6},
			new int[]{y - 30, y - 30, y - 20, y - 20, y - 6, y - 20, y - 20},
			7);
		Polygon arrow = new Polygon(
			new int[]{x - 8, x + 8, x + 8, x + 14, x, x - 14, x - 8},
			new int[]{y - 32, y - 32, y - 22, y - 22, y - 8, y - 22, y - 22},
			7);

		graphics.setColor(ARROW_SHADOW);
		graphics.fillPolygon(shadow);
		graphics.setColor(new Color(255, 196, 55, arrowAlpha));
		graphics.fillPolygon(arrow);
		graphics.setColor(ARROW_OUTLINE);
		graphics.setStroke(ARROW_STROKE);
		graphics.drawPolygon(arrow);
		graphics.setStroke(oldStroke);
		return null;
	}
}

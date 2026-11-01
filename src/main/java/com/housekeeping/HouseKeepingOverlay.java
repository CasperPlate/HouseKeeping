package com.housekeeping;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

class HouseKeepingOverlay extends WidgetItemOverlay
{
	private static final int DOT_SIZE = 6;

	private final Client client;
	private final HouseKeepingPlugin plugin;
	private final HouseKeepingConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	HouseKeepingOverlay(Client client, HouseKeepingPlugin plugin, HouseKeepingConfig config, TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tooltipManager = tooltipManager;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.highlightBank())
		{
			return;
		}

		Suggestion suggestion = plugin.getVisibleSuggestions().get(itemId);
		if (suggestion == null)
		{
			return;
		}

		Color color = plugin.getColor(suggestion.getCategory());
		Rectangle bounds = widgetItem.getCanvasBounds();
		int x = bounds.x + bounds.width - DOT_SIZE - 1;
		int y = bounds.y + 1;

		graphics.setColor(Color.BLACK);
		graphics.fillOval(x - 1, y - 1, DOT_SIZE + 2, DOT_SIZE + 2);
		graphics.setColor(color);
		graphics.fillOval(x, y, DOT_SIZE, DOT_SIZE);

		boolean maybe = suggestion.getConfidence() == Confidence.MAYBE;
		if (maybe)
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
			graphics.setColor(Color.BLACK);
			graphics.drawString("?", x - 5, y + DOT_SIZE + 2);
			graphics.setColor(color);
			graphics.drawString("?", x - 6, y + DOT_SIZE + 1);
		}

		Point mouse = client.getMouseCanvasPosition();
		if (bounds.contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip(ColorUtil.wrapWithColorTag("HouseKeeping", color)
				+ (maybe ? " (maybe)" : "") + "</br>" + suggestion.getReason()));
		}
	}
}

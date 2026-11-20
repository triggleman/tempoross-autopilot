package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Urgency;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** The route to the next step's target, drawn on the minimap. */
public class MinimapPathOverlay extends Overlay
{
	private final Client client;
	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public MinimapPathOverlay(Client client, TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<WorldPoint> path = plugin.getPath();
		WorldView wv = client.getTopLevelWorldView();
		if (!config.minimapPath() || path.size() < 2 || wv == null)
		{
			return null;
		}
		g.setColor(plugin.getInstruction().getUrgency() == Urgency.NOW ? config.urgentColor() : config.pathColor());
		g.setStroke(new BasicStroke(2));
		Point prev = null;
		for (WorldPoint wp : path)
		{
			LocalPoint lp = LocalPoint.fromWorld(wv, wp);
			Point p = lp == null ? null : Perspective.localToMinimap(client, lp);
			if (p != null && prev != null)
			{
				g.drawLine(prev.getX(), prev.getY(), p.getX(), p.getY());
			}
			prev = p;
		}
		return null;
	}
}

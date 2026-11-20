package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Urgency;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * The walking line to the next step's target, like GOTR Coach's: it starts at your feet, drops
 * the legs you've already walked, ends on the target and disappears once you're next to it.
 */
public class PathOverlay extends Overlay
{
	private final Client client;
	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public PathOverlay(Client client, TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		TemporossAutopilotConfig.PathStyle style = config.pathStyle();
		List<WorldPoint> path = plugin.getPath();
		WorldView wv = client.getTopLevelWorldView();
		if (style == TemporossAutopilotConfig.PathStyle.OFF || path.size() < 2 || wv == null)
		{
			return null;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color color = plugin.getInstruction().getUrgency() == Urgency.NOW ? config.urgentColor() : config.pathColor();

		if (style == TemporossAutopilotConfig.PathStyle.TILES || style == TemporossAutopilotConfig.PathStyle.LINE_AND_TILES)
		{
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(60, color.getAlpha()));
			List<WorldPoint> all = plugin.getPathTiles();
			for (int i = 1; i < all.size(); i++)
			{
				LocalPoint lp = LocalPoint.fromWorld(wv, all.get(i));
				Polygon poly = lp == null ? null : Perspective.getCanvasTilePoly(client, lp);
				if (poly != null)
				{
					OverlayUtil.renderPolygon(g, poly, color, fill, new BasicStroke(1));
				}
			}
		}
		if (style == TemporossAutopilotConfig.PathStyle.LINE || style == TemporossAutopilotConfig.PathStyle.LINE_AND_TILES)
		{
			drawLine(g, wv, path, color);
		}
		return null;
	}

	private void drawLine(Graphics2D g, WorldView wv, List<WorldPoint> path, Color color)
	{
		Player me = client.getLocalPlayer();
		LocalPoint here = me == null ? null : me.getLocalLocation();
		if (here == null)
		{
			return;
		}
		WorldPoint hereTile = WorldPoint.fromLocal(client, here);
		WorldPoint goal = path.get(path.size() - 1);
		if (hereTile != null && hereTile.getPlane() == goal.getPlane()
			&& Math.abs(hereTile.getX() - goal.getX()) <= 1 && Math.abs(hereTile.getY() - goal.getY()) <= 1)
		{
			return;
		}

		// Skip legs already walked: start after the leg you're standing nearest to.
		int first = 1;
		double nearest = 2.0;
		double px = here.getX() / 128.0;
		double py = here.getY() / 128.0;
		for (int k = 0; k + 1 < path.size(); k++)
		{
			LocalPoint a = LocalPoint.fromWorld(wv, path.get(k));
			LocalPoint b = LocalPoint.fromWorld(wv, path.get(k + 1));
			if (a == null || b == null)
			{
				continue;
			}
			double d = distanceToLeg(px, py, a.getX() / 128.0, a.getY() / 128.0, b.getX() / 128.0, b.getY() / 128.0);
			if (d <= nearest)
			{
				nearest = d;
				first = k + 1;
			}
		}

		List<Point> pts = new ArrayList<>();
		Point start = Perspective.localToCanvas(client, here, wv.getPlane());
		if (start != null)
		{
			pts.add(start);
		}
		for (int i = first; i < path.size(); i++)
		{
			LocalPoint lp = LocalPoint.fromWorld(wv, path.get(i));
			Point p = lp == null ? null : Perspective.localToCanvas(client, lp, wv.getPlane());
			if (p != null)
			{
				pts.add(p);
			}
		}
		Point end = targetCentre(wv);
		if (end != null)
		{
			pts.add(end);
		}
		if (pts.size() < 2)
		{
			return;
		}

		g.setColor(color);
		g.setStroke(new BasicStroke(config.pathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Path2D.Double shape = new Path2D.Double();
		shape.moveTo(pts.get(0).getX(), pts.get(0).getY());
		if (config.roundCorners())
		{
			for (int i = 1; i < pts.size() - 1; i++)
			{
				Point c = pts.get(i);
				Point n = pts.get(i + 1);
				shape.quadTo(c.getX(), c.getY(), (c.getX() + n.getX()) / 2.0, (c.getY() + n.getY()) / 2.0);
			}
		}
		else
		{
			for (int i = 1; i < pts.size() - 1; i++)
			{
				shape.lineTo(pts.get(i).getX(), pts.get(i).getY());
			}
		}
		shape.lineTo(pts.get(pts.size() - 1).getX(), pts.get(pts.size() - 1).getY());
		g.draw(shape);
	}

	/** Where the line ends: the centre of the target object or NPC, at ground level. */
	private Point targetCentre(WorldView wv)
	{
		LocalPoint lp = null;
		GameObject obj = plugin.getTargetObject();
		NPC npc = plugin.getTargetNpc();
		if (obj != null)
		{
			lp = obj.getLocalLocation();
		}
		else if (npc != null)
		{
			lp = npc.getLocalLocation();
		}
		return lp == null ? null : Perspective.localToCanvas(client, lp, wv.getPlane());
	}

	private static double distanceToLeg(double px, double py, double ax, double ay, double bx, double by)
	{
		double abx = bx - ax;
		double aby = by - ay;
		double len = abx * abx + aby * aby;
		double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * abx + (py - ay) * aby) / len));
		double dx = px - ax - t * abx;
		double dy = py - ay - t * aby;
		return Math.sqrt(dx * dx + dy * dy);
	}
}

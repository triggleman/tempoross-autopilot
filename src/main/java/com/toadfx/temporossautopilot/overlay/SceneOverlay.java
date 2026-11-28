package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Target;
import com.toadfx.temporossautopilot.logic.Urgency;
import com.toadfx.temporossautopilot.state.GameTracker;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import com.toadfx.temporossautopilot.state.Ids;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Outlines in the game world: the next target, double spots, fires, broken masts and the crate counter. */
public class SceneOverlay extends Overlay
{
	private static final Stroke THIN = new BasicStroke(1.5f);
	private static final Stroke THICK = new BasicStroke(2.5f);
	private static final int PIE = 22;

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public SceneOverlay(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		GameTracker tracker = plugin.getTracker();
		Snapshot s = plugin.getSnapshot();
		if (tracker == null || !(s.inGame() || s.phase == com.toadfx.temporossautopilot.logic.Phase.WAITING))
		{
			return null;
		}
		Instruction step = plugin.getInstruction();
		Color target = step.getUrgency() == Urgency.NOW ? config.urgentColor() : config.targetColor();

		if (config.highlightFires())
		{
			for (NPC fire : tracker.getFires())
			{
				outlineTile(g, fire.getCanvasTilePoly(), config.fireColor(), THIN);
			}
		}
		if (config.highlightBroken())
		{
			for (GameObject o : tracker.getDamagedTethers())
			{
				outlineObject(g, o, config.fireColor(), THIN);
			}
		}
		long now = System.currentTimeMillis();
		if (config.showFireTimers() && s.inGame())
		{
			for (Map.Entry<GameObject, Long> e : tracker.getFireSpread().entrySet())
			{
				long left = e.getValue() - now;
				if (left > 0)
				{
					timer(g, e.getKey().getCanvasTextLocation(g, "0.0", 0), left, Ids.FIRE_SPREAD_MS, config.timerColor(), "Spreads");
				}
			}
		}
		long waveAt = tracker.getWaveHitsAt();
		if (config.showWaveTimers() && waveAt > now && s.inGame())
		{
			long left = waveAt - now;
			Color c = config.timerColor();
			for (GameObject t : tracker.getTethers())
			{
				outlineObject(g, t, c, THIN);
				timer(g, t.getCanvasTextLocation(g, "0.0", 160), left, Ids.WAVE_MS, c, s.tethered ? "Tethered" : "Tether");
			}
			for (GameObject t : tracker.getDamagedTethers())
			{
				timer(g, t.getCanvasTextLocation(g, "0.0", 160), left, Ids.WAVE_MS, c, "Broken");
			}
		}

		NPC dbl = tracker.getDoubleSpot();
		if (config.highlightDouble() && dbl != null && s.inGame())
		{
			outlineTile(g, dbl.getCanvasTilePoly(), config.doubleColor(), THICK);
			String t = tracker.doubleTimer(dbl);
			String text = t.isEmpty() ? "DOUBLE SPOT" : "DOUBLE SPOT  " + t;
			label(g, dbl.getCanvasTextLocation(g, text, 0), text, config.doubleColor());
		}

		if (config.showLoadCounter() && s.inGame())
		{
			NPC crate = plugin.getTargetCrate();
			if (crate != null)
			{
				int targetLoad = plugin.getLoadTarget();
				String text = targetLoad > 0 ? plugin.getLoadDone() + " / " + targetLoad : "Loaded " + s.loadedThisRound;
				label(g, crate.getCanvasTextLocation(g, text, 120), text, config.targetColor());
			}
		}

		if (!config.highlightTarget() || step.getTarget() == Target.NONE)
		{
			return null;
		}
		NPC npc = plugin.getTargetNpc();
		GameObject obj = plugin.getTargetObject();
		String text = targetLabel(step.getTarget());
		if (npc != null && npc != dbl)
		{
			Shape hull = npc.getConvexHull();
			if (hull != null)
			{
				OverlayUtil.renderPolygon(g, hull, target, THICK);
			}
			else
			{
				outlineTile(g, npc.getCanvasTilePoly(), target, THICK);
			}
			label(g, npc.getCanvasTextLocation(g, text, 60), text, target);
		}
		else if (obj != null)
		{
			outlineObject(g, obj, target, THICK);
			label(g, obj.getCanvasTextLocation(g, text, 120), text, target);
		}
		else if (step.getTarget() == Target.SUPPLY_CRATES)
		{
			for (GameObject crate : tracker.getSupplyCrates())
			{
				outlineObject(g, crate, target, THIN);
			}
		}
		return null;
	}

	private static String targetLabel(Target t)
	{
		switch (t)
		{
			case CRATE:
				return "Load here";
			case OTHER_CRATE:
				return "Load this crate";
			case DROP_CRATE:
				return "Cannon 1";
			case SHRINE:
				return "Cook here";
			case SPIRIT_POOL:
				return "Spirit pool";
			case TETHER:
				return "Tether here";
			case DAMAGED_TETHER:
				return "Repair (+40)";
			case FIRE:
				return "Douse (+40)";
			case WATER_PUMP:
				return "Fill buckets";
			case FISHING_SPOT:
				return "Fish here";
			default:
				return "";
		}
	}

	private static void outlineObject(Graphics2D g, GameObject o, Color color, Stroke stroke)
	{
		Shape hull = o.getConvexHull();
		if (hull != null)
		{
			OverlayUtil.renderPolygon(g, hull, color, stroke);
			return;
		}
		outlineTile(g, o.getCanvasTilePoly(), color, stroke);
	}

	private static void outlineTile(Graphics2D g, Polygon poly, Color color, Stroke stroke)
	{
		if (poly != null)
		{
			OverlayUtil.renderPolygon(g, poly, color, stroke);
		}
	}

	/** A shrinking pie with the seconds left in it, and a word under it. */
	private static void timer(Graphics2D g, Point at, long msLeft, int msTotal, Color color, String word)
	{
		if (at == null)
		{
			return;
		}
		int x = at.getX();
		int y = at.getY() - PIE;
		double frac = Math.max(0, Math.min(1, msLeft / (double) msTotal));
		g.setColor(new Color(0, 0, 0, 150));
		g.fillOval(x - PIE / 2, y - PIE / 2, PIE, PIE);
		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 170));
		g.fill(new Arc2D.Double(x - PIE / 2.0, y - PIE / 2.0, PIE, PIE, 90, 360 * frac, Arc2D.PIE));
		g.setColor(color);
		g.drawOval(x - PIE / 2, y - PIE / 2, PIE, PIE);
		String secs = String.format("%.1f", msLeft / 1000.0);
		int w = g.getFontMetrics().stringWidth(secs);
		OverlayUtil.renderTextLocation(g, new Point(x - w / 2, y + 4), secs, Color.WHITE);
		int ww = g.getFontMetrics().stringWidth(word);
		OverlayUtil.renderTextLocation(g, new Point(x - ww / 2, y + PIE / 2 + 12), word, color);
	}

	private static void label(Graphics2D g, Point at, String text, Color color)
	{
		if (at != null && !text.isEmpty())
		{
			OverlayUtil.renderTextLocation(g, at, text, color);
		}
	}
}

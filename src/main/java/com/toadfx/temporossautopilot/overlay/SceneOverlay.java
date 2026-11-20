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
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Outlines in the game world: the next target, double spots, fires, broken masts and the crate counter. */
public class SceneOverlay extends Overlay
{
	private static final Stroke THIN = new BasicStroke(1.5f);
	private static final Stroke THICK = new BasicStroke(2.5f);

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
		NPC dbl = tracker.getDoubleSpot();
		if (config.highlightDouble() && dbl != null && s.inGame())
		{
			outlineTile(g, dbl.getCanvasTilePoly(), config.doubleColor(), THICK);
			label(g, dbl.getCanvasTextLocation(g, "DOUBLE SPOT", 0), "DOUBLE SPOT", config.doubleColor());
		}

		if (config.showLoadCounter() && s.inGame())
		{
			NPC crate = plugin.getTargetCrate();
			if (crate != null)
			{
				int targetLoad = plugin.getLoadTarget();
				String text = targetLoad > 0 ? s.loadedThisRound + " / " + targetLoad : "Loaded " + s.loadedThisRound;
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

	private static void label(Graphics2D g, Point at, String text, Color color)
	{
		if (at != null && !text.isEmpty())
		{
			OverlayUtil.renderTextLocation(g, at, text, color);
		}
	}
}

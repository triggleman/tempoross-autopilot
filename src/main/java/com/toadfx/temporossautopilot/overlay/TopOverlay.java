package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Urgency;
import com.toadfx.temporossautopilot.state.GameTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The box at the top of the screen: what to do now, the game clock,
 * timers for what's coming (wave, fire, storm) and the next steps after this one.
 */
public class TopOverlay extends Overlay
{
	private static final int WIDTH = 300;
	private static final int PAD = 8;
	private static final Color BG = new Color(12, 14, 16, 215);
	private static final Color MUTED = new Color(190, 190, 190);
	private static final Color SOON = new Color(255, 210, 0);

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public TopOverlay(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** One line of the "coming up" list. */
	private static final class Row
	{
		final String left;
		final String right;
		final Color color;

		Row(String left, String right, Color color)
		{
			this.left = left;
			this.right = right;
			this.color = color;
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Snapshot s = plugin.getSnapshot();
		Instruction step = plugin.getInstruction();
		GameTracker tracker = plugin.getTracker();
		if (!config.showTopBox() || tracker == null || step.isEmpty() || s.phase == Phase.IDLE)
		{
			return null;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font small = FontManager.getRunescapeSmallFont();
		Font bold = FontManager.getRunescapeBoldFont();
		long now = System.currentTimeMillis();

		// Work out every line first so the box can be sized.
		g.setFont(small);
		FontMetrics fs = g.getFontMetrics();
		g.setFont(bold);
		FontMetrics fb = g.getFontMetrics();
		int inner = WIDTH - PAD * 2;

		List<String> detail = wrap(step.getDetail(), fs, inner);
		List<Row> coming = config.showUpcoming() ? comingUp(s, tracker, now) : new ArrayList<>();
		List<String> then = new ArrayList<>();
		if (config.showUpcoming() && !plugin.getUpcoming().isEmpty())
		{
			StringBuilder sb = new StringBuilder("Then: ");
			for (int i = 0; i < plugin.getUpcoming().size(); i++)
			{
				sb.append(i == 0 ? "" : "  >  ").append(plugin.getUpcoming().get(i).getTitle());
			}
			then = wrap(sb.toString(), fs, inner);
		}

		int ls = fs.getHeight();
		int h = PAD + ls + 2 + fb.getHeight() + detail.size() * ls;
		if (!coming.isEmpty())
		{
			h += 6 + coming.size() * ls;
		}
		if (!then.isEmpty())
		{
			h += 6 + then.size() * ls;
		}
		h += PAD;

		g.setColor(BG);
		g.fillRoundRect(0, 0, WIDTH, h, 10, 10);
		Color stepColor = step.getUrgency() == Urgency.NOW ? config.urgentColor()
			: step.getUrgency() == Urgency.SOON ? SOON : config.targetColor();
		g.setColor(stepColor);
		g.fillRoundRect(0, 0, 4, h, 4, 4);

		// Header: name and clock.
		int y = PAD + fs.getAscent();
		g.setFont(small);
		g.setColor(config.targetColor());
		g.drawString("NOW", PAD, y);
		String clock = clock(s, tracker, now);
		if (!clock.isEmpty())
		{
			g.setColor(Color.WHITE);
			g.drawString(clock, WIDTH - PAD - fs.stringWidth(clock), y);
		}
		y += 2;

		// The step.
		g.setFont(bold);
		y += fb.getAscent();
		g.setColor(stepColor);
		g.drawString(step.getTitle(), PAD, y);
		y += fb.getDescent();
		g.setFont(small);
		g.setColor(MUTED);
		for (String line : detail)
		{
			y += ls;
			g.drawString(line, PAD, y - fs.getDescent());
		}

		// Coming up: timers.
		if (!coming.isEmpty())
		{
			y += 6;
			for (Row r : coming)
			{
				y += ls;
				g.setColor(MUTED);
				g.drawString(r.left, PAD, y - fs.getDescent());
				g.setColor(r.color);
				g.drawString(r.right, WIDTH - PAD - fs.stringWidth(r.right), y - fs.getDescent());
			}
		}

		// Next steps.
		if (!then.isEmpty())
		{
			y += 6;
			g.setColor(Color.WHITE);
			for (String line : then)
			{
				y += ls;
				g.drawString(line, PAD, y - fs.getDescent());
			}
		}
		return new Dimension(WIDTH, h);
	}

	private String clock(Snapshot s, GameTracker tracker, long now)
	{
		if (!config.showGameTimer() || tracker.getGameStartMillis() <= 0 || !s.inGame())
		{
			return s.phase == Phase.WAITING ? "Before the game" : "";
		}
		int secs = (int) ((now - tracker.getGameStartMillis()) / 1000);
		String phase = s.phase == Phase.ESSENCE ? "Spirit pool" : "Round " + s.round;
		return phase + "   " + secs / 60 + ":" + String.format("%02d", secs % 60);
	}

	private List<Row> comingUp(Snapshot s, GameTracker tracker, long now)
	{
		List<Row> rows = new ArrayList<>();
		if (!s.inGame())
		{
			return rows;
		}
		long wave = tracker.getWaveHitsAt();
		if (wave > now)
		{
			rows.add(new Row(s.tethered ? "Wave hits (tethered)" : "Wave hits: TETHER", secs(wave - now), s.tethered ? config.timerColor() : config.urgentColor()));
		}
		long fire = Long.MAX_VALUE;
		for (long t : tracker.getClouds().values())
		{
			if (t > now && t < fire)
			{
				fire = t;
			}
		}
		if (fire != Long.MAX_VALUE)
		{
			rows.add(new Row("Lightning fire lands", secs(fire - now), config.timerColor()));
		}
		if (!tracker.getFireSpread().isEmpty())
		{
			long spread = Long.MAX_VALUE;
			for (long t : tracker.getFireSpread().values())
			{
				if (t > now && t < spread)
				{
					spread = t;
				}
			}
			if (spread != Long.MAX_VALUE)
			{
				rows.add(new Row(tracker.getFireSpread().size() + " fire" + (tracker.getFireSpread().size() == 1 ? "" : "s") + ", next spread",
					secs(spread - now), config.timerColor()));
			}
		}
		if (s.phase == Phase.ENERGY && s.stormSecondsLeft > 0 && s.stormSecondsLeft <= 120)
		{
			rows.add(new Row("Storm fills (" + s.storm + "%)", "~" + s.stormSecondsLeft + "s", s.stormSecondsLeft < 30 ? config.urgentColor() : config.timerColor()));
		}
		if (s.phase == Phase.ENERGY && s.energy >= 0)
		{
			rows.add(new Row("Tempoross energy", s.energy + "%", config.targetColor()));
		}
		if (s.doubleSpot && s.phase == Phase.ENERGY)
		{
			rows.add(new Row("Double spot", "open now", config.doubleColor()));
		}
		return rows;
	}

	private static String secs(long ms)
	{
		return String.format("%.1fs", ms / 1000.0);
	}

	/** Splits text into lines that fit the width. */
	static List<String> wrap(String text, FontMetrics fm, int width)
	{
		List<String> lines = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return lines;
		}
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String next = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(next) > width && line.length() > 0)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(next);
			}
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
	}
}

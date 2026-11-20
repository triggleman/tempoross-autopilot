package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Snapshot;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Storm intensity bar with the danger mark (93% by default), the time until it fills at
 * the current rate, and a wave warning.
 */
public class StormOverlay extends Overlay
{
	private static final int WIDTH = 240;
	private static final int PAD = 6;
	private static final Color BG = new Color(12, 14, 16, 210);
	private static final Color TRACK = new Color(58, 58, 58);
	private static final Color STORM = new Color(240, 180, 76);
	private static final Color MUTED = new Color(200, 200, 200);
	private static final Color WAVE = new Color(255, 231, 184);

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public StormOverlay(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Snapshot s = plugin.getSnapshot();
		boolean timerOn = config.showGameTimer() && s.inGame() && plugin.getTracker() != null
			&& plugin.getTracker().getGameStartMillis() > 0;
		boolean stormOn = config.showStorm() && s.phase == Phase.ENERGY && s.storm >= 0;
		if (!timerOn && !stormOn)
		{
			return null;
		}
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		int line = fm.getHeight();
		int top = timerOn ? line + 4 : 0;
		int height = PAD + top + (stormOn ? line + 4 + 8 + 4 + line : 0) + PAD - (stormOn ? 0 : 4);

		g.setColor(BG);
		g.fillRoundRect(0, 0, WIDTH, height, 8, 8);

		if (timerOn)
		{
			long ms = System.currentTimeMillis() - plugin.getTracker().getGameStartMillis();
			int secs = (int) (ms / 1000);
			String clock = secs / 60 + ":" + String.format("%02d", secs % 60);
			String phase = s.phase == Phase.ESSENCE ? "Spirit pool" : "Round " + s.round;
			long waveAt = plugin.getTracker().getWaveHitsAt();
			if (waveAt > System.currentTimeMillis())
			{
				phase = String.format("Wave hits in %.1fs", (waveAt - System.currentTimeMillis()) / 1000.0);
			}
			int ty = PAD + fm.getAscent();
			g.setColor(Color.WHITE);
			g.drawString(clock, PAD, ty);
			g.setColor(waveAt > System.currentTimeMillis() ? (s.tethered ? new Color(122, 212, 138) : config.urgentColor()) : MUTED);
			g.drawString(phase, WIDTH - PAD - fm.stringWidth(phase), ty);
		}
		if (!stormOn)
		{
			return new Dimension(WIDTH, height);
		}

		int y = PAD + top + fm.getAscent();
		boolean danger = s.storm >= config.stormDanger();
		g.setColor(MUTED);
		g.drawString("Storm intensity", PAD, y);
		String pct = s.storm + "%";
		g.setColor(danger ? config.urgentColor() : STORM);
		g.drawString(pct, WIDTH - PAD - fm.stringWidth(pct), y);

		int barY = PAD + top + line + 4;
		int barW = WIDTH - PAD * 2;
		g.setColor(TRACK);
		g.fillRoundRect(PAD, barY, barW, 8, 4, 4);
		g.setColor(danger ? config.urgentColor() : STORM);
		g.fillRoundRect(PAD, barY, barW * Math.min(100, s.storm) / 100, 8, 4, 4);
		int markX = PAD + barW * config.stormDanger() / 100;
		g.setColor(config.urgentColor());
		g.fillRect(markX - 1, barY - 2, 2, 12);

		y = barY + 8 + 4 + fm.getAscent();
		String left;
		Color leftColor;
		if (s.waveIncoming)
		{
			left = s.tethered ? "Wave! You're tethered" : "Wave incoming: tether now";
			leftColor = s.tethered ? WAVE : config.urgentColor();
		}
		else if (s.stormSecondsLeft > 0)
		{
			left = "Fills in ~" + s.stormSecondsLeft + "s";
			leftColor = s.stormSecondsLeft < 30 ? config.urgentColor() : WAVE;
		}
		else
		{
			left = "Steady";
			leftColor = MUTED;
		}
		g.setColor(leftColor);
		g.drawString(left, PAD, y);
		String right = s.secondsToCrate >= 0 ? "Crate " + s.secondsToCrate + "s away" : "Last trip by " + config.stormDanger() + "%";
		g.setColor(MUTED);
		g.drawString(right, WIDTH - PAD - fm.stringWidth(right), y);

		return new Dimension(WIDTH, height);
	}
}

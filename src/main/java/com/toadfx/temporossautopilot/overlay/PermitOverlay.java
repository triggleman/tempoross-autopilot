package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Snapshot;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;

/** Points, permits earned and how far to the next permit. */
public class PermitOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(240, 180, 76);
	private static final Color MUTED = new Color(170, 170, 170);

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public PermitOverlay(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Snapshot s = plugin.getSnapshot();
		if (!config.showPermits() || !(s.inGame() || s.phase == Phase.WON))
		{
			return null;
		}
		panelComponent.setPreferredSize(new Dimension(170, 0));
		int permits = Permits.earned(s.points);

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Points").right(String.format("%,d", s.points)).build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Permits").right(String.valueOf(permits)).rightColor(GOLD).build());

		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMinimum(0);
		bar.setMaximum(100);
		bar.setValue(Permits.progressPercent(s.points));
		bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
		bar.setCenterLabel(permits == 0 ? "First permit" : Permits.bonusChance(s.points) + "% bonus roll");
		bar.setForegroundColor(GOLD);
		bar.setBackgroundColor(new Color(58, 58, 58));
		panelComponent.getChildren().add(bar);

		panelComponent.getChildren().add(LineComponent.builder()
			.left(String.format("%,d", Permits.toNext(s.points)) + " to permit " + (permits + 1))
			.leftColor(MUTED)
			.build());
		return super.render(g);
	}
}

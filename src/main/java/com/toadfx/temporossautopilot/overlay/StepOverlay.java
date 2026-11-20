package com.toadfx.temporossautopilot.overlay;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Urgency;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** The next-step box: what to do now, why, and your fish count. */
public class StepOverlay extends OverlayPanel
{
	private static final Color MUTED = new Color(170, 170, 170);
	private static final Color SOON = new Color(255, 210, 0);

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;

	@Inject
	public StepOverlay(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Snapshot s = plugin.getSnapshot();
		Instruction step = plugin.getInstruction();
		if (!config.showStep() || s.phase == Phase.IDLE || step.isEmpty())
		{
			return null;
		}
		panelComponent.setPreferredSize(new Dimension(220, 0));

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Tempoross Autopilot · " + config.gameMode())
			.leftColor(config.targetColor())
			.build());
		Color color = step.getUrgency() == Urgency.NOW ? config.urgentColor()
			: step.getUrgency() == Urgency.SOON ? SOON : Color.WHITE;
		panelComponent.getChildren().add(TitleComponent.builder().text(step.getTitle()).color(color).build());
		if (!step.getDetail().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().left(step.getDetail()).leftColor(MUTED).build());
		}

		if (config.showFishCount() && s.inGame())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Raw " + s.raw + "  Cooked " + (s.cooked + s.crystal))
				.right("Loaded " + s.loadedThisRound)
				.leftColor(Color.WHITE)
				.rightColor(config.targetColor())
				.build());
		}
		return super.render(g);
	}
}

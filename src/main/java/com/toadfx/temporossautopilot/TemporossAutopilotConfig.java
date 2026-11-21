package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.model.CookRule;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(TemporossAutopilotConfig.GROUP)
public interface TemporossAutopilotConfig extends Config
{
	String GROUP = "temporossautopilot";

	enum PathStyle
	{
		LINE("Line"),
		TILES("Tiles"),
		LINE_AND_TILES("Line and tiles"),
		OFF("Off");

		private final String label;

		PathStyle(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(name = "Play style", description = "What you're at Tempoross for", position = 0)
	String styleSection = "style";

	@ConfigSection(name = "Mass and solo", description = "Settings for the way you play", position = 1)
	String modeSection = "mode";

	@ConfigSection(name = "Overlays", description = "What to show on screen", position = 2)
	String overlaySection = "overlays";

	@ConfigSection(name = "Path", description = "The walking line to your next step", position = 3)
	String pathSection = "path";

	@ConfigSection(name = "Colours", description = "Highlight colours", position = 4, closedByDefault = true)
	String colourSection = "colours";

	@ConfigSection(name = "Alerts", description = "Notifications", position = 5)
	String alertSection = "alerts";

	@ConfigSection(name = "Report card", description = "Post-game grade, tips and history", position = 6)
	String coachSection = "coaching";

	// ------------------------------------------------------------------ play style

	@ConfigItem(keyName = "playStyle", name = "Play style", position = 0, section = styleSection,
		description = "How you play. Styles depend on the mode: pick Solo for the wiki solo routes. A style that doesn't fit the mode is swapped for that mode's recommended one.")
	default PlayStyle playStyle()
	{
		return PlayStyle.RECOMMENDED_MASS;
	}

	@ConfigItem(keyName = "pointsTarget", name = "Points target", position = 1, section = styleSection,
		description = "Points to aim for in Max points, Firefighter and Custom. Permits come at 2,000, then every 700.")
	@Range(min = 2000, max = 32000)
	default int pointsTarget()
	{
		return 4800;
	}

	@ConfigItem(keyName = "customCook", name = "Custom: cooking", position = 2, section = styleSection,
		description = "When to cook in the Custom style")
	default CookRule customCook()
	{
		return CookRule.ALWAYS;
	}

	@ConfigItem(keyName = "barehanded", name = "Barehanded fishing", position = 3, section = styleSection,
		description = "Don't ask you to grab a harpoon (needs 55 Fishing and Barbarian Training)")
	default boolean barehanded()
	{
		return false;
	}

	@ConfigItem(keyName = "gearReminders", name = "Gear reminders", position = 4, section = styleSection,
		description = "Remind you to bring your best harpoon and wear your angler or spirit angler outfit, if you own them")
	default boolean gearReminders()
	{
		return true;
	}

	@ConfigItem(keyName = "bucketsWanted", name = "Buckets to carry", position = 5, section = styleSection,
		description = "Buckets to take before the game (Firefighter always takes at least 4)")
	@Range(min = 0, max = 10)
	default int bucketsWanted()
	{
		return 2;
	}

	// ------------------------------------------------------------------ mode

	@ConfigItem(keyName = "gameMode", name = "Mode", position = 0, section = modeSection,
		description = "Mass world, solo or duo")
	default GameMode gameMode()
	{
		return GameMode.MASS;
	}

	@ConfigItem(keyName = "stormDanger", name = "Storm danger mark", position = 2, section = modeSection,
		description = "Storm intensity where Autopilot says load now. The wiki's last safe trip from the shrine is 93%.")
	@Range(min = 50, max = 99)
	@Units(Units.PERCENT)
	default int stormDanger()
	{
		return 93;
	}

	// ------------------------------------------------------------------ overlays

	@ConfigItem(keyName = "showTopBox", name = "Top instructions box", position = 0, section = overlaySection,
		description = "Box at the top of the screen: what to do now, the game timer, the storm, what's coming up and the next steps")
	default boolean showTopBox()
	{
		return true;
	}

	@ConfigItem(keyName = "showUpcoming", name = "Top box: coming up", position = 1, section = overlaySection,
		description = "Show wave, fire and storm timers and the next steps in the top box")
	default boolean showUpcoming()
	{
		return true;
	}

	@ConfigItem(keyName = "showSocials", name = "Channel links in panel", position = 13, section = overlaySection,
		description = "Show the ToadFx YouTube, Twitch, Kick and TikTok buttons at the bottom of the side panel (takes effect after restarting the plugin)")
	default boolean showSocials()
	{
		return true;
	}

	@ConfigItem(keyName = "showStep", name = "Small step box (top left)", position = 12, section = overlaySection,
		description = "An extra small box with the next step and your fish count, if you'd rather have it at the side")
	default boolean showStep()
	{
		return false;
	}

	@ConfigItem(keyName = "showPermits", name = "Points and permits box", position = 1, section = overlaySection,
		description = "Points, permits earned and points to the next permit")
	default boolean showPermits()
	{
		return true;
	}

	@ConfigItem(keyName = "showFishCount", name = "Fish counter", position = 3, section = overlaySection,
		description = "Raw, cooked and loaded fish this round")
	default boolean showFishCount()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightTarget", name = "Highlight next target", position = 4, section = overlaySection,
		description = "Outline what the next step points at, with a label")
	default boolean highlightTarget()
	{
		return true;
	}

	@ConfigItem(keyName = "showLoadCounter", name = "Cannon load counter", position = 5, section = overlaySection,
		description = "Fish loaded this round above the crate (and the target on solo routes)")
	default boolean showLoadCounter()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightDouble", name = "Highlight double spots", position = 6, section = overlaySection,
		description = "Always outline the double harpoonfish spot")
	default boolean highlightDouble()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightFires", name = "Highlight fires", position = 7, section = overlaySection,
		description = "Outline every fire")
	default boolean highlightFires()
	{
		return true;
	}

	@ConfigItem(keyName = "showGameTimer", name = "Game timer", position = 9, section = overlaySection,
		description = "Game time and round in the top box")
	default boolean showGameTimer()
	{
		return true;
	}

	@ConfigItem(keyName = "showFireTimers", name = "Lightning and fire timers", position = 10, section = overlaySection,
		description = "Countdown on lightning clouds until the fire lands, and on fires until they spread")
	default boolean showFireTimers()
	{
		return true;
	}

	@ConfigItem(keyName = "showWaveTimers", name = "Wave timers on masts", position = 11, section = overlaySection,
		description = "Countdown on masts and totem poles until the wave hits (green once you're tethered)")
	default boolean showWaveTimers()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightBroken", name = "Highlight broken masts", position = 8, section = overlaySection,
		description = "Outline broken masts and totems")
	default boolean highlightBroken()
	{
		return true;
	}

	// ------------------------------------------------------------------ path

	@ConfigItem(keyName = "pathStyle", name = "Path to next step", position = 0, section = pathSection,
		description = "Draw the walking route to what the next step points at")
	default PathStyle pathStyle()
	{
		return PathStyle.LINE;
	}

	@ConfigItem(keyName = "minimapPath", name = "Path on minimap", position = 1, section = pathSection,
		description = "Also draw the route on the minimap")
	default boolean minimapPath()
	{
		return true;
	}

	@ConfigItem(keyName = "pathWidth", name = "Path width", position = 2, section = pathSection,
		description = "Line thickness")
	@Range(min = 1, max = 8)
	default int pathWidth()
	{
		return 3;
	}

	@ConfigItem(keyName = "roundCorners", name = "Rounded corners", position = 3, section = pathSection,
		description = "Smooth the line's corners")
	default boolean roundCorners()
	{
		return true;
	}

	@ConfigItem(keyName = "pathDetail", name = "Path detail", position = 4, section = pathSection,
		description = "1 = every tile of your walk. Higher merges tiles into longer straight stretches")
	@Range(min = 1, max = 30)
	default int pathDetail()
	{
		return 1;
	}

	@Alpha
	@ConfigItem(keyName = "pathColor", name = "Path colour", position = 5, section = pathSection,
		description = "The path's colour (it turns to the urgent colour when a step has to happen now)")
	default Color pathColor()
	{
		return new Color(255, 0, 241, 200);
	}

	// ------------------------------------------------------------------ colours

	@Alpha
	@ConfigItem(keyName = "targetColor", name = "Target", position = 0, section = colourSection, description = "Next step: the outline on what to do next and the step text")
	default Color targetColor()
	{
		return new Color(255, 0, 241);
	}

	@Alpha
	@ConfigItem(keyName = "doubleColor", name = "Double spot", position = 1, section = colourSection, description = "Double spot outline")
	default Color doubleColor()
	{
		return new Color(255, 115, 0);
	}

	@Alpha
	@ConfigItem(keyName = "fireColor", name = "Fire", position = 2, section = colourSection, description = "Fire outline")
	default Color fireColor()
	{
		return new Color(255, 106, 77);
	}

	@Alpha
	@ConfigItem(keyName = "timerColor", name = "Timers", position = 4, section = colourSection,
		description = "Lightning, fire and wave timers, and the countdowns in the top box")
	default Color timerColor()
	{
		return new Color(255, 235, 0);
	}

	@Alpha
	@ConfigItem(keyName = "urgentColor", name = "Urgent", position = 3, section = colourSection, description = "Colour for urgent steps (waves, storm)")
	default Color urgentColor()
	{
		return new Color(255, 30, 30);
	}

	// ------------------------------------------------------------------ alerts

	@ConfigItem(keyName = "notifyWave", name = "Wave incoming", position = 0, section = alertSection,
		description = "Notify when a colossal wave is coming and you aren't tethered")
	default boolean notifyWave()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyDouble", name = "Double spot", position = 1, section = alertSection,
		description = "Notify when a double harpoonfish spot appears")
	default boolean notifyDouble()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyStorm", name = "Storm warning", position = 2, section = alertSection,
		description = "Notify when the storm passes the warning level")
	default boolean notifyStorm()
	{
		return true;
	}

	@ConfigItem(keyName = "stormWarnAt", name = "Storm warning at", position = 3, section = alertSection,
		description = "Storm intensity for the warning")
	@Range(min = 30, max = 99)
	@Units(Units.PERCENT)
	default int stormWarnAt()
	{
		return 85;
	}

	@ConfigItem(keyName = "notifyLoadTarget", name = "Load target reached", position = 4, section = alertSection,
		description = "Notify when you've loaded a solo route's target for the round")
	default boolean notifyLoadTarget()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyPermit", name = "New permit", position = 5, section = alertSection,
		description = "Notify each time your points reach another permit")
	default boolean notifyPermit()
	{
		return false;
	}

	// ------------------------------------------------------------------ coaching

	@ConfigItem(keyName = "chatSummary", name = "Game summary in chat", position = 0, section = coachSection,
		description = "Post your grade, points and top tip in chat after each game")
	default boolean chatSummary()
	{
		return true;
	}

	@ConfigItem(keyName = "saveHistory", name = "Save game history", position = 1, section = coachSection,
		description = "Keep your last 50 games and personal bests (saved per character)")
	default boolean saveHistory()
	{
		return true;
	}

	@ConfigItem(keyName = "coachReset", name = "Reset history", position = 2, section = coachSection,
		description = "Tick to clear saved history and personal bests")
	default boolean coachReset()
	{
		return false;
	}
}

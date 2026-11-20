package com.toadfx.temporossautopilot.logic;

import com.toadfx.temporossautopilot.model.CookRule;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import com.toadfx.temporossautopilot.model.SoloMethod;

/** The player's choices that shape the plan. */
public class PlannerSettings
{
	public PlayStyle style = PlayStyle.MAX_PERMITS;
	public GameMode mode = GameMode.MASS;
	public SoloMethod soloMethod = SoloMethod.STYLE;
	/** Cooking rule for the Custom style. */
	public CookRule customCook = CookRule.ALWAYS;
	/** Points goal for Max points, Firefighter and Custom. */
	public int pointsTarget = 4800;
	/** Storm intensity (percent) treated as the last safe moment to start a trip to the crate. */
	public int stormDanger = 93;
	/** Skip the "grab a harpoon" check (barehanded fishing). */
	public boolean barehanded;
	/** Buckets to carry before the game starts. */
	public int bucketsWanted = 2;

	public CookRule cookRule()
	{
		return style == PlayStyle.CUSTOM ? customCook : style.getCookRule();
	}

	/** The batch size the wiki uses for the cooking method: 7 fish, or 9 at 85+ Fishing. */
	public static int batchSize(int fishingLevel)
	{
		return fishingLevel >= 85 ? 9 : 7;
	}
}

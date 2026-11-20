package com.toadfx.temporossautopilot.model;

/**
 * What the player is at Tempoross for. The next step, overlays and the post-game grade
 * all follow the chosen style.
 */
public enum PlayStyle
{
	RECOMMENDED_MASS("Recommended mass", "Theoatrix mass run: catch 9, cook, fill up at doubles, load a full bag", CookRule.OPENER_THEN_FULL_BAG, "permits per game"),
	MY_ROUTE("ToadFx mass route", "Your route: 8, cook, double till the wave, fill and cook, load, pool to 90%, then load at 35%", CookRule.OPENER_THEN_FULL_BAG, "permits per game"),
	MAX_PERMITS("Max permits / hr", "Wiki cooking method: cook 7 at a time (9 at 85+), load from 49% energy", CookRule.BATCH, "permits per hour"),
	MAX_POINTS("Max points / game", "Theoatrix max points: catch 8, cook while you wait for a double, fill up there", CookRule.OPENER_THEN_FULL_BAG, "points per game"),
	MAX_XP("Max Fishing XP", "Theoatrix XP: never cook, crystal harpoon, spirit angler outfit", CookRule.NEVER, "Fishing XP per hour"),
	FIREFIGHTER("Firefighter", "Douse fires and fix masts for 40 points each (Humidify helps)", CookRule.ALWAYS, "points per game"),
	CHILL("Chill: one permit", "Least clicking for 1 reward", CookRule.NEVER, "reaching 2,000 points"),
	CUSTOM("Custom", "Your own rules from settings", CookRule.ALWAYS, "your points target");

	private final String label;
	private final String tagline;
	private final CookRule cookRule;
	private final String gradedOn;

	PlayStyle(String label, String tagline, CookRule cookRule, String gradedOn)
	{
		this.label = label;
		this.tagline = tagline;
		this.cookRule = cookRule;
		this.gradedOn = gradedOn;
	}

	public String getLabel()
	{
		return label;
	}

	public String getTagline()
	{
		return tagline;
	}

	/** The style's default cooking rule. Custom uses the setting instead. */
	public CookRule getCookRule()
	{
		return cookRule;
	}

	public String getGradedOn()
	{
		return gradedOn;
	}

	/** Fish to catch and cook first, before filling the bag (Theoatrix: 9 on mass worlds, about 8 for max points). */
	public int openingBatch()
	{
		return this == MAX_POINTS ? 8 : this == MY_ROUTE ? 8 : 9;
	}

	/** Styles that go out of their way to douse fires and repair masts and totems. */
	public boolean fightsFires()
	{
		return this == FIREFIGHTER || this == MAX_POINTS;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

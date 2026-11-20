package com.toadfx.temporossautopilot.model;

/**
 * What the player is at Tempoross for. The next step, overlays and the post-game grade
 * all follow the chosen style.
 */
public enum PlayStyle
{
	MAX_PERMITS("Max permits / hr", "Fast games, more rewards per hour", CookRule.ALWAYS, "permits per hour"),
	MAX_POINTS("Max points / game", "Long rounds, most permits per game", CookRule.ALWAYS, "points per game"),
	MAX_XP("Max Fishing XP", "Never cook, just fish", CookRule.NEVER, "Fishing XP per hour"),
	BALANCED("Balanced XP + permits", "The wiki cooking method", CookRule.BATCH, "XP and permits together"),
	FIREFIGHTER("Firefighter", "Douse and repair for bonus points", CookRule.ALWAYS, "points per game"),
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

package com.toadfx.temporossautopilot.model;

/**
 * What the player is at Tempoross for. The next step, overlays and the post-game grade
 * all follow the chosen style.
 */
public enum PlayStyle
{
	// ---- Mass and duo
	RECOMMENDED_MASS("Recommended mass", "Theoatrix mass run: catch 9, cook, fill up at doubles, load a full bag", CookRule.OPENER_THEN_FULL_BAG, "permits per game", Modes.TEAM, SoloMethod.STYLE),
	MY_ROUTE("ToadFx mass route", "Your route: 8, cook, double till the wave, fill and cook, load, pool to 90%, then load at 35%", CookRule.OPENER_THEN_FULL_BAG, "permits per game", Modes.MASS, SoloMethod.STYLE),
	DUO_ROUTE("ToadFx duo route", "Your duo route: same start as your mass route, then take Tempoross down in thirds: 100 to 60, 60 to 30, 30 to 0", CookRule.OPENER_THEN_FULL_BAG, "permits per game", Modes.DUO, SoloMethod.STYLE),
	MAX_PERMITS("Max permits / hr", "Wiki cooking method: cook 7 at a time (9 at 85+), load from 49% energy", CookRule.BATCH, "permits per hour", Modes.TEAM, SoloMethod.STYLE),
	MAX_POINTS("Max points / game", "Theoatrix max points: catch 8, cook while you wait for a double, fill up there", CookRule.OPENER_THEN_FULL_BAG, "points per game", Modes.TEAM, SoloMethod.STYLE),
	MAX_XP("Max Fishing XP", "Theoatrix XP: never cook, crystal harpoon, spirit angler outfit", CookRule.NEVER, "Fishing XP per hour", Modes.TEAM, SoloMethod.STYLE),
	FIREFIGHTER("Firefighter", "Douse fires and fix masts for 40 points each (Humidify helps)", CookRule.ALWAYS, "points per game", Modes.TEAM, SoloMethod.STYLE),
	// ---- Solo (OSRS Wiki solo methods)
	SOLO_RECOMMENDED("Recommended solo", "Wiki 17 / 19 / 19 / 27: load 17, then 19, pool, 19, pool, 27. Humidify helps", CookRule.ALWAYS, "permits per hour", Modes.SOLO, SoloMethod.ROUTE_17_19_19_27),
	SOLO_FIREFIGHT("Solo max permits", "Wiki cooking + firefighting: 17, 19, pool, 19, pool to 50%, 2, and douse fires for points (Humidify helps)", CookRule.ALWAYS, "permits per game", Modes.SOLO, SoloMethod.FIREFIGHTING),
	SOLO_FIVE_ROUNDS("Solo 5 rounds", "Wiki 5-round method: 26, pool, 26, then 28 split 1 / 5 + 13 / keep 9, catch 9 more, finish. 8,500-9,500 points", CookRule.ALWAYS, "points per game", Modes.SOLO, SoloMethod.FIVE_ROUNDS),
	SOLO_TEN_PERMITS("Solo 10 permits", "Jagd's 10+ permits route: 16 (Tempoross to 10%), full bag, pool to 60%, full bag, pool to 33%, full bag dropped at cannon 1, 16 into cannon 2, pick up, load cannon 1", CookRule.ALWAYS, "points per game", Modes.SOLO, SoloMethod.TEN_PERMITS),
	SOLO_XP("Solo max Fishing XP", "Wiki no-cooking method: 26, then 27 raw a trip. Crystal harpoon", CookRule.NEVER, "Fishing XP per hour", Modes.SOLO, SoloMethod.NO_COOK),
	// ---- Any mode
	CHILL("Chill: one permit", "Least clicking for 1 reward", CookRule.NEVER, "reaching 2,000 points", Modes.ALL, SoloMethod.STYLE),
	CUSTOM("Custom", "Your own rules from settings", CookRule.ALWAYS, "your points target", Modes.ALL, SoloMethod.STYLE);

	/** Which modes a style is offered in. */
	private static final class Modes
	{
		static final GameMode[] MASS = {GameMode.MASS};
		static final GameMode[] DUO = {GameMode.DUO};
		static final GameMode[] TEAM = {GameMode.MASS, GameMode.DUO};
		static final GameMode[] SOLO = {GameMode.SOLO};
		static final GameMode[] ALL = {GameMode.MASS, GameMode.DUO, GameMode.SOLO};
	}

	private final String label;
	private final String tagline;
	private final CookRule cookRule;
	private final String gradedOn;
	private final GameMode[] modes;
	private final SoloMethod soloMethod;

	PlayStyle(String label, String tagline, CookRule cookRule, String gradedOn, GameMode[] modes, SoloMethod soloMethod)
	{
		this.label = label;
		this.tagline = tagline;
		this.cookRule = cookRule;
		this.gradedOn = gradedOn;
		this.modes = modes;
		this.soloMethod = soloMethod;
	}

	/** Whether this style is offered for a mode. */
	public boolean offeredIn(GameMode mode)
	{
		for (GameMode m : modes)
		{
			if (m == mode)
			{
				return true;
			}
		}
		return false;
	}

	/** The styles offered for a mode, recommended first. */
	public static java.util.List<PlayStyle> forMode(GameMode mode)
	{
		java.util.List<PlayStyle> out = new java.util.ArrayList<>();
		out.add(recommended(mode));
		for (PlayStyle s : values())
		{
			if (s.offeredIn(mode) && !out.contains(s))
			{
				out.add(s);
			}
		}
		return out;
	}

	/** The style to use when nothing better is picked for a mode. */
	public static PlayStyle recommended(GameMode mode)
	{
		return mode == GameMode.SOLO ? SOLO_RECOMMENDED : RECOMMENDED_MASS;
	}

	/** The chosen style if the mode offers it, else the mode's recommended style. */
	public static PlayStyle effective(PlayStyle chosen, GameMode mode)
	{
		return chosen != null && chosen.offeredIn(mode) ? chosen : recommended(mode);
	}

	/** The wiki solo route this style follows (STYLE if none). */
	public SoloMethod getSoloMethod()
	{
		return soloMethod;
	}

	/** The name to show for a mode ("Recommended duo" in duo). */
	public String labelFor(GameMode mode)
	{
		if (this == RECOMMENDED_MASS && mode == GameMode.DUO)
		{
			return "Recommended duo";
		}
		return label;
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
		return this == MAX_POINTS || this == MY_ROUTE || this == DUO_ROUTE ? 8 : 9;
	}

	/** What to bring for this style. */
	public Loadout loadout()
	{
		return Loadout.of(this);
	}

	/** Todd's own routes (mass and duo). */
	public boolean isToadFxRoute()
	{
		return this == MY_ROUTE || this == DUO_ROUTE;
	}

	/** Styles that go out of their way to douse fires and repair masts and totems. */
	public boolean fightsFires()
	{
		return this == FIREFIGHTER || this == MAX_POINTS || this == SOLO_FIREFIGHT;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

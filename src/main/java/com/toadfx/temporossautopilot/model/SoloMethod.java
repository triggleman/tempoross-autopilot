package com.toadfx.temporossautopilot.model;

/**
 * Solo routes from the OSRS Wiki's Tempoross strategies page. Each one is a list of how many
 * cooked fish to load per energy round. Loading more than the target makes Tempoross
 * vulnerable too early, so Autopilot stops you at the number.
 */
public enum SoloMethod
{
	/** Follow the play style; no fixed load counts. */
	STYLE("Follow play style", new int[0]),
	/** 17 / 19 / 19 / 27 (wiki: ~55-60 permits an hour). */
	ROUTE_17_19_19_27("17 / 19 / 19 / 27", new int[]{17, 19, 19, 27}),
	/** Five-round method: 26-27 per round, most points per game. */
	FIVE_ROUNDS("5 rounds (26 / 26 / 28)", new int[]{26, 26, 28}),
	/** Cooking + firefighting: ~57 fish over the game. */
	FIREFIGHTING("Cooking + firefighting (17 / 19 / 19)", new int[]{17, 19, 19}),
	/** No cooking: 26, then 27 a trip, all raw (max Fishing XP). */
	NO_COOK("No cooking (26 / 27 / 27 / 27)", new int[]{26, 27, 27, 27});

	private final String label;
	private final int[] loads;

	SoloMethod(String label, int[] loads)
	{
		this.label = label;
		this.loads = loads;
	}

	/** Fish to load in the given 1-based energy round, or -1 for no fixed target. */
	public int loadTarget(int round)
	{
		if (loads.length == 0 || round < 1)
		{
			return -1;
		}
		return loads[Math.min(round, loads.length) - 1];
	}

	public boolean hasTargets()
	{
		return loads.length > 0;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

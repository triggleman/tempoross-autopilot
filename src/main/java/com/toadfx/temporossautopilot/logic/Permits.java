package com.toadfx.temporossautopilot.logic;

/**
 * Points and reward permit maths from the OSRS Wiki Tempoross page.
 * <p>
 * One permit for the first 2,000 points, then one more for every 700 points after that.
 * Points left over give a matching percent chance at one extra permit.
 */
public final class Permits
{
	public static final int FIRST = 2000;
	public static final int STEP = 700;

	public static final int FISH = 5;
	public static final int COOK = 10;
	public static final int DEPOSIT_RAW = 20;
	public static final int DEPOSIT_COOKED = 65;
	public static final int REPAIR = 40;
	public static final int DOUSE = 40;
	public static final int WAVE = 10;
	public static final int SCATTER = 55;

	public static final int ENERGY_RAW = 10;
	public static final int ENERGY_COOKED = 15;

	private Permits()
	{
	}

	/** Guaranteed permits for a score. */
	public static int earned(int points)
	{
		if (points < FIRST)
		{
			return 0;
		}
		return 1 + (points - FIRST) / STEP;
	}

	/** Points needed for the next guaranteed permit. */
	public static int nextAt(int points)
	{
		if (points < FIRST)
		{
			return FIRST;
		}
		return FIRST + earned(points) * STEP;
	}

	public static int toNext(int points)
	{
		return nextAt(points) - Math.max(0, points);
	}

	/** Percent chance (0-99) at one more permit from the leftover points. */
	public static int bonusChance(int points)
	{
		if (points < FIRST)
		{
			return 0;
		}
		return (points - FIRST) % STEP * 100 / STEP;
	}

	/** How far through the current permit step the player is, 0-100. */
	public static int progressPercent(int points)
	{
		if (points <= 0)
		{
			return 0;
		}
		if (points < FIRST)
		{
			return points * 100 / FIRST;
		}
		return bonusChance(points);
	}

	/** Cooked fish to load to reach the next permit, rounded up. */
	public static int cookedFishToNext(int points)
	{
		return (toNext(points) + DEPOSIT_COOKED - 1) / DEPOSIT_COOKED;
	}

	/** Expected permits including the bonus roll, e.g. 4.74. */
	public static double expected(int points)
	{
		return earned(points) + bonusChance(points) / 100.0;
	}
}

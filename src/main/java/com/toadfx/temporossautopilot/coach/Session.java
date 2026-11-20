package com.toadfx.temporossautopilot.coach;

/** Totals since the plugin started (or the session was reset). */
public class Session
{
	private long startMillis = System.currentTimeMillis();
	private int games;
	private int wins;
	private int permits;
	private int points;
	private int fishingXp;

	public void add(GameRecord r)
	{
		games++;
		if (r.won)
		{
			wins++;
		}
		permits += r.permits();
		points += r.points;
		fishingXp += r.fishingXp;
	}

	public void reset()
	{
		startMillis = System.currentTimeMillis();
		games = wins = permits = points = fishingXp = 0;
	}

	public int getGames()
	{
		return games;
	}

	public int getWins()
	{
		return wins;
	}

	public int getPermits()
	{
		return permits;
	}

	public int getPoints()
	{
		return points;
	}

	public int getFishingXp()
	{
		return fishingXp;
	}

	public double hours()
	{
		return Math.max(1, System.currentTimeMillis() - startMillis) / 3_600_000.0;
	}

	public double permitsPerHour()
	{
		return games == 0 ? 0 : permits / hours();
	}

	public double permitsPerGame()
	{
		return games == 0 ? 0 : permits / (double) games;
	}
}

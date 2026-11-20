package com.toadfx.temporossautopilot.logic;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Watches storm intensity tick by tick and estimates how long until it reaches a mark.
 * The storm resets when Tempoross submerges, so a drop clears the history.
 */
public class StormClock
{
	private static final int WINDOW = 25;
	private static final double SECONDS_PER_TICK = 0.6;

	private final Deque<Integer> samples = new ArrayDeque<>();

	public void reset()
	{
		samples.clear();
	}

	/** Records the storm percent for this tick (-1 = unknown). */
	public void sample(int storm)
	{
		if (storm < 0)
		{
			return;
		}
		Integer last = samples.peekLast();
		if (last != null && storm < last)
		{
			samples.clear();
		}
		samples.addLast(storm);
		while (samples.size() > WINDOW)
		{
			samples.removeFirst();
		}
	}

	/** Percent per tick over the window, or 0 if flat or unknown. */
	public double ratePerTick()
	{
		if (samples.size() < 5)
		{
			return 0;
		}
		int first = samples.peekFirst();
		int last = samples.peekLast();
		double rate = (last - first) / (double) (samples.size() - 1);
		return rate > 0 ? rate : 0;
	}

	/** Seconds until the storm reaches {@code mark} percent, 0 if past it, -1 if not rising. */
	public int secondsUntil(int mark)
	{
		Integer last = samples.peekLast();
		if (last == null)
		{
			return -1;
		}
		if (last >= mark)
		{
			return 0;
		}
		double rate = ratePerTick();
		if (rate <= 0)
		{
			return -1;
		}
		return (int) Math.round((mark - last) / rate * SECONDS_PER_TICK);
	}

	/** Seconds to run a number of tiles (2 tiles a tick). */
	public static int runSeconds(int tiles)
	{
		if (tiles < 0)
		{
			return -1;
		}
		return (int) Math.ceil(tiles / 2.0 * SECONDS_PER_TICK);
	}
}

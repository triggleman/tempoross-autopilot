package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.logic.StormClock;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PermitsTest
{
	@Test
	public void firstPermitAt2000()
	{
		assertEquals(0, Permits.earned(1999));
		assertEquals(1, Permits.earned(2000));
		assertEquals(2000, Permits.nextAt(0));
		assertEquals(2700, Permits.nextAt(2000));
	}

	@Test
	public void thenEvery700()
	{
		assertEquals(2, Permits.earned(2840));
		assertEquals(560, Permits.toNext(2840));
		assertEquals(20, Permits.bonusChance(2840));
		assertEquals(4, Permits.earned(4620));
		assertEquals(74, Permits.bonusChance(4620));
		assertEquals(180, Permits.toNext(4620));
		assertEquals(3, Permits.cookedFishToNext(4620));
	}

	@Test
	public void progressBeforeFirstPermit()
	{
		assertEquals(50, Permits.progressPercent(1000));
		assertEquals(0, Permits.bonusChance(1000));
	}

	@Test
	public void stormClockEstimatesTimeLeft()
	{
		StormClock c = new StormClock();
		for (int i = 0; i <= 10; i++)
		{
			c.sample(50 + i);
		}
		// 1% a tick, 40% to go = 40 ticks = 24 s.
		assertEquals(24, c.secondsUntil(100));
		c.sample(0);
		assertEquals(-1, c.secondsUntil(100));
		assertEquals(3, StormClock.runSeconds(10));
	}
}

package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Lookahead;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Target;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** Todd's duo route. */
public class DuoRouteTest
{
	private static PlannerSettings duo()
	{
		PlannerSettings p = new PlannerSettings();
		p.mode = GameMode.DUO;
		p.style = PlayStyle.DUO_ROUTE;
		return p;
	}

	private static Snapshot start()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 100;
		s.storm = 5;
		s.hasHarpoon = true;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 3;
		s.fishingLevel = 80;
		s.freeSlots = 24;
		return s;
	}

	private static Snapshot pool(int round, int essence)
	{
		Snapshot s = start();
		s.phase = Phase.ESSENCE;
		s.spiritPool = true;
		s.energy = 0;
		s.round = round;
		s.essence = essence;
		return s;
	}

	@Test
	public void offeredInDuoOnly()
	{
		assertTrue(PlayStyle.forMode(GameMode.DUO).contains(PlayStyle.DUO_ROUTE));
		assertTrue(!PlayStyle.forMode(GameMode.MASS).contains(PlayStyle.DUO_ROUTE));
		assertTrue(!PlayStyle.forMode(GameMode.SOLO).contains(PlayStyle.DUO_ROUTE));
		assertEquals(8, PlayStyle.DUO_ROUTE.openingBatch());
	}

	@Test
	public void sameOpenerAsMassRoute()
	{
		Snapshot s = start();
		s.raw = 5;
		s.freeSlots = 19;
		s.doubleSpot = true;
		assertEquals("Fish harpoonfish", Planner.plan(s, duo()).getTitle().replace("Fish the double spot", "Fish harpoonfish"));
		s.raw = 8;
		s.freeSlots = 16;
		assertEquals(Target.SHRINE, Planner.plan(s, duo()).getTarget());
		s.raw = 0;
		s.cooked = 8;
		assertEquals("Fish the double spot", Planner.plan(s, duo()).getTitle());
		assertEquals("Stay here until the wave comes.", Planner.plan(s, duo()).getDetail());
	}

	@Test
	public void fillCookLoadRepeatUntilZero()
	{
		Snapshot s = start();
		s.wavesThisRound = 1;
		s.cooked = 8;
		s.freeSlots = 4;
		s.raw = 12;
		assertEquals("Fish until full", Planner.plan(s, duo()).getTitle());
		s.freeSlots = 0;
		s.raw = 16;
		assertEquals(Target.SHRINE, Planner.plan(s, duo()).getTarget());
		s.raw = 0;
		s.cooked = 24;
		assertEquals(Target.CRATE, Planner.plan(s, duo()).getTarget());
		// Loaded it all but Tempoross is still up: go again.
		s.cooked = 0;
		s.freeSlots = 24;
		s.loadedThisRound = 24;
		s.nearCrate = false;
		s.energy = 40;
		assertEquals("Fish until full", Planner.plan(s, duo()).getTitle());
		s.energy = 0;
		Instruction go = Planner.plan(s, duo());
		assertEquals("Head to the spirit pool", go.getTitle());
		assertTrue(go.getDetail().contains("60%"));
	}

	@Test
	public void poolInThirds()
	{
		assertEquals("Attack the spirit pool", Planner.plan(pool(1, 80), duo()).getTitle());
		assertTrue(Planner.plan(pool(1, 80), duo()).getDetail().contains("60%"));
		assertEquals("Back to fishing", Planner.plan(pool(1, 60), duo()).getTitle());

		assertEquals("Attack the spirit pool", Planner.plan(pool(2, 45), duo()).getTitle());
		assertTrue(Planner.plan(pool(2, 45), duo()).getDetail().contains("30%"));
		assertEquals("Back to fishing", Planner.plan(pool(2, 29), duo()).getTitle());

		Instruction last = Planner.plan(pool(3, 10), duo());
		assertEquals("Attack the spirit pool", last.getTitle());
		assertTrue(last.getDetail().contains("subdued"));
	}

	@Test
	public void afterShareGetFullCookedBagForNextRound()
	{
		Snapshot s = pool(1, 58);
		s.raw = 24;
		s.freeSlots = 0;
		assertEquals("Cook at the shrine", Planner.plan(s, duo()).getTitle());
		s.raw = 0;
		s.cooked = 24;
		assertEquals("Wait for the next round", Planner.plan(s, duo()).getTitle());
		// Tempoross gets back up: round 2 loads the full cooked bag.
		s.phase = Phase.ENERGY;
		s.spiritPool = false;
		s.round = 2;
		s.energy = 100;
		assertEquals("Load the cannon", Planner.plan(s, duo()).getTitle());
	}

	@Test
	public void prepThreeBucketsAndNoHammerWithSpiritSet()
	{
		Snapshot s = start();
		s.phase = Phase.WAITING;
		s.waterBuckets = 1;
		assertEquals("Grab buckets", Planner.plan(s, duo()).getTitle());
		s.waterBuckets = 3;
		s.hasHammer = false;
		assertEquals("Grab a hammer", Planner.plan(s, duo()).getTitle());
		for (int i = 0; i < 4; i++)
		{
			s.ownsSpirit[i] = true;
			s.wearsSpirit[i] = true;
		}
		assertEquals("Ready", Planner.plan(s, duo()).getTitle());
	}

	@Test
	public void lookaheadRunsThroughTheRounds()
	{
		List<Instruction> next = Lookahead.next(pool(1, 80), duo(), 3);
		assertTrue(next.size() >= 2);
		assertEquals("Back to fishing", next.get(0).getTitle());
	}
}

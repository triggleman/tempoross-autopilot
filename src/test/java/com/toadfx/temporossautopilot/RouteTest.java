package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Lookahead;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Target;
import com.toadfx.temporossautopilot.model.PlayStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** Todd's own mass route. */
public class RouteTest
{
	private static PlannerSettings route()
	{
		PlannerSettings p = new PlannerSettings();
		p.style = PlayStyle.MY_ROUTE;
		return p;
	}

	private static Snapshot start()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 100;
		s.storm = 5;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 2;
		s.fishingLevel = 80;
		return s;
	}

	@Test
	public void roundOneOpener()
	{
		Snapshot s = start();
		s.raw = 7;
		s.freeSlots = 17;
		s.doubleSpot = true;
		assertEquals("Opener first, even with a double up", "Fish the double spot", Planner.plan(s, route()).getTitle());
		s.raw = 8;
		s.freeSlots = 16;
		assertEquals(Target.SHRINE, Planner.plan(s, route()).getTarget());
	}

	@Test
	public void finishCookingTheOpener()
	{
		Snapshot s = start();
		s.raw = 4;
		s.cooked = 4;
		s.freeSlots = 16;
		assertEquals("Keep cooking", Planner.plan(s, route()).getTitle());
		s.raw = 0;
		s.cooked = 8;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, route()).getTarget());
	}

	@Test
	public void keepCookingWhileCookingAnyStyle()
	{
		Snapshot s = start();
		s.round = 2;
		s.energy = 70;
		s.raw = 10;
		s.cooked = 18;
		s.freeSlots = 0;
		s.cooking = true;
		assertEquals("Keep cooking", Planner.plan(s, route()).getTitle());
		PlannerSettings b = new PlannerSettings();
		b.style = PlayStyle.MAX_PERMITS;
		Snapshot t = start();
		t.raw = 3;
		t.cooked = 4;
		t.freeSlots = 21;
		t.cooking = true;
		assertEquals("Keep cooking", Planner.plan(t, b).getTitle());
		t.cooking = false;
		assertEquals(Target.FISHING_SPOT, Planner.plan(t, b).getTarget());
	}

	@Test
	public void doubleUntilTheWaveThenFill()
	{
		Snapshot s = start();
		s.cooked = 7;
		s.freeSlots = 17;
		s.doubleSpot = true;
		assertEquals("Stay here until the wave comes.", Planner.plan(s, route()).getDetail());
		s.doubleSpot = false;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, route()).getTarget());
		s.wavesThisRound = 1;
		s.raw = 10;
		s.freeSlots = 7;
		assertEquals("Fish until full", Planner.plan(s, route()).getTitle());
		s.raw = 17;
		s.freeSlots = 0;
		assertEquals(Target.SHRINE, Planner.plan(s, route()).getTarget());
		s.raw = 0;
		s.cooked = 24;
		assertEquals(Target.CRATE, Planner.plan(s, route()).getTarget());
		// Part way through loading.
		s.cooked = 10;
		s.freeSlots = 14;
		s.nearCrate = true;
		s.loadedThisRound = 14;
		assertEquals(Target.CRATE, Planner.plan(s, route()).getTarget());
		s.cooked = 0;
		s.freeSlots = 24;
		s.loadedThisRound = 24;
		assertEquals("Head to the spirit pool", Planner.plan(s, route()).getTitle());
	}

	@Test
	public void firstPoolUntil90()
	{
		Snapshot s = start();
		s.phase = Phase.ESSENCE;
		s.spiritPool = true;
		s.energy = 60;
		assertEquals("Attack the spirit pool", Planner.plan(s, route()).getTitle());
		s.energy = 90;
		assertEquals("Back to fishing", Planner.plan(s, route()).getTitle());
	}

	@Test
	public void roundTwoFullCookedBagLoadsEarlyThenWaitsAtPool()
	{
		Snapshot s = start();
		s.round = 2;
		s.energy = 70;
		s.cooked = 28;
		s.freeSlots = 0;
		assertEquals(Target.CRATE, Planner.plan(s, route()).getTarget());
		s.cooked = 12;
		s.freeSlots = 16;
		s.nearCrate = true;
		s.loadedThisRound = 16;
		assertEquals("Keep loading till empty", Target.CRATE, Planner.plan(s, route()).getTarget());
		s.cooked = 0;
		s.freeSlots = 28;
		s.loadedThisRound = 28;
		s.nearCrate = false;
		Instruction wait = Planner.plan(s, route());
		assertEquals("Wait at the spirit pool", wait.getTitle());
		assertEquals(Target.SPIRIT_POOL, wait.getTarget());
	}

	@Test
	public void roundTwoLoadsAt35ThenPoolTillTheEnd()
	{
		Snapshot s = start();
		s.round = 2;
		s.energy = 60;
		s.raw = 5;
		s.freeSlots = 19;
		assertEquals("Fish harpoonfish", Planner.plan(s, route()).getTitle());
		s.energy = 35;
		Instruction load = Planner.plan(s, route());
		assertEquals(Target.CRATE, load.getTarget());
		assertTrue(load.getDetail().contains("everything"));
		s.phase = Phase.ESSENCE;
		s.spiritPool = true;
		s.energy = 95;
		s.raw = 0;
		assertEquals("Stay until Tempoross is subdued, then leave.", Planner.plan(s, route()).getDetail());
		s.phase = Phase.WON;
		assertEquals(Target.LEAVE, Planner.plan(s, route()).getTarget());
	}

	@Test
	public void lookaheadFollowsTheRoute()
	{
		Snapshot s = start();
		List<Instruction> next = Lookahead.next(s, route(), 2);
		assertEquals("Cook at the shrine", next.get(0).getTitle());
		assertEquals("Fish the double spot", next.size() > 1 && next.get(1).getTitle().startsWith("Fish") ? "Fish the double spot" : next.get(1).getTitle());
	}
}

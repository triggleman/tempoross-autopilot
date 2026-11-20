package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Target;
import com.toadfx.temporossautopilot.logic.Urgency;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import com.toadfx.temporossautopilot.model.SoloMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PlannerTest
{
	private static Snapshot energy()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 80;
		s.essence = 100;
		s.storm = 20;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 2;
		s.fishingLevel = 70;
		return s;
	}

	private static PlannerSettings style(PlayStyle st)
	{
		PlannerSettings p = new PlannerSettings();
		p.style = st;
		return p;
	}

	@Test
	public void waveWithoutTetherIsUrgent()
	{
		Snapshot s = energy();
		s.waveIncoming = true;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals(Target.TETHER, i.getTarget());
		assertEquals(Urgency.NOW, i.getUrgency());

		s.tethered = true;
		assertEquals("Hold on", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
	}

	@Test
	public void brokenMastNearbyGetsRepairedBeforeTethering()
	{
		Snapshot s = energy();
		s.waveIncoming = true;
		s.damagedTetherDistance = 3;
		assertEquals(Target.DAMAGED_TETHER, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTarget());
	}

	@Test
	public void doubleSpotBeatsCooking()
	{
		Snapshot s = energy();
		s.raw = 5;
		s.freeSlots = 15;
		s.doubleSpot = true;
		assertEquals(Target.DOUBLE_SPOT, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTarget());
	}

	@Test
	public void cooksABatchWhenNoDouble()
	{
		Snapshot s = energy();
		s.raw = 7;
		s.freeSlots = 13;
		assertEquals(Target.SHRINE, Planner.plan(s, style(PlayStyle.BALANCED)).getTarget());
		s.raw = 6;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, style(PlayStyle.BALANCED)).getTarget());
		s.fishingLevel = 90;
		s.raw = 8;
		assertEquals("9 at 85+ Fishing", Target.FISHING_SPOT, Planner.plan(s, style(PlayStyle.BALANCED)).getTarget());
	}

	@Test
	public void theoatrixMassRunCooksOpenerThenFullBag()
	{
		PlannerSettings p = style(PlayStyle.MAX_PERMITS);
		Snapshot s = energy();
		s.raw = 8;
		s.freeSlots = 20;
		assertEquals("Opener is 9", Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
		s.raw = 9;
		s.freeSlots = 19;
		assertEquals(Target.SHRINE, Planner.plan(s, p).getTarget());
		s.raw = 10;
		s.cooked = 9;
		s.freeSlots = 9;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
		s.raw = 19;
		s.freeSlots = 0;
		assertEquals(Target.SHRINE, Planner.plan(s, p).getTarget());
		s.raw = 0;
		s.cooked = 28;
		assertEquals(Target.CRATE, Planner.plan(s, p).getTarget());
		Snapshot m = energy();
		m.raw = 8;
		m.freeSlots = 20;
		assertEquals(Target.SHRINE, Planner.plan(m, style(PlayStyle.MAX_POINTS)).getTarget());
	}

	@Test
	public void xpStyleNeverCooks()
	{
		Snapshot s = energy();
		s.raw = 20;
		s.freeSlots = 0;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_XP));
		assertEquals(Target.CRATE, i.getTarget());
		assertTrue(i.getDetail().contains("+400"));
	}

	@Test
	public void fullBagOfRawGetsCookedFirst()
	{
		Snapshot s = energy();
		s.raw = 20;
		s.freeSlots = 0;
		assertEquals(Target.SHRINE, Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTarget());
		s.raw = 0;
		s.cooked = 20;
		assertEquals(Target.CRATE, Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTarget());
	}

	@Test
	public void stormForcesLoad()
	{
		Snapshot s = energy();
		s.cooked = 8;
		s.freeSlots = 12;
		s.storm = 94;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals(Target.CRATE, i.getTarget());
		assertEquals(Urgency.NOW, i.getUrgency());

		s.storm = 70;
		s.stormSecondsLeft = 10;
		s.secondsToCrate = 8;
		assertEquals(Urgency.NOW, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getUrgency());
	}

	@Test
	public void soloRouteStopsAtTarget()
	{
		PlannerSettings p = style(PlayStyle.MAX_PERMITS);
		p.mode = GameMode.SOLO;
		p.soloMethod = SoloMethod.ROUTE_17_19_19_27;

		Snapshot s = energy();
		s.cooked = 17;
		s.freeSlots = 11;
		Instruction load = Planner.plan(s, p);
		assertEquals(Target.CRATE, load.getTarget());
		assertTrue(load.getDetail().contains("Stop at 17"));

		s.loadedThisRound = 17;
		s.cooked = 0;
		assertEquals("Round target met", Planner.plan(s, p).getTitle());

		s.round = 2;
		s.loadedThisRound = 0;
		s.cooked = 10;
		assertEquals("Needs 19 in round 2", Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
	}

	@Test
	public void massIgnoresSoloRoute()
	{
		PlannerSettings p = style(PlayStyle.MAX_PERMITS);
		p.soloMethod = SoloMethod.ROUTE_17_19_19_27;
		Snapshot s = energy();
		s.cooked = 17;
		s.freeSlots = 11;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
	}

	@Test
	public void firefighterDousesFires()
	{
		Snapshot s = energy();
		s.fireDistance = 8;
		assertEquals(Target.FIRE, Planner.plan(s, style(PlayStyle.FIREFIGHTER)).getTarget());
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTarget());
		s.fireDistance = 1;
		assertEquals(Target.FIRE, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTarget());
	}

	@Test
	public void chillRelaxesAfter2000()
	{
		Snapshot s = energy();
		s.points = 2050;
		assertEquals("You're safe", Planner.plan(s, style(PlayStyle.CHILL)).getTitle());
	}

	@Test
	public void spiritPoolPhase()
	{
		Snapshot s = energy();
		s.phase = Phase.ESSENCE;
		s.energy = 0;
		s.spiritPool = true;
		assertEquals(Target.SPIRIT_POOL, Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTarget());
	}

	@Test
	public void energyZeroHeadsToSpiritPool()
	{
		Snapshot s = energy();
		s.phase = Phase.ESSENCE;
		s.energy = 0;
		s.spiritPool = false;
		s.poolInactive = true;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals("Head to the spirit pool", i.getTitle());
		assertEquals(Target.SPIRIT_POOL, i.getTarget());
		s.spiritPool = true;
		assertEquals("Attack the spirit pool", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
	}

	@Test
	public void almostZeroEnergyWithNothingToLoadHeadsToPool()
	{
		Snapshot s = energy();
		s.energy = 2;
		assertEquals("Head to the spirit pool", Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTitle());
		s.cooked = 5;
		s.freeSlots = 23;
		assertEquals("Load what you have first", Target.CRATE, Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTarget());
	}

	@Test
	public void prepAsksForMissingGear()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.WAITING;
		s.hasHarpoon = true;
		assertEquals("Grab a rope", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
		s.hasRope = true;
		s.hasHammer = true;
		assertEquals("Grab buckets", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
		s.emptyBuckets = 2;
		assertEquals(Target.WATER_PUMP, Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTarget());
		s.emptyBuckets = 0;
		s.waterBuckets = 2;
		assertEquals("Ready", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
		assertEquals("Grab buckets", Planner.plan(s, style(PlayStyle.FIREFIGHTER)).getTitle());
	}

	@Test
	public void wonSaysLeaveForPermitsStyle()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.WON;
		s.points = 4620;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals(Target.LEAVE, i.getTarget());
		assertTrue(i.getDetail().contains("4 permits + 74% roll"));
	}
}

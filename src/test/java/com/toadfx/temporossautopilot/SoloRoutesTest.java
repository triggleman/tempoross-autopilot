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
import com.toadfx.temporossautopilot.model.SoloMethod;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** The wiki solo routes, leg by leg. */
public class SoloRoutesTest
{
	private static PlannerSettings solo(PlayStyle st)
	{
		PlannerSettings p = new PlannerSettings();
		p.mode = GameMode.SOLO;
		p.style = st;
		p.soloMethod = st.getSoloMethod();
		return p;
	}

	private static Snapshot up()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 100;
		s.essence = 100;
		s.storm = 10;
		s.hasHarpoon = true;
		s.hasRope = true;
		s.hasHammer = true;
		s.fishingLevel = 90;
		s.freeSlots = 28;
		return s;
	}

	private static Snapshot under(Snapshot s)
	{
		s.phase = Phase.ESSENCE;
		s.energy = 0;
		s.spiritPool = true;
		return s;
	}

	@Test
	public void legsCountOverTheWholeGame()
	{
		SoloMethod m = SoloMethod.FIVE_ROUNDS;
		assertEquals(0, m.legIndex(0));
		assertEquals(1, m.legIndex(26));
		assertEquals(2, m.legIndex(52));
		assertEquals(3, m.legIndex(53));
		assertEquals(4, m.legIndex(71));
		assertEquals(5, m.legIndex(89));
		assertEquals(3, SoloMethod.TEN_PERMITS.legIndex(16 + 19 + 19));
	}

	@Test
	public void fiveRoundsOneLoads18PausesThenTheRest()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_FIVE_ROUNDS);
		Snapshot s = up();
		s.raw = 10;
		s.freeSlots = 18;
		assertEquals("Cook at the shrine", Planner.plan(s, p).getTitle());
		s.raw = 0;
		s.cooked = 26;
		s.freeSlots = 2;
		Instruction load = Planner.plan(s, p);
		assertEquals(Target.CRATE, load.getTarget());
		assertTrue(load.getDetail().contains("Load 18 first"));
		s.loadedTotal = 18;
		s.cooked = 8;
		s.msSinceLoad = 500;
		assertEquals("Wait a moment", Planner.plan(s, p).getTitle());
		s.msSinceLoad = 3000;
		assertTrue(Planner.plan(s, p).getDetail().contains("Load the other 8"));
	}

	@Test
	public void fiveRoundsPoolRules()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_FIVE_ROUNDS);
		// After round 1: fish the pool until he resurfaces.
		Snapshot s = under(up());
		s.loadedTotal = 26;
		s.essence = 70;
		assertEquals("Attack the spirit pool", Planner.plan(s, p).getTitle());
		// After round 2: skip the pool, go catch 28.
		s.loadedTotal = 52;
		s.essence = 45;
		s.poolOpenEssence = 52;
		s.emptyBuckets = 2;
		s.freeSlots = 24;
		assertEquals("Drop your buckets", Planner.plan(s, p).getTitle());
		s.emptyBuckets = 0;
		s.freeSlots = 28;
		Instruction fish = Planner.plan(s, p);
		assertEquals(Target.FISHING_SPOT, fish.getTarget());
		assertTrue(fish.getDetail().contains("/ 28"));
		assertTrue(fish.getDetail().contains("Skip the spirit pool"));
		// ...unless essence was still above 60%: down to 55% first.
		s.poolOpenEssence = 64;
		s.essence = 62;
		assertEquals("Attack the spirit pool", Planner.plan(s, p).getTitle());
		s.essence = 55;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
	}

	@Test
	public void fiveRoundsRoundThree()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_FIVE_ROUNDS);
		Snapshot s = up();
		s.round = 3;
		s.energy = 8;
		s.loadedTotal = 52;
		s.cooked = 28;
		s.freeSlots = 0;
		assertEquals("Load 1 fish", Planner.plan(s, p).getTitle());
		s.loadedTotal = 53;
		s.cooked = 27;
		s.freeSlots = 1;
		assertEquals("Wait for him to go under", Planner.plan(s, p).getTitle());
		under(s);
		s.nearCrate = true;
		s.atFirstCrate = true;
		Instruction five = Planner.plan(s, p);
		assertEquals("Load 5 in one crate", five.getTitle());
		assertTrue(five.getDetail().contains("13 in the other"));
		s.loadedTotal = 58;
		s.cooked = 22;
		s.loadedHere = 5;
		Instruction other = Planner.plan(s, p);
		assertEquals("Go to the other crate", other.getTitle());
		assertEquals(Target.OTHER_CRATE, other.getTarget());
		s.atFirstCrate = false;
		s.loadedHere = 0;
		assertEquals("Load this crate", Planner.plan(s, p).getTitle());
		// 5 + 13 in: keep 9, catch and cook 9 more, no doubles.
		s.loadedTotal = 71;
		s.cooked = 9;
		s.freeSlots = 19;
		s.doubleSpot = true;
		Instruction more = Planner.plan(s, p);
		assertEquals(Target.FISHING_SPOT, more.getTarget());
		assertTrue(more.getDetail().contains("/ 18"));
		s.cooked = 18;
		s.freeSlots = 10;
		assertEquals("Wait for him to come back up", Planner.plan(s, p).getTitle());
		// Back up: 5 in one crate, the rest in the other, then finish.
		s.phase = Phase.ENERGY;
		s.spiritPool = false;
		s.energy = 40;
		s.nearCrate = false;
		assertEquals("Load 5 in one crate", Planner.plan(s, p).getTitle());
		s.loadedTotal = 89;
		s.cooked = 0;
		under(s);
		assertTrue(Planner.plan(s, p).getDetail().contains("subdued"));
	}

	@Test
	public void roundTwoLoads9Or10ByStartingEnergy()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_FIVE_ROUNDS);
		Snapshot s = up();
		s.round = 2;
		s.loadedTotal = 26;
		s.cooked = 26;
		s.freeSlots = 2;
		s.roundStartEnergy = 49;
		assertTrue(Planner.plan(s, p).getDetail().contains("Load 9 first"));
		s.roundStartEnergy = 55;
		assertTrue(Planner.plan(s, p).getDetail().contains("Load 10 first"));
	}

	@Test
	public void tenPermitsJagdRoute()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		Snapshot s = up();
		s.cooked = 16;
		s.freeSlots = 3;
		assertTrue(Planner.plan(s, p).getDetail().contains("Load exactly 16"));
		// 16 in: 10% energy, no waves. Put the fires out, then a full bag.
		s.loadedTotal = 16;
		s.energy = 10;
		s.cooked = 0;
		s.freeSlots = 19;
		s.waterBuckets = 7;
		s.fireDistance = 9;
		assertEquals("Put out the fires", Planner.plan(s, p).getTitle());
		s.fireDistance = -1;
		Instruction fish = Planner.plan(s, p);
		assertEquals(Target.FISHING_SPOT, fish.getTarget());
		assertTrue(fish.getDetail().contains("/ 19"));
		// After the full bag: pool to 63%.
		under(s);
		s.loadedTotal = 35;
		s.essence = 80;
		assertTrue(Planner.plan(s, p).getDetail().contains("63%"));
		s.essence = 62;
		assertEquals(Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
		// After the next full bag: pool to 33%.
		s.loadedTotal = 54;
		s.essence = 45;
		assertTrue(Planner.plan(s, p).getDetail().contains("33%"));
	}

	@Test
	public void finishTheLoadBeforeThePool()
	{
		// Tempoross goes under after a few of the 19: keep loading, then the pool.
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		Snapshot s = under(up());
		s.loadedTotal = 20;
		s.cooked = 15;
		s.freeSlots = 4;
		s.essence = 100;
		Instruction load = Planner.plan(s, p);
		assertEquals(Target.CRATE, load.getTarget());
		assertTrue(load.getDetail().contains("Finish the load"));
		s.loadedTotal = 35;
		s.cooked = 0;
		assertTrue(Planner.plan(s, p).getDetail().contains("63%"));
		// Storm salvage: only raw left, so pool first and load the rest after.
		s.loadedTotal = 28;
		s.raw = 7;
		assertEquals(Target.SPIRIT_POOL, Planner.plan(s, p).getTarget());
	}

	@Test
	public void tenPermitsCatchAll16BeforeCooking()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		Snapshot s = up();
		s.raw = 12;
		s.freeSlots = 7;
		Instruction i = Planner.plan(s, p);
		assertEquals(Target.FISHING_SPOT, i.getTarget());
		assertTrue(i.getDetail().contains("12 / 16"));
		s.raw = 16;
		s.freeSlots = 3;
		assertEquals("Cook at the shrine", Planner.plan(s, p).getTitle());
	}

	@Test
	public void tenPermitsNeverCooksEarlyAtLowLevel()
	{
		// Todd: after emptying the bag, at 7 fish it kept sending him to cook (starter batch at <85 Fishing).
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		for (int loaded : new int[]{0, 16, 35, 54})
		{
			Snapshot s = up();
			s.fishingLevel = 70;
			s.loadedTotal = loaded;
			s.raw = 7;
			s.freeSlots = 12;
			assertEquals("loaded " + loaded, Target.FISHING_SPOT, Planner.plan(s, p).getTarget());
		}
	}

	@Test
	public void tenPermitsStormSalvage()
	{
		// Storm at 93% before the full bag is cooked: load the cooked ones now (never raw).
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		Snapshot s = up();
		s.loadedTotal = 16;
		s.energy = 10;
		s.cooked = 12;
		s.raw = 7;
		s.freeSlots = 0;
		s.storm = 93;
		Instruction now = Planner.plan(s, p);
		assertEquals("Load now", now.getTitle());
		assertTrue(now.getDetail().contains("Load 12"));
	}

	@Test
	public void tenPermitsTwoCannonFinish()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_TEN_PERMITS);
		Snapshot s = up();
		s.round = 3;
		s.loadedTotal = 54;
		s.cooked = 19;
		s.freeSlots = 0;
		assertEquals("Drop your fish at cannon 1", Planner.plan(s, p).getTitle());
		s.groundFish = 19;
		s.cooked = 0;
		s.freeSlots = 19;
		Instruction fish = Planner.plan(s, p);
		assertEquals(Target.FISHING_SPOT, fish.getTarget());
		assertTrue(fish.getDetail().contains("/ 16"));
		s.cooked = 16;
		s.freeSlots = 3;
		s.nearCrate = true;
		s.atDropCrate = true;
		Instruction two = Planner.plan(s, p);
		assertEquals("Load cannon 2", two.getTitle());
		assertEquals(Target.OTHER_CRATE, two.getTarget());
		s.atDropCrate = false;
		assertEquals(Target.CRATE, Planner.plan(s, p).getTarget());
		s.loadedTotal = 70;
		s.cooked = 0;
		s.freeSlots = 19;
		Instruction pick = Planner.plan(s, p);
		assertEquals("Pick up your fish", pick.getTitle());
		assertEquals(Target.DROP_CRATE, pick.getTarget());
		s.groundFish = 0;
		s.cooked = 19;
		s.freeSlots = 0;
		Instruction one = Planner.plan(s, p);
		assertEquals("Load cannon 1", one.getTitle());
		assertEquals(Target.DROP_CRATE, one.getTarget());
		s.loadedTotal = 89;
		s.cooked = 0;
		under(s);
		assertTrue(Planner.plan(s, p).getDetail().contains("subdued"));
	}

	@Test
	public void lookaheadFollowsTheLegs()
	{
		PlannerSettings p = solo(PlayStyle.SOLO_FIVE_ROUNDS);
		Snapshot s = up();
		s.cooked = 26;
		s.freeSlots = 2;
		List<Instruction> next = Lookahead.next(s, p, 3);
		assertTrue(next.size() >= 2);
		assertEquals("Wait a moment", next.get(0).getTitle());
		List<Instruction> ten = Lookahead.next(up(), solo(PlayStyle.SOLO_TEN_PERMITS), 3);
		assertTrue(ten.size() >= 2);
	}
}

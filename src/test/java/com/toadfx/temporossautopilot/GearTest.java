package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.model.Harpoon;
import com.toadfx.temporossautopilot.model.PlayStyle;
import com.toadfx.temporossautopilot.state.GearMemory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class GearTest
{
	private static Snapshot boat()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.WAITING;
		s.hasHarpoon = true;
		s.carriedHarpoon = Harpoon.BASIC;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 2;
		return s;
	}

	private static PlannerSettings style(PlayStyle st)
	{
		PlannerSettings p = new PlannerSettings();
		p.style = st;
		return p;
	}

	@Test
	public void remindsToBringBetterHarpoon()
	{
		Snapshot s = boat();
		s.ownedHarpoons.add(Harpoon.DRAGON);
		assertEquals("Bring your dragon harpoon", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
		s.carriedHarpoon = Harpoon.DRAGON;
		assertEquals("Ready", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
	}

	@Test
	public void harpoonDependsOnStyle()
	{
		Snapshot s = boat();
		s.ownedHarpoons.add(Harpoon.CRYSTAL);
		s.ownedHarpoons.add(Harpoon.INFERNAL);
		assertEquals("Bring your crystal harpoon", Planner.plan(s, style(PlayStyle.MAX_XP)).getTitle());
		assertEquals("Bring your infernal harpoon", Planner.plan(s, style(PlayStyle.MAX_POINTS)).getTitle());
	}

	@Test
	public void noHarpoonSuggestsYourOwn()
	{
		Snapshot s = boat();
		s.hasHarpoon = false;
		s.carriedHarpoon = null;
		s.ownedHarpoons.add(Harpoon.DRAGON);
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals("Bring your dragon harpoon", i.getTitle());
		assertTrue(i.getDetail().contains("crate"));
	}

	@Test
	public void remindsToWearSpiritAnglerOverAngler()
	{
		Snapshot s = boat();
		for (int i = 0; i < 4; i++)
		{
			s.ownsSpirit[i] = true;
			s.ownsAngler[i] = true;
		}
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_PERMITS));
		assertEquals("Wear your spirit angler outfit", i.getTitle());
		assertTrue(i.getDetail().contains("rope"));
		for (int k = 0; k < 4; k++)
		{
			s.wearsSpirit[k] = true;
		}
		assertEquals("Ready", Planner.plan(s, style(PlayStyle.MAX_PERMITS)).getTitle());
	}

	@Test
	public void anglerPiecesFillGaps()
	{
		Snapshot s = boat();
		s.ownsSpirit[0] = true;
		s.wearsSpirit[0] = true;
		s.ownsAngler[1] = true;
		Instruction i = Planner.plan(s, style(PlayStyle.MAX_XP));
		assertEquals("Wear your angler outfit", i.getTitle());
		assertTrue(i.getDetail().contains("2 of 4"));
		s.wearsAngler[1] = true;
		assertEquals("Ready", Planner.plan(s, style(PlayStyle.MAX_XP)).getTitle());
	}

	@Test
	public void remindersCanBeTurnedOff()
	{
		Snapshot s = boat();
		s.ownedHarpoons.add(Harpoon.DRAGON);
		s.ownsSpirit[0] = true;
		PlannerSettings p = style(PlayStyle.MAX_PERMITS);
		p.gearReminders = false;
		assertEquals("Ready", Planner.plan(s, p).getTitle());
		assertTrue(Planner.gearReminders(s, style(PlayStyle.MAX_PERMITS)).size() == 2);
	}

	@Test
	public void memoryTracksBankAndForgetsSoldItems()
	{
		GearMemory g = new GearMemory();
		g.sawCarried(new int[]{ItemID.HARPOON}, new int[]{ItemID.SPIRIT_ANGLER_HAT});
		g.sawBank(new int[]{ItemID.DRAGON_HARPOON, ItemID.TRAWLER_REWARD_TOP, 995});
		assertTrue(g.owns(ItemID.DRAGON_HARPOON));
		assertTrue(g.owns(ItemID.SPIRIT_ANGLER_HAT));
		assertEquals(1, g.ownedPieces(GearMemory.ANGLER));
		assertTrue(!g.owns(995));

		GearMemory back = new GearMemory();
		back.load(g.serialize());
		assertEquals(g.getOwned(), back.getOwned());

		g.sawBank(new int[0]);
		assertTrue(!g.owns(ItemID.DRAGON_HARPOON));
		assertTrue(g.owns(ItemID.HARPOON));
	}
}

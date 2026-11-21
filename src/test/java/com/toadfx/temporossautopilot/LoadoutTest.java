package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** What to bring for each style. */
public class LoadoutTest
{
	private static PlannerSettings style(PlayStyle st, GameMode mode)
	{
		PlannerSettings p = new PlannerSettings();
		p.mode = mode;
		p.style = st;
		p.soloMethod = st.getSoloMethod();
		return p;
	}

	private static Snapshot boat()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.WAITING;
		s.hasHarpoon = true;
		s.hasRope = true;
		s.hasHammer = true;
		return s;
	}

	private static String find(List<Planner.BringItem> items, String start)
	{
		for (Planner.BringItem i : items)
		{
			if (i.text.startsWith(start))
			{
				return i.text + "|" + i.have;
			}
		}
		return null;
	}

	@Test
	public void bucketsPerStyle()
	{
		assertEquals(4, PlayStyle.SOLO_RECOMMENDED.loadout().buckets(2));
		assertEquals(6, PlayStyle.SOLO_FIREFIGHT.loadout().buckets(2));
		assertEquals(0, PlayStyle.SOLO_FIVE_ROUNDS.loadout().buckets(2));
		assertEquals(2, PlayStyle.RECOMMENDED_MASS.loadout().buckets(2));
		assertEquals(4, PlayStyle.FIREFIGHTER.loadout().buckets(2));
		assertEquals(6, PlayStyle.FIREFIGHTER.loadout().buckets(6));
		assertEquals(3, PlayStyle.DUO_ROUTE.loadout().buckets(2));
	}

	@Test
	public void recommendedSoloAsksForFourBucketsAndHumidify()
	{
		PlannerSettings p = style(PlayStyle.SOLO_RECOMMENDED, GameMode.SOLO);
		Snapshot s = boat();
		s.waterBuckets = 2;
		assertEquals("Grab buckets", Planner.plan(s, p).getTitle());
		List<Planner.BringItem> list = Planner.bringList(s, p);
		assertEquals("4 buckets of water (crate + pump)|false", find(list, "4 buckets"));
		assertEquals("Humidify runes in a rune pouch|false", find(list, "Humidify"));
		assertTrue(Planner.gearReminders(s, p).get(0).startsWith("Humidify"));
		s.hasHumidifyRunes = true;
		s.waterBuckets = 4;
		assertEquals("Humidify runes in a rune pouch|true", find(Planner.bringList(s, p), "Humidify"));
		assertTrue(Planner.gearReminders(s, p).isEmpty());
	}

	@Test
	public void fiveRoundsWantsAnEmptyBag()
	{
		PlannerSettings p = style(PlayStyle.SOLO_FIVE_ROUNDS, GameMode.SOLO);
		Snapshot s = boat();
		s.freeSlots = 20;
		List<Planner.BringItem> list = Planner.bringList(s, p);
		assertEquals("27 free slots for fish|false", find(list, "27 free"));
		assertEquals(null, find(list, "0 buckets"));
		assertEquals("Ready", Planner.plan(s, p).getTitle());
	}

	@Test
	public void spiritSetReplacesRope()
	{
		PlannerSettings p = style(PlayStyle.RECOMMENDED_MASS, GameMode.MASS);
		Snapshot s = boat();
		assertTrue(find(Planner.bringList(s, p), "Rope") != null);
		for (int i = 0; i < 4; i++)
		{
			s.ownsSpirit[i] = true;
		}
		List<Planner.BringItem> list = Planner.bringList(s, p);
		assertEquals(null, find(list, "Rope"));
		assertEquals("Spirit angler outfit (works as a rope)|false", find(list, "Spirit angler"));
	}
}

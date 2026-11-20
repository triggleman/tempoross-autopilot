package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Lookahead;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.model.PlayStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class LookaheadTest
{
	private static Snapshot energy()
	{
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 80;
		s.storm = 20;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 2;
		s.fishingLevel = 70;
		return s;
	}

	@Test
	public void fishThenCookThenFishMore()
	{
		PlannerSettings p = new PlannerSettings();
		p.style = PlayStyle.MAX_POINTS;
		List<Instruction> next = Lookahead.next(energy(), p, 2);
		assertEquals(2, next.size());
		assertEquals("Cook at the shrine", next.get(0).getTitle());
		assertEquals("Fish harpoonfish", next.get(1).getTitle());
	}

	@Test
	public void cookedBagLoadsThenPool()
	{
		PlannerSettings p = new PlannerSettings();
		Snapshot s = energy();
		s.energy = 8;
		s.cooked = 20;
		s.freeSlots = 8;
		List<Instruction> next = Lookahead.next(s, p, 2);
		assertTrue(!next.isEmpty());
		assertEquals("Head to the spirit pool", next.get(0).getTitle());
	}

	@Test
	public void waveThenBackToWork()
	{
		Snapshot s = energy();
		s.waveIncoming = true;
		List<Instruction> next = Lookahead.next(s, new PlannerSettings(), 1);
		assertEquals("Fish harpoonfish", next.get(0).getTitle());
	}
}

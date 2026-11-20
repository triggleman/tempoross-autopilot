package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.logic.Instruction;
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

public class ModeStylesTest
{
	@Test
	public void soloOnlyOffersSoloStyles()
	{
		List<PlayStyle> solo = PlayStyle.forMode(GameMode.SOLO);
		assertEquals(PlayStyle.SOLO_RECOMMENDED, solo.get(0));
		assertTrue(solo.contains(PlayStyle.SOLO_FIREFIGHT));
		assertTrue(solo.contains(PlayStyle.CHILL));
		assertTrue(!solo.contains(PlayStyle.RECOMMENDED_MASS));
		assertTrue(!solo.contains(PlayStyle.MY_ROUTE));
	}

	@Test
	public void duoHasTeamStylesButNotTheMassRoute()
	{
		List<PlayStyle> duo = PlayStyle.forMode(GameMode.DUO);
		assertEquals(PlayStyle.RECOMMENDED_MASS, duo.get(0));
		assertEquals("Recommended duo", duo.get(0).labelFor(GameMode.DUO));
		assertTrue(!duo.contains(PlayStyle.MY_ROUTE));
		assertTrue(!duo.contains(PlayStyle.SOLO_RECOMMENDED));
		assertTrue(PlayStyle.forMode(GameMode.MASS).contains(PlayStyle.MY_ROUTE));
	}

	@Test
	public void wrongStyleForModeFallsBackToRecommended()
	{
		assertEquals(PlayStyle.SOLO_RECOMMENDED, PlayStyle.effective(PlayStyle.MY_ROUTE, GameMode.SOLO));
		assertEquals(PlayStyle.RECOMMENDED_MASS, PlayStyle.effective(PlayStyle.SOLO_XP, GameMode.MASS));
		assertEquals(PlayStyle.CHILL, PlayStyle.effective(PlayStyle.CHILL, GameMode.SOLO));
	}

	@Test
	public void recommendedSoloFollowsTheWikiRoute()
	{
		PlannerSettings p = new PlannerSettings();
		p.mode = GameMode.SOLO;
		p.style = PlayStyle.SOLO_RECOMMENDED;
		p.soloMethod = p.style.getSoloMethod();
		Snapshot s = new Snapshot();
		s.phase = Phase.ENERGY;
		s.energy = 100;
		s.storm = 10;
		s.hasRope = true;
		s.hasHammer = true;
		s.waterBuckets = 2;
		s.cooked = 17;
		s.freeSlots = 11;
		Instruction i = Planner.plan(s, p);
		assertEquals(Target.CRATE, i.getTarget());
		assertTrue(i.getDetail().contains("Stop at 17"));
	}
}

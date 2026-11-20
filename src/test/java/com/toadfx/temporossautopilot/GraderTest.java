package com.toadfx.temporossautopilot;

import com.toadfx.temporossautopilot.coach.GameRecord;
import com.toadfx.temporossautopilot.coach.Grader;
import com.toadfx.temporossautopilot.coach.PersonalBests;
import com.toadfx.temporossautopilot.model.PlayStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GraderTest
{
	private static GameRecord clean()
	{
		GameRecord r = new GameRecord();
		r.won = true;
		r.seconds = 468;
		r.points = 4620;
		r.fishCaught = 38;
		r.fishCooked = 36;
		r.loadedCooked = 36;
		r.scatters = 26;
		r.wavesSafe = 2;
		return r;
	}

	@Test
	public void cleanGameNearPermitGetsTip()
	{
		GameRecord r = clean();
		Grader.grade(r, 4800);
		assertEquals("S", r.grade);
		assertTrue(r.tips.get(0).contains("180 more points was permit 5"));
	}

	@Test
	public void pointsBreakdown()
	{
		GameRecord r = clean();
		assertEquals(2340, r.loadingPoints());
		assertEquals(1430, r.poolPoints());
		assertEquals(190, r.fishingPoints());
		assertEquals(360, r.cookingPoints());
		assertEquals(20, r.wavePoints());
		assertEquals(280, r.otherPoints());
	}

	@Test
	public void mistakesCostGrade()
	{
		GameRecord r = clean();
		r.doubles = 4;
		r.doublesFished = 2;
		r.wavesHit = 1;
		Grader.grade(r, 4800);
		assertEquals("B", r.grade);
		assertEquals(3, r.tips.size());
		assertTrue(r.tips.get(0).contains("missed 2 double spots"));
	}

	@Test
	public void xpStylePenalisesCooking()
	{
		GameRecord r = clean();
		r.style = PlayStyle.MAX_XP;
		Grader.grade(r, 4800);
		assertTrue(r.score <= 80);
		assertTrue(r.tips.get(0).contains("Skip the shrine"));
	}

	@Test
	public void chillOnlyNeeds2000()
	{
		GameRecord r = clean();
		r.style = PlayStyle.CHILL;
		r.points = 2100;
		Grader.grade(r, 4800);
		assertEquals("S", r.grade);
		r.points = 1500;
		Grader.grade(r, 4800);
		assertEquals("D", r.grade);
	}

	@Test
	public void recordRoundTrips()
	{
		GameRecord r = clean();
		r.style = PlayStyle.FIREFIGHTER;
		Grader.grade(r, 4800);
		GameRecord back = GameRecord.parse(r.serialize());
		assertNotNull(back);
		assertEquals(r.points, back.points);
		assertEquals(PlayStyle.FIREFIGHTER, back.style);
		assertEquals(r.grade, back.grade);
		assertEquals(r.scatters, back.scatters);
	}

	@Test
	public void bestsOnlyCountWins()
	{
		PersonalBests b = new PersonalBests();
		GameRecord r = clean();
		Grader.grade(r, 4800);
		b.update(r);
		GameRecord better = clean();
		better.points = 5140;
		Grader.grade(better, 4800);
		assertTrue(b.update(better).get(0).contains("most points (5,140)"));
		GameRecord lost = clean();
		lost.won = false;
		lost.points = 9000;
		assertTrue(b.update(lost).isEmpty());
		assertEquals(5140, PersonalBests.parse(b.serialize()).mostPoints);
	}
}

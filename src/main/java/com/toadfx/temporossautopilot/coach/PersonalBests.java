package com.toadfx.temporossautopilot.coach;

import java.util.ArrayList;
import java.util.List;

/** Best-ever games, saved per character. Only wins count. */
public class PersonalBests
{
	public int mostPoints;
	public int mostPermits;
	/** Fastest win in seconds (0 = none yet). */
	public int fastestWin;
	public int bestScore = -1;
	public String bestGrade = "";

	/** Checks a finished game and returns what it beat, e.g. "most points (5,140)". */
	public List<String> update(GameRecord r)
	{
		List<String> beaten = new ArrayList<>();
		if (r == null || !r.won)
		{
			return beaten;
		}
		if (r.points > mostPoints)
		{
			if (mostPoints > 0)
			{
				beaten.add("most points (" + String.format("%,d", r.points) + ")");
			}
			mostPoints = r.points;
		}
		if (r.permits() > mostPermits)
		{
			if (mostPermits > 0)
			{
				beaten.add("most permits (" + r.permits() + ")");
			}
			mostPermits = r.permits();
		}
		if (r.seconds > 60 && (fastestWin == 0 || r.seconds < fastestWin))
		{
			if (fastestWin > 0)
			{
				beaten.add("fastest game (" + r.duration() + ")");
			}
			fastestWin = r.seconds;
		}
		if (r.score > bestScore)
		{
			if (bestScore >= 0)
			{
				beaten.add("best grade (" + r.grade + " " + r.score + ")");
			}
			bestScore = r.score;
			bestGrade = r.grade;
		}
		return beaten;
	}

	public boolean isEmpty()
	{
		return mostPoints == 0 && fastestWin == 0 && bestScore < 0;
	}

	public String serialize()
	{
		return mostPoints + "," + mostPermits + "," + fastestWin + "," + bestScore + "," + bestGrade;
	}

	public static PersonalBests parse(String s)
	{
		PersonalBests b = new PersonalBests();
		if (s == null || s.isEmpty())
		{
			return b;
		}
		try
		{
			String[] p = s.split(",", -1);
			b.mostPoints = Integer.parseInt(p[0]);
			b.mostPermits = Integer.parseInt(p[1]);
			b.fastestWin = Integer.parseInt(p[2]);
			b.bestScore = Integer.parseInt(p[3]);
			b.bestGrade = p.length > 4 ? p[4] : "";
		}
		catch (RuntimeException e)
		{
			return new PersonalBests();
		}
		return b;
	}
}

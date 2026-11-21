package com.toadfx.temporossautopilot.coach;

import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.model.PlayStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Grades a finished game against the play style it was played with, S to D,
 * and picks the three tips that would have helped most.
 */
public final class Grader
{
	private Grader()
	{
	}

	private static final class Tip
	{
		final int cost;
		final String text;

		Tip(int cost, String text)
		{
			this.cost = cost;
			this.text = text;
		}
	}

	public static void grade(GameRecord r, int pointsTarget)
	{
		List<Tip> tips = new ArrayList<>();
		int score = 100;

		if (!r.won)
		{
			score -= 30;
			tips.add(new Tip(30, "Tempoross got away. Load before the storm passes 93%."));
		}

		int missed = Math.max(0, r.doubles - r.doublesFished);
		if (missed > 0 && r.style != PlayStyle.CHILL)
		{
			int cost = Math.min(24, missed * 6);
			score -= cost;
			tips.add(new Tip(cost, "You missed " + missed + " double spot" + (missed == 1 ? "" : "s")
				+ ". Fish the double first, cook after."));
		}

		if (r.wavesHit > 0)
		{
			int cost = Math.min(30, r.wavesHit * 10);
			score -= cost;
			tips.add(new Tip(cost, r.wavesHit == 1 ? "A wave caught you untethered." : r.wavesHit + " waves caught you untethered."));
		}

		if (r.idleSeconds > 30)
		{
			int cost = Math.min(20, (r.idleSeconds - 30) / 5);
			score -= cost;
			tips.add(new Tip(cost, "You stood still for " + r.idleSeconds + " seconds. Keep fishing or cooking."));
		}

		if (r.won && r.leftoverFish > 0 && r.style != PlayStyle.CHILL)
		{
			int cost = Math.min(16, r.leftoverFish * 2);
			score -= cost;
			tips.add(new Tip(cost, r.leftoverFish + " fish were left in your bag. Load them before the last hit."));
		}

		score += styleScore(r, pointsTarget, tips);

		int toNext = Permits.toNext(r.points);
		if (r.won && r.points >= Permits.FIRST && toNext <= 3 * Permits.DEPOSIT_COOKED)
		{
			int next = Permits.earned(r.points) + 1;
			tips.add(new Tip(5, String.format("%,d", toNext) + " more points was permit " + next + ". That's "
				+ Permits.cookedFishToNext(r.points) + " more cooked fish loaded."));
		}

		r.score = Math.max(0, Math.min(100, score));
		r.grade = letter(r.score);
		r.tips.clear();
		tips.sort(Comparator.comparingInt((Tip t) -> -t.cost));
		for (Tip t : tips)
		{
			if (r.tips.size() == 3)
			{
				break;
			}
			r.tips.add(t.text);
		}
		if (r.tips.isEmpty())
		{
			r.tips.add("Clean game. Nothing to fix.");
		}
	}

	/** Score change for the play style's own goal. */
	private static int styleScore(GameRecord r, int pointsTarget, List<Tip> tips)
	{
		switch (r.style)
		{
			case MAX_XP:
			case SOLO_XP:
				if (r.fishCooked > 0)
				{
					int cost = Math.min(20, r.fishCooked);
					tips.add(new Tip(cost, "You cooked " + r.fishCooked + " fish. Skip the shrine for more Fishing XP."));
					return -cost;
				}
				return 0;
			case MAX_PERMITS:
				if (r.fishCaught > 10 && r.fishCooked * 10 < r.fishCaught * 8)
				{
					tips.add(new Tip(10, "Only " + r.fishCooked + " of " + r.fishCaught + " fish were cooked. Cook in batches of 7 (9 at 85+)."));
					return -10;
				}
				return 0;
			case MY_ROUTE:
			case DUO_ROUTE:
			case RECOMMENDED_MASS:
				if (r.fishCaught > 10 && r.fishCooked * 10 < r.fishCaught * 8)
				{
					tips.add(new Tip(10, "Only " + r.fishCooked + " of " + r.fishCaught + " fish were cooked. Cook your whole bag before loading."));
					return -10;
				}
				return 0;
			case CHILL:
				if (r.points < Permits.FIRST)
				{
					tips.add(new Tip(50, "You finished on " + String.format("%,d", r.points) + " points, short of 2,000 for a permit."));
					return -50;
				}
				return 0;
			case MAX_POINTS:
			case FIREFIGHTER:
			case CUSTOM:
				int cost = 0;
				if (r.points < pointsTarget)
				{
					int short_ = pointsTarget - r.points;
					cost = Math.min(25, short_ / 100);
					tips.add(new Tip(cost, String.format("%,d", short_) + " points short of your " + String.format("%,d", pointsTarget) + " target."));
				}
				if (r.style == PlayStyle.FIREFIGHTER && r.otherPoints() < 200)
				{
					tips.add(new Tip(8, "Only " + r.otherPoints() / 40 + " fires or repairs. Each one is 40 points."));
					cost += 8;
				}
				return -cost;
			default:
				return 0;
		}
	}

	public static String letter(int score)
	{
		if (score >= 95)
		{
			return "S";
		}
		if (score >= 85)
		{
			return "A";
		}
		if (score >= 70)
		{
			return "B";
		}
		if (score >= 55)
		{
			return "C";
		}
		return "D";
	}
}

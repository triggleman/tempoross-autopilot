package com.toadfx.temporossautopilot.coach;

import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import java.util.ArrayList;
import java.util.List;

/** What happened in one Tempoross game, plus its grade once it's over. */
public class GameRecord
{
	public long endedAtMillis;
	public PlayStyle style = PlayStyle.MAX_PERMITS;
	public GameMode mode = GameMode.MASS;
	public boolean won;
	public int seconds;
	public int points;
	public int rounds = 1;

	public int fishCaught;
	public int fishCooked;
	public int loadedRaw;
	public int loadedCooked;
	public int scatters;
	public int doubles;
	public int doublesFished;
	public int wavesSafe;
	public int wavesHit;
	public int idleSeconds;
	public int fishingXp;
	public int cookingXp;
	public int leftoverFish;

	// Filled in by the grader.
	public int score;
	public String grade = "";
	public final List<String> tips = new ArrayList<>();

	public int permits()
	{
		return Permits.earned(points);
	}

	public int bonusChance()
	{
		return Permits.bonusChance(points);
	}

	public int fishingPoints()
	{
		return fishCaught * Permits.FISH;
	}

	public int cookingPoints()
	{
		return fishCooked * Permits.COOK;
	}

	public int loadingPoints()
	{
		return loadedRaw * Permits.DEPOSIT_RAW + loadedCooked * Permits.DEPOSIT_COOKED;
	}

	public int poolPoints()
	{
		return scatters * Permits.SCATTER;
	}

	public int wavePoints()
	{
		return wavesSafe * Permits.WAVE;
	}

	/** Points not explained by the counters above: fires, repairs and anything missed. */
	public int otherPoints()
	{
		return Math.max(0, points - fishingPoints() - cookingPoints() - loadingPoints() - poolPoints() - wavePoints());
	}

	/** Permits an hour for this game, counting about a minute on the boat between games. */
	public double permitsPerHour()
	{
		if (seconds <= 0)
		{
			return 0;
		}
		return Permits.expected(points) * 3600.0 / (seconds + 60);
	}

	public double fishingXpPerHour()
	{
		if (seconds <= 0)
		{
			return 0;
		}
		return fishingXp * 3600.0 / (seconds + 60);
	}

	public String duration()
	{
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	/** One line for saving. Tips are worked out again on load, so they are not saved. */
	public String serialize()
	{
		return endedAtMillis + "," + style.name() + "," + mode.name() + "," + (won ? 1 : 0) + "," + seconds + ","
			+ points + "," + rounds + "," + fishCaught + "," + fishCooked + "," + loadedRaw + "," + loadedCooked + ","
			+ scatters + "," + doubles + "," + doublesFished + "," + wavesSafe + "," + wavesHit + "," + idleSeconds + ","
			+ fishingXp + "," + cookingXp + "," + leftoverFish + "," + score + "," + grade;
	}

	public static GameRecord parse(String line)
	{
		try
		{
			String[] p = line.split(",", -1);
			if (p.length < 22)
			{
				return null;
			}
			GameRecord r = new GameRecord();
			int i = 0;
			r.endedAtMillis = Long.parseLong(p[i++]);
			r.style = PlayStyle.valueOf(p[i++]);
			r.mode = GameMode.valueOf(p[i++]);
			r.won = "1".equals(p[i++]);
			r.seconds = Integer.parseInt(p[i++]);
			r.points = Integer.parseInt(p[i++]);
			r.rounds = Integer.parseInt(p[i++]);
			r.fishCaught = Integer.parseInt(p[i++]);
			r.fishCooked = Integer.parseInt(p[i++]);
			r.loadedRaw = Integer.parseInt(p[i++]);
			r.loadedCooked = Integer.parseInt(p[i++]);
			r.scatters = Integer.parseInt(p[i++]);
			r.doubles = Integer.parseInt(p[i++]);
			r.doublesFished = Integer.parseInt(p[i++]);
			r.wavesSafe = Integer.parseInt(p[i++]);
			r.wavesHit = Integer.parseInt(p[i++]);
			r.idleSeconds = Integer.parseInt(p[i++]);
			r.fishingXp = Integer.parseInt(p[i++]);
			r.cookingXp = Integer.parseInt(p[i++]);
			r.leftoverFish = Integer.parseInt(p[i++]);
			r.score = Integer.parseInt(p[i++]);
			r.grade = p[i];
			return r;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}
}

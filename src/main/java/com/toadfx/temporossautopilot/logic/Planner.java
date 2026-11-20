package com.toadfx.temporossautopilot.logic;

import com.toadfx.temporossautopilot.model.CookRule;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;

/**
 * Picks the next step. Pure: everything comes in through the snapshot and settings,
 * so it can be unit tested without a game client.
 * <p>
 * Order of priority: survive the wave, beat the storm, then the play style's plan
 * (prep, chores, load, cook, fish).
 */
public final class Planner
{
	/** Fires this close get doused by every style: they spread and burn your fish and tools. */
	private static final int FIRE_IN_THE_WAY = 2;
	/** How far firefighting styles walk for a fire or a broken mast. */
	private static final int CHORE_RANGE = 12;
	/** Tiles from a broken tether point that are worth fixing before a wave. */
	private static final int REPAIR_RANGE = 6;
	/** Extra seconds kept in hand when racing the storm. */
	private static final int STORM_MARGIN = 4;
	/** Energy at which the cooking method starts loading. */
	private static final int BALANCED_LOAD_ENERGY = 49;

	private Planner()
	{
	}

	public static Instruction plan(Snapshot s, PlannerSettings set)
	{
		switch (s.phase)
		{
			case WAITING:
				return prep(s, set);
			case WON:
				return won(s, set);
			case ENERGY:
			case ESSENCE:
				return inGame(s, set);
			default:
				return Instruction.NONE;
		}
	}

	// --------------------------------------------------------------------- before the game

	static Instruction prep(Snapshot s, PlannerSettings set)
	{
		Instruction gear = gear(s, set);
		if (gear != null)
		{
			return gear;
		}
		if (s.emptyBuckets > 0)
		{
			return Instruction.of("Fill your buckets", "Use the water pump so you can put out fires.", Target.WATER_PUMP);
		}
		return Instruction.of("Ready", readyLine(set), Target.NONE);
	}

	/** Missing tools, or null if the player has what they need. */
	private static Instruction gear(Snapshot s, PlannerSettings set)
	{
		if (!s.hasHarpoon && !set.barehanded)
		{
			return Instruction.of("Grab a harpoon", "Take one from the harpoon crate.", Target.SUPPLY_CRATES);
		}
		if (!s.hasRope)
		{
			return Instruction.of("Grab a rope", "You need it to tether during waves.", Target.SUPPLY_CRATES);
		}
		if (!s.hasHammer)
		{
			return Instruction.of("Grab a hammer", "For fixing broken masts and totems.", Target.SUPPLY_CRATES);
		}
		int wanted = bucketsWanted(set);
		int have = s.emptyBuckets + s.waterBuckets;
		if (have < wanted && s.phase == Phase.WAITING)
		{
			return Instruction.of("Grab buckets", "Take " + (wanted - have) + " more from the bucket crate.", Target.SUPPLY_CRATES);
		}
		return null;
	}

	private static int bucketsWanted(PlannerSettings set)
	{
		if (set.style == PlayStyle.FIREFIGHTER)
		{
			return Math.max(4, set.bucketsWanted);
		}
		return set.bucketsWanted;
	}

	private static String readyLine(PlannerSettings set)
	{
		switch (set.style)
		{
			case MAX_XP:
				return "Fish nonstop when the game starts. No cooking.";
			case CHILL:
				return "Aim for 2,000 points, then take it easy.";
			case FIREFIGHTER:
				return "Fish and cook, and put out every fire you can.";
			case MAX_POINTS:
				return "Cook everything and aim for " + String.format("%,d", set.pointsTarget) + " points.";
			default:
				return set.mode == GameMode.MASS ? "Head for a fishing spot when the game starts." : "Start fishing as soon as you land.";
		}
	}

	// --------------------------------------------------------------------- after the game

	static Instruction won(Snapshot s, PlannerSettings set)
	{
		String permits = Permits.earned(s.points) + " permit" + (Permits.earned(s.points) == 1 ? "" : "s");
		if (Permits.bonusChance(s.points) > 0)
		{
			permits += " + " + Permits.bonusChance(s.points) + "% roll";
		}
		if (set.style == PlayStyle.MAX_PERMITS)
		{
			return Instruction.soon("Leave now", "Subdued! " + permits + ". Right-click Leave on the first mate for the next game.", Target.LEAVE);
		}
		return Instruction.of("Tempoross subdued", permits + ". Leave when you're ready.", Target.LEAVE);
	}

	// --------------------------------------------------------------------- during the game

	static Instruction inGame(Snapshot s, PlannerSettings set)
	{
		Instruction wave = wave(s);
		if (wave != null)
		{
			return wave;
		}
		if (!s.hasHarpoon && !set.barehanded)
		{
			return Instruction.now("Grab a harpoon", "You lost it. Take one from the harpoon crate.", Target.SUPPLY_CRATES);
		}
		if (s.phase == Phase.ESSENCE || s.spiritPool && s.energy == 0)
		{
			return essence(s, set);
		}
		return energy(s, set);
	}

	private static Instruction wave(Snapshot s)
	{
		if (!s.waveIncoming)
		{
			return null;
		}
		if (s.tethered)
		{
			return Instruction.of("Hold on", "Stay tethered until the wave passes.", Target.TETHER);
		}
		if (s.damagedTetherDistance >= 0 && s.damagedTetherDistance <= REPAIR_RANGE && s.hasHammer)
		{
			return Instruction.now("Repair, then tether", "A wave is coming and the closest tether point is broken.", Target.DAMAGED_TETHER);
		}
		if (!s.hasRope)
		{
			return Instruction.now("Wave incoming", "No rope: drop to a safe spot or you may lose your fish.", Target.TETHER);
		}
		return Instruction.now("Tether now", "Wave incoming. Tie on to a mast or totem to keep your fish and tools.", Target.TETHER);
	}

	private static Instruction essence(Snapshot s, PlannerSettings set)
	{
		if (set.style == PlayStyle.CHILL && s.points >= Permits.FIRST)
		{
			return Instruction.of("You're safe", "2,000 points reached. Fish the spirit pool if you like.", Target.SPIRIT_POOL);
		}
		return Instruction.of("Attack the spirit pool", "+55 points a scatter until Tempoross is subdued.", Target.SPIRIT_POOL);
	}

	static Instruction energy(Snapshot s, PlannerSettings set)
	{
		CookRule cook = set.cookRule();
		int loadable = loadable(s, cook);
		int target = loadTarget(s, set);
		int remaining = target < 0 ? -1 : Math.max(0, target - s.loadedThisRound);

		// Race the storm.
		if (loadable > 0 && remaining != 0 && stormDanger(s, set))
		{
			return Instruction.now("Load now", "Storm at " + s.storm + "%. Get " + loadable + " fish in before it fills.", Target.CRATE);
		}

		if (set.style == PlayStyle.CHILL && s.points >= Permits.FIRST)
		{
			return Instruction.of("You're safe", "2,000 points (1 permit). Relax, but still tether for waves.", Target.NONE);
		}

		// Fires right next to you get doused whatever the style.
		if (s.fireDistance >= 0 && s.fireDistance <= FIRE_IN_THE_WAY && s.waterBuckets > 0)
		{
			return Instruction.soon("Douse the fire", "It's right next to you and spreads fast (+40).", Target.FIRE);
		}

		if (set.style.fightsFires())
		{
			if (s.fireDistance >= 0 && s.fireDistance <= CHORE_RANGE && s.waterBuckets > 0)
			{
				return Instruction.of("Douse the fire", "+40 points.", Target.FIRE);
			}
			if (s.damagedTetherDistance >= 0 && s.damagedTetherDistance <= CHORE_RANGE && s.hasHammer)
			{
				return Instruction.of("Repair the mast", "+40 points, and it's ready for the next wave.", Target.DAMAGED_TETHER);
			}
			if (set.style == PlayStyle.FIREFIGHTER && s.waterBuckets == 0 && s.emptyBuckets > 0 && s.fish() == 0)
			{
				return Instruction.of("Fill your buckets", "Use a water pump so you're ready for the next fire.", Target.WATER_PUMP);
			}
		}

		if (remaining == 0)
		{
			return Instruction.of("Round target met", "Don't load more: it makes Tempoross vulnerable too early. "
				+ (s.fish() > 0 ? "Hold your fish for the next round." : "Fish for the next load."), Target.FISHING_SPOT);
		}

		// Cook a full bag before loading it.
		if (cook != CookRule.NEVER && s.raw > 0 && s.bagFull())
		{
			return cookStep(s);
		}

		if (shouldLoad(s, set, cook, loadable, remaining))
		{
			int count = remaining > 0 ? Math.min(loadable, remaining) : loadable;
			return Instruction.of("Load the cannon", loadLine(s, cook, count, remaining), Target.CRATE);
		}

		// Cook a batch while there's no double spot to fish.
		if (cook != CookRule.NEVER && s.raw > 0 && !s.doubleSpot && s.raw >= cookAt(s, set, cook, remaining))
		{
			return cookStep(s);
		}

		if (s.doubleSpot && !s.bagFull())
		{
			return Instruction.of("Fish the double spot", "Chance of 2 fish a catch." + fishLine(s, set, remaining), Target.DOUBLE_SPOT);
		}
		if (s.bagFull())
		{
			// Full of fish we won't cook and can't load yet (e.g. holding for the next round).
			return Instruction.of("Bag full", "Wait for the next round, then load.", Target.CRATE);
		}
		return Instruction.of("Fish harpoonfish", fishLine(s, set, remaining).trim(), Target.FISHING_SPOT);
	}

	private static Instruction cookStep(Snapshot s)
	{
		return Instruction.of("Cook at the shrine", s.raw + " raw in your bag (+" + s.raw * Permits.COOK + " points).", Target.SHRINE);
	}

	/** Fish that can go in the crate under this cooking rule. */
	static int loadable(Snapshot s, CookRule cook)
	{
		return cook == CookRule.NEVER ? s.fish() : s.cooked + s.crystal;
	}

	/** Fixed load count for this round (solo routes), or -1. */
	static int loadTarget(Snapshot s, PlannerSettings set)
	{
		if (set.mode != GameMode.SOLO)
		{
			return -1;
		}
		return set.soloMethod.loadTarget(s.round);
	}

	static boolean stormDanger(Snapshot s, PlannerSettings set)
	{
		if (s.storm < 0)
		{
			return false;
		}
		if (s.storm >= set.stormDanger)
		{
			return true;
		}
		return s.stormSecondsLeft >= 0 && s.secondsToCrate >= 0 && s.stormSecondsLeft <= s.secondsToCrate + STORM_MARGIN;
	}

	private static boolean shouldLoad(Snapshot s, PlannerSettings set, CookRule cook, int loadable, int remaining)
	{
		if (loadable <= 0)
		{
			return false;
		}
		if (remaining > 0)
		{
			// Fixed route: load once the whole target is in hand (or the bag is full).
			return loadable >= remaining || s.bagFull();
		}
		if (s.bagFull())
		{
			return true;
		}
		// Already standing at the crate: top up.
		if (s.nearCrate && (cook == CookRule.NEVER || s.raw == 0))
		{
			return true;
		}
		if (set.style == PlayStyle.BALANCED && s.energy >= 0 && s.energy <= BALANCED_LOAD_ENERGY && s.raw == 0)
		{
			return true;
		}
		// Energy almost gone: get your deposit points in before Tempoross goes down.
		return s.energy >= 0 && s.energy <= 10 && (cook == CookRule.NEVER || s.raw == 0);
	}

	/** Raw fish to collect before cooking. */
	private static int cookAt(Snapshot s, PlannerSettings set, CookRule cook, int remaining)
	{
		if (remaining > 0)
		{
			// Fixed route: cook once you hold enough for the load.
			return Math.max(1, Math.min(remaining - s.cooked - s.crystal, s.raw + s.freeSlots));
		}
		if (cook == CookRule.BATCH)
		{
			return PlannerSettings.batchSize(s.fishingLevel);
		}
		// Always-cook styles: cook in batches too, so you aren't walking back after every fish.
		return PlannerSettings.batchSize(s.fishingLevel);
	}

	private static String loadLine(Snapshot s, CookRule cook, int count, int remaining)
	{
		int cooked = Math.min(count, s.cooked);
		int other = count - cooked;
		int pts = cooked * Permits.DEPOSIT_COOKED + other * Permits.DEPOSIT_RAW;
		String line = "Load " + count + " fish (+" + String.format("%,d", pts) + " points).";
		if (remaining > 0)
		{
			line += " Stop at " + remaining + ".";
		}
		return line;
	}

	private static String fishLine(Snapshot s, PlannerSettings set, int remaining)
	{
		if (remaining > 0)
		{
			return " " + s.fish() + " / " + remaining + " for this load.";
		}
		if (set.style == PlayStyle.MAX_POINTS || set.style == PlayStyle.CUSTOM)
		{
			int goal = Math.max(set.pointsTarget, 0);
			if (s.points < goal)
			{
				return " " + String.format("%,d", goal - s.points) + " points to your target.";
			}
		}
		if (set.style != PlayStyle.MAX_XP)
		{
			return " " + Permits.cookedFishToNext(s.points) + " cooked fish to the next permit.";
		}
		return "";
	}
}

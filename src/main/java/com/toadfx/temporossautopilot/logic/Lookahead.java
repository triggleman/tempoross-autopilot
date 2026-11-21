package com.toadfx.temporossautopilot.logic;

import com.toadfx.temporossautopilot.model.CookRule;
import com.toadfx.temporossautopilot.model.Harpoon;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out the next few steps after the current one, by pretending each step is done
 * and asking the planner again. Used for the "Then:" line in the top box.
 */
public final class Lookahead
{
	private Lookahead()
	{
	}

	public static List<Instruction> next(Snapshot now, PlannerSettings set, int count)
	{
		List<Instruction> out = new ArrayList<>();
		Snapshot s = now.copy();
		Instruction step = Planner.plan(s, set);
		String last = step.getTitle();
		for (int i = 0; i < count * 3 && out.size() < count; i++)
		{
			if (!advance(s, step, set))
			{
				break;
			}
			step = Planner.plan(s, set);
			if (step.isEmpty())
			{
				break;
			}
			if (!step.getTitle().equals(last))
			{
				out.add(step);
				last = step.getTitle();
			}
		}
		return out;
	}

	/** Pretends the step is done. False if we can't guess what happens next. */
	static boolean advance(Snapshot s, Instruction step, PlannerSettings set)
	{
		CookRule cook = set.cookRule();
		if (set.mode == com.toadfx.temporossautopilot.model.GameMode.SOLO && set.soloMethod.hasTargets())
		{
			Boolean solo = advanceSolo(s, step, set, cook);
			if (solo != null)
			{
				return solo;
			}
		}
		switch (step.getTarget())
		{
			case DOUBLE_SPOT:
			case FISHING_SPOT:
			{
				if (step.getTitle().startsWith("Round target"))
				{
					// Holding fish for the next round: next is the spirit pool.
					s.phase = Phase.ESSENCE;
					s.energy = 0;
					s.spiritPool = false;
					return true;
				}
				int target = Planner.loadTarget(s, set);
				int want;
				if (target > 0)
				{
					want = Math.max(1, target - s.loadedThisRound - s.cooked - s.crystal);
				}
				else if (cook == CookRule.NEVER)
				{
					want = s.freeSlots;
				}
				else if (s.phase == Phase.ESSENCE)
				{
					// Fishing while the pool is still up: fill the bag for the next round.
					want = s.freeSlots;
				}
				else if (set.style.isToadFxRoute())
				{
					boolean opener = s.round <= 1 && s.wavesThisRound == 0 && s.cooked + s.crystal == 0 && s.loadedThisRound == 0;
					if (opener)
					{
						want = Math.max(1, set.style.openingBatch() - s.raw);
					}
					else if (s.round <= 1 && s.wavesThisRound == 0)
					{
						// Fishing the double until the wave: the next thing is the wave.
						s.waveIncoming = true;
						return true;
					}
					else
					{
						want = s.freeSlots;
					}
				}
				else if (cook == CookRule.OPENER_THEN_FULL_BAG)
				{
					want = s.cooked + s.crystal == 0 ? Math.max(1, set.style.openingBatch() - s.raw) : s.freeSlots;
				}
				else
				{
					want = Math.max(1, PlannerSettings.batchSize(s.fishingLevel) - s.raw);
				}
				want = Math.min(want, s.freeSlots);
				if (want <= 0)
				{
					return false;
				}
				s.raw += want;
				s.freeSlots -= want;
				s.points += want * Permits.FISH;
				s.doubleSpot = false;
				return true;
			}
			case SHRINE:
				s.points += s.raw * Permits.COOK;
				s.cooked += s.raw;
				s.raw = 0;
				return true;
			case CRATE:
			{
				if (step.getTitle().equals("Wait for the next round"))
				{
					nextRound(s);
					return true;
				}
				if (step.getTitle().startsWith("Wait"))
				{
					s.energy = Math.min(s.energy, 35);
					return true;
				}
				int load = set.style.isToadFxRoute() && s.round > 1 ? s.fish() : Planner.loadable(s, cook);
				if (load <= 0)
				{
					return false;
				}
				int cookedOut = Math.min(load, s.cooked);
				int other = load - cookedOut;
				s.points += cookedOut * Permits.DEPOSIT_COOKED + other * Permits.DEPOSIT_RAW;
				s.cooked -= cookedOut;
				int crystalOut = Math.min(other, s.crystal);
				s.crystal -= crystalOut;
				s.raw -= other - crystalOut;
				s.freeSlots += load;
				s.loadedThisRound += load;
				s.nearCrate = true;
				s.stormSecondsLeft = -1;
				if (s.storm >= set.stormDanger)
				{
					s.storm = set.stormDanger - 1;
				}
				int target = Planner.loadTarget(s, set);
				if (target > 0 && s.loadedThisRound >= target || s.energy >= 0 && s.energy <= 10)
				{
					s.phase = Phase.ESSENCE;
					s.energy = 0;
					s.spiritPool = false;
				}
				return true;
			}
			case SPIRIT_POOL:
				if (step.getTitle().startsWith("Head to") || step.getTitle().startsWith("Wait at"))
				{
					// Walked there: the pool opens.
					s.phase = Phase.ESSENCE;
					s.energy = 0;
					s.spiritPool = true;
					return true;
				}
				if (set.style == com.toadfx.temporossautopilot.model.PlayStyle.DUO_ROUTE && s.phase == Phase.ESSENCE && Planner.duoPoolTarget(s.round) > 0
					&& (s.essence < 0 || s.essence > Planner.duoPoolTarget(s.round)))
				{
					s.essence = Planner.duoPoolTarget(s.round);
					return true;
				}
				if (set.style == com.toadfx.temporossautopilot.model.PlayStyle.MY_ROUTE && s.round == 1 && s.phase == Phase.ESSENCE && s.energy < 90)
				{
					s.energy = 90;
					return true;
				}
				// Tempoross recovers (or is subdued); plan for the next round.
				nextRound(s);
				return true;
			case TETHER:
				s.waveIncoming = false;
				s.tethered = false;
				s.wavesThisRound++;
				return true;
			case DAMAGED_TETHER:
				s.damagedTetherDistance = -1;
				return true;
			case FIRE:
				s.fireDistance = -1;
				s.points += Permits.DOUSE;
				return true;
			case WATER_PUMP:
				s.waterBuckets += s.emptyBuckets;
				s.emptyBuckets = 0;
				return true;
			case SUPPLY_CRATES:
				if (!s.hasHarpoon)
				{
					s.hasHarpoon = true;
					s.carriedHarpoon = s.carriedHarpoon == null ? Harpoon.BASIC : s.carriedHarpoon;
				}
				else if (!s.hasRope)
				{
					s.hasRope = true;
				}
				else if (!s.hasHammer)
				{
					s.hasHammer = true;
				}
				else
				{
					s.emptyBuckets += 4;
				}
				s.freeSlots--;
				return true;
			default:
				return false;
		}
	}

	/** Solo routes: pretend a leg step is done. Null to use the general rules. */
	private static Boolean advanceSolo(Snapshot s, Instruction step, PlannerSettings set, CookRule cook)
	{
		com.toadfx.temporossautopilot.model.SoloMethod m = set.soloMethod;
		int li = m.legIndex(s.loadedTotal);
		if (li >= m.legCount())
		{
			return step.getTarget() == Target.SPIRIT_POOL ? Boolean.FALSE : null;
		}
		com.toadfx.temporossautopilot.model.SoloMethod.Leg leg = m.leg(li);
		int done = s.loadedTotal - m.start(li);
		int left = leg.fish - done;
		String title = step.getTitle();
		switch (step.getTarget())
		{
			case DOUBLE_SPOT:
			case FISHING_SPOT:
			{
				int need = !leg.dropTrick ? (done == 0 ? leg.gather : left)
					: done == 0 && s.groundFish < leg.dropCount ? leg.dropCount
					: done < leg.secondBatch() ? leg.secondBatch() - done : left;
				int want = Math.min(Math.max(1, need - s.fish()), s.freeSlots);
				if (want <= 0)
				{
					return false;
				}
				s.raw += want;
				s.freeSlots -= want;
				s.points += want * Permits.FISH;
				s.doubleSpot = false;
				return true;
			}
			case NONE:
				if (title.startsWith("Drop your buckets"))
				{
					s.freeSlots += s.emptyBuckets + s.waterBuckets;
					s.emptyBuckets = 0;
					s.waterBuckets = 0;
					return true;
				}
				return false;
			case CRATE:
			case OTHER_CRATE:
			case DROP_CRATE:
			{
				if (title.equals("Wait a moment"))
				{
					s.msSinceLoad = Planner.PAUSE_MS + 1;
					return true;
				}
				if (title.startsWith("Wait for him to go under"))
				{
					pool(s);
					return true;
				}
				if (title.startsWith("Wait for him to come back up"))
				{
					nextRound(s);
					return true;
				}
				if (title.startsWith("Drop "))
				{
					int dropped = s.cooked;
					s.groundFish += dropped;
					s.cooked = 0;
					s.freeSlots += dropped;
					return true;
				}
				if (title.startsWith("Pick up"))
				{
					s.cooked += s.groundFish;
					s.freeSlots -= s.groundFish;
					s.groundFish = 0;
					return true;
				}
				int n = left;
				int chunk = leg.chunkFor(s.roundStartEnergy);
				if (chunk > 0 && done < chunk)
				{
					n = chunk - done;
				}
				else if (leg.split > 0 && done < leg.split)
				{
					n = leg.split - done;
				}
				else if (leg.dropTrick && done < leg.secondBatch())
				{
					n = leg.secondBatch() - done;
				}
				n = Math.min(n, Planner.loadable(s, cook));
				if (n <= 0)
				{
					return false;
				}
				int cookedOut = Math.min(n, s.cooked);
				s.cooked -= cookedOut;
				int crystalOut = Math.min(n - cookedOut, s.crystal);
				s.crystal -= crystalOut;
				s.raw -= n - cookedOut - crystalOut;
				s.points += cookedOut * Permits.DEPOSIT_COOKED + (n - cookedOut) * Permits.DEPOSIT_RAW;
				s.freeSlots += n;
				s.loadedTotal += n;
				s.loadedThisRound += n;
				s.msSinceLoad = 0;
				s.nearCrate = true;
				s.atFirstCrate = step.getTarget() == Target.CRATE && done == 0 || s.atFirstCrate && step.getTarget() == Target.CRATE;
				if (done + n >= leg.fish && leg.submerges && s.phase == Phase.ENERGY)
				{
					pool(s);
				}
				return true;
			}
			case SPIRIT_POOL:
			{
				if (title.startsWith("Head to"))
				{
					pool(s);
					return true;
				}
				com.toadfx.temporossautopilot.model.SoloMethod.PoolRule rule = m.poolAfter(li);
				if (rule.leaveAt > 0 && rule.leaveAt <= 100 && (s.essence < 0 || s.essence > rule.leaveAt))
				{
					s.essence = rule.leaveAt;
					return true;
				}
				nextRound(s);
				return true;
			}
			default:
				return null;
		}
	}

	/** Tempoross goes under: the spirit pool opens. */
	private static void pool(Snapshot s)
	{
		s.phase = Phase.ESSENCE;
		s.energy = 0;
		s.spiritPool = true;
		s.poolOpenEssence = s.essence;
		s.atFirstCrate = false;
		s.nearCrate = false;
	}

	/** Tempoross gets back up: the next energy round starts. */
	private static void nextRound(Snapshot s)
	{
		s.phase = Phase.ENERGY;
		s.energy = 100;
		s.storm = 0;
		s.round++;
		s.loadedThisRound = 0;
		s.wavesThisRound = 0;
		s.spiritPool = false;
		s.nearCrate = false;
		s.atFirstCrate = false;
		s.roundStartEnergy = 100;
		s.poolOpenEssence = -1;
	}
}

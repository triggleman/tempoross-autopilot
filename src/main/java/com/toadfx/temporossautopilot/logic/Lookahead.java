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
				else if (set.style == com.toadfx.temporossautopilot.model.PlayStyle.MY_ROUTE)
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
				if (step.getTitle().startsWith("Wait"))
				{
					s.energy = Math.min(s.energy, 35);
					return true;
				}
				int load = set.style == com.toadfx.temporossautopilot.model.PlayStyle.MY_ROUTE && s.round > 1 ? s.fish() : Planner.loadable(s, cook);
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
				if (set.style == com.toadfx.temporossautopilot.model.PlayStyle.MY_ROUTE && s.round == 1 && s.phase == Phase.ESSENCE && s.energy < 90)
				{
					s.energy = 90;
					return true;
				}
				// Tempoross recovers (or is subdued); plan for the next round.
				s.phase = Phase.ENERGY;
				s.energy = 100;
				s.storm = 0;
				s.round++;
				s.loadedThisRound = 0;
				s.spiritPool = false;
				s.nearCrate = false;
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
}

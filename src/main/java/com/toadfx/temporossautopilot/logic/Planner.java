package com.toadfx.temporossautopilot.logic;

import com.toadfx.temporossautopilot.model.CookRule;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.Harpoon;
import com.toadfx.temporossautopilot.model.PlayStyle;
import com.toadfx.temporossautopilot.model.SoloMethod;
import java.util.ArrayList;
import java.util.List;

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
	private static final int COOKING_METHOD_LOAD_ENERGY = 49;

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
			Harpoon own = bestOwnedHarpoon(s, set);
			if (own != null && set.gearReminders)
			{
				return Instruction.of("Bring your " + own.getLabel(), "It's in your bank. Or take a harpoon from the crate.", Target.SUPPLY_CRATES);
			}
			return Instruction.of("Grab a harpoon", "Take one from the harpoon crate.", Target.SUPPLY_CRATES);
		}
		if (!s.hasRope)
		{
			return Instruction.of("Grab a rope", "You need it to tether during waves.", Target.SUPPLY_CRATES);
		}
		if (!s.hasHammer && !(set.style == PlayStyle.DUO_ROUTE && countSpirit(s.wearsSpirit) == 4))
		{
			return Instruction.of("Grab a hammer", "For fixing broken masts and totems.", Target.SUPPLY_CRATES);
		}
		int wanted = bucketsWanted(set);
		int have = s.emptyBuckets + s.waterBuckets;
		if (have < wanted && s.phase == Phase.WAITING)
		{
			return Instruction.of("Grab buckets", "Take " + (wanted - have) + " more from the bucket crate.", Target.SUPPLY_CRATES);
		}
		if (set.gearReminders)
		{
			Instruction better = betterHarpoon(s, set);
			if (better != null)
			{
				return better;
			}
			Instruction hammer = imcando(s);
			if (hammer != null)
			{
				return hammer;
			}
			Instruction outfit = outfit(s);
			if (outfit != null)
			{
				return outfit;
			}
		}
		return null;
	}

	/** A reminder to bring the Imcando hammer if you own it and didn't bring it, or null. */
	static Instruction imcando(Snapshot s)
	{
		if (!s.ownsImcando || s.carriesImcando)
		{
			return null;
		}
		return Instruction.of("Bring your Imcando hammer", "Waves and fires can't take it, and it goes in your off-hand.", Target.NONE);
	}

	// --------------------------------------------------------------------- your own gear

	/** The best harpoon you own for this style, or null if none is known. */
	public static Harpoon bestOwnedHarpoon(Snapshot s, PlannerSettings set)
	{
		Harpoon best = null;
		for (Harpoon h : s.ownedHarpoons)
		{
			best = Harpoon.best(best, h, set.style);
		}
		return best;
	}

	/** A reminder to bring a better harpoon you own, or null. */
	static Instruction betterHarpoon(Snapshot s, PlannerSettings set)
	{
		Harpoon own = bestOwnedHarpoon(s, set);
		if (own == null || set.barehanded)
		{
			return null;
		}
		if (s.carriedHarpoon != null && s.carriedHarpoon.rank(set.style) >= own.rank(set.style))
		{
			return null;
		}
		return Instruction.of("Bring your " + own.getLabel(), own.getWhy(), Target.NONE);
	}

	/** A reminder to wear the angler or spirit angler pieces you own, or null. */
	static Instruction outfit(Snapshot s)
	{
		int missing = 0;
		boolean spirit = false;
		int owned = 0;
		for (int i = 0; i < 4; i++)
		{
			boolean wantSpirit = s.ownsSpirit[i];
			boolean wantAngler = !wantSpirit && s.ownsAngler[i];
			if (wantSpirit || wantAngler)
			{
				owned++;
			}
			if (wantSpirit && !s.wearsSpirit[i])
			{
				missing++;
				spirit = true;
			}
			else if (wantAngler && !s.wearsSpirit[i] && !s.wearsAngler[i])
			{
				missing++;
			}
		}
		if (missing == 0)
		{
			return null;
		}
		String name = spirit ? "spirit angler outfit" : "angler outfit";
		String detail = owned == 4 ? "The full set gives +2.5% Fishing XP." : "You own " + owned + " of 4 pieces. The full set gives +2.5% Fishing XP.";
		if (spirit && countSpirit(s.ownsSpirit) == 4)
		{
			detail += " The full spirit set also works as a rope.";
		}
		return Instruction.of("Wear your " + name, detail, Target.NONE);
	}

	private static int countSpirit(boolean[] b)
	{
		int n = 0;
		for (boolean x : b)
		{
			if (x)
			{
				n++;
			}
		}
		return n;
	}

	/** Everything you own but didn't bring, for a one-line chat reminder. Empty when all set. */
	public static List<String> gearReminders(Snapshot s, PlannerSettings set)
	{
		List<String> out = new ArrayList<>();
		Instruction h = s.hasHarpoon ? betterHarpoon(s, set) : null;
		if (!s.hasHarpoon && !set.barehanded && bestOwnedHarpoon(s, set) != null)
		{
			h = Instruction.of("Bring your " + bestOwnedHarpoon(s, set).getLabel(), "", Target.NONE);
		}
		if (h != null)
		{
			out.add(h.getTitle().replace("Bring your ", "your "));
		}
		if (imcando(s) != null)
		{
			out.add("your Imcando hammer");
		}
		Instruction o = outfit(s);
		if (o != null)
		{
			out.add(o.getTitle().replace("Wear your ", "your "));
		}
		if (set.style.loadout().wantsHumidify() && !s.hasHumidifyRunes)
		{
			out.add("Humidify runes (rune pouch, Lunar spellbook)");
		}
		return out;
	}

	/** One line of the "what to bring" list: the item and whether you have it (null = can't tell). */
	public static final class BringItem
	{
		public final String text;
		public final Boolean have;

		BringItem(String text, Boolean have)
		{
			this.text = text;
			this.have = have;
		}
	}

	/**
	 * Everything to bring for the chosen style, ticked against what you're carrying. Tools you
	 * can take from the crates on the dock are marked as such.
	 */
	public static List<BringItem> bringList(Snapshot s, PlannerSettings set)
	{
		com.toadfx.temporossautopilot.model.Loadout kit = set.style.loadout();
		List<BringItem> out = new ArrayList<>();
		Harpoon own = bestOwnedHarpoon(s, set);
		if (set.barehanded)
		{
			out.add(new BringItem("Barehanded: no harpoon", null));
		}
		else if (own != null)
		{
			out.add(new BringItem("Your " + own.getLabel(), s.carriedHarpoon != null && s.carriedHarpoon.rank(set.style) >= own.rank(set.style)));
		}
		else
		{
			out.add(new BringItem("A harpoon (crate on the dock)", s.hasHarpoon));
		}
		boolean spiritSet = countSpirit(s.wearsSpirit) == 4;
		if (countSpirit(s.ownsSpirit) == 4)
		{
			out.add(new BringItem("Spirit angler outfit (works as a rope)", spiritSet));
		}
		else
		{
			out.add(new BringItem("Rope (crate on the dock)", s.hasRope));
			if (countSpirit(s.ownsSpirit) + countSpirit(s.ownsAngler) > 0)
			{
				out.add(new BringItem("Your angler pieces (+XP)", outfit(s) == null));
			}
		}
		if (s.ownsImcando)
		{
			out.add(new BringItem("Imcando hammer (wear it)", s.carriesImcando));
		}
		else if (!(set.style == PlayStyle.DUO_ROUTE && spiritSet))
		{
			out.add(new BringItem("Hammer (crate on the dock)", s.hasHammer));
		}
		int buckets = bucketsWanted(set);
		if (buckets > 0)
		{
			out.add(new BringItem(buckets + " buckets of water (crate + pump)", s.waterBuckets >= buckets));
		}
		if (kit.wantsHumidify())
		{
			out.add(new BringItem("Humidify runes in a rune pouch", s.hasHumidifyRunes));
		}
		if (kit.getFreeSlots() > 0)
		{
			out.add(new BringItem(kit.getFreeSlots() + " free slots for fish", s.freeSlots >= kit.getFreeSlots()));
		}
		for (String tip : kit.getTips())
		{
			out.add(new BringItem(tip, null));
		}
		if (s.fishingLevel > 1 && s.fishingLevel < 90)
		{
			out.add(new BringItem("Admiral pie or fishing potion for more XP", null));
		}
		return out;
	}

	private static int bucketsWanted(PlannerSettings set)
	{
		return set.style.loadout().buckets(set.bucketsWanted);
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
				return "Fish and cook, and put out every fire you can. Humidify refills buckets.";
			case MAX_POINTS:
				return "Catch 8 and cook them while you wait for a double spot. Aim for " + String.format("%,d", set.pointsTarget) + " points.";
			case RECOMMENDED_MASS:
				return "Catch 9 at the nearest spot and cook them, then fill up at a double spot.";
			case MY_ROUTE:
				return "Catch 8 and cook them, then fish the double spot until the wave.";
			case DUO_ROUTE:
				return "Catch 8 and cook them, then fish the double spot until the wave. First pool: take Tempoross from 100% to " + DUO_POOL_TO[0] + "%.";
			case SOLO_RECOMMENDED:
			case SOLO_FIREFIGHT:
			case SOLO_FIVE_ROUNDS:
			case SOLO_XP:
				return "Solo-start from the rope ladder. First load: " + set.soloMethod.firstLoad() + " fish.";
			case SOLO_TEN_PERMITS:
				return "Solo-start from the rope ladder. Catch and cook 16 and load them: Tempoross drops to 10% energy.";
			case MAX_PERMITS:
				return "Cook 7 at a time (9 at 85+ Fishing) and start loading at 49% energy.";
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
		if (set.style != PlayStyle.CHILL && set.style != PlayStyle.CUSTOM && set.style != PlayStyle.MAX_POINTS && set.style != PlayStyle.FIREFIGHTER)
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
		if (set.mode == GameMode.SOLO && set.soloMethod.hasTargets())
		{
			return solo(s, set);
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
		if (!s.spiritPool)
		{
			return Instruction.soon("Head to the spirit pool", "Energy is 0. Be there when it opens: +55 points a scatter.", Target.SPIRIT_POOL);
		}
		if (set.style == PlayStyle.MY_ROUTE && s.round == 1)
		{
			if (s.energy >= ROUTE_POOL_UNTIL)
			{
				return fishStep(s, "Back to fishing", "Tempoross is at " + s.energy + "%. Fish for the next load.");
			}
			return Instruction.of("Attack the spirit pool", "Stay until Tempoross is back to " + ROUTE_POOL_UNTIL + "% (now " + Math.max(0, s.energy) + "%).", Target.SPIRIT_POOL);
		}
		if (set.style == PlayStyle.MY_ROUTE)
		{
			return Instruction.of("Attack the spirit pool", "Stay until Tempoross is subdued, then leave.", Target.SPIRIT_POOL);
		}
		if (set.style == PlayStyle.DUO_ROUTE)
		{
			return duoPool(s);
		}
		return Instruction.of("Attack the spirit pool", "+55 points a scatter until Tempoross is subdued.", Target.SPIRIT_POOL);
	}

	// --------------------------------------------------------------------- ToadFx mass route

	/** Leave the first spirit pool once Tempoross is back to this energy. */
	static final int ROUTE_POOL_UNTIL = 90;
	/** From the second round on, load everything once energy drops to this. */
	static final int ROUTE_LOAD_AT = 35;

	/**
	 * Todd's route. Round 1: catch 8, cook them, fish the double spot until the wave, after the
	 * wave fish the double (or the nearest spot) until the bag is full, cook it all, load until
	 * empty (or energy hits 0), then the spirit pool until energy is back to 90%. Round 2 on: fish
	 * and cook a full bag; load it all when it's cooked or at 35% energy (whichever is first), wait at
	 * the spirit pool until energy is 0, then fish the pool until the end.
	 */
	static Instruction route(Snapshot s, PlannerSettings set)
	{
		int cooked = s.cooked + s.crystal;
		if (s.round <= 1)
		{
			// Already loading: keep going until the bag is empty.
			if (s.nearCrate && cooked > 0 && s.raw == 0)
			{
				return Instruction.of("Load the cannon", "Load until your bag is empty (" + cooked + " left).", Target.CRATE);
			}
			if (s.bagFull() && s.raw > 0)
			{
				return Instruction.of("Cook at the shrine", "Bag full: cook all " + s.raw + " before loading.", Target.SHRINE);
			}
			if (s.bagFull() && cooked > 0)
			{
				return Instruction.of("Load the cannon", "Load until your bag is empty or Tempoross hits 0%.", Target.CRATE);
			}
			if (s.wavesThisRound == 0 && s.loadedThisRound == 0 && cooked > 0 && cooked < set.style.openingBatch() && s.raw > 0)
			{
				return Instruction.of("Keep cooking", "Finish cooking your first " + set.style.openingBatch() + " (" + s.raw + " left).", Target.SHRINE);
			}
			boolean opener = s.wavesThisRound == 0 && cooked == 0 && s.loadedThisRound == 0;
			if (opener && s.raw >= set.style.openingBatch())
			{
				return Instruction.of("Cook at the shrine", "Cook your first " + s.raw + ", then go to the double spot.", Target.SHRINE);
			}
			if (opener)
			{
				return fishStep(s, "Fish harpoonfish", "Catch " + set.style.openingBatch() + " to start (" + s.raw + " so far).");
			}
			if (s.wavesThisRound == 0)
			{
				return s.doubleSpot
					? Instruction.of("Fish the double spot", "Stay here until the wave comes.", Target.DOUBLE_SPOT)
					: fishStep(s, "Fish harpoonfish", "No double spot yet. Fish here until the wave.");
			}
			if (s.loadedThisRound > 0 && s.fish() == 0)
			{
				return Instruction.soon("Head to the spirit pool", "All loaded. Wait at the pool for Tempoross to go down.", Target.SPIRIT_POOL);
			}
			return fishStep(s, "Fish until full", s.freeSlots + " slots left, then cook it all.");
		}

		// Round 2 on.
		if (s.energy >= 0 && s.energy <= ROUTE_LOAD_AT && s.fish() > 0)
		{
			return Instruction.of("Load the cannon", "Tempoross is at " + s.energy + "%: load everything (" + s.fish() + " fish).", Target.CRATE);
		}
		// Already loading: keep going until the bag is empty.
		if (s.nearCrate && cooked > 0 && s.raw == 0)
		{
			return Instruction.of("Load the cannon", "Load until your bag is empty (" + cooked + " left).", Target.CRATE);
		}
		if (s.bagFull() && s.raw > 0)
		{
			return Instruction.of("Cook at the shrine", "Bag full: cook all " + s.raw + ", then load it.", Target.SHRINE);
		}
		if (s.bagFull())
		{
			return Instruction.of("Load the cannon", "Bag all cooked before " + ROUTE_LOAD_AT + "%: load it all, then wait at the spirit pool.", Target.CRATE);
		}
		if (s.loadedThisRound > 0 && s.fish() == 0 || s.energy >= 0 && s.energy <= 3)
		{
			return Instruction.soon("Wait at the spirit pool", "Tempoross is at " + Math.max(0, s.energy) + "%. Wait here until he hits 0%, then fish the pool to the end.", Target.SPIRIT_POOL);
		}
		return fishStep(s, "Fish harpoonfish", "Fill and cook your bag, or load at " + ROUTE_LOAD_AT + "% (Tempoross is at " + Math.max(0, s.energy) + "%).");
	}

	// --------------------------------------------------------------------- ToadFx duo route

	/** Stop attacking the spirit pool once Tempoross's essence is down to this, by round. */
	static final int[] DUO_POOL_TO = {60, 30, 0};

	/** Essence to take Tempoross down to at this round's spirit pool. */
	static int duoPoolTarget(int round)
	{
		return DUO_POOL_TO[Math.max(0, Math.min(DUO_POOL_TO.length - 1, round - 1))];
	}

	/**
	 * Todd's duo spirit pool: round 1 takes Tempoross from 100% to 60%, round 2 from 60% to 30%,
	 * round 3 from 30% to 0%. Once this round's share is done, go and fill and cook a full bag
	 * for the next round.
	 */
	static Instruction duoPool(Snapshot s)
	{
		int target = duoPoolTarget(s.round);
		if (target == 0 || s.essence < 0 || s.essence > target)
		{
			String until = target == 0 ? "Finish him: stay until Tempoross is subdued, then leave."
				: "Take Tempoross down to " + target + "%" + (s.essence >= 0 ? " (now " + s.essence + "%)." : ".");
			return Instruction.of("Attack the spirit pool", until, Target.SPIRIT_POOL);
		}
		// This round's share is done: get a full bag of cooked fish ready for the next round.
		int cooked = s.cooked + s.crystal;
		if (s.bagFull() && s.raw > 0 || s.cooking && s.raw > 0)
		{
			return Instruction.of("Cook at the shrine", "Down to " + s.essence + "%. Cook all " + s.raw + " for the next round.", Target.SHRINE);
		}
		if (s.bagFull() && cooked > 0)
		{
			return Instruction.of("Wait for the next round", "Full bag of cooked fish. Load it when Tempoross gets back up.", Target.CRATE);
		}
		return fishStep(s, "Back to fishing", "Down to " + s.essence + "%. Fill your bag with fish and cook it all for the next round.");
	}

	/**
	 * Todd's duo route in the energy phase. Round 1 starts like the mass route (8, cook, double
	 * spot until the wave). After that, every round: fill the bag, cook it all, load until empty,
	 * and repeat until Tempoross is at 0%, then go to the spirit pool.
	 */
	static Instruction duoRoute(Snapshot s, PlannerSettings set)
	{
		int cooked = s.cooked + s.crystal;
		if (s.round <= 1 && s.wavesThisRound == 0 && s.loadedThisRound == 0 && !s.bagFull())
		{
			// Same opener as the mass route.
			return route(s, set);
		}
		if (s.nearCrate && cooked > 0 && s.raw == 0)
		{
			return Instruction.of("Load the cannon", "Load until your bag is empty (" + cooked + " left).", Target.CRATE);
		}
		if (s.bagFull() && s.raw > 0)
		{
			return Instruction.of("Cook at the shrine", "Bag full: cook all " + s.raw + " before loading.", Target.SHRINE);
		}
		if (s.bagFull() && cooked > 0)
		{
			return Instruction.of("Load the cannon", "Full bag of cooked fish: load it all.", Target.CRATE);
		}
		if (s.energy >= 0 && s.energy <= 3 && s.fish() == 0)
		{
			return Instruction.soon("Head to the spirit pool", "Tempoross is at " + Math.max(0, s.energy) + "%. Next pool: take him down to " + duoPoolTarget(s.round) + "%.", Target.SPIRIT_POOL);
		}
		return fishStep(s, "Fish until full", s.freeSlots + " slots left, then cook it all and load it (Tempoross is at " + Math.max(0, s.energy) + "%).");
	}

	/** Fish the double spot if there is one, otherwise the nearest spot. */
	private static Instruction fishStep(Snapshot s, String title, String detail)
	{
		if (s.doubleSpot && !s.bagFull())
		{
			return Instruction.of("Fish the double spot", detail, Target.DOUBLE_SPOT);
		}
		return Instruction.of(title, detail, Target.FISHING_SPOT);
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

		// Don't break off half way through cooking.
		if (s.cooking && s.raw > 0 && cook != CookRule.NEVER)
		{
			return Instruction.of("Keep cooking", s.raw + " raw left to cook.", Target.SHRINE);
		}

		if (set.style.isToadFxRoute() && set.mode != GameMode.SOLO)
		{
			if (s.fireDistance >= 0 && s.fireDistance <= FIRE_IN_THE_WAY && s.waterBuckets > 0)
			{
				return Instruction.soon("Douse the fire", "It's right next to you and spreads fast (+40).", Target.FIRE);
			}
			return set.style == PlayStyle.DUO_ROUTE ? duoRoute(s, set) : route(s, set);
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

		// Energy almost gone and nothing to load: get in place for the spirit pool.
		if (s.energy >= 0 && s.energy <= 3 && loadable == 0 && set.style != PlayStyle.CHILL)
		{
			return Instruction.soon("Head to the spirit pool", "Tempoross is about to go down. Be first at the pool.", Target.SPIRIT_POOL);
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

	/** Fish left to load in this leg of a solo route, or -1 if the style has no fixed counts. */
	static int loadTarget(Snapshot s, PlannerSettings set)
	{
		if (set.mode != GameMode.SOLO || !set.soloMethod.hasTargets())
		{
			return -1;
		}
		SoloMethod m = set.soloMethod;
		int li = m.legIndex(s.loadedTotal);
		return li >= m.legCount() ? 0 : m.start(li) + m.leg(li).fish - s.loadedTotal;
	}

	// --------------------------------------------------------------------- solo routes (OSRS Wiki)

	/** Fires this close get put out before a clearFires leg. */
	static final int CLEAR_FIRES_RANGE = 15;
	/** Pause this long after the first part of a split load (wiki: "wait a second"). */
	static final int PAUSE_MS = 2400;

	/**
	 * Solo routes, leg by leg. Loads are counted over the whole game, so it doesn't matter
	 * whether Tempoross is up or under when a leg is loaded.
	 */
	static Instruction solo(Snapshot s, PlannerSettings set)
	{
		SoloMethod m = set.soloMethod;
		int li = m.legIndex(s.loadedTotal);
		boolean poolPhase = s.phase == Phase.ESSENCE || s.spiritPool && s.energy == 0;
		CookRule cook = set.cookRule();
		int loadable = loadable(s, cook);

		// Tempoross often goes under part way through a load: finish the whole load first,
		// then go to the pool (Todd: "load the 19 entirely before heading towards the spirit pool").
		boolean finishing = false;
		if (poolPhase && li < m.legCount())
		{
			SoloMethod.Leg cur = m.leg(li);
			int curDone = s.loadedTotal - m.start(li);
			finishing = curDone > 0 && curDone < cur.fish && (cur.dropTrick || loadable > 0);
		}
		if (poolPhase && !finishing)
		{
			Instruction pool = soloPool(s, m.poolAfter(li), li >= m.legCount());
			if (pool != null)
			{
				return pool;
			}
		}
		if (li >= m.legCount())
		{
			return Instruction.soon("Head to the spirit pool", "Everything's loaded. Wait for the pool to open, then finish Tempoross off.", Target.SPIRIT_POOL);
		}

		SoloMethod.Leg leg = m.leg(li);
		int done = s.loadedTotal - m.start(li);
		int left = leg.fish - done;
		SoloMethod.PoolRule rule = m.poolAfter(li);
		boolean wentToPool = rule.leaveAt > 0 && rule.leaveAt <= 100 && (rule.onlyIfAbove < 0 || s.poolOpenEssence > rule.onlyIfAbove);
		String skip = !poolPhase ? "" : finishing ? " Finish the load, then the spirit pool."
			: wentToPool ? (s.fish() == 0 ? " Leave the pool now." : "") : " Skip the spirit pool.";

		// Race the storm (not while holding a split or drop-trick load: those go in a set order).
		if (loadable > 0 && stormDanger(s, set) && !leg.dropTrick && leg.split == 0)
		{
			return Instruction.now("Load now", "Storm at " + s.storm + "%. Load " + Math.min(loadable, left) + " now, top up after.", Target.CRATE);
		}
		if (s.cooking && s.raw > 0 && cook != CookRule.NEVER)
		{
			return Instruction.of("Keep cooking", s.raw + " raw left to cook.", Target.SHRINE);
		}
		if (s.fireDistance >= 0 && s.fireDistance <= FIRE_IN_THE_WAY && s.waterBuckets > 0)
		{
			return Instruction.soon("Douse the fire", "It's right next to you and spreads fast (+40).", Target.FIRE);
		}

		if (leg.dropTrick)
		{
			return dropTrick(s, set, leg, done, cook);
		}

		// Mid-load: follow the split.
		if (done > 0)
		{
			int chunk = leg.chunkFor(s.roundStartEnergy);
			if (chunk > 0 && done == chunk && s.msSinceLoad >= 0 && s.msSinceLoad < PAUSE_MS)
			{
				return Instruction.of("Wait a moment", "Fill a bucket, fix a mast or stand still so no fish are wasted. Then load the other " + left + ".", Target.CRATE);
			}
			if (chunk > 0 && done < chunk && loadable > 0)
			{
				return Instruction.of("Load the cannon", "Load " + (chunk - done) + " more, then pause a moment.", Target.CRATE);
			}
			if (leg.split > 0 && loadable > 0)
			{
				return splitStep(s, leg.split, done, left);
			}
			if (loadable >= left || loadable > 0 && s.nearCrate)
			{
				return Instruction.of("Load the cannon", "Load the other " + left + "." + skip, Target.CRATE);
			}
			return gather(s, set, cook, left, leg.noDoubles, "for the rest of this load." + skip);
		}

		// Before the leg: chores, room, then catch and cook enough.
		if (leg.clearFires && s.fireDistance >= 0 && s.fireDistance <= CLEAR_FIRES_RANGE)
		{
			if (s.waterBuckets > 0)
			{
				return Instruction.of("Put out the fires", "No waves at 10% energy, so fires stay and spread. Clear them now (+40 each).", Target.FIRE);
			}
			if (s.emptyBuckets > 0)
			{
				return Instruction.of("Fill your buckets", "Then put out the fires before they spread.", Target.WATER_PUMP);
			}
		}
		if (set.style.fightsFires())
		{
			Instruction chore = chore(s);
			if (chore != null)
			{
				return chore;
			}
		}
		if (leg.dropBuckets && s.emptyBuckets + s.waterBuckets > 0 && s.fish() + s.freeSlots < leg.gather)
		{
			return Instruction.of("Drop your buckets", "You need " + leg.gather + " free slots for fish. Keep them only if fires block the spots." + skip, Target.NONE);
		}
		// A full bag that's all cooked is ready even if it's a few short: load it and top up after.
		boolean ready = loadable >= leg.gather || s.bagFull() && (cook == CookRule.NEVER || s.raw == 0) && loadable > 0;
		if (!ready)
		{
			return gather(s, set, cook, leg.gather, leg.noDoubles, "before you load." + skip);
		}
		if (leg.afterResurface && poolPhase)
		{
			return Instruction.of("Wait for him to come back up", "Hold your " + loadable + " fish. When Tempoross is back up, load " + leg.split + " in one crate and the rest in the other.", Target.CRATE);
		}
		if (leg.afterSubmerge && !poolPhase)
		{
			return Instruction.of("Wait for him to go under", "Hold your " + loadable + " fish. Once Tempoross goes under, load " + leg.split + " in one crate and " + (leg.fish - leg.split) + " in the other.", Target.CRATE);
		}
		int chunk = leg.chunkFor(s.roundStartEnergy);
		if (chunk > 0)
		{
			return Instruction.of("Load the cannon", "Load " + chunk + " first, pause a moment, then load " + (leg.fish - chunk) + ".", Target.CRATE);
		}
		if (leg.split > 0)
		{
			return splitStep(s, leg.split, 0, left);
		}
		if (leg.fish == 1)
		{
			return Instruction.of("Load 1 fish", "Just one. Then wait for Tempoross to go under. Keep the rest.", Target.CRATE);
		}
		if (loadable < left)
		{
			return Instruction.of("Load the cannon", "Load your " + loadable + ", then catch " + (left - loadable) + " more to make " + left + "." + skip, Target.CRATE);
		}
		boolean keep = loadable > left;
		String thenPool = leg.submerges && !poolPhase ? " Load all of them before you go to the spirit pool." : "";
		return Instruction.of("Load the cannon", "Load exactly " + left + (keep ? " and keep the other " + (loadable - left) : "") + "." + thenPool + skip, Target.CRATE);
	}

	/** The spirit pool for a solo route, or null to get on with the next leg. */
	private static Instruction soloPool(Snapshot s, SoloMethod.PoolRule rule, boolean finish)
	{
		if (finish || rule.leaveAt == 0)
		{
			return !s.spiritPool
				? Instruction.soon("Head to the spirit pool", "Wait for it to open, then finish Tempoross off.", Target.SPIRIT_POOL)
				: Instruction.of("Attack the spirit pool", "Finish him: stay until Tempoross is subdued.", Target.SPIRIT_POOL);
		}
		if (rule.leaveAt > 100)
		{
			return null;
		}
		if (rule.onlyIfAbove >= 0 && (s.poolOpenEssence < 0 || s.poolOpenEssence <= rule.onlyIfAbove))
		{
			return null;
		}
		if (rule.leaveAt > 0 && s.essence >= 0 && s.essence <= rule.leaveAt)
		{
			return null;
		}
		String detail = rule.leaveAt < 0
			? "+55 a hit until Tempoross resurfaces."
			: "Take him down to " + rule.leaveAt + "%" + (s.essence >= 0 ? " (now " + s.essence + "%)" : "") + ", then go fishing.";
		return !s.spiritPool
			? Instruction.soon("Head to the spirit pool", detail, Target.SPIRIT_POOL)
			: Instruction.of("Attack the spirit pool", detail, Target.SPIRIT_POOL);
	}

	/** Put {@code first} in one crate and the rest in the other. */
	private static Instruction splitStep(Snapshot s, int first, int done, int left)
	{
		if (done < first)
		{
			return Instruction.of("Load " + first + " in one crate", "Then " + (done + left - first) + " in the other. " + done + " / " + first + " in this one.", Target.CRATE);
		}
		if (s.nearCrate && !s.atFirstCrate)
		{
			return Instruction.of("Load this crate", "Load the other " + left + " here.", Target.CRATE);
		}
		return Instruction.of("Go to the other crate", "Load the other " + left + " there.", Target.OTHER_CRATE);
	}

	/**
	 * Jagd's finish: catch and cook a full bag, drop it at cannon 1, catch and cook 16, load
	 * them into cannon 2, then pick the dropped fish up and load cannon 1.
	 */
	private static Instruction dropTrick(Snapshot s, PlannerSettings set, SoloMethod.Leg leg, int done, CookRule cook)
	{
		int drop = leg.dropCount;
		int second = leg.secondBatch();
		int cooked = loadable(s, cook);
		if (done == 0 && s.groundFish < drop)
		{
			if (cooked >= drop || cooked > 0 && s.raw == 0 && s.bagFull())
			{
				return Instruction.of("Drop your fish at cannon 1", "Don't load them: drop all " + cooked + " on the ground next to the cannon, then run back and fish 16.", Target.CRATE);
			}
			return gather(s, set, cook, drop, false, "then drop them at cannon 1.");
		}
		if (done < second)
		{
			if (cooked >= second - done || done > 0 && cooked > 0)
			{
				return s.nearCrate && !s.atDropCrate
					? Instruction.of("Load cannon 2", "Load all " + (second - done) + " here. Your dropped fish wait at cannon 1.", Target.CRATE)
					: Instruction.of("Load cannon 2", "Load your " + (second - done) + " into the other cannon, not the one your fish are by.", Target.OTHER_CRATE);
			}
			return gather(s, set, cook, second - done, false, "then load them into cannon 2.");
		}
		int left = leg.fish - done;
		if (s.groundFish > 0 && cooked < left)
		{
			return Instruction.of("Pick up your fish", "Quickly grab the " + s.groundFish + " at cannon 1.", Target.DROP_CRATE);
		}
		if (cooked > 0)
		{
			return Instruction.of("Load cannon 1", "Load all " + Math.min(cooked, left) + " you picked up, straight away, no matter what. Then the spirit pool.", Target.DROP_CRATE);
		}
		return gather(s, set, cook, left, false, "to finish the load.");
	}

	/** Catch (and cook) until you hold {@code need} fish ready to load. */
	private static Instruction gather(Snapshot s, PlannerSettings set, CookRule cook, int need, boolean noDoubles, String why)
	{
		int ready = loadable(s, cook);
		if (cook != CookRule.NEVER && s.raw > 0)
		{
			boolean enough = ready + s.raw >= need;
			boolean opener = ready == 0 && s.raw >= PlannerSettings.batchSize(s.fishingLevel) && (!s.doubleSpot || noDoubles);
			if (enough || s.bagFull() || opener)
			{
				return Instruction.of("Cook at the shrine", "Cook your " + s.raw + " raw. " + Math.min(need, ready + s.raw) + " / " + need + " " + why, Target.SHRINE);
			}
		}
		if (s.bagFull())
		{
			return Instruction.of("Make room", "Your bag is full. Drop buckets you don't need: you want room for " + need + " fish.", Target.NONE);
		}
		String line = s.fish() + " / " + need + " " + why;
		if (s.doubleSpot && !noDoubles)
		{
			return Instruction.of("Fish the double spot", line, Target.DOUBLE_SPOT);
		}
		return Instruction.of("Fish harpoonfish", line, Target.FISHING_SPOT);
	}

	/** Fires and broken masts close by, for firefighting routes. */
	private static Instruction chore(Snapshot s)
	{
		if (s.fireDistance >= 0 && s.fireDistance <= CHORE_RANGE && s.waterBuckets > 0)
		{
			return Instruction.of("Douse the fire", "+40 points.", Target.FIRE);
		}
		if (s.damagedTetherDistance >= 0 && s.damagedTetherDistance <= CHORE_RANGE && s.hasHammer)
		{
			return Instruction.of("Repair the mast", "+40 points, and it's ready for the next wave.", Target.DAMAGED_TETHER);
		}
		return null;
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
		if (set.style == PlayStyle.MAX_PERMITS && s.energy >= 0 && s.energy <= COOKING_METHOD_LOAD_ENERGY && s.raw == 0)
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
		if (cook == CookRule.OPENER_THEN_FULL_BAG)
		{
			// Opening batch while waiting for a double; after that, only a full bag gets cooked.
			int done = s.cooked + s.crystal;
			if (done == 0)
			{
				return set.style.openingBatch();
			}
			// Part way through cooking the opener: keep going.
			return done < set.style.openingBatch() && s.loadedThisRound == 0 ? 1 : Integer.MAX_VALUE;
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

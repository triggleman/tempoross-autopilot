package com.toadfx.temporossautopilot.model;

/**
 * Solo routes from the OSRS Wiki's Tempoross strategies. A route is a list of legs: how many
 * fish to load, counted over the whole game (Tempoross can stay up between legs, and fish
 * loaded while he's under still count). After each leg there's a rule for the spirit pool.
 */
public enum SoloMethod
{
	/** Follow the play style; no fixed load counts. */
	STYLE("Follow play style", new Leg[0], new PoolRule[0]),

	/** Wiki solo cooking: 17, 19, pool, 19, pool, 27, finish. */
	ROUTE_17_19_19_27("17 / 19 / 19 / 27",
		new Leg[]{Leg.of(17).staysUp(), Leg.of(19), Leg.of(19), Leg.of(27)},
		new PoolRule[]{PoolRule.ATTACK, PoolRule.ATTACK, PoolRule.ATTACK, PoolRule.FINISH}),

	/**
	 * Wiki 5-round method: 26 (load 18, pause, the rest), pool until he resurfaces (aim for under 50%), 26 (load 9 or 10,
	 * pause, 17), no pool, catch 28: load 1, then 5 + 13 in two crates while he's under, keep 9,
	 * catch 9 more and load 18 over both crates, then finish.
	 */
	FIVE_ROUNDS("5 rounds (26 / 26 / 1 / 18 / 18)",
		new Leg[]{
			Leg.of(26).chunk(18).cookOpener(),
			Leg.of(26).chunk(Leg.CHUNK_9_OR_10),
			Leg.of(1).gather(28).dropBuckets(),
			Leg.of(18).split(5).afterSubmerge().staysUp(),
			Leg.of(18).split(5).noDoubles().afterResurface()},
		new PoolRule[]{PoolRule.ATTACK, PoolRule.SKIP_UNLESS_60, PoolRule.SKIP, PoolRule.SKIP, PoolRule.FINISH}),

	/**
	 * Jagd's 10+ permits route ("How to Solo Tempoross & Get 10+ Permits EVERY Run"): 16 takes
	 * Tempoross to 10% energy (no waves or lightning), put the fires out, a full bag (19) before
	 * 93% storm, pool to ~60%, a full bag, pool to 33%, then a full bag dropped at cannon 1,
	 * 16 into cannon 2, pick up and load cannon 1, finish. A full bag is 19 with 7 buckets, a
	 * rope and a hammer (barbarian fishing, or 6 buckets and a harpoon).
	 */
	TEN_PERMITS("10 permits, Jagd (16 / 19 / 19 / 19 + 16)",
		new Leg[]{Leg.of(16).staysUp(), Leg.of(19).clearFires(), Leg.of(19), Leg.of(35).dropTrick(19)},
		new PoolRule[]{PoolRule.to(60), PoolRule.to(60), PoolRule.to(33), PoolRule.FINISH}),

	/** Wiki cooking + firefighting: 17, 19, pool, 19, pool to under 50%, 2, finish. */
	FIREFIGHTING("Cooking + firefighting (17 / 19 / 19 / 2)",
		new Leg[]{Leg.of(17).staysUp(), Leg.of(19), Leg.of(19), Leg.of(2)},
		new PoolRule[]{PoolRule.ATTACK, PoolRule.ATTACK, PoolRule.to(50), PoolRule.FINISH}),

	/** No cooking: 26, then 27 a trip, all raw (max Fishing XP). */
	NO_COOK("No cooking (26 / 27 / 27 / 27)",
		new Leg[]{Leg.of(26), Leg.of(27), Leg.of(27), Leg.of(27)},
		new PoolRule[]{PoolRule.ATTACK, PoolRule.ATTACK, PoolRule.ATTACK, PoolRule.FINISH});

	/** One leg of a route. */
	public static final class Leg
	{
		/** {@link #chunk}: load 9 if Tempoross started the round at 52% or less, else 10. */
		public static final int CHUNK_9_OR_10 = -2;

		/** Fish to load in this leg. */
		public final int fish;
		/** Cooked fish to hold before loading starts (usually the same). */
		public int gather;
		/** Load this many first, pause a moment, then the rest (0 = all at once). */
		public int chunk;
		/** Put this many in one crate and the rest in the other (0 = any crate). */
		public int split;
		/** Drop your buckets before fishing this leg (you need every slot). */
		public boolean dropBuckets;
		/** Only load once Tempoross has gone under. */
		public boolean afterSubmerge;
		/** Tempoross goes under when this leg is loaded (false: e.g. 17 only takes him to ~4%). */
		public boolean submerges = true;
		/** Only load once Tempoross is back up. */
		public boolean afterResurface;
		/** Double spots vanish while he's under: fish normal spots. */
		public boolean noDoubles;
		/** Catch {@link #dropCount}, drop them at cannon 1, catch the rest, load cannon 2, pick up, load cannon 1. */
		public boolean dropTrick;
		/** Fish dropped at cannon 1 in the drop trick. */
		public int dropCount;
		/** Cook a first batch of 7-10 before the bag is full (wiki 5-round round 1); otherwise catch the whole count, then cook. */
		public boolean cookOpener;
		/** Put out the fires before this leg (no waves at 10% energy, so fires stay and spread). */
		public boolean clearFires;

		private Leg(int fish)
		{
			this.fish = fish;
			this.gather = fish;
		}

		static Leg of(int fish)
		{
			return new Leg(fish);
		}

		Leg gather(int n)
		{
			gather = n;
			return this;
		}

		Leg chunk(int n)
		{
			chunk = n;
			return this;
		}

		Leg split(int n)
		{
			split = n;
			return this;
		}

		Leg dropBuckets()
		{
			dropBuckets = true;
			return this;
		}

		Leg afterSubmerge()
		{
			afterSubmerge = true;
			return this;
		}

		Leg staysUp()
		{
			submerges = false;
			return this;
		}

		Leg afterResurface()
		{
			afterResurface = true;
			return this;
		}

		Leg noDoubles()
		{
			noDoubles = true;
			return this;
		}

		Leg dropTrick(int drop)
		{
			dropTrick = true;
			dropCount = drop;
			gather = drop;
			return this;
		}

		Leg cookOpener()
		{
			cookOpener = true;
			return this;
		}

		Leg clearFires()
		{
			clearFires = true;
			return this;
		}

		/** Fish loaded into cannon 2 in the drop trick, before picking the dropped ones up. */
		public int secondBatch()
		{
			return fish - dropCount;
		}

		/** Fish to load before the pause, given the energy Tempoross started the round at. */
		public int chunkFor(int roundStartEnergy)
		{
			if (chunk == CHUNK_9_OR_10)
			{
				return roundStartEnergy >= 0 && roundStartEnergy <= 52 ? 9 : 10;
			}
			return chunk;
		}
	}

	/** What to do at the spirit pool after a leg. */
	public static final class PoolRule
	{
		/** Fish the pool until Tempoross resurfaces. */
		public static final PoolRule ATTACK = new PoolRule(-1, -1);
		/** Fish the pool until he's subdued. */
		public static final PoolRule FINISH = new PoolRule(0, -1);
		/** Don't fish the pool: get on with the next leg. */
		public static final PoolRule SKIP = new PoolRule(101, -1);
		/** Skip it, unless essence is still above 60% when the pool opens: then down to 55%. */
		public static final PoolRule SKIP_UNLESS_60 = new PoolRule(55, 60);

		/** Leave once essence is at or below this (-1 = stay until he resurfaces, 0 = finish, 101 = don't go). */
		public final int leaveAt;
		/** Only go if essence was above this when the pool opened (-1 = always). */
		public final int onlyIfAbove;

		private PoolRule(int leaveAt, int onlyIfAbove)
		{
			this.leaveAt = leaveAt;
			this.onlyIfAbove = onlyIfAbove;
		}

		static PoolRule to(int essence)
		{
			return new PoolRule(essence, -1);
		}
	}

	private final String label;
	private final Leg[] legs;
	private final PoolRule[] pools;

	SoloMethod(String label, Leg[] legs, PoolRule[] pools)
	{
		this.label = label;
		this.legs = legs;
		this.pools = pools;
	}

	public boolean hasTargets()
	{
		return legs.length > 0;
	}

	public int legCount()
	{
		return legs.length;
	}

	public Leg leg(int i)
	{
		return legs[i];
	}

	/** Fish loaded in the whole game before leg i starts. */
	public int start(int i)
	{
		int n = 0;
		for (int k = 0; k < i && k < legs.length; k++)
		{
			n += legs[k].fish;
		}
		return n;
	}

	/** The leg you're on, given fish loaded this game (= legs finished; legCount() when all done). */
	public int legIndex(int loadedTotal)
	{
		int n = 0;
		for (int i = 0; i < legs.length; i++)
		{
			n += legs[i].fish;
			if (loadedTotal < n)
			{
				return i;
			}
		}
		return legs.length;
	}

	/** The spirit pool rule once this many legs are done. */
	public PoolRule poolAfter(int legsDone)
	{
		if (legsDone <= 0 || pools.length == 0)
		{
			return PoolRule.ATTACK;
		}
		return pools[Math.min(legsDone, pools.length) - 1];
	}

	/** Fish in the first leg (for the "Ready" line). */
	public int firstLoad()
	{
		return legs.length == 0 ? -1 : legs[0].chunk > 0 ? legs[0].chunk : legs[0].fish;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

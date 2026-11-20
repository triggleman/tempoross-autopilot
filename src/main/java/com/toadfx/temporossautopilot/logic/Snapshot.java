package com.toadfx.temporossautopilot.logic;

import com.toadfx.temporossautopilot.model.Harpoon;
import java.util.EnumSet;
import java.util.Set;

/** Everything the planner needs about the game right now. Plain fields so tests can build one. */
public class Snapshot
{
	public Phase phase = Phase.IDLE;

	/** HUD bars, 0-100, or -1 when unknown. */
	public int energy = -1;
	public int essence = -1;
	public int storm = -1;
	public int points;
	/** 1-based energy round (goes up each time Tempoross recovers from the spirit pool). */
	public int round = 1;

	// Inventory
	public int raw;
	public int cooked;
	public int crystal;
	public int freeSlots = 28;
	public int emptyBuckets;
	public int waterBuckets;
	public boolean hasRope;
	public boolean hasHammer;
	public boolean hasHarpoon = true;
	public int fishingLevel = 1;

	// Gear you own (from the bank, inventory and worn items) vs what you have on you.
	/** Best harpoon carried or worn, or null. */
	public Harpoon carriedHarpoon;
	public Set<Harpoon> ownedHarpoons = EnumSet.noneOf(Harpoon.class);
	/** Per slot (hat, top, legs, boots): owned and worn pieces of each outfit. */
	public boolean[] ownsSpirit = new boolean[4];
	public boolean[] ownsAngler = new boolean[4];
	public boolean[] wearsSpirit = new boolean[4];
	public boolean[] wearsAngler = new boolean[4];
	public boolean ownsImcando;
	public boolean carriesImcando;

	// Scene
	public boolean waveIncoming;
	public boolean tethered;
	public boolean doubleSpot;
	/** The spirit pool is open. */
	public boolean spiritPool;
	/** The spirit pool is there but not open yet. */
	public boolean poolInactive;
	/** Tiles to the closest fire, or -1 if none. */
	public int fireDistance = -1;
	/** Tiles to the closest broken mast or totem, or -1 if none. */
	public int damagedTetherDistance = -1;
	public boolean nearCrate;
	public boolean cooking;

	/** Fish this player has loaded since the current energy round started. */
	public int loadedThisRound;
	/** Seconds until the storm reaches the danger mark at its current rate, or -1 if not rising. */
	public int stormSecondsLeft = -1;
	/** Seconds to walk to the crate from here, or -1 if unknown. */
	public int secondsToCrate = -1;

	/** A copy for looking ahead (arrays and sets copied too). */
	public Snapshot copy()
	{
		Snapshot c = new Snapshot();
		c.phase = phase; c.energy = energy; c.essence = essence; c.storm = storm; c.points = points; c.round = round;
		c.raw = raw; c.cooked = cooked; c.crystal = crystal; c.freeSlots = freeSlots;
		c.emptyBuckets = emptyBuckets; c.waterBuckets = waterBuckets;
		c.hasRope = hasRope; c.hasHammer = hasHammer; c.hasHarpoon = hasHarpoon; c.fishingLevel = fishingLevel;
		c.carriedHarpoon = carriedHarpoon;
		c.ownedHarpoons = ownedHarpoons.isEmpty() ? EnumSet.noneOf(Harpoon.class) : EnumSet.copyOf(ownedHarpoons);
		c.ownsSpirit = ownsSpirit.clone(); c.ownsAngler = ownsAngler.clone();
		c.wearsSpirit = wearsSpirit.clone(); c.wearsAngler = wearsAngler.clone();
		c.ownsImcando = ownsImcando; c.carriesImcando = carriesImcando;
		c.waveIncoming = waveIncoming; c.tethered = tethered; c.doubleSpot = doubleSpot;
		c.spiritPool = spiritPool; c.poolInactive = poolInactive;
		c.fireDistance = fireDistance; c.damagedTetherDistance = damagedTetherDistance;
		c.nearCrate = nearCrate; c.cooking = cooking; c.loadedThisRound = loadedThisRound;
		c.stormSecondsLeft = stormSecondsLeft; c.secondsToCrate = secondsToCrate;
		return c;
	}

	public int fish()
	{
		return raw + cooked + crystal;
	}

	public boolean bagFull()
	{
		return freeSlots <= 0;
	}

	public boolean inGame()
	{
		return phase == Phase.ENERGY || phase == Phase.ESSENCE;
	}
}

package com.toadfx.temporossautopilot.state;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Tempoross game IDs, all from RuneLite's gameval API. */
public final class Ids
{
	private Ids()
	{
	}

	/** Tempoross Cove (the instance), found with WorldPoint.fromLocalInstance. */
	public static final int COVE_REGION = 12076;
	/** The boat at the Ruins of Unkah where players wait for the next game. */
	public static final int BOAT_REGION = 12332;
	/** Ruins of Unkah around the boat and reward pool. */
	public static final int LOBBY_REGION = 12588;

	// NPCs
	public static final int DOUBLE_SPOT = NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL;
	public static final int SPIRIT_POOL = NpcID.TEMPOROSS_P2_FISHINGSPOT;
	/** The spirit pool before it opens. */
	public static final int SPIRIT_POOL_INACTIVE = NpcID.TEMPOROSS_P2_FISHINGSPOT_INACTIVE;
	public static final Set<Integer> FISHING_SPOTS = set(
		NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH,
		NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SOUTH,
		NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL);
	public static final Set<Integer> AMMO_CRATES = set(
		NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1,
		NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2,
		NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3,
		NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4);
	public static final Set<Integer> FIRES = set(NpcID.TEMPOROSS_FIRE_HITBOX);
	public static final Set<Integer> VICTORY_HOSTS = set(
		NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY,
		NpcID.TEMPOROSS_INSTANCE_HOST_W_VICTORY,
		NpcID.TEMPOROSS_INSTANCE_HOST_N_VICTORY,
		NpcID.TEMPOROSS_INSTANCE_HOST_S_VICTORY);

	// Objects
	public static final int SHRINE = ObjectID.TEMPOROSS_SHRINE_FIRE;
	public static final Set<Integer> TETHERS = set(
		ObjectID.TEMPOROSS_MAST_BOTTOM_WEST,
		ObjectID.TEMPOROSS_MAST_BOTTOM_EAST,
		ObjectID.TEMPOROSS_TOTEM_NORTH,
		ObjectID.TEMPOROSS_TOTEM_SOUTH);
	public static final Set<Integer> DAMAGED_TETHERS = set(
		ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN,
		ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN,
		ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN,
		ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN);
	public static final Set<Integer> WATER_PUMPS = set(
		ObjectID.TEMPOROSS_WATER_PUMP,
		ObjectID.TEMPOROSS_WATER_PUMP_DOCK);
	public static final Set<Integer> SUPPLY_CRATES = set(
		ObjectID.TEMPOROSS_CRATE_HAMMER,
		ObjectID.TEMPOROSS_CRATE_ROPE,
		ObjectID.TEMPOROSS_CRATE_BUCKET,
		ObjectID.TEMPOROSS_CRATE_HARPOON);

	/** Lightning cloud shadow: a fire lands here. Short = a fire about to jump to this tile. */
	public static final int CLOUD = ObjectID.TEMPOROSS_LIGHTNING_SHADOW;
	public static final int CLOUD_SHORT = ObjectID.TEMPOROSS_LIGHTNING_SHADOW_SHORT;
	public static final int FIRE_VISUAL = ObjectID.TEMPOROSS_FIRE_VISUALS;

	// Timings (milliseconds), as observed in game.
	/** Cloud shadow to fire. */
	public static final int CLOUD_MS = 9600;
	/** Short shadow (spreading fire) to fire. */
	public static final int CLOUD_SHORT_MS = 1200;
	/** A fire spreads to a new tile this long after it starts. */
	public static final int FIRE_SPREAD_MS = 24000;
	/** "A colossal wave closes in" to the wave hitting. */
	public static final int WAVE_MS = 7800;

	// Items
	public static final int RAW_FISH = ItemID.TEMPOROSS_RAW_HARPOONFISH;
	public static final int COOKED_FISH = ItemID.TEMPOROSS_HARPOONFISH;
	public static final int CRYSTAL_FISH = ItemID.TEMPOROSS_CRYSTALLISED_HARPOONFISH;

	public static boolean isHarpoonfish(int id)
	{
		return id == RAW_FISH || id == COOKED_FISH || id == CRYSTAL_FISH;
	}
	public static final Set<Integer> HARPOONS = set(
		ItemID.HARPOON,
		ItemID.HUNTING_BARBED_HARPOON,
		ItemID.DRAGON_HARPOON,
		ItemID.INFERNAL_HARPOON,
		ItemID.INFERNAL_HARPOON_EMPTY,
		ItemID.CRYSTAL_HARPOON,
		ItemID.CRYSTAL_HARPOON_INACTIVE,
		ItemID.TRAILBLAZER_HARPOON,
		ItemID.TRAILBLAZER_HARPOON_EMPTY,
		ItemID.TRAILBLAZER_HARPOON_NO_INFERNAL,
		ItemID.TRAILBLAZER_RELOADED_HARPOON,
		ItemID.TRAILBLAZER_RELOADED_HARPOON_EMPTY,
		ItemID.TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL);
	public static final Set<Integer> HAMMERS = set(ItemID.HAMMER, ItemID.IMCANDO_HAMMER);
	public static final Set<Integer> SPIRIT_ANGLER = set(
		ItemID.SPIRIT_ANGLER_HAT,
		ItemID.SPIRIT_ANGLER_TOP,
		ItemID.SPIRIT_ANGLER_LEGS,
		ItemID.SPIRIT_ANGLER_BOOTS);

	// Chat (lower case, matched with contains)
	public static final String WAVE_INCOMING = "a colossal wave closes in";
	public static final String WAVE_SAFE = "as the wave washes over you";
	public static final String WAVE_HIT = "the wave slams into you";
	public static final String VULNERABLE = "tempoross is vulnerable";
	public static final String SCATTER = "the glowing fish scatter";
	public static final String SUBDUED = "tempoross is subdued";
	public static final String CATCH = "you catch a";
	public static final String CATCH_EXTRA = "catch an extra harpoonfish";
	public static final String COOKED = "you successfully cook a harpoonfish";

	private static Set<Integer> set(int... ids)
	{
		Set<Integer> s = new HashSet<>();
		for (int id : ids)
		{
			s.add(id);
		}
		return s;
	}
}

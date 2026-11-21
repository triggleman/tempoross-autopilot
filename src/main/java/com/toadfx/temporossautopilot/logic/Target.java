package com.toadfx.temporossautopilot.logic;

/** What the current step points at in the scene. */
public enum Target
{
	NONE,
	SUPPLY_CRATES,
	DOUBLE_SPOT,
	FISHING_SPOT,
	SHRINE,
	CRATE,
	/** The ammunition crate you didn't load into first (split loads). */
	OTHER_CRATE,
	/** The ammunition crate your dropped fish are lying by (drop trick, "cannon 1"). */
	DROP_CRATE,
	SPIRIT_POOL,
	TETHER,
	DAMAGED_TETHER,
	FIRE,
	WATER_PUMP,
	LEAVE
}

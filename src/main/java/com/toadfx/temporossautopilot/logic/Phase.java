package com.toadfx.temporossautopilot.logic;

/** Where the player is in a Tempoross game. */
public enum Phase
{
	/** Not at Tempoross. */
	IDLE,
	/** In the cove, the game has not started (or the HUD is not up yet). */
	WAITING,
	/** Load harpoonfish into the cannons to drain Tempoross's energy. */
	ENERGY,
	/** Energy is 0: fish the spirit pool to drain essence. */
	ESSENCE,
	/** Tempoross is subdued. */
	WON
}

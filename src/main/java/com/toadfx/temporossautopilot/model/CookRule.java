package com.toadfx.temporossautopilot.model;

/** When to cook harpoonfish at the shrine. */
public enum CookRule
{
	/** Cook every fish before loading (cooked = 65 points and 15 energy, raw = 20 and 10). */
	ALWAYS("Always"),
	/** Never cook: fish and load raw. */
	NEVER("Never"),
	/** The wiki cooking method: catch a batch (7, or 9 at 85+ Fishing), then cook it. */
	BATCH("In batches");

	private final String label;

	CookRule(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

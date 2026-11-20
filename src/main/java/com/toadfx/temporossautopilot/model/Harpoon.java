package com.toadfx.temporossautopilot.model;

import net.runelite.api.gameval.ItemID;

/** Harpoons that work at Tempoross, with what the OSRS Wiki says each is best for. */
public enum Harpoon
{
	BASIC("harpoon", "", ItemID.HARPOON),
	BARB_TAIL("barb-tail harpoon", "Same speed as a harpoon, but you wield it and free a slot.", ItemID.HUNTING_BARBED_HARPOON),
	DRAGON("dragon harpoon", "Its special adds +3 Fishing levels. Save it for double spots.", ItemID.DRAGON_HARPOON),
	INFERNAL("infernal harpoon", "Cooks 1 in 3 fish for you: the best for points.",
		ItemID.INFERNAL_HARPOON, ItemID.INFERNAL_HARPOON_EMPTY,
		ItemID.TRAILBLAZER_HARPOON, ItemID.TRAILBLAZER_HARPOON_EMPTY, ItemID.TRAILBLAZER_HARPOON_NO_INFERNAL,
		ItemID.TRAILBLAZER_RELOADED_HARPOON, ItemID.TRAILBLAZER_RELOADED_HARPOON_EMPTY, ItemID.TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL),
	CRYSTAL("crystal harpoon", "1 in 3 chance of crystallised fish: the best for Fishing XP.",
		ItemID.CRYSTAL_HARPOON, ItemID.CRYSTAL_HARPOON_INACTIVE);

	private final String label;
	private final String why;
	private final int[] ids;

	Harpoon(String label, String why, int... ids)
	{
		this.label = label;
		this.why = why;
		this.ids = ids;
	}

	public String getLabel()
	{
		return label;
	}

	public String getWhy()
	{
		return why;
	}

	public static Harpoon of(int itemId)
	{
		for (Harpoon h : values())
		{
			for (int id : h.ids)
			{
				if (id == itemId)
				{
					return h;
				}
			}
		}
		return null;
	}

	/** How good this harpoon is for a play style. Higher is better. */
	public int rank(PlayStyle style)
	{
		switch (this)
		{
			case CRYSTAL:
				return style == PlayStyle.MAX_XP ? 5 : 4;
			case INFERNAL:
				return style == PlayStyle.MAX_XP ? 4 : 5;
			case DRAGON:
				return 3;
			case BARB_TAIL:
				return 2;
			default:
				return 1;
		}
	}

	/** The better of two harpoons for a style (either may be null). */
	public static Harpoon best(Harpoon a, Harpoon b, PlayStyle style)
	{
		if (a == null)
		{
			return b;
		}
		if (b == null)
		{
			return a;
		}
		return b.rank(style) > a.rank(style) ? b : a;
	}
}

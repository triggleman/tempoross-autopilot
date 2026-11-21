package com.toadfx.temporossautopilot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What to bring for a play style (OSRS Wiki Tempoross strategies, Theoatrix for the team styles,
 * Todd for his own routes). Buckets, Humidify runes and free slots are checked in the lobby; the
 * tips are shown in the side panel.
 */
public final class Loadout
{
	/** Use the "Buckets to carry" setting. */
	public static final int SETTING = -1;

	/** Buckets to start with, or {@link #SETTING}. */
	private final int buckets;
	/** True if the setting may raise the bucket count. */
	private boolean atLeast;
	/** Humidify runes (rune pouch with astral, fire and water; Lunar spellbook). */
	private final boolean humidify;
	/** Free inventory slots wanted when the game starts (0 = no rule). */
	private final int freeSlots;
	/** Style-specific tips, best first. */
	private final List<String> tips;

	Loadout(int buckets, boolean humidify, int freeSlots, String... tips)
	{
		this.buckets = buckets;
		this.humidify = humidify;
		this.freeSlots = freeSlots;
		List<String> t = new ArrayList<>();
		Collections.addAll(t, tips);
		this.tips = Collections.unmodifiableList(t);
	}

	public static Loadout of(PlayStyle style)
	{
		switch (style)
		{
			case SOLO_RECOMMENDED:
				return new Loadout(4, true, 0, "Infernal or barb-tail harpoon");
			case SOLO_FIREFIGHT:
				return new Loadout(6, true, 19, "Infernal or dragon harpoon", "Tome of water, or a steam battlestaff at high Fishing");
			case SOLO_FIVE_ROUNDS:
				return new Loadout(0, false, 27, "Dragon or barb-tail harpoon (wear it)", "Take about 4 buckets between rounds, up to 10 if fires are near the spots");
			case SOLO_TEN_PERMITS:
				return new Loadout(7, false, 19, "Barbarian fishing, or swap a bucket for a harpoon", "A full bag is 19 fish: 7 buckets + rope + hammer leaves 19 slots");
			case SOLO_XP:
				return new Loadout(SETTING, false, 0, "Crystal harpoon (most XP), or dragon", "Elven signet saves crystal harpoon charges");
			case MAX_XP:
				return new Loadout(SETTING, false, 0, "Crystal harpoon (most XP), or dragon");
			case FIREFIGHTER:
			{
				Loadout l = new Loadout(4, false, 0, "Humidify runes refill buckets without the pump");
				l.atLeast = true;
				return l;
			}
			case DUO_ROUTE:
			{
				Loadout l = new Loadout(3, false, 0, "Best harpoon you own");
				l.atLeast = true;
				return l;
			}
			case CHILL:
				return new Loadout(SETTING, false, 0);
			default:
				return new Loadout(SETTING, false, 0, "Best harpoon you own");
		}
	}

	/** Buckets to start with; the setting is used when the style has no number, and can raise Firefighter and the duo route. */
	public int buckets(int setting)
	{
		if (buckets == SETTING)
		{
			return setting;
		}
		return atLeast ? Math.max(buckets, setting) : buckets;
	}

	public boolean wantsHumidify()
	{
		return humidify;
	}

	public int getFreeSlots()
	{
		return freeSlots;
	}

	public List<String> getTips()
	{
		return tips;
	}
}

package com.toadfx.temporossautopilot.state;

import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.gameval.ItemID;

/**
 * Remembers which Tempoross gear the player owns (harpoons, angler and spirit angler pieces),
 * from the bank, inventory and worn items, so the plugin can remind them to bring it.
 * Saved per character. Seeing the bank refreshes the list, so sold items drop off.
 */
public class GearMemory
{
	public static final int[] ANGLER = {
		ItemID.TRAWLER_REWARD_HAT, ItemID.TRAWLER_REWARD_TOP, ItemID.TRAWLER_REWARD_LEGS, ItemID.TRAWLER_REWARD_BOOTS
	};
	public static final int[] SPIRIT_ANGLER = {
		ItemID.SPIRIT_ANGLER_HAT, ItemID.SPIRIT_ANGLER_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.SPIRIT_ANGLER_BOOTS
	};

	private final Set<Integer> owned = new TreeSet<>();
	private int[] lastInventory = new int[0];
	private int[] lastWorn = new int[0];
	private boolean changed;

	public static boolean tracked(int id)
	{
		return Ids.HARPOONS.contains(id) || contains(ANGLER, id) || contains(SPIRIT_ANGLER, id);
	}

	/** Items carried or worn: always owned. */
	public void sawCarried(int[] inventory, int[] worn)
	{
		lastInventory = inventory;
		lastWorn = worn;
		addAll(inventory);
		addAll(worn);
	}

	/** The whole bank: what's owned is now exactly bank + carried + worn. */
	public void sawBank(int[] bank)
	{
		Set<Integer> before = new TreeSet<>(owned);
		owned.clear();
		addAll(bank);
		addAll(lastInventory);
		addAll(lastWorn);
		changed |= !before.equals(owned);
	}

	private void addAll(int[] ids)
	{
		for (int id : ids)
		{
			if (tracked(id) && owned.add(id))
			{
				changed = true;
			}
		}
	}

	public boolean owns(int id)
	{
		return owned.contains(id);
	}

	public int ownedPieces(int[] set)
	{
		int n = 0;
		for (int id : set)
		{
			if (owned.contains(id))
			{
				n++;
			}
		}
		return n;
	}

	public Set<Integer> getOwned()
	{
		return owned;
	}

	/** True once since the last call if the owned list changed (time to save it). */
	public boolean takeChanged()
	{
		boolean c = changed;
		changed = false;
		return c;
	}

	public String serialize()
	{
		StringBuilder sb = new StringBuilder();
		for (int id : owned)
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(id);
		}
		return sb.toString();
	}

	public void load(String s)
	{
		owned.clear();
		if (s == null || s.isEmpty())
		{
			return;
		}
		for (String part : s.split(","))
		{
			try
			{
				int id = Integer.parseInt(part.trim());
				if (tracked(id))
				{
					owned.add(id);
				}
			}
			catch (NumberFormatException ignored)
			{
				// skip
			}
		}
		changed = false;
	}

	static boolean contains(int[] set, int id)
	{
		for (int s : set)
		{
			if (s == id)
			{
				return true;
			}
		}
		return false;
	}
}

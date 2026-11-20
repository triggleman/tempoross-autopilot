package com.toadfx.temporossautopilot.state;

import com.toadfx.temporossautopilot.coach.GameRecord;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.StormClock;
import com.toadfx.temporossautopilot.model.Harpoon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Reads the Tempoross game from the client every tick: HUD bars, points, inventory,
 * fishing spots, crates, fires, masts and chat. Keeps a record of the current game
 * for the planner and report card.
 */
public class GameTracker
{
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");
	/** Tiles from a crate that count as "at the crate" for loading. */
	private static final int CRATE_REACH = 2;
	private static final int IDLE_GRACE_TICKS = 3;

	private final Client client;
	private final GearMemory gear;
	private final StormClock storm = new StormClock();
	private int region;

	// Scene, kept from spawn and despawn events.
	private final Set<NPC> spots = new HashSet<>();
	private final Set<NPC> crates = new HashSet<>();
	private final Set<NPC> fires = new HashSet<>();
	private NPC doubleSpot;
	/** There's a spirit pool on each side of the cove; we always point at the nearest one. */
	private final Set<NPC> pools = new HashSet<>();
	private final Set<NPC> inactivePools = new HashSet<>();
	/** Ticks spent outside the cove; a game only ends after a few, so a loading blip doesn't split it. */
	private int outsideTicks;
	private static final int OUTSIDE_TICKS_TO_END = 5;
	/** Timers: when each lightning cloud turns to fire, and when each fire next spreads (epoch ms). */
	private final Map<GameObject, Long> clouds = new ConcurrentHashMap<>();
	private final Map<GameObject, Long> fireSpread = new ConcurrentHashMap<>();
	private volatile long waveHitsAt;
	private final Set<GameObject> shrines = new HashSet<>();
	private final Set<GameObject> tethers = new HashSet<>();
	private final Set<GameObject> damagedTethers = new HashSet<>();
	private final Set<GameObject> pumps = new HashSet<>();
	private final Set<GameObject> supplyCrates = new HashSet<>();

	private Phase phase = Phase.IDLE;
	private boolean hudUp;
	private int energy = -1;
	private int essence = -1;
	private int stormPercent = -1;
	private int points;
	private int round = 1;
	private int loadedThisRound;
	private boolean waveIncoming;
	private boolean wonSeen;
	/** Lowest essence seen while the spirit pool was open this game (0 = subdued). */
	private int lowestPoolEssence = 100;
	private int permitsAtStart = -1;
	/** Catches and cooks from chat this tick (more reliable than inventory changes). */
	private int chatCooks;

	// Inventory last tick, for counting catches, cooks and loads.
	private int lastRaw;
	private int lastCooked;
	private int lastCrystal;
	private Snapshot snapshot = new Snapshot();

	// The current game.
	private GameRecord game;
	private long gameStartMillis;
	private int startFishingXp;
	private int startCookingXp;
	private boolean doubleFished;
	private WorldPoint lastTile;
	private int stillTicks;

	private Consumer<GameRecord> onGameEnd = r -> { };
	private Runnable onDoubleSpot = () -> { };
	private Runnable onWave = () -> { };

	public GameTracker(Client client, GearMemory gear)
	{
		this.client = client;
		this.gear = gear;
	}

	public void setListeners(Consumer<GameRecord> onGameEnd, Runnable onDoubleSpot, Runnable onWave)
	{
		this.onGameEnd = onGameEnd;
		this.onDoubleSpot = onDoubleSpot;
		this.onWave = onWave;
	}

	// ------------------------------------------------------------------ scene events

	public void npcSpawned(NPC npc)
	{
		int id = npc.getId();
		if (Ids.FISHING_SPOTS.contains(id))
		{
			spots.add(npc);
		}
		if (id == Ids.DOUBLE_SPOT)
		{
			doubleSpot = npc;
			doubleFished = false;
			if (game != null && phase == Phase.ENERGY && nearMe(npc, 20))
			{
				// Only doubles on your side of the island count (mass worlds have them everywhere).
				game.doubles++;
				onDoubleSpot.run();
			}
		}
		else if (id == Ids.SPIRIT_POOL)
		{
			pools.add(npc);
		}
		else if (id == Ids.SPIRIT_POOL_INACTIVE)
		{
			inactivePools.add(npc);
		}
		else if (Ids.AMMO_CRATES.contains(id))
		{
			crates.add(npc);
		}
		else if (Ids.FIRES.contains(id))
		{
			fires.add(npc);
		}
		else if (Ids.VICTORY_HOSTS.contains(id))
		{
			wonSeen = true;
		}
	}

	public void npcDespawned(NPC npc)
	{
		spots.remove(npc);
		crates.remove(npc);
		fires.remove(npc);
		if (npc == doubleSpot)
		{
			doubleSpot = null;
		}
		pools.remove(npc);
		inactivePools.remove(npc);
	}

	public void objectSpawned(GameObject obj)
	{
		int id = obj.getId();
		if (id == Ids.SHRINE)
		{
			shrines.add(obj);
		}
		else if (Ids.TETHERS.contains(id))
		{
			tethers.add(obj);
		}
		else if (Ids.DAMAGED_TETHERS.contains(id))
		{
			damagedTethers.add(obj);
		}
		else if (Ids.WATER_PUMPS.contains(id))
		{
			pumps.add(obj);
		}
		else if (Ids.SUPPLY_CRATES.contains(id))
		{
			supplyCrates.add(obj);
		}
		else if (id == Ids.CLOUD)
		{
			clouds.put(obj, System.currentTimeMillis() + Ids.CLOUD_MS);
		}
		else if (id == Ids.CLOUD_SHORT)
		{
			clouds.put(obj, System.currentTimeMillis() + Ids.CLOUD_SHORT_MS);
		}
		else if (id == Ids.FIRE_VISUAL)
		{
			fireSpread.put(obj, System.currentTimeMillis() + Ids.FIRE_SPREAD_MS);
		}
	}

	public void objectDespawned(GameObject obj)
	{
		shrines.remove(obj);
		tethers.remove(obj);
		damagedTethers.remove(obj);
		pumps.remove(obj);
		supplyCrates.remove(obj);
		clouds.remove(obj);
		fireSpread.remove(obj);
	}

	/** Scene is reloading: every object and NPC will spawn again. */
	public void clearScene()
	{
		spots.clear();
		crates.clear();
		fires.clear();
		doubleSpot = null;
		pools.clear();
		inactivePools.clear();
		clouds.clear();
		fireSpread.clear();
		shrines.clear();
		tethers.clear();
		damagedTethers.clear();
		pumps.clear();
		supplyCrates.clear();
	}

	public void chat(String raw)
	{
		String msg = raw.toLowerCase();
		if (msg.contains(Ids.WAVE_INCOMING))
		{
			waveIncoming = true;
			waveHitsAt = System.currentTimeMillis() + Ids.WAVE_MS;
			onWave.run();
		}
		else if (msg.contains(Ids.WAVE_SAFE))
		{
			waveIncoming = false;
			waveHitsAt = 0;
			if (game != null)
			{
				game.wavesSafe++;
			}
		}
		else if (msg.contains(Ids.WAVE_HIT))
		{
			waveIncoming = false;
			waveHitsAt = 0;
			if (game != null)
			{
				game.wavesHit++;
			}
		}
		else if (msg.contains(Ids.SCATTER) && game != null)
		{
			game.scatters++;
		}
		else if (msg.contains(Ids.SUBDUED))
		{
			wonSeen = true;
		}
		else if (game != null && (msg.startsWith(Ids.CATCH) || msg.contains(Ids.CATCH_EXTRA)))
		{
			game.fishCaught++;
		}
		else if (game != null && msg.startsWith(Ids.COOKED))
		{
			game.fishCooked++;
			chatCooks++;
		}
	}

	// ------------------------------------------------------------------ every tick

	public void tick()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return;
		}
		WorldPoint here = me.getWorldLocation();
		region = WorldPoint.fromLocalInstance(client, me.getLocalLocation()).getRegionID();
		boolean reallyInCove = region == Ids.COVE_REGION;
		outsideTicks = reallyInCove ? 0 : outsideTicks + 1;
		// While a game is on, only believe we've left once it's been a few ticks.
		boolean inCove = reallyInCove || game != null && outsideTicks < OUTSIDE_TICKS_TO_END;
		boolean onBoat = region == Ids.BOAT_REGION;

		readHud(inCove);
		Snapshot s = new Snapshot();
		readInventory(s);

		Phase before = phase;
		if (game != null && !pools.isEmpty() && essence >= 0)
		{
			lowestPoolEssence = Math.min(lowestPoolEssence, essence);
		}
		phase = nextPhase(inCove, onBoat);
		if (phase == Phase.IDLE || phase == Phase.WAITING)
		{
			waveIncoming = false;
			waveHitsAt = 0;
		}
		// Fires keep spreading: roll each timer on to the next spread.
		long now = System.currentTimeMillis();
		for (Map.Entry<GameObject, Long> e : fireSpread.entrySet())
		{
			if (e.getValue() < now)
			{
				e.setValue(e.getValue() + Ids.FIRE_SPREAD_MS * ((now - e.getValue()) / Ids.FIRE_SPREAD_MS + 1));
			}
		}

		// Start and end of games.
		if (game == null && (phase == Phase.ENERGY || phase == Phase.ESSENCE) && hudUp)
		{
			startGame();
		}
		if (game != null)
		{
			if (phase == Phase.WON)
			{
				endGame(true, s.fish());
			}
			else if (!inCove)
			{
				endGame(wonByEvidence(), 0);
			}
		}

		// Rounds: back to the energy phase after the spirit pool.
		if (before == Phase.ESSENCE && phase == Phase.ENERGY)
		{
			round++;
			loadedThisRound = 0;
			if (game != null)
			{
				game.rounds = round;
			}
		}

		countInventoryChanges(s, here);
		if (game != null && phase == Phase.ENERGY)
		{
			trackDoubleAndIdle(me, here);
		}
		storm.sample(phase == Phase.ENERGY ? stormPercent : -1);
		if (phase != Phase.ENERGY)
		{
			storm.reset();
		}

		fillSnapshot(s, me, here);
		snapshot = s;
	}

	/**
	 * Did we win, judging by what we saw? The essence bar hitting 0, the reward permit count going
	 * up, or the victory hosts / subdued message all count.
	 */
	private boolean wonByEvidence()
	{
		if (wonSeen || lowestPoolEssence <= 0)
		{
			return true;
		}
		int permits = client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS);
		return permitsAtStart >= 0 && permits > permitsAtStart;
	}

	private Phase nextPhase(boolean inCove, boolean onBoat)
	{
		if (!inCove)
		{
			if (game == null)
			{
				wonSeen = false;
			}
			return onBoat ? Phase.WAITING : Phase.IDLE;
		}
		if (wonSeen || game != null && lowestPoolEssence <= 0)
		{
			return Phase.WON;
		}
		if (!hudUp)
		{
			return game != null ? phase : Phase.WAITING;
		}
		// The pool stays open while Tempoross's energy recharges, so the pool decides the phase.
		if (energy == 0 || !pools.isEmpty())
		{
			return Phase.ESSENCE;
		}
		return Phase.ENERGY;
	}

	private void startGame()
	{
		game = new GameRecord();
		gameStartMillis = System.currentTimeMillis();
		startFishingXp = client.getSkillExperience(Skill.FISHING);
		startCookingXp = client.getSkillExperience(Skill.COOKING);
		round = 1;
		loadedThisRound = 0;
		lowestPoolEssence = 100;
		wonSeen = false;
		permitsAtStart = client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS);
		lastTile = null;
		stillTicks = 0;
		storm.reset();
	}

	private void endGame(boolean won, int leftover)
	{
		GameRecord r = game;
		game = null;
		r.won = won;
		r.points = Math.max(r.points, points);
		r.endedAtMillis = System.currentTimeMillis();
		r.seconds = (int) ((r.endedAtMillis - gameStartMillis) / 1000);
		r.fishingXp = Math.max(0, client.getSkillExperience(Skill.FISHING) - startFishingXp);
		r.cookingXp = Math.max(0, client.getSkillExperience(Skill.COOKING) - startCookingXp);
		r.leftoverFish = leftover;
		r.rounds = round;
		waveIncoming = false;
		if (!wonSeen && won)
		{
			wonSeen = true;
		}
		onGameEnd.accept(r);
	}

	// ------------------------------------------------------------------ HUD

	private void readHud(boolean inCove)
	{
		Widget content = inCove ? client.getWidget(InterfaceID.TemporossHud.CONTENT) : null;
		hudUp = content != null;
		if (!hudUp)
		{
			energy = essence = stormPercent = -1;
			return;
		}
		energy = readBar(InterfaceID.TemporossHud.ENERGY_TITLE, InterfaceID.TemporossHud.ENERGY_BAR, InterfaceID.TemporossHud.ENERGY_BAR_BACK);
		essence = readBar(InterfaceID.TemporossHud.ESSENCE_TITLE, InterfaceID.TemporossHud.ESSENCE_BAR, InterfaceID.TemporossHud.ESSENCE_BAR_BACK);
		stormPercent = readBar(InterfaceID.TemporossHud.STORM_INTENSITY_TITLE, InterfaceID.TemporossHud.STORM_INTENSITY_BAR, InterfaceID.TemporossHud.STORM_INTENSITY_BAR_BACK);
		int varbitPoints = client.getVarbitValue(VarbitID.TEMPOROSS_CURRENT_POINTS);
		points = varbitPoints > 0 ? varbitPoints : Math.max(0, number(client.getWidget(InterfaceID.TemporossHud.POINTS_TEXT)));
		if (game != null)
		{
			game.points = Math.max(game.points, points);
		}
	}

	/** A bar's percent from its title text ("Energy: 64%"), else from the bar's width. */
	private int readBar(int title, int fill, int back)
	{
		int fromText = number(client.getWidget(title));
		if (fromText >= 0)
		{
			return Math.min(100, fromText);
		}
		Widget f = client.getWidget(fill);
		Widget b = client.getWidget(back);
		if (f == null || b == null || b.getWidth() <= 0)
		{
			return -1;
		}
		return Math.max(0, Math.min(100, f.getWidth() * 100 / b.getWidth()));
	}

	static int firstNumber(String text)
	{
		if (text == null)
		{
			return -1;
		}
		Matcher m = NUMBER.matcher(text);
		if (!m.find())
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(m.group(1));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private static int number(Widget w)
	{
		if (w == null)
		{
			return -1;
		}
		int n = firstNumber(w.getText());
		if (n >= 0)
		{
			return n;
		}
		Widget[] kids = w.getDynamicChildren();
		if (kids != null)
		{
			for (Widget k : kids)
			{
				n = firstNumber(k == null ? null : k.getText());
				if (n >= 0)
				{
					return n;
				}
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------ inventory

	private void readInventory(Snapshot s)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int used = 0;
		boolean harpoon = false;
		boolean hammer = false;
		boolean rope = false;
		if (inv != null)
		{
			for (Item it : inv.getItems())
			{
				int id = it.getId();
				if (id <= 0)
				{
					continue;
				}
				used++;
				if (id == Ids.RAW_FISH)
				{
					s.raw++;
				}
				else if (id == Ids.COOKED_FISH)
				{
					s.cooked++;
				}
				else if (id == Ids.CRYSTAL_FISH)
				{
					s.crystal++;
				}
				else if (id == net.runelite.api.gameval.ItemID.BUCKET_EMPTY)
				{
					s.emptyBuckets++;
				}
				else if (id == net.runelite.api.gameval.ItemID.BUCKET_WATER)
				{
					s.waterBuckets++;
				}
				else if (id == net.runelite.api.gameval.ItemID.ROPE)
				{
					rope = true;
				}
				harpoon |= Ids.HARPOONS.contains(id);
				hammer |= Ids.HAMMERS.contains(id);
				s.carriesImcando |= id == net.runelite.api.gameval.ItemID.IMCANDO_HAMMER;
				s.carriedHarpoon = Harpoon.best(s.carriedHarpoon, Harpoon.of(id), com.toadfx.temporossautopilot.model.PlayStyle.MAX_PERMITS);
			}
		}
		int angler = 0;
		if (worn != null)
		{
			for (Item it : worn.getItems())
			{
				int id = it.getId();
				harpoon |= Ids.HARPOONS.contains(id);
				hammer |= Ids.HAMMERS.contains(id);
				s.carriesImcando |= id == net.runelite.api.gameval.ItemID.IMCANDO_HAMMER;
				s.carriedHarpoon = Harpoon.best(s.carriedHarpoon, Harpoon.of(id), com.toadfx.temporossautopilot.model.PlayStyle.MAX_PERMITS);
				for (int i = 0; i < 4; i++)
				{
					if (id == GearMemory.SPIRIT_ANGLER[i])
					{
						s.wearsSpirit[i] = true;
						angler++;
					}
					if (id == GearMemory.ANGLER[i])
					{
						s.wearsAngler[i] = true;
					}
				}
			}
		}
		for (int i = 0; i < 4; i++)
		{
			s.ownsSpirit[i] = gear.owns(GearMemory.SPIRIT_ANGLER[i]);
			s.ownsAngler[i] = gear.owns(GearMemory.ANGLER[i]);
		}
		s.ownsImcando = gear.owns(net.runelite.api.gameval.ItemID.IMCANDO_HAMMER);
		for (int id : gear.getOwned())
		{
			Harpoon h = Harpoon.of(id);
			if (h != null)
			{
				s.ownedHarpoons.add(h);
			}
		}
		s.freeSlots = 28 - used;
		s.hasHarpoon = harpoon;
		s.hasHammer = hammer;
		// The full spirit angler's outfit works as a rope.
		s.hasRope = rope || angler >= 4;
	}

	private void countInventoryChanges(Snapshot s, WorldPoint here)
	{
		if (game != null && (phase == Phase.ENERGY || phase == Phase.ESSENCE))
		{
			int dRaw = s.raw - lastRaw;
			int dCooked = s.cooked - lastCooked;
			int dCrystal = s.crystal - lastCrystal;
			boolean atCrate = nearestDistance(crates, here) <= CRATE_REACH;

			// Catches and shrine cooks come from chat. A cooked fish appearing with no raw fish
			// used and no cook message is the infernal harpoon cooking it as it's caught.
			if (dCooked > 0 && dRaw >= 0 && chatCooks == 0 && !atCrate)
			{
				game.fishCooked += dCooked;
			}
			if (atCrate && !waveIncoming)
			{
				int rawOut = Math.max(0, -dRaw) + Math.max(0, -dCrystal);
				int cookedOut = Math.max(0, -dCooked);
				game.loadedRaw += rawOut;
				game.loadedCooked += cookedOut;
				loadedThisRound += rawOut + cookedOut;
			}
		}
		lastRaw = s.raw;
		lastCooked = s.cooked;
		lastCrystal = s.crystal;
		chatCooks = 0;
	}

	private void trackDoubleAndIdle(Player me, WorldPoint here)
	{
		Actor target = me.getInteracting();
		if (doubleSpot != null && target == doubleSpot && !doubleFished)
		{
			doubleFished = true;
			game.doublesFished++;
		}
		boolean still = here.equals(lastTile) && me.getAnimation() == -1 && target == null;
		stillTicks = still && !waveIncoming ? stillTicks + 1 : 0;
		if (stillTicks > IDLE_GRACE_TICKS && stillTicks % 5 == 0)
		{
			// Five ticks are three seconds.
			game.idleSeconds += 3;
		}
		lastTile = here;
	}

	private void fillSnapshot(Snapshot s, Player me, WorldPoint here)
	{
		s.phase = phase;
		s.energy = energy;
		s.essence = essence;
		s.storm = stormPercent;
		s.points = points;
		s.round = round;
		s.fishingLevel = client.getRealSkillLevel(Skill.FISHING);
		s.waveIncoming = waveIncoming;
		s.tethered = client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) > 0;
		s.doubleSpot = doubleSpot != null;
		s.spiritPool = !pools.isEmpty();
		s.poolInactive = !inactivePools.isEmpty();
		s.fireDistance = nearestDistance(fires, here);
		s.damagedTetherDistance = nearestObjectDistance(damagedTethers, here);
		int crate = nearestDistance(crates, here);
		s.nearCrate = crate >= 0 && crate <= CRATE_REACH;
		s.secondsToCrate = StormClock.runSeconds(crate);
		s.loadedThisRound = loadedThisRound;
		s.stormSecondsLeft = storm.secondsUntil(100);
	}

	// ------------------------------------------------------------------ helpers

	private boolean nearMe(NPC npc, int tiles)
	{
		Player me = client.getLocalPlayer();
		if (me == null || npc.getWorldLocation() == null)
		{
			return false;
		}
		return npc.getWorldLocation().distanceTo(me.getWorldLocation()) <= tiles;
	}

	private static int nearestDistance(Set<NPC> npcs, WorldPoint here)
	{
		int best = -1;
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (p == null || p.getPlane() != here.getPlane())
			{
				continue;
			}
			int d = p.distanceTo(here);
			if (best < 0 || d < best)
			{
				best = d;
			}
		}
		return best;
	}

	private static int nearestObjectDistance(Set<GameObject> objs, WorldPoint here)
	{
		int best = -1;
		for (GameObject o : objs)
		{
			WorldPoint p = o.getWorldLocation();
			if (p == null || p.getPlane() != here.getPlane())
			{
				continue;
			}
			int d = p.distanceTo(here);
			if (best < 0 || d < best)
			{
				best = d;
			}
		}
		return best;
	}

	public NPC nearestNpc(Set<NPC> npcs)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		WorldPoint here = me.getWorldLocation();
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (p == null)
			{
				continue;
			}
			int d = p.distanceTo(here);
			if (d < bestD)
			{
				bestD = d;
				best = n;
			}
		}
		return best;
	}

	public GameObject nearestObject(Set<GameObject> objs)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		WorldPoint here = me.getWorldLocation();
		GameObject best = null;
		int bestD = Integer.MAX_VALUE;
		for (GameObject o : objs)
		{
			WorldPoint p = o.getWorldLocation();
			if (p == null)
			{
				continue;
			}
			int d = p.distanceTo(here);
			if (d < bestD)
			{
				bestD = d;
				best = o;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ getters

	public Snapshot getSnapshot()
	{
		return snapshot;
	}

	public Phase getPhase()
	{
		return phase;
	}

	public GameRecord getGame()
	{
		return game;
	}

	public StormClock getStorm()
	{
		return storm;
	}

	public NPC getDoubleSpot()
	{
		return doubleSpot;
	}

	public NPC getSpiritPool()
	{
		return nearestNpc(pools);
	}

	/** The open spirit pool, or the closed one waiting to open. */
	public NPC getAnySpiritPool()
	{
		return !pools.isEmpty() ? nearestNpc(pools) : nearestNpc(inactivePools);
	}

	public Map<GameObject, Long> getClouds()
	{
		return clouds;
	}

	public Map<GameObject, Long> getFireSpread()
	{
		return fireSpread;
	}

	/** When the current wave hits (epoch ms), or 0 if there's no wave coming. */
	public long getWaveHitsAt()
	{
		return waveHitsAt;
	}

	/** When the current game started (epoch ms), or 0 if there's no game. */
	public long getGameStartMillis()
	{
		return game != null ? gameStartMillis : 0;
	}

	public int getRound()
	{
		return round;
	}

	public Set<NPC> getSpots()
	{
		return spots;
	}

	public Set<NPC> getCrates()
	{
		return crates;
	}

	public Set<NPC> getFires()
	{
		return fires;
	}

	public Set<GameObject> getShrines()
	{
		return shrines;
	}

	public Set<GameObject> getTethers()
	{
		return tethers;
	}

	public Set<GameObject> getDamagedTethers()
	{
		return damagedTethers;
	}

	public Set<GameObject> getPumps()
	{
		return pumps;
	}

	public List<GameObject> getSupplyCrates()
	{
		return new ArrayList<>(supplyCrates);
	}

	public int getRegion()
	{
		return region;
	}

	public int getLoadedThisRound()
	{
		return loadedThisRound;
	}
}

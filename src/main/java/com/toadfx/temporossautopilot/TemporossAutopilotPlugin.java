package com.toadfx.temporossautopilot;

import com.google.inject.Provides;
import com.toadfx.temporossautopilot.coach.GameRecord;
import com.toadfx.temporossautopilot.coach.Grader;
import com.toadfx.temporossautopilot.coach.PersonalBests;
import com.toadfx.temporossautopilot.coach.Session;
import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.PlannerSettings;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Target;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.overlay.PermitOverlay;
import com.toadfx.temporossautopilot.overlay.SceneOverlay;
import com.toadfx.temporossautopilot.overlay.StepOverlay;
import com.toadfx.temporossautopilot.overlay.StormOverlay;
import com.toadfx.temporossautopilot.state.GameTracker;
import com.toadfx.temporossautopilot.state.GearMemory;
import com.toadfx.temporossautopilot.state.Ids;
import com.toadfx.temporossautopilot.ui.TemporossAutopilotPanel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Tempoross Autopilot",
	description = "Tempoross: next-step guidance for mass, solo and duo, play styles (max permits, points or XP), a live permit tracker, storm timer and a grade after every game",
	tags = {"tempoross", "fishing", "cooking", "minigame", "boss", "skilling", "autopilot", "guide", "permits", "mass"}
)
public class TemporossAutopilotPlugin extends Plugin
{
	private static final String KEY_HISTORY = "history";
	private static final String KEY_BESTS = "bests";
	private static final String KEY_GEAR = "ownedGear";
	private static final int HISTORY_MAX = 50;

	@Inject
	private Client client;

	@Inject
	private TemporossAutopilotConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private StepOverlay stepOverlay;

	@Inject
	private PermitOverlay permitOverlay;

	@Inject
	private StormOverlay stormOverlay;

	@Inject
	private SceneOverlay sceneOverlay;

	private GameTracker tracker;
	private final GearMemory gear = new GearMemory();
	private boolean inLobby;
	private TemporossAutopilotPanel panel;
	private NavigationButton navButton;

	private volatile Snapshot snapshot = new Snapshot();
	private volatile Instruction instruction = Instruction.NONE;
	private volatile NPC targetNpc;
	private volatile GameObject targetObject;
	private volatile NPC targetCrate;
	private volatile int loadTarget = -1;

	private final Session session = new Session();
	private final List<GameRecord> history = Collections.synchronizedList(new ArrayList<>());
	private PersonalBests bests = new PersonalBests();
	private GameRecord lastGame;
	private boolean historyLoaded;

	// Alert bookkeeping.
	private boolean stormWarned;
	private boolean loadTargetWarned;
	private int lastPermits;
	private int panelTick;

	@Provides
	TemporossAutopilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TemporossAutopilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		tracker = new GameTracker(client, gear);
		tracker.setListeners(this::onGameEnd, this::onDoubleSpot, this::onWave);
		overlayManager.add(stepOverlay);
		overlayManager.add(permitOverlay);
		overlayManager.add(stormOverlay);
		overlayManager.add(sceneOverlay);

		BufferedImage logo = ImageUtil.loadImageResource(TemporossAutopilotPlugin.class, "panel_logo.png");
		BufferedImage icon = ImageUtil.loadImageResource(TemporossAutopilotPlugin.class, "panel_icon.png");
		panel = new TemporossAutopilotPanel(this, config, configManager, logo);
		navButton = NavigationButton.builder()
			.tooltip("Tempoross Autopilot")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		historyLoaded = false;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(stepOverlay);
		overlayManager.remove(permitOverlay);
		overlayManager.remove(stormOverlay);
		overlayManager.remove(sceneOverlay);
		clientToolbar.removeNavigation(navButton);
		tracker = null;
		snapshot = new Snapshot();
		instruction = Instruction.NONE;
		targetNpc = null;
		targetObject = null;
		targetCrate = null;
	}

	// ------------------------------------------------------------------ events

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (tracker == null)
		{
			return;
		}
		if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN || e.getGameState() == GameState.HOPPING)
		{
			tracker.clearScene();
		}
		if (e.getGameState() == GameState.LOGGED_IN && !historyLoaded)
		{
			loadHistory();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		int id = e.getContainerId();
		if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			gear.sawCarried(ids(client.getItemContainer(InventoryID.INV)), ids(client.getItemContainer(InventoryID.WORN)));
		}
		else if (id == InventoryID.BANK)
		{
			gear.sawBank(ids(e.getItemContainer()));
		}
		if (gear.takeChanged() && historyLoaded)
		{
			configManager.setRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_GEAR, gear.serialize());
		}
	}

	private static int[] ids(ItemContainer c)
	{
		if (c == null)
		{
			return new int[0];
		}
		Item[] items = c.getItems();
		int[] out = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			out[i] = items[i].getId();
		}
		return out;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (tracker != null)
		{
			tracker.npcSpawned(e.getNpc());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		if (tracker != null)
		{
			tracker.npcDespawned(e.getNpc());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (tracker != null)
		{
			tracker.objectSpawned(e.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		if (tracker != null)
		{
			tracker.objectDespawned(e.getGameObject());
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (tracker != null && (e.getType() == ChatMessageType.GAMEMESSAGE || e.getType() == ChatMessageType.SPAM))
		{
			tracker.chat(e.getMessage());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!TemporossAutopilotConfig.GROUP.equals(e.getGroup()))
		{
			return;
		}
		if ("coachReset".equals(e.getKey()) && config.coachReset())
		{
			history.clear();
			bests = new PersonalBests();
			lastGame = null;
			session.reset();
			configManager.unsetRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_HISTORY);
			configManager.unsetRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_BESTS);
			configManager.setConfiguration(TemporossAutopilotConfig.GROUP, "coachReset", false);
		}
		if (panel != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				panel.syncSettings(config);
				panel.refresh();
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (tracker == null)
		{
			return;
		}
		tracker.tick();
		Snapshot s = tracker.getSnapshot();
		PlannerSettings settings = settings();
		Instruction step = Planner.plan(s, settings);
		loadTarget = s.phase == Phase.ENERGY && settings.mode == GameMode.SOLO ? settings.soloMethod.loadTarget(s.round) : -1;

		resolveTargets(step);
		alerts(s);
		lobbyReminder(s, settings);

		snapshot = s;
		instruction = step;

		// The panel only needs a few updates a second.
		if (panel != null && (++panelTick % 2 == 0 || s.phase != Phase.IDLE))
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	private PlannerSettings settings()
	{
		PlannerSettings p = new PlannerSettings();
		p.style = config.playStyle();
		p.mode = config.gameMode();
		p.soloMethod = config.soloMethod();
		p.customCook = config.customCook();
		p.pointsTarget = config.pointsTarget();
		p.stormDanger = config.stormDanger();
		p.barehanded = config.barehanded();
		p.bucketsWanted = config.bucketsWanted();
		p.gearReminders = config.gearReminders();
		return p;
	}

	/** Once per visit to the Ruins of Unkah: a chat line naming gear you own but didn't bring. */
	private void lobbyReminder(Snapshot s, PlannerSettings settings)
	{
		int region = tracker.getRegion();
		boolean lobby = region == Ids.LOBBY_REGION || region == Ids.BOAT_REGION;
		if (lobby && !inLobby && settings.gearReminders)
		{
			List<String> missing = Planner.gearReminders(s, settings);
			if (!missing.isEmpty())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=4fc3cf>Tempoross Autopilot</col>: you didn't bring " + String.join(" or ", missing) + ".", null);
			}
		}
		if (region != Ids.COVE_REGION)
		{
			inLobby = lobby;
		}
	}

	/** Finds what the step points at, for the scene overlay. */
	private void resolveTargets(Instruction step)
	{
		NPC npc = null;
		GameObject obj = null;
		Target t = step.getTarget();
		switch (t)
		{
			case DOUBLE_SPOT:
				npc = tracker.getDoubleSpot();
				break;
			case FISHING_SPOT:
				npc = tracker.getDoubleSpot() != null ? tracker.getDoubleSpot() : tracker.nearestNpc(tracker.getSpots());
				break;
			case CRATE:
				npc = tracker.nearestNpc(tracker.getCrates());
				break;
			case SPIRIT_POOL:
				npc = tracker.getAnySpiritPool();
				break;
			case FIRE:
				npc = tracker.nearestNpc(tracker.getFires());
				break;
			case SHRINE:
				obj = tracker.nearestObject(tracker.getShrines());
				break;
			case TETHER:
				obj = tracker.nearestObject(tracker.getTethers());
				break;
			case DAMAGED_TETHER:
				obj = tracker.nearestObject(tracker.getDamagedTethers());
				break;
			case WATER_PUMP:
				obj = tracker.nearestObject(tracker.getPumps());
				break;
			default:
				break;
		}
		targetNpc = npc;
		targetObject = obj;
		targetCrate = tracker.nearestNpc(tracker.getCrates());
	}

	private void alerts(Snapshot s)
	{
		if (s.phase != Phase.ENERGY)
		{
			stormWarned = false;
		}
		else if (config.notifyStorm() && s.storm >= config.stormWarnAt() && !stormWarned)
		{
			stormWarned = true;
			notifier.notify("Tempoross Autopilot: storm at " + s.storm + "%. Load your fish!");
		}
		else if (s.storm >= 0 && s.storm < config.stormWarnAt() - 10)
		{
			stormWarned = false;
		}

		if (loadTarget > 0 && s.loadedThisRound >= loadTarget)
		{
			if (!loadTargetWarned && config.notifyLoadTarget())
			{
				notifier.notify("Tempoross Autopilot: " + loadTarget + " loaded. Stop loading this round.");
			}
			loadTargetWarned = true;
		}
		else
		{
			loadTargetWarned = false;
		}

		int permits = s.inGame() ? Permits.earned(s.points) : 0;
		if (permits > lastPermits && config.notifyPermit())
		{
			notifier.notify("Tempoross Autopilot: permit " + permits + " earned (" + String.format("%,d", s.points) + " points).");
		}
		lastPermits = permits;
	}

	private void onDoubleSpot()
	{
		if (config.notifyDouble())
		{
			notifier.notify("Tempoross Autopilot: a double harpoonfish spot appeared.");
		}
	}

	private void onWave()
	{
		if (config.notifyWave() && client.getVarbitValue(net.runelite.api.gameval.VarbitID.TEMPOROSS_TETHERED) == 0)
		{
			notifier.notify("Tempoross Autopilot: wave incoming! Tether to a mast or totem.");
		}
	}

	// ------------------------------------------------------------------ game end

	private void onGameEnd(GameRecord r)
	{
		if (r.points <= 0 && r.seconds < 30)
		{
			return;
		}
		r.style = config.playStyle();
		r.mode = config.gameMode();
		Grader.grade(r, config.pointsTarget());
		lastGame = r;
		session.add(r);
		List<String> beaten = bests.update(r);
		history.add(r);
		while (history.size() > HISTORY_MAX)
		{
			history.remove(0);
		}
		if (config.saveHistory())
		{
			saveHistory();
		}
		if (config.chatSummary())
		{
			String msg = "<col=4fc3cf>Tempoross Autopilot</col>: " + r.grade + " (" + r.score + ") · "
				+ String.format("%,d", r.points) + " points · " + r.permits() + " permit" + (r.permits() == 1 ? "" : "s")
				+ (r.bonusChance() > 0 ? " + " + r.bonusChance() + "%" : "")
				+ (r.tips.isEmpty() ? "" : " · " + r.tips.get(0));
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
			if (!beaten.isEmpty())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=f0b44c>New personal best</col>: " + String.join(", ", beaten), null);
			}
		}
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	private void saveHistory()
	{
		StringBuilder sb = new StringBuilder();
		for (GameRecord r : getHistory())
		{
			if (sb.length() > 0)
			{
				sb.append(';');
			}
			sb.append(r.serialize());
		}
		configManager.setRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_HISTORY, sb.toString());
		configManager.setRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_BESTS, bests.serialize());
	}

	private void loadHistory()
	{
		historyLoaded = true;
		history.clear();
		String saved = configManager.getRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_HISTORY);
		if (saved != null && !saved.isEmpty())
		{
			for (String line : saved.split(";"))
			{
				GameRecord r = GameRecord.parse(line);
				if (r != null)
				{
					Grader.grade(r, config.pointsTarget());
					history.add(r);
				}
			}
		}
		bests = PersonalBests.parse(configManager.getRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_BESTS));
		gear.load(configManager.getRSProfileConfiguration(TemporossAutopilotConfig.GROUP, KEY_GEAR));
		gear.sawCarried(ids(client.getItemContainer(InventoryID.INV)), ids(client.getItemContainer(InventoryID.WORN)));
		if (!history.isEmpty())
		{
			lastGame = history.get(history.size() - 1);
		}
		if (panel != null)
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	// ------------------------------------------------------------------ getters

	public GameTracker getTracker()
	{
		return tracker;
	}

	public Snapshot getSnapshot()
	{
		return snapshot;
	}

	public Instruction getInstruction()
	{
		return instruction;
	}

	public NPC getTargetNpc()
	{
		return targetNpc;
	}

	public GameObject getTargetObject()
	{
		return targetObject;
	}

	public NPC getTargetCrate()
	{
		return targetCrate;
	}

	public int getLoadTarget()
	{
		return loadTarget;
	}

	public Session getSession()
	{
		return session;
	}

	public PersonalBests getBests()
	{
		return bests;
	}

	public GameRecord getLastGame()
	{
		return lastGame;
	}

	public List<GameRecord> getHistory()
	{
		synchronized (history)
		{
			return Collections.unmodifiableList(new ArrayList<>(history));
		}
	}
}

package com.toadfx.temporossautopilot.ui;

import com.toadfx.temporossautopilot.TemporossAutopilotConfig;
import com.toadfx.temporossautopilot.TemporossAutopilotPlugin;
import com.toadfx.temporossautopilot.coach.GameRecord;
import com.toadfx.temporossautopilot.coach.PersonalBests;
import com.toadfx.temporossautopilot.coach.Session;
import com.toadfx.temporossautopilot.logic.Instruction;
import com.toadfx.temporossautopilot.logic.Permits;
import com.toadfx.temporossautopilot.logic.Phase;
import com.toadfx.temporossautopilot.logic.Planner;
import com.toadfx.temporossautopilot.logic.Snapshot;
import com.toadfx.temporossautopilot.logic.Urgency;
import com.toadfx.temporossautopilot.model.GameMode;
import com.toadfx.temporossautopilot.model.PlayStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar tab: play style and mode pickers, the next step, the live game, points and
 * permits, the session, the last game's grade and tips, personal bests and history.
 */
public class TemporossAutopilotPanel extends PluginPanel
{
	private static final Color ACCENT = new Color(79, 195, 207);
	private static final Color GOLD = new Color(240, 180, 76);
	private static final Color NOW = new Color(255, 80, 60);
	private static final Color GOOD = new Color(122, 212, 138);
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	private static final int HISTORY_ROWS = 8;

	private final TemporossAutopilotPlugin plugin;
	private final TemporossAutopilotConfig config;
	private final ConfigManager configManager;

	private final JComboBox<PlayStyle> styleBox = new JComboBox<>();
	private final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
	private final JLabel styleNote = new JLabel();
	private final JLabel stepTitle = new JLabel();
	private final JLabel stepDetail = new JLabel();
	private final JPanel bringRows = new JPanel();
	private JPanel bringSection;
	private final JPanel gameRows = new JPanel();
	private final JPanel pointRows = new JPanel();
	private final JPanel sessionRows = new JPanel();
	private final JPanel lastRows = new JPanel();
	private final JPanel bestRows = new JPanel();
	private final JPanel historyRows = new JPanel();
	private boolean syncing;

	public TemporossAutopilotPanel(TemporossAutopilotPlugin plugin, TemporossAutopilotConfig config, ConfigManager configManager, BufferedImage logo)
	{
		super(true);
		this.plugin = plugin;
		this.config = config;
		this.configManager = configManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(content, BorderLayout.NORTH);

		if (logo != null)
		{
			JLabel logoLabel = new JLabel(new ImageIcon(logo));
			logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
			JPanel logoRow = new JPanel(new BorderLayout());
			logoRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
			logoRow.add(logoLabel, BorderLayout.CENTER);
			logoRow.setBorder(new EmptyBorder(0, 0, 8, 0));
			content.add(logoRow);
		}

		content.add(section("Play style", settingsCard(config)));
		bringSection = section("What to bring", rows(bringRows));
		content.add(bringSection);

		JPanel stepCard = card();
		stepTitle.setFont(FontManager.getRunescapeBoldFont());
		stepTitle.setForeground(ACCENT);
		stepDetail.setFont(FontManager.getRunescapeSmallFont());
		stepDetail.setForeground(Color.WHITE);
		stepCard.add(stepTitle);
		stepCard.add(Box.createVerticalStrut(2));
		stepCard.add(stepDetail);
		content.add(section("Next step", stepCard));

		content.add(section("Game", rows(gameRows)));
		content.add(section("Points & permits", rows(pointRows)));
		content.add(section("Session", rows(sessionRows)));
		content.add(section("Last game", rows(lastRows)));
		content.add(section("Personal bests", rows(bestRows)));
		content.add(section("History", rows(historyRows)));

		JLabel credit = new JLabel("Made by ToadFx");
		credit.setFont(FontManager.getRunescapeSmallFont());
		credit.setForeground(new Color(120, 120, 120));
		credit.setBorder(new EmptyBorder(6, 2, 0, 0));
		content.add(credit);
		if (config.showSocials())
		{
			JPanel socials = SocialLinks.row();
			socials.setBorder(new EmptyBorder(4, 0, 0, 0));
			content.add(socials);
		}

		refresh();
	}

	// ------------------------------------------------------------------ settings

	private JPanel settingsCard(TemporossAutopilotConfig config)
	{
		JPanel p = card();
		modeBox.setSelectedItem(config.gameMode());
		fillStyles(config.gameMode(), config.playStyle());
		styleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		modeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		styleNote.setFont(FontManager.getRunescapeSmallFont());
		styleNote.setForeground(MUTED);
		styleNote.setAlignmentX(Component.LEFT_ALIGNMENT);
		// Show "Recommended duo" in duo, and mark the recommended style.
		styleBox.setRenderer(new javax.swing.DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus)
			{
				GameMode mode = (GameMode) modeBox.getSelectedItem();
				String text = value instanceof PlayStyle && mode != null ? ((PlayStyle) value).labelFor(mode) : String.valueOf(value);
				if (value instanceof PlayStyle && mode != null && value == PlayStyle.recommended(mode) && index >= 0)
				{
					text = text + "  \u2605";
				}
				return super.getListCellRendererComponent(list, text, index, selected, focus);
			}
		});

		styleBox.addActionListener(e ->
		{
			if (!syncing && styleBox.getSelectedItem() != null)
			{
				configManager.setConfiguration(TemporossAutopilotConfig.GROUP, "playStyle", styleBox.getSelectedItem());
			}
			updateStyleNote();
		});
		modeBox.addActionListener(e ->
		{
			GameMode mode = (GameMode) modeBox.getSelectedItem();
			if (syncing || mode == null)
			{
				return;
			}
			configManager.setConfiguration(TemporossAutopilotConfig.GROUP, "gameMode", mode);
			// Only this mode's styles; keep the current one if it fits, else the recommended one.
			PlayStyle current = (PlayStyle) styleBox.getSelectedItem();
			PlayStyle next = PlayStyle.effective(current, mode);
			fillStyles(mode, next);
			configManager.setConfiguration(TemporossAutopilotConfig.GROUP, "playStyle", next);
		});

		p.add(small("Mode"));
		p.add(modeBox);
		p.add(Box.createVerticalStrut(6));
		p.add(small("Style (\u2605 = recommended)"));
		p.add(styleBox);
		p.add(Box.createVerticalStrut(2));
		p.add(styleNote);
		updateStyleNote();
		return p;
	}

	/** Puts only the mode's styles in the dropdown and selects one. */
	private void fillStyles(GameMode mode, PlayStyle select)
	{
		boolean was = syncing;
		syncing = true;
		styleBox.removeAllItems();
		for (PlayStyle st : PlayStyle.forMode(mode))
		{
			styleBox.addItem(st);
		}
		styleBox.setSelectedItem(PlayStyle.effective(select, mode));
		syncing = was;
		updateStyleNote();
	}

	private void updateStyleNote()
	{
		PlayStyle st = (PlayStyle) styleBox.getSelectedItem();
		GameMode mode = (GameMode) modeBox.getSelectedItem();
		if (st != null)
		{
			String rec = mode != null && st == PlayStyle.recommended(mode) ? "Recommended for " + mode.toString().toLowerCase() + ". " : "";
			styleNote.setText(html(rec + st.getTagline() + ". Graded on " + st.getGradedOn() + "."));
		}
	}

	/** Matches the pickers to the config after it changes in the settings panel. */
	public void syncSettings(TemporossAutopilotConfig config)
	{
		syncing = true;
		modeBox.setSelectedItem(config.gameMode());
		syncing = false;
		fillStyles(config.gameMode(), config.playStyle());
	}

	// ------------------------------------------------------------------ refresh

	public void refresh()
	{
		Snapshot s = plugin.getSnapshot();
		Instruction step = plugin.getInstruction();

		if (step.isEmpty())
		{
			stepTitle.setText("Not at Tempoross");
			stepTitle.setForeground(MUTED);
			stepDetail.setText(html("Board the boat at the Ruins of Unkah to start."));
		}
		else
		{
			stepTitle.setText(step.getTitle());
			stepTitle.setForeground(step.getUrgency() == Urgency.NOW ? config.urgentColor() : step.getUrgency() == Urgency.SOON ? GOLD : config.targetColor());
			String then = "";
			for (Instruction next : plugin.getUpcoming())
			{
				then += (then.isEmpty() ? "Then: " : " > ") + next.getTitle();
			}
			stepDetail.setText(html(step.getDetail() + (then.isEmpty() ? "" : "\n" + then)).replace("\n", "<br>"));
		}

		bringRows.removeAll();
		bringSection.setVisible(!s.inGame());
		if (!s.inGame())
		{
			for (Planner.BringItem item : Planner.bringList(s, plugin.settings()))
			{
				String mark = item.have == null ? "\u2022 " : item.have ? "\u2714 " : "\u2718 ";
				JLabel l = new JLabel(html(mark + item.text));
				l.setForeground(item.have == null ? MUTED : item.have ? GOOD : NOW);
				l.setAlignmentX(Component.LEFT_ALIGNMENT);
				bringRows.add(l);
			}
		}

		gameRows.removeAll();
		if (s.inGame())
		{
			addRow(gameRows, "Round " + s.round, s.phase == Phase.ESSENCE ? "Spirit pool" : "Energy phase", Color.WHITE);
			addRow(gameRows, "Tempoross energy", pct(s.energy), ACCENT);
			addRow(gameRows, "Essence", pct(s.essence), new Color(143, 168, 255));
			addRow(gameRows, "Storm intensity", pct(s.storm), s.storm >= 85 ? NOW : GOLD);
			addRow(gameRows, "Raw / cooked", s.raw + " / " + (s.cooked + s.crystal), Color.WHITE);
			addRow(gameRows, "Loaded this round", String.valueOf(s.loadedThisRound), ACCENT);
			if (s.waveIncoming)
			{
				addRow(gameRows, "Wave", s.tethered ? "Tethered" : "TETHER NOW", s.tethered ? GOOD : NOW);
			}
		}
		else
		{
			addRow(gameRows, s.phase == Phase.WAITING ? "Waiting for the game" : s.phase == Phase.WON ? "Game over" : "No game", "", MUTED);
		}

		pointRows.removeAll();
		if (s.inGame() || s.phase == Phase.WON)
		{
			int permits = Permits.earned(s.points);
			addRow(pointRows, "Points", String.format("%,d", s.points), Color.WHITE);
			addRow(pointRows, "Permits earned", String.valueOf(permits), GOLD);
			addRow(pointRows, "To permit " + (permits + 1), String.format("%,d", Permits.toNext(s.points)), MUTED);
			addRow(pointRows, "Bonus roll", Permits.bonusChance(s.points) + "%", MUTED);
		}
		else
		{
			addRow(pointRows, "Permits at 2,000, then every 700", "", MUTED);
		}

		Session session = plugin.getSession();
		sessionRows.removeAll();
		addRow(sessionRows, "Games", session.getGames() + (session.getGames() > 0 ? " (" + session.getWins() + " won)" : ""), Color.WHITE);
		addRow(sessionRows, "Permits", session.getPermits() + (session.getGames() > 0
			? String.format(" (%.1f / game)", session.permitsPerGame()) : ""), GOLD);
		if (session.getGames() > 0)
		{
			addRow(sessionRows, "Permits / hr", String.format("%.0f", session.permitsPerHour()), Color.WHITE);
			addRow(sessionRows, "Fishing XP", String.format("%,d", session.getFishingXp()), Color.WHITE);
		}

		lastRows.removeAll();
		GameRecord last = plugin.getLastGame();
		if (last == null)
		{
			addRow(lastRows, "Finish a game for your report card", "", MUTED);
		}
		else
		{
			addRow(lastRows, (last.won ? "Subdued" : "Failed") + " · " + last.duration(), last.grade + " (" + last.score + ")", gradeColor(last.grade));
			addRow(lastRows, "Points", String.format("%,d", last.points), Color.WHITE);
			addRow(lastRows, "Permits", last.permits() + (last.bonusChance() > 0 ? " + " + last.bonusChance() + "%" : ""), GOLD);
			addRow(lastRows, "Loading", String.format("%,d", last.loadingPoints()), MUTED);
			addRow(lastRows, "Spirit pool", String.format("%,d", last.poolPoints()), MUTED);
			addRow(lastRows, "Fishing + cooking", String.format("%,d", last.fishingPoints() + last.cookingPoints()), MUTED);
			addRow(lastRows, "Fires, repairs, waves", String.format("%,d", last.otherPoints() + last.wavePoints()), MUTED);
			if (last.style == PlayStyle.MAX_XP)
			{
				addRow(lastRows, "Fishing XP", String.format("%,d", last.fishingXp), Color.WHITE);
			}
			else
			{
				addRow(lastRows, "Permits / hr pace", String.format("%.0f", last.permitsPerHour()), Color.WHITE);
			}
			for (int i = 0; i < last.tips.size(); i++)
			{
				JLabel tip = new JLabel(html((i + 1) + ". " + last.tips.get(i)));
				tip.setFont(FontManager.getRunescapeSmallFont());
				tip.setForeground(Color.WHITE);
				tip.setBorder(new EmptyBorder(3, 0, 0, 0));
				lastRows.add(tip);
			}
		}

		bestRows.removeAll();
		PersonalBests b = plugin.getBests();
		if (b.isEmpty())
		{
			addRow(bestRows, "No wins saved yet", "", MUTED);
		}
		else
		{
			addRow(bestRows, "Most points", String.format("%,d", b.mostPoints), Color.WHITE);
			addRow(bestRows, "Most permits", String.valueOf(b.mostPermits), GOLD);
			if (b.fastestWin > 0)
			{
				addRow(bestRows, "Fastest win", b.fastestWin / 60 + ":" + String.format("%02d", b.fastestWin % 60), Color.WHITE);
			}
			if (b.bestScore >= 0)
			{
				addRow(bestRows, "Best grade", b.bestGrade + " (" + b.bestScore + ")", gradeColor(b.bestGrade));
			}
		}

		historyRows.removeAll();
		List<GameRecord> history = plugin.getHistory();
		if (history.isEmpty())
		{
			addRow(historyRows, "No games yet", "", MUTED);
		}
		SimpleDateFormat time = new SimpleDateFormat("MMM d HH:mm");
		for (int i = history.size() - 1, shown = 0; i >= 0 && shown < HISTORY_ROWS; i--, shown++)
		{
			GameRecord r = history.get(i);
			addRow(historyRows, time.format(new Date(r.endedAtMillis)) + "  " + String.format("%,d", r.points),
				r.grade + " · " + r.permits() + "p", gradeColor(r.grade));
		}

		revalidate();
		repaint();
	}

	// ------------------------------------------------------------------ helpers

	private static String pct(int v)
	{
		return v < 0 ? "-" : v + "%";
	}

	private static Color gradeColor(String grade)
	{
		switch (grade)
		{
			case "S":
			case "A":
				return GOOD;
			case "B":
				return ACCENT;
			case "C":
				return GOLD;
			default:
				return NOW;
		}
	}

	private static String html(String s)
	{
		return "<html><body style='width:150px'>" + s.replace("&", "&amp;").replace("<", "&lt;") + "</body></html>";
	}

	private static JLabel small(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private static JPanel card()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		p.setBorder(new EmptyBorder(6, 8, 6, 8));
		return p;
	}

	private static JPanel rows(JPanel inner)
	{
		inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
		inner.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		inner.setBorder(new EmptyBorder(6, 8, 6, 8));
		return inner;
	}

	private static JPanel section(String title, JPanel body)
	{
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrap.setBorder(new EmptyBorder(0, 0, 8, 0));
		JLabel label = new JLabel(title);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(0, 2, 3, 0));
		wrap.add(label, BorderLayout.NORTH);
		wrap.add(body, BorderLayout.CENTER);
		return wrap;
	}

	private static void addRow(JPanel panel, String left, String right, Color rightColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel l = new JLabel(left);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(right.isEmpty() ? rightColor : MUTED);
		JLabel r = new JLabel(right);
		r.setFont(FontManager.getRunescapeSmallFont());
		r.setForeground(rightColor);
		row.add(l, BorderLayout.WEST);
		row.add(r, BorderLayout.EAST);
		panel.add(row);
	}
}

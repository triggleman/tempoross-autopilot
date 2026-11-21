package com.toadfx.temporossautopilot.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/** A row of buttons to ToadFx's channels. Each opens in the default web browser. */
final class SocialLinks
{
	private static final int SIZE = 22;

	enum Social
	{
		YOUTUBE("YouTube", "https://www.youtube.com/@toadfxx", new Color(255, 0, 0)),
		TWITCH("Twitch", "https://www.twitch.tv/toadfxx", new Color(145, 70, 255)),
		KICK("Kick", "https://kick.com/toadfx", new Color(83, 252, 24)),
		TIKTOK("TikTok", "https://www.tiktok.com/@toadfx", new Color(20, 20, 20));

		final String name;
		final String url;
		final Color color;

		Social(String name, String url, Color color)
		{
			this.name = name;
			this.url = url;
			this.color = color;
		}
	}

	private SocialLinks()
	{
	}

	static JPanel row()
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(0f);
		for (Social s : Social.values())
		{
			JLabel b = new JLabel(new ImageIcon(icon(s)));
			b.setToolTipText("ToadFx on " + s.name);
			b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			b.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					LinkBrowser.browse(s.url);
				}
			});
			row.add(b);
		}
		return row;
	}

	/** A simple badge in the channel's colour with a white mark (not the official logos). */
	static BufferedImage icon(Social s)
	{
		BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(s.color);
		g.fill(new RoundRectangle2D.Float(0, 0, SIZE, SIZE, 7, 7));
		if (s == Social.TIKTOK)
		{
			g.setColor(new Color(80, 80, 80));
			g.setStroke(new BasicStroke(1f));
			g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, SIZE - 1, SIZE - 1, 7, 7));
		}
		Color mark = s == Social.KICK ? Color.BLACK : Color.WHITE;
		g.setColor(mark);
		switch (s)
		{
			case YOUTUBE:
			{
				Path2D p = new Path2D.Float();
				p.moveTo(8, 6);
				p.lineTo(16, 11);
				p.lineTo(8, 16);
				p.closePath();
				g.fill(p);
				break;
			}
			case TWITCH:
			{
				// A chat bubble with two bars.
				g.setStroke(new BasicStroke(1.8f));
				Path2D p = new Path2D.Float();
				p.moveTo(5, 4.5);
				p.lineTo(17, 4.5);
				p.lineTo(17, 13);
				p.lineTo(13, 16);
				p.lineTo(9.5, 16);
				p.lineTo(7, 18.5);
				p.lineTo(7, 16);
				p.lineTo(5, 16);
				p.closePath();
				g.draw(p);
				g.fillRect(9, 8, 2, 4);
				g.fillRect(13, 8, 2, 4);
				break;
			}
			default:
			{
				String letter = s == Social.KICK ? "K" : "♪";
				g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
				int w = g.getFontMetrics().stringWidth(letter);
				g.drawString(letter, (SIZE - w) / 2f, 16);
				break;
			}
		}
		g.dispose();
		return img;
	}
}

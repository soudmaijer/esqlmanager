package nl.errorsoft.esql.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Year;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import nl.errorsoft.esql.ui.icon.ImageLoader;

/**
 * The splash: the application logo on a dark gradient with the name, version, commit and a status line, shown over the main window. At start up it
 * closes itself after a time and then runs {@code done}; opened from About (time 0) it scrolls the credits at the bottom and closes on a click, Esc, Enter or Space.
 * <p>
 * The splash is artwork with fixed colours, the same in every theme. Everything is painted by the content pane in {@code paintComponent}: a window that
 * paints in {@code Window.paint} can show up empty when Swing repaints its root pane from the back buffer instead. The logo is an SVG, drawn at the
 * scale of the screen. The closing timer starts when the window is open, so the splash is shown for the whole time.
 */
public class SplashWindow extends JWindow {
	static final Dimension SIZE = new Dimension(560, 340);
	private static final Color TOP = new Color(0x1b2029);
	private static final Color BOTTOM = new Color(0x2c3442);
	private static final Color EDGE = new Color(0x3d4757);
	private static final Color TITLE = new Color(0xf2f4f7);
	private static final Color MUTED = new Color(0x9aa4b2);
	private static final int LOGO = 132;
	/** The area the credits scroll in (About), above the bottom line. */
	static final Rectangle CREDITS = new Rectangle(24, 222, SIZE.width - 48, 76);

	/** The text on the splash. */
	public record Info(String name, String version, String commit) {
	}

	private CreditsPanel credits;

	/** Must be created on the event thread. */
	public SplashWindow(Window owner, Info info, int time, Runnable done) {
		super(owner);
		boolean about = time == 0;
		Artwork content = new Artwork(info, about ? "Click or press Esc to close" : "Starting...");
		content.setLayout(null);
		setContentPane(content);
		setSize(SIZE);
		setLocation(centeredIn(owner.getBounds(), SIZE));

		if (about) {
			credits = new CreditsPanel();
			credits.setBounds(CREDITS);
			content.add(credits);
			content.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					cleanUp();
				}
			});
			for (int key : new int[]{KeyEvent.VK_ESCAPE, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE}) {
				content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), "close");
			}
			content.getActionMap().put("close", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					cleanUp();
				}
			});
		} else {
			// A Swing timer, so that closing the splash and what follows run on the event thread.
			Timer timer = new Timer(time, e -> {
				cleanUp();
				done.run();
			});
			timer.setRepeats(false);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					timer.start();
				}
			});
		}
		setVisible(true);
		toFront();
		if (about) {
			requestFocus();
		}
		content.repaint();
	}

	/** Where a window of this size is centred over the owner. */
	static Point centeredIn(Rectangle owner, Dimension size) {
		return new Point(owner.x + (owner.width - size.width) / 2, owner.y + (owner.height - size.height) / 2);
	}

	public void cleanUp() {
		if (credits != null) {
			credits.switchOff();
			credits = null;
		}
		dispose();
	}

	/** The gradient, the logo, the name, version and commit, the status line and the copyright. */
	static final class Artwork extends JComponent {
		private final Info info;
		private final String status;
		private final Icon logo = ImageLoader.logoIcon(LOGO);

		Artwork(Info info, String status) {
			this.info = info;
			this.status = status;
			setOpaque(true);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setPaint(new GradientPaint(0, 0, TOP, 0, getHeight(), BOTTOM));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(EDGE);
				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				int logoY = 52;
				logo.paintIcon(this, g2, 40, logoY);

				int x = 40 + LOGO + 32;
				g2.setColor(TITLE);
				g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
				g2.drawString(info.name(), x, logoY + 58);
				g2.setColor(MUTED);
				g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
				g2.drawString("Version " + info.version() + "  ·  commit " + info.commit(), x, logoY + 86);
				g2.drawString("SQL database manager", x, logoY + 108);

				// The bottom line: what happens (or how to close) on the left, the copyright on the right.
				g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
				int baseline = getHeight() - 16;
				g2.setColor(TITLE);
				g2.drawString(status, 24, baseline);
				g2.setColor(MUTED);
				String copyright = "\u00A9 Errorsoft 2002-" + Year.now().getValue();
				g2.drawString(copyright, getWidth() - 24 - g2.getFontMetrics().stringWidth(copyright), baseline);
			} finally {
				g2.dispose();
			}
		}
	}
}

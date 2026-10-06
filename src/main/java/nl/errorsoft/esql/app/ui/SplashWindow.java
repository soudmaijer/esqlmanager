package nl.errorsoft.esql.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Year;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;

import nl.errorsoft.esql.app.ApplicationContext;

/**
 * The splash: the artwork with the name, version and commit, shown over the main window. At start up it closes itself after a time and then runs
 * {@code done}; opened from About (time 0) it scrolls the credits over the artwork and closes on a click.
 * <p>
 * Everything is painted by the content pane in {@code paintComponent}, the Swing way: a window that paints in {@code Window.paint} can show up empty
 * when Swing repaints its root pane from the window's back buffer instead. The closing timer starts when the window is open, so the splash is shown
 * for the whole time.
 */
public class SplashWindow extends JWindow {
	static final Dimension SIZE = new Dimension(400, 240);

	/** The text on the splash. */
	public record Info(String name, String version, String commit) {
	}

	private CreditsPanel credits;

	/** Must be created on the event thread. */
	public SplashWindow(Window owner, Info info, int time, Runnable done) {
		super(owner);
		Image artwork = ApplicationContext.get().imageLoader().getImage("esql");
		Artwork content = new Artwork(artwork, info);
		content.setLayout(null);
		setContentPane(content);
		setSize(SIZE);
		setLocation(centeredIn(owner.getBounds(), SIZE));

		if (time == 0) {
			credits = new CreditsPanel();
			credits.setBounds(0, 165, 400, 75);
			content.add(credits);
			content.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
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

	/** The artwork with the name, version, commit and copyright. */
	static final class Artwork extends JComponent {
		private final Image artwork;
		private final Info info;

		Artwork(Image artwork, Info info) {
			this.artwork = artwork;
			this.info = info;
			setOpaque(true);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				// The colours of the artwork, which is the same in every theme.
				g2.setColor(Color.white);
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.drawImage(artwork, 0, 0, this);
				g2.setColor(Color.black);
				g2.setFont(new Font("Arial", Font.BOLD, 12));
				g2.drawString(info.name(), 17, 196);
				g2.setFont(new Font("Arial", Font.PLAIN, 11));
				g2.drawString("Version " + info.version(), 17, 212);
				g2.drawString("Commit " + info.commit(), 17, 227);
				g2.drawString("http://www.errorsoft.nl", 274, 212);
				g2.drawString("© Copyright Errorsoft 2002-" + Year.now().getValue(), 230, 227);
			} finally {
				g2.dispose();
			}
		}
	}
}

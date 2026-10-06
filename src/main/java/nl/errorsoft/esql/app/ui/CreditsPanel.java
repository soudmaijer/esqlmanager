package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.app.DataDirectory;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The credits scrolling up in a translucent panel on the dark splash. Painted by Swing in {@code paintComponent}; fixed colours, as the splash. */
public class CreditsPanel extends JComponent {
	private static final Logger log = LogManager.getLogger(CreditsPanel.class);
	// The colours of the splash the credits scroll on.
	private static final Color BORDER = new Color(0x4a5568);
	private static final Color HEADING = new Color(0x8cb4ff);
	private static final Color TEXT = new Color(0xd5dae1);
	private static final Color PANEL = new Color(0, 0, 0, 60);
	private static final int LINE = 14;
	/** Moves the credits up a pixel at a time, on the event thread. */
	private final Timer scroller = new Timer(45, e -> scroll());
	private final Font bold = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private final Font plain = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private List<String> lines = List.of();
	/** Where the first line is, from the top; starts below the panel. */
	private int offset = -1;

	public CreditsPanel() {
		setOpaque(false);
		try {
			lines = Files.readAllLines(DataDirectory.file("credits.txt").toPath(), StandardCharsets.UTF_8);
			scroller.start();
		} catch (Exception e) {
			// Without credits the panel stays empty, the splash and the About window still work.
			log.warn("The credits could not be read: {}", e.getMessage());
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			if (offset < 0) {
				offset = getHeight();
			}
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(PANEL);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
			g2.setColor(BORDER);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
			g2.clipRect(1, 1, getWidth() - 2, getHeight() - 2);

			FontMetrics boldMetrics = g2.getFontMetrics(bold);
			FontMetrics plainMetrics = g2.getFontMetrics(plain);
			for (int count = 0; count < lines.size(); count++) {
				String text = lines.get(count);
				if (text.equals("-")) {
					g2.setColor(BORDER);
					g2.drawLine(10, offset + count * LINE + 7, getWidth() - 10, offset + count * LINE + 7);
				} else {
					boolean heading = text.startsWith("<h>");
					text = heading ? text.substring(3) : text;
					g2.setColor(heading ? HEADING : TEXT);
					g2.setFont(heading ? bold : plain);
					int width = (heading ? boldMetrics : plainMetrics).stringWidth(text);
					g2.drawString(text, (getWidth() - width) / 2, offset + (count + 1) * LINE);
				}
			}
		} finally {
			g2.dispose();
		}
	}

	public void switchOff() {
		scroller.stop();
	}

	private void scroll() {
		if (offset < 0) {
			return;
		}
		offset--;
		if (offset + lines.size() * LINE < 0) {
			offset = getHeight();
		}
		repaint();
	}
}

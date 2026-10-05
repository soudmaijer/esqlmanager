package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.app.DataDirectory;
import java.awt.*;
import java.io.*;
import javax.swing.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreditsPanel extends Canvas {
	private static final Logger log = LogManager.getLogger(CreditsPanel.class);
	// The colours of the splash artwork the credits scroll over.
	private static final Color BORDER = new Color(0x5B5150);
	private static final Color HEADING = new Color(0xA24811);
	private static final Color TEXT = new Color(0x000000);
	/** Moves the credits up a pixel at a time, on the event thread. */
	private final Timer scroller = new Timer(45, e -> scroll());
	private CreditObject root;
	private CreditObject curr;

	private Image img = null;
	private int y_offset = 0;

	private int nodes = 0;

	private Image bg;

	public CreditsPanel() {
		try {
			try (BufferedReader fin = new BufferedReader(new FileReader(DataDirectory.file("credits.txt")))) {
				String in = fin.readLine();
				root = new CreditObject(in);
				curr = root;
				nodes++;
				while ((in = fin.readLine()) != null) {
					nodes++;
					CreditObject tmp = new CreditObject(in);
					curr.next = tmp;
					curr = tmp;
				}
			}

			scroller.start();
		} catch (Exception e) {
			// Without credits the panel stays empty, the splash and the About window still work.
			log.warn("The credits could not be read: {}", e.getMessage());
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (img == null) {
			img = createImage((int) this.getSize().getWidth(), (int) this.getSize().getHeight());
			y_offset = (int) this.getSize().getHeight();
		}

		Graphics2D g2 = (Graphics2D) img.getGraphics();
		Composite old = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 0.8));
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getHeight());

		g2.setComposite(old);

		g2.setColor(BORDER);
		g2.drawRect(0, 0, (int) this.getSize().getWidth() - 1, (int) this.getSize().getHeight() - 1);

		g2.setColor(Color.black);

		Font fb = new Font("Arial", Font.BOLD, 11);
		Font fp = new Font("Arial", Font.PLAIN, 11);

		FontMetrics fmb = this.getFontMetrics(fb);
		FontMetrics fmp = this.getFontMetrics(fp);

		curr = root;
		int count = 0;
		while (curr != null) {
			if (!curr.txt.equalsIgnoreCase("-")) {
				int width;
				String txt = curr.txt;
				if (curr.txt.startsWith("<h>")) {
					txt = txt.substring(3, txt.length());
					width = fmb.stringWidth(txt);
					g2.setColor(HEADING);
					g2.setFont(fb);
				} else {
					width = fmp.stringWidth(txt);
					g2.setColor(TEXT);
					g2.setFont(fp);
				}
				g2.drawString(txt, (((int) this.getSize().getWidth()) - width) / 2, y_offset + ((count + 1) * 11));
			} else {
				g2.setColor(BORDER);
				g2.drawLine(10, y_offset + (count * 11) + 7, (int) this.getSize().getWidth() - 10, y_offset + (count * 11) + 7);
			}
			curr = curr.next;
			count++;
		}

		g = this.getGraphics();
		g.drawImage(img, 0, 0, this);
	}

	public void switchoff() {
		scroller.stop();
	}

	private void scroll() {
		y_offset--;
		repaint();

		if (y_offset + nodes * 11 < 0) {
			y_offset = (int) this.getSize().getHeight();
		}
	}
}

class CreditObject {
	String txt;
	CreditObject next = null;

	public CreditObject(String txt) {
		this.txt = txt;
	}
}

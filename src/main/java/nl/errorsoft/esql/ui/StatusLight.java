package nl.errorsoft.esql.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/** A small red or green light, shown in front of the status message. */
public class StatusLight extends JComponent {
	private static final int SIZE = 15;

	private final ImageLoader imgldr;
	private boolean red = false;

	public StatusLight(ImageLoader imgldr) {
		this.imgldr = imgldr;
		setPreferredSize(new Dimension(SIZE, SIZE));
	}

	public void switchRedLight(boolean red) {
		this.red = red;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage(imgldr.getImage(red ? "redLight" : "greenLight"), 0, (getHeight() - SIZE) / 2, this);
	}
}

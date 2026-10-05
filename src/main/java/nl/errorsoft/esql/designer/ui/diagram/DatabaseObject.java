package nl.errorsoft.esql.designer.ui.diagram;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

import nl.errorsoft.esql.app.ApplicationContext;

/** The database of the model, drawn as a pill with the database icon and its name. */
public class DatabaseObject extends ModelObject {
	private static final int HEIGHT = 44;

	private String name = "";
	private String description = "";

	public DatabaseObject(String name, int identifier) {
		this.name = name;
		this.setOpaque(false);
		this.setIdentifier(identifier);
		this.reviewSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		DesignerTheme.smooth(g2);

		Rectangle card = localCard();
		RoundRectangle2D shape = new RoundRectangle2D.Float(card.x, card.y, card.width - 1, card.height - 1, card.height, card.height);

		paintShadow(g2, shape);
		g2.setColor(DesignerTheme.card());
		g2.fill(shape);

		int circle = card.height - 12;
		g2.setColor(DesignerTheme.accent());
		g2.fillOval(card.x + 6, card.y + 6, circle, circle);
		Icon icon = ApplicationContext.get().imageLoader().getIcon("dbimgsel");
		if (icon != null) {
			icon.paintIcon(this, g2, card.x + 6 + (circle - 16) / 2, card.y + 6 + (circle - 16) / 2);
		}

		g2.setFont(DesignerTheme.bold());
		FontMetrics bold = g2.getFontMetrics();
		g2.setFont(DesignerTheme.small());
		FontMetrics small = g2.getFontMetrics();
		int textX = card.x + circle + 16;
		int top = card.y + (card.height - bold.getHeight() - small.getHeight()) / 2;

		g2.setColor(DesignerTheme.text());
		g2.setFont(DesignerTheme.bold());
		g2.drawString(name, textX, top + bold.getAscent());
		g2.setColor(DesignerTheme.muted());
		g2.setFont(DesignerTheme.small());
		g2.drawString("database", textX, top + bold.getHeight() + small.getAscent());

		g2.setColor(isSelected() ? DesignerTheme.accent() : DesignerTheme.border());
		g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
		g2.draw(shape);
		g2.dispose();
	}

	public void reviewSize() {
		int width = HEIGHT - 12 + 16
			+ Math.max(getFontMetrics(DesignerTheme.bold()).stringWidth(name), getFontMetrics(DesignerTheme.small()).stringWidth("database"))
			+ 22;
		setCardSize(width, HEIGHT);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setName(String name) {
		this.name = name;
		this.reviewSize();
		this.repaint();
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

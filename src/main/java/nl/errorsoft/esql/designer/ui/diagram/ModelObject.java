package nl.errorsoft.esql.designer.ui.diagram;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import java.awt.event.*;

public class ModelObject extends JPanel {
	private final List<ModelObject> references;
	private boolean selected;

	private boolean hidden = true;

	public int xc = 0;
	public int yc = 0;

	private int identifier = -1;

	public ModelObject() {
		references = new ArrayList<>();
		this.setLayout(null);
	}

	public void addReference(ModelObject mo) {
		references.add(mo);
	}

	public void removeReference(ModelObject mo) {
		references.remove(mo);
	}

	public List<ModelObject> getReferences() {
		return references;
	}

	public boolean isSelected() {
		return selected;
	}

	/** Fires the property "selected", so menus that depend on the selection follow every way it changes (a click, Select all, a new card). */
	public void setSelected(boolean selected) {
		boolean old = this.selected;
		this.selected = selected;
		if (selected) {
			this.requestFocus();
		}
		this.repaint();
		firePropertyChange("selected", old, selected);
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	public int getIdentifier() {
		return identifier;
	}

	/** Where the card is drawn in the viewer, without the margin around it that holds the shadow. */
	public Rectangle cardBounds() {
		int m = DesignerTheme.SHADOW;
		return new Rectangle(getX() + m, getY() + m, getWidth() - 2 * m, getHeight() - 2 * m);
	}

	/** Places the card (not the shadow margin) at a point of the viewer, the position a model file stores. */
	public void setCardLocation(int x, int y) {
		setLocation(x - DesignerTheme.SHADOW, y - DesignerTheme.SHADOW);
	}

	/** Sets the size of the card, the component is larger by the shadow margin. */
	public void setCardSize(int width, int height) {
		setSize(width + 2 * DesignerTheme.SHADOW, height + 2 * DesignerTheme.SHADOW);
	}

	/** Only the card takes the mouse, a click on the shadow goes to the viewer (and to a connector below it). */
	@Override
	public boolean contains(int x, int y) {
		int m = DesignerTheme.SHADOW;
		return x >= m && y >= m && x < getWidth() - m && y < getHeight() - m;
	}

	/** The card area in component coordinates. */
	protected Rectangle localCard() {
		int m = DesignerTheme.SHADOW;
		return new Rectangle(m, m, getWidth() - 2 * m, getHeight() - 2 * m);
	}

	/** A soft shadow under a shape: a few translucent copies, a little lower and wider each time. */
	protected void paintShadow(Graphics2D g2, Shape card) {
		g2.setColor(DesignerTheme.shadow());
		for (int i = 1; i <= 4; i++) {
			java.awt.geom.AffineTransform move = java.awt.geom.AffineTransform.getTranslateInstance(0, i * 0.75);
			g2.fill(move.createTransformedShape(new java.awt.BasicStroke(i * 1.5f).createStrokedShape(card)));
			g2.fill(move.createTransformedShape(card));
		}
	}
}

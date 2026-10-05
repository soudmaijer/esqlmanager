package nl.errorsoft.esql.ui.util;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Small helpers for dialogs built with layout managers: the padding, a row of buttons and a label/field form. */
public final class Forms {
	/** Space around the content of a dialog. */
	public static final int PADDING = 12;
	/** Space between rows and between a label and its field. */
	public static final int GAP = 6;

	private Forms() {
	}

	/** Gives a component the padding of a dialog. */
	public static <T extends JComponent> T padded(T component) {
		component.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
		return component;
	}

	/** A titled border with room inside, for a group of fields. */
	public static <T extends JComponent> T titled(T component, String title) {
		component.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)));
		return component;
	}

	/** A right aligned row of buttons, for the bottom of a dialog. */
	public static JPanel buttonRow(Component... buttons) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 0));
		row.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
		for (Component b : buttons) {
			row.add(b);
		}
		return row;
	}

	/** A panel of rows: a label in the first column and a field that takes the rest of the width. */
	public static final class Grid {
		private final JPanel panel = new JPanel(new GridBagLayout());
		private int row;

		public JPanel panel() {
			return panel;
		}

		/** Adds a label and a field on one row. */
		public Grid row(Component label, Component field) {
			panel.add(label, constraints(0, 1, 0));
			panel.add(field, constraints(1, 1, 1));
			row++;
			return this;
		}

		/** Adds a label and a field that takes the remaining height, such as a text area. */
		public Grid area(Component label, Component field) {
			GridBagConstraints l = constraints(0, 1, 0);
			l.anchor = GridBagConstraints.NORTHWEST;
			panel.add(label, l);
			GridBagConstraints c = constraints(1, 1, 1);
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			panel.add(field, c);
			row++;
			return this;
		}

		/** Adds a component that spans the whole row. */
		public Grid full(Component component) {
			panel.add(component, constraints(0, 2, 1));
			row++;
			return this;
		}

		/** Adds a component that spans the row and takes the remaining height. */
		public Grid fill(Component component) {
			GridBagConstraints c = constraints(0, 2, 1);
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			panel.add(component, c);
			row++;
			return this;
		}

		/** Pushes the rows to the top when the panel is taller than they are. */
		public JPanel done() {
			GridBagConstraints c = constraints(0, 2, 1);
			c.weighty = 1;
			panel.add(new JPanel() {
				{
					setOpaque(false);
				}
			}, c);
			return panel;
		}

		private GridBagConstraints constraints(int x, int width, double weightx) {
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = x;
			c.gridy = row;
			c.gridwidth = width;
			c.weightx = weightx;
			c.anchor = GridBagConstraints.WEST;
			c.fill = weightx > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
			c.insets = new Insets(row == 0 ? 0 : GAP, x == 0 ? 0 : GAP, 0, 0);
			return c;
		}
	}
}

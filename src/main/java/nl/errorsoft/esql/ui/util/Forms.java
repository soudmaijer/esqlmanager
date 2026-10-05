package nl.errorsoft.esql.ui.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;

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

	/** Puts a form in a scroll pane that scrolls vertically when the window is too small for it. Otherwise the form fills the window as it would without one. */
	public static JScrollPane verticalScroll(JComponent content) {
		JPanel view = new FitWidthPanel();
		view.add(content, BorderLayout.CENTER);
		JScrollPane scroll = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	/** Follows the width of the viewport, and its height when the viewport is taller than the content. */
	private static final class FitWidthPanel extends JPanel implements Scrollable {
		FitWidthPanel() {
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return getParent() != null && getParent().getHeight() > getPreferredSize().height;
		}
	}

	/** A text with an optional mnemonic: "&Name" underlines the N. A literal ampersand is written "&&". */
	public record MnemonicText(String text, char mnemonic, int index) {
		public static final MnemonicText NONE = new MnemonicText("", '\0', -1);

		/** Splits "&Name" into the text "Name", the mnemonic 'N' and its index 0. Without an ampersand there is no mnemonic (index -1). */
		public static MnemonicText parse(String source) {
			StringBuilder text = new StringBuilder();
			char mnemonic = '\0';
			int index = -1;

			for (int i = 0; i < source.length(); i++) {
				char c = source.charAt(i);
				if (c == '&' && i + 1 < source.length()) {
					char next = source.charAt(++i);
					if (next != '&' && index < 0) {
						mnemonic = next;
						index = text.length();
					}
					text.append(next);
				} else {
					text.append(c);
				}
			}
			return new MnemonicText(text.toString(), mnemonic, index);
		}
	}

	/** A button with the text of "&Create" and its mnemonic. */
	public static JButton button(String source) {
		JButton button = new JButton();
		mnemonic(button, source);
		return button;
	}

	/** Sets the text and the mnemonic of a button or checkbox from "&Text". */
	public static <T extends AbstractButton> T mnemonic(T button, String source) {
		MnemonicText parsed = MnemonicText.parse(source);
		button.setText(parsed.text());
		if (parsed.index() >= 0) {
			button.setMnemonic(Character.toUpperCase(parsed.mnemonic()));
			button.setDisplayedMnemonicIndex(parsed.index());
		}
		return button;
	}

	/** A label from "&Name:" that focuses the field when its mnemonic is pressed. The field may be null. */
	public static JLabel label(String source, Component field) {
		MnemonicText parsed = MnemonicText.parse(source);
		JLabel label = new JLabel(parsed.text());
		if (parsed.index() >= 0) {
			label.setDisplayedMnemonic(Character.toUpperCase(parsed.mnemonic()));
			label.setDisplayedMnemonicIndex(parsed.index());
		}
		if (field != null) {
			label.setLabelFor(field);
		}
		return label;
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

	/** Buttons at the right and one apart at the left, for example a destructive Delete away from Save. */
	public static JPanel buttonRowWithLeading(Component leading, Component... buttons) {
		JPanel row = new JPanel(new BorderLayout());
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		left.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, 0, 0));
		left.add(leading);
		row.add(left, BorderLayout.WEST);
		row.add(buttonRow(buttons), BorderLayout.EAST);
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

		/** Adds a label from "&Name:" (mnemonic focuses the field) and a field on one row. */
		public Grid row(String label, Component field) {
			return row(label(label, field), field);
		}

		/** Adds a label from "&Name:" and a field that takes the remaining height. */
		public Grid area(String label, Component field) {
			return area(label(label, field instanceof javax.swing.JScrollPane scroll ? scroll.getViewport().getView() : field), field);
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
			c.insets = new Insets(0, 0, 0, 0);
			JPanel filler = new JPanel();
			filler.setOpaque(false);
			filler.setPreferredSize(new Dimension(0, 0));
			panel.add(filler, c);
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

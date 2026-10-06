package nl.errorsoft.esql.ui.util;

import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/** The look of the buttons in the toolbars of the application, the explorer and the views: 20px icons, 4px margin, about 32x30. */
public final class ToolbarButtons {
	/** Height of a toolbar button, also used for other controls in a toolbar. */
	public static final int HEIGHT = 30;
	public static final int ICON_SIZE = 20;

	private ToolbarButtons() {
	}

	/** Gives the buttons the toolbar margin, a 20px icon and takes them out of the focus cycle. */
	public static void style(AbstractButton... buttons) {
		for (AbstractButton button : buttons) {
			button.setIcon(toolbarSize(button.getIcon()));
			button.setMargin(new Insets(4, 4, 4, 4));
			button.setFocusable(false);
		}
	}

	/**
	 * A toolbar for these buttons, laid out the same everywhere: one row, left aligned, 2px apart, 2px padding left and right, not floatable. It never wraps
	 * (a wrapped row would be cut off by the fixed height), buttons that do not fit are clipped at the right.
	 */
	public static javax.swing.JToolBar toolbar() {
		javax.swing.JToolBar toolbar = new javax.swing.JToolBar();
		toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0) {
			@Override
			public void layoutContainer(java.awt.Container target) {
				// Lay out as if there were room for every component, so nothing wraps to a second row.
				synchronized (target.getTreeLock()) {
					java.awt.Insets insets = target.getInsets();
					int x = insets.left + getHgap();
					int height = target.getHeight() - insets.top - insets.bottom;
					for (java.awt.Component component : target.getComponents()) {
						java.awt.Dimension size = component.getPreferredSize();
						component.setBounds(x, insets.top + (height - size.height) / 2, size.width, size.height);
						x += size.width + getHgap();
					}
				}
			}
		});
		toolbar.setFloatable(false);
		toolbar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 2, 0, 2));
		return toolbar;
	}

	/** A thin vertical line between groups of buttons in such a toolbar. */
	public static javax.swing.JSeparator separator() {
		javax.swing.JSeparator separator = new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL);
		separator.setPreferredSize(new java.awt.Dimension(6, HEIGHT - 8));
		return separator;
	}

	private static final String TOOLTIP = "esql.tooltip";

	/**
	 * Enables a button or menu item when nothing is missing, otherwise disables it and adds what is needed to its tooltip ("New query: select a database
	 * first."). The tooltip the component had the first time is its own text; {@code missing} is a sentence ("Select a database first.") or null.
	 */
	public static void setAvailable(javax.swing.JComponent component, String missing) {
		Object own = component.getClientProperty(TOOLTIP);
		if (own == null) {
			own = component.getToolTipText() != null
				? component.getToolTipText()
				: component instanceof AbstractButton button && button.getText() != null ? button.getText().replace("...", "") : "";
			component.putClientProperty(TOOLTIP, own);
		}
		component.setEnabled(missing == null);
		String text = own.toString();
		if (missing == null) {
			component.setToolTipText(text.isEmpty() || component instanceof javax.swing.JMenuItem ? null : text);
		} else {
			String lower = Character.toLowerCase(missing.charAt(0)) + missing.substring(1);
			component.setToolTipText(text.isEmpty() ? missing : text + ": " + lower);
		}
	}

	private static Icon toolbarSize(Icon icon) {
		if (icon instanceof FlatSVGIcon svg && svg.getIconWidth() != ICON_SIZE) {
			return svg.derive(ICON_SIZE, ICON_SIZE);
		}
		return icon;
	}
}

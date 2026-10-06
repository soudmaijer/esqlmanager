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

	/** A toolbar for these buttons, laid out the same everywhere: left aligned, 2px apart, not floatable. */
	public static javax.swing.JToolBar toolbar() {
		javax.swing.JToolBar toolbar = new javax.swing.JToolBar();
		toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
		toolbar.setFloatable(false);
		return toolbar;
	}

	/** A thin vertical line between groups of buttons in such a toolbar. */
	public static javax.swing.JSeparator separator() {
		javax.swing.JSeparator separator = new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL);
		separator.setPreferredSize(new java.awt.Dimension(6, HEIGHT - 8));
		return separator;
	}

	private static Icon toolbarSize(Icon icon) {
		if (icon instanceof FlatSVGIcon svg && svg.getIconWidth() != ICON_SIZE) {
			return svg.derive(ICON_SIZE, ICON_SIZE);
		}
		return icon;
	}
}

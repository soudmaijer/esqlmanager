package nl.errorsoft.esql.ui.util;

import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/** The look of the buttons in the toolbars of the application and the connection windows: 20px icons, 4px margin, about 32x30. */
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

	private static Icon toolbarSize(Icon icon) {
		if (icon instanceof FlatSVGIcon svg && svg.getIconWidth() != ICON_SIZE) {
			return svg.derive(ICON_SIZE, ICON_SIZE);
		}
		return icon;
	}
}

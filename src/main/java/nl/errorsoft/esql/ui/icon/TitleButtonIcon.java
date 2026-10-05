package nl.errorsoft.esql.ui.icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * A button icon in the title bar of an internal frame (icons/svg/window-*.svg). It takes the title foreground of the theme, the inactive one when the frame is
 * not selected, and the close hover foreground while the mouse is over the close button.
 */
public class TitleButtonIcon implements Icon {
	private static final Color BASE = new Color(0x6e6e6e);

	private final FlatSVGIcon icon;
	private final boolean close;
	private Color current;

	public TitleButtonIcon(String svg, int size, boolean close) {
		this.close = close;
		this.icon = new FlatSVGIcon("icons/svg/" + svg + ".svg", size, size, getClass().getClassLoader());
		icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> color.equals(BASE) && current != null ? current : color));
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		current = UIManager.getColor(colorKey(c));
		icon.paintIcon(c, g, x + (getIconWidth() - icon.getIconWidth()) / 2, y + (getIconHeight() - icon.getIconHeight()) / 2);
	}

	private String colorKey(Component c) {
		if (close && c instanceof AbstractButton button && (button.getModel().isRollover() || button.getModel().isPressed())) {
			return "InternalFrame.closeHoverForeground";
		}
		JInternalFrame frame = (JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, c);
		return frame == null || frame.isSelected() ? "InternalFrame.activeTitleForeground" : "InternalFrame.inactiveTitleForeground";
	}

	/** The title pane sizes its buttons by their icon, so the icon is as large as InternalFrame.buttonSize and paints the SVG in the middle. */
	@Override
	public int getIconWidth() {
		return buttonSize().width;
	}

	@Override
	public int getIconHeight() {
		return buttonSize().height;
	}

	private Dimension buttonSize() {
		Dimension size = UIManager.getDimension("InternalFrame.buttonSize");
		return size != null ? size : new Dimension(icon.getIconWidth(), icon.getIconHeight());
	}
}

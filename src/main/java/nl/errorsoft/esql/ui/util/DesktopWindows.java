package nl.errorsoft.esql.ui.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.beans.PropertyVetoException;

import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

/** Arranges the internal frames on the desktop of the main window (tile, cascade, keep inside) and opens links in the browser. */
public final class DesktopWindows {
	private static final Logger log = LogManager.getLogger(DesktopWindows.class);

	/** How far each cascaded frame is moved from the previous one. */
	private static final int CASCADE_OFFSET = 24;
	// Where the next cascaded frame goes; only touched on the event thread.
	private static int nextX;
	private static int nextY;

	private DesktopWindows() {
	}

	/** Tiles the visible frames in a grid of about as many rows as columns. */
	public static void tileVertical(JDesktopPane desktop) {
		DesktopManager manager = desktop.getDesktopManager();
		if (manager == null) {
			// No desktop manager - do nothing
			return;
		}

		Component[] comps = desktop.getComponents();
		Component comp;
		int count = 0;

		// Count and handle only the internal frames
		for (int i = 0; i < comps.length; i++) {
			comp = comps[i];
			if (comp instanceof JInternalFrame && comp.isVisible()) {
				count++;
			}
		}

		if (count != 0) {
			double root = Math.sqrt((double) count);
			int rows = (int) root;
			int columns = count / rows;
			int spares = count - (columns * rows);

			Dimension paneSize = desktop.getSize();
			int columnWidth = paneSize.width / columns;

			int availableHeight = paneSize.height;
			int mainHeight = availableHeight / rows;
			int smallerHeight = availableHeight / (rows + 1);
			int rowHeight = mainHeight;
			int x = 0;
			int y = 0;
			int thisRow = rows;
			int normalColumns = columns - spares;

			for (int i = comps.length - 1; i >= 0; i--) {
				comp = comps[i];
				if (comp instanceof JInternalFrame && comp.isVisible()) {
					manager.setBoundsForFrame((JComponent) comp, x, y,
						columnWidth, rowHeight);
					y += rowHeight;
					if (--thisRow == 0) {
						// Filled the row
						y = 0;
						x += columnWidth;

						// Switch to smaller rows if necessary
						if (--normalColumns <= 0) {
							thisRow = rows + 1;
							rowHeight = smallerHeight;
						} else {
							thisRow = rows;
						}
					}
				}
			}
		}
	}

	/** Stacks the open frames over the full width, the minimized ones stay in a row at the bottom. */
	public static void tileHorizontal(JDesktopPane desktop) {
		JInternalFrame[] frames = desktop.getAllFrames();
		int resizable = 0;
		for (JInternalFrame frame : frames) {
			if (frame.isVisible() && !frame.isIcon()) {
				if (frame.isResizable()) {
					resizable++;
				} else {
					unmaximize(frame);
				}
			}
		}
		int width = desktop.getBounds().width;
		int height = arrangeIcons(desktop);
		if (resizable == 0) {
			return;
		}
		int frameHeight = height / resizable;
		int y = 0;
		for (JInternalFrame frame : frames) {
			if (frame.isVisible() && frame.isResizable() && !frame.isIcon()) {
				frame.setSize(width, frameHeight);
				frame.setLocation(0, y);
				y += frameHeight;
			}
		}
	}

	private static void unmaximize(JInternalFrame frame) {
		try {
			frame.setMaximum(false);
		} catch (PropertyVetoException e) {
			// The frame keeps its size when it refuses, tiling the others still works.
			log.debug("{} was not restored: {}", frame.getTitle(), e.getMessage());
		}
	}

	/**
	 * Puts the icons of the minimized frames in rows at the bottom of the desktop.
	 * @return the height above the icons, what is left for the open frames
	 */
	public static int arrangeIcons(JDesktopPane desktop) {
		int height = desktop.getBounds().height;
		int width = desktop.getBounds().width;
		int y = height;
		int x = 0;
		for (JInternalFrame frame : desktop.getAllFrames()) {
			if (frame.isVisible() && frame.isIcon()) {
				Dimension icon = frame.getDesktopIcon().getSize();
				if (y == height) {
					y = height - icon.height;
				}
				if (x + icon.width > width && x != 0) {
					x = 0;
					y -= icon.height;
				}
				frame.getDesktopIcon().setLocation(x, y);
				x += icon.width;
			}
		}
		return y;
	}

	/** Cascades the visible frames from the top left corner. */
	public static void cascadeAll(JDesktopPane desktop) {
		Component[] comps = desktop.getComponents();
		int count = comps.length;
		nextX = 0;
		nextY = 0;

		for (int i = count - 1; i >= 0; i--) {
			Component comp = comps[i];
			if (comp instanceof JInternalFrame && comp.isVisible()) {
				cascade(comp, desktop);
			}
		}
	}

	public static void minimizeAll(JDesktopPane desktop) {
		Component[] comps = desktop.getComponents();
		int count = comps.length;

		for (int i = count - 1; i >= 0; i--) {
			Component comp = comps[i];
			if (comp instanceof JInternalFrame jif && comp.isVisible()) {
				if (jif.isIconifiable()) {
					try {
						jif.setIcon(true);
					} catch (PropertyVetoException e) {
						// The frame refused to be iconified (it asks something first); it simply stays open.
						log.debug("{} was not minimized: {}", jif.getTitle(), e.getMessage());
					}
				}
			}
		}
	}

	/** Places a frame a little right of and below the previous one, back at the corner when it would not fit. */
	private static void cascade(Component comp, JDesktopPane desktop) {
		Dimension paneSize = desktop.getSize();
		int targetWidth = 3 * paneSize.width / 4;
		int targetHeight = 3 * paneSize.height / 4;

		DesktopManager manager = desktop.getDesktopManager();
		if (manager == null) {
			comp.setBounds(0, 0, targetWidth, targetHeight);
			return;
		}

		if (nextX + targetWidth > paneSize.width ||
			nextY + targetHeight > paneSize.height) {
			nextX = 0;
			nextY = 0;
		}

		manager.setBoundsForFrame((JComponent) comp, nextX, nextY,
			targetWidth, targetHeight);

		nextX += CASCADE_OFFSET;
		nextY += CASCADE_OFFSET;
	}

	/** Keeps the frames that are not maximized inside the desktop, so a smaller window never hides a title bar outside it. */
	public static void keepFramesInside(JDesktopPane desktop) {
		Dimension size = desktop.getSize();
		if (size.width <= 0 || size.height <= 0) {
			return;
		}
		for (JInternalFrame frame : desktop.getAllFrames()) {
			if (frame.isMaximum() || frame.isIcon()) {
				continue;
			}
			int width = Math.min(frame.getWidth(), size.width);
			int height = Math.min(frame.getHeight(), size.height);
			int x = Math.max(0, Math.min(frame.getX(), size.width - width));
			int y = Math.max(0, Math.min(frame.getY(), size.height - height));
			if (x != frame.getX() || y != frame.getY() || width != frame.getWidth() || height != frame.getHeight()) {
				frame.setBounds(x, y, width, height);
			}
		}
	}

	/** Opens a web page in the system browser. */
	public static void openInBrowser(java.net.URI uri) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			log.warn("No browser available to open {}", uri);
			return;
		}
		try {
			Desktop.getDesktop().browse(uri);
		} catch (java.io.IOException e) {
			log.warn("Could not open {}: {}", uri, e.getMessage());
		}
	}
}

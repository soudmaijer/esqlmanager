package nl.errorsoft.esql.ui.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/** Arranges the internal frames on the desktop of the main window (tile, cascade, keep inside) and opens links in the browser. */
public class DesktopWindows {
	private static final Logger log = LogManager.getLogger(DesktopWindows.class);

	protected static final int UNUSED_HEIGHT = 0;
	protected static int nextX; // Next X position
	protected static int nextY; // Next Y position
	protected static final int DEFAULT_OFFSETX = 24;
	protected static final int DEFAULT_OFFSETY = 24;
	protected static int offsetX = DEFAULT_OFFSETX;
	protected static int offsetY = DEFAULT_OFFSETY;

	// Layout all of the children of this container
	// so that they are tiled.
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

			// We leave some space at the bottom that doesn't get covered
			int availableHeight = paneSize.height - UNUSED_HEIGHT;
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
	} // end of TileAll

	public static final void tileHorizontal(JDesktopPane desktop) {
		int _resizableCnt = 0;
		JInternalFrame _allFrames[] = desktop.getAllFrames();
		for (int _x = 0; _x < _allFrames.length; _x++) {
			JInternalFrame _frame = _allFrames[_x];
			if ((_frame.isVisible()) && (!_frame.isIcon())) {
				if (!_frame.isResizable()) {
					try {
						_frame.setMaximum(false);
					} catch (Exception _e) {
						// OK, to take no action here
					}
				}
				if (_frame.isResizable()) {
					_resizableCnt++;
				}
			}
		} // End for
		int _width = desktop.getBounds().width;
		int _height = arrangeIcons(desktop);
		if (_resizableCnt != 0) {
			int _fHeight = _height / _resizableCnt;
			int _yPos = 0;
			for (int _x = 0; _x < _allFrames.length; _x++) {
				JInternalFrame _frame = _allFrames[_x];
				if ((_frame.isVisible()) &&
					(_frame.isResizable()) &&
					(!_frame.isIcon())) {
					_frame.setSize(_width, _fHeight);
					_frame.setLocation(0, _yPos);
					_yPos += _fHeight;
				}
			} // End for
		}
	}

	public static final int arrangeIcons(JDesktopPane desktop) {
		int _iconCnt = 0;
		JInternalFrame _allFrames[] = desktop.getAllFrames();
		for (int _x = 0; _x < _allFrames.length; _x++) {
			if ((_allFrames[_x].isVisible()) && (_allFrames[_x].isIcon())) {
				_iconCnt++;
			}
		}
		int _height = desktop.getBounds().height;
		int _yPos = _height;
		if (_iconCnt != 0) {
			int _width = desktop.getBounds().width;
			int _xPos = 0;
			for (int _x = 0; _x < _allFrames.length; _x++) {
				JInternalFrame _frame = _allFrames[_x];
				if ((_frame.isVisible()) && (_frame.isIcon())) {
					Dimension _dim = _frame.getDesktopIcon().getSize();
					int _iWidth = _dim.width;
					int _iHeight = _dim.height;
					if (_yPos == _height) {
						_yPos = _height - _iHeight;
					}
					if ((_xPos + _iWidth > _width) && (_xPos != 0)) {
						_xPos = 0;
						_yPos -= _iHeight;
					}
					_frame.getDesktopIcon().setLocation(_xPos, _yPos);
					_xPos += _iWidth;
				} // End if
			} // End for
		} // End if
		return (_yPos);
	} // End method

	// Layout all of the children of this container
	// so that they are cascaded.
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
					} catch (java.beans.PropertyVetoException e) {
						// The frame refused to be iconified (it asks something first); it simply stays open.
						log.debug("{} was not minimized: {}", jif.getTitle(), e.getMessage());
					}
				}
			}
		}
	}

	// Place a component so that it is cascaded
	// relative to the previous one
	protected static void cascade(Component comp, JDesktopPane desktop) {
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

		nextX += offsetX;
		nextY += offsetY;
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

package nl.errorsoft.esql.ui.util;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/** What a mouse click means. A double click opens or toggles only with the left button: a quick double right click (or Ctrl+click on macOS) is a menu. */
public final class MouseClicks {
	private MouseClicks() {
	}

	public static boolean isDoubleClick(MouseEvent e) {
		return isDoubleClick(e.getButton(), e.getClickCount(), e.isPopupTrigger(), e.getModifiersEx());
	}

	/** Two clicks of the left button without Ctrl and not a popup trigger. */
	static boolean isDoubleClick(int button, int clickCount, boolean popupTrigger, int modifiersEx) {
		return button == MouseEvent.BUTTON1 && clickCount == 2 && !popupTrigger && (modifiersEx & InputEvent.CTRL_DOWN_MASK) == 0;
	}
}

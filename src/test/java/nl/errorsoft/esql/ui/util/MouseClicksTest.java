package nl.errorsoft.esql.ui.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.junit.jupiter.api.Test;

class MouseClicksTest {
	@Test
	void onlyTwoLeftClicksOpen() {
		assertTrue(MouseClicks.isDoubleClick(MouseEvent.BUTTON1, 2, false, 0));
		assertFalse(MouseClicks.isDoubleClick(MouseEvent.BUTTON1, 1, false, 0));
		assertFalse(MouseClicks.isDoubleClick(MouseEvent.BUTTON3, 2, false, 0));
		assertFalse(MouseClicks.isDoubleClick(MouseEvent.BUTTON3, 2, true, 0));
		assertFalse(MouseClicks.isDoubleClick(MouseEvent.BUTTON1, 2, true, 0));
		assertFalse(MouseClicks.isDoubleClick(MouseEvent.BUTTON1, 2, false, InputEvent.CTRL_DOWN_MASK));
	}
}

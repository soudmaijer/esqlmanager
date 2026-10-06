package nl.errorsoft.esql.app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

class SplashWindowTest {
	@Test
	void isCentredOverTheMainWindow() {
		assertEquals(new Point(300, 180), SplashWindow.centeredIn(new Rectangle(100, 50, 800, 500), SplashWindow.SIZE));
	}
}

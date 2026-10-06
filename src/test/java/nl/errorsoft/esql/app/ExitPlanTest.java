package nl.errorsoft.esql.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExitPlanTest {

	@Test
	void nothingOpenQuitsWithoutAsking() {
		assertFalse(new ExitPlan(0, 0, true).needsConfirmation());
		assertTrue(new ExitPlan(1, 0, true).needsConfirmation());
		assertTrue(new ExitPlan(0, 1, false).needsConfirmation());
	}

	@Test
	void theMessageNamesWhatCloses() {
		assertEquals("Close 2 connections and quit?", new ExitPlan(2, 0, true).message());
		assertEquals("Close 1 connection and exit?", new ExitPlan(1, 0, false).message());
		assertEquals("Close 1 designer and exit?", new ExitPlan(0, 1, false).message());
		assertEquals("Close 2 connections and 1 designer, then quit?", new ExitPlan(2, 1, true).message());
	}

	@Test
	void macSaysQuitOthersSayExit() {
		assertEquals("Quit eSQLManager", new ExitPlan(1, 0, true).title("eSQLManager"));
		assertEquals("Exit eSQLManager", new ExitPlan(1, 0, false).title("eSQLManager"));
		assertEquals("Quit", new ExitPlan(1, 0, true).verb());
	}
}

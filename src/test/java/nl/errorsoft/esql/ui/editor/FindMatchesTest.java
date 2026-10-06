package nl.errorsoft.esql.ui.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class FindMatchesTest {
	private static final String LOG = "SELECT count(*) FROM orders\nselect 1\nSelect 2";

	@Test
	void findsEveryOccurrenceIgnoringCaseUnlessAsked() {
		assertEquals(List.of(0, 28, 37), FindMatches.positions(LOG, "select", false));
		assertEquals(List.of(28), FindMatches.positions(LOG, "select", true));
		assertEquals(List.of(), FindMatches.positions(LOG, "", false));
	}

	@Test
	void saysWhichMatchIsSelected() {
		List<Integer> positions = FindMatches.positions(LOG, "select", false);
		assertEquals("2 of 3", FindMatches.describe(positions, 28));
		assertEquals("3 matches", FindMatches.describe(positions, 5));
		assertEquals("No results", FindMatches.describe(List.of(), 0));
	}
}

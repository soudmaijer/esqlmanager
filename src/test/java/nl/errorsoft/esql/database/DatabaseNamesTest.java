package nl.errorsoft.esql.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatabaseNamesTest {
	@Test
	void anEmptyNameAsksForOne() {
		assertEquals("Enter a name for the database.", DatabaseNames.problem("  ", "database", List.of()));
	}

	@Test
	void aTakenNameIsRefusedExactly() {
		assertEquals("A database named 'shop' exists already.", DatabaseNames.problem(" shop ", "database", List.of("shop")));
		// The server compares quoted names exactly, so another case is a different database.
		assertNull(DatabaseNames.problem("Shop", "database", List.of("shop")));
	}
}

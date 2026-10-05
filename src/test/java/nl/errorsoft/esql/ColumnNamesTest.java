package nl.errorsoft.esql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import nl.errorsoft.esql.table.ColumnNames;
import org.junit.jupiter.api.Test;

class ColumnNamesTest {
	@Test
	void nextNameIsTheFirstFree() {
		assertEquals("column_1", ColumnNames.next(List.of()));
		assertEquals("column_2", ColumnNames.next(List.of("column_1", "id")));
		assertEquals("column_1", ColumnNames.next(List.of("column_2")));
		assertEquals("column_3", ColumnNames.next(List.of("COLUMN_1", "column_2")));
	}

	@Test
	void usedIgnoresCaseAndSpaces() {
		assertTrue(ColumnNames.isUsed(" Id ", List.of("id")));
		assertFalse(ColumnNames.isUsed("name", List.of("id")));
	}
}

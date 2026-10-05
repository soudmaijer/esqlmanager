package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatabaseSelectionTest {

	@Test
	void csvRoundTripKeepsTheOrderAndSkipsBlanks() {
		DatabaseSelection selection = DatabaseSelection.parse(" shop , ,crm,shop,");

		assertEquals(List.of("shop", "crm"), selection.databases());
		assertEquals("shop,crm", selection.toCsv());
		assertEquals(selection, DatabaseSelection.parse(selection.toCsv()));
	}

	@Test
	void nothingSelectedShowsEverything() {
		DatabaseSelection none = DatabaseSelection.parse(null);

		assertTrue(none.isEmpty());
		assertTrue(none.showsDatabase("anything"));
		assertTrue(none.showsSchema("anything", "public"));
		assertEquals("", none.toCsv());
		assertFalse(none.hasSchemaFilter());
	}

	@Test
	void aSelectedDatabaseWithoutSchemasShowsAllItsSchemas() {
		DatabaseSelection selection = DatabaseSelection.parse("shop");

		assertTrue(selection.showsDatabase("shop"));
		assertTrue(selection.showsDatabase("SHOP"));
		assertFalse(selection.showsDatabase("crm"));
		assertTrue(selection.showsSchema("shop", "public"));
		assertTrue(selection.showsSchema("shop", "archive"));
		assertFalse(selection.showsSchema("crm", "public"));
	}

	@Test
	void selectedSchemasLimitTheirDatabaseOnly() {
		DatabaseSelection selection = DatabaseSelection.parse("shop,crm").withSchema("shop", "public", true);

		assertTrue(selection.hasSchemaFilter());
		assertTrue(selection.showsSchema("shop", "public"));
		assertFalse(selection.showsSchema("shop", "archive"));
		assertTrue(selection.showsSchema("crm", "archive"));
	}

	@Test
	void tickingASchemaTicksItsDatabase() {
		DatabaseSelection selection = DatabaseSelection.NONE.withSchema("shop", "public", true);

		assertEquals(List.of("shop"), selection.databases());
		assertEquals(Set.of("public"), selection.schemasOf("shop"));
	}

	@Test
	void untickingADatabaseDropsItsSchemas() {
		DatabaseSelection selection = DatabaseSelection.NONE.withSchema("shop", "public", true).withDatabase("shop", false);

		assertTrue(selection.isEmpty());
		assertEquals(Set.of(), selection.schemasOf("shop"));
	}

	@Test
	void untickingTheLastSchemaShowsAllSchemasAgain() {
		DatabaseSelection selection = DatabaseSelection.NONE.withSchema("shop", "public", true).withSchema("shop", "public", false);

		assertEquals(List.of("shop"), selection.databases());
		assertTrue(selection.showsSchema("shop", "anything"));
	}

	@Test
	void onlyDatabasesDropsTheOnesThatAreGone() {
		DatabaseSelection selection = DatabaseSelection.parse("shop,gone").withSchema("gone", "x", true).onlyDatabases("shop"::equals);

		assertEquals(List.of("shop"), selection.databases());
	}

	@Test
	void aDatabaseNameWithACommaIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new DatabaseSelection(Map.of("a,b", Set.of())));
		assertFalse(DatabaseSelection.isStorable("a,b"));
		assertFalse(DatabaseSelection.isStorable(" "));
		assertTrue(DatabaseSelection.isStorable("shop"));
	}
}

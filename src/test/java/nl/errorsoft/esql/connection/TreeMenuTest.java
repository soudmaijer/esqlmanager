package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.connection.TreeMenu.Item;
import nl.errorsoft.esql.connection.TreeMenu.Node;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.Dialects;

class TreeMenuTest {
	private static final Dialect MY_SQL = Dialects.forType(ServerType.MY_SQL);
	private static final Dialect POSTGRES = Dialects.forType(ServerType.POSTGRES);
	private static final Dialect SQL_SERVER = Dialects.forType(ServerType.MS_SQL_SERVER);

	@Test
	void mySqlTableMenuHasEveryMaintenanceCommand() {
		List<Item> items = TreeMenu.itemsFor(Node.TABLE, MY_SQL);
		assertTrue(items.containsAll(List.of(Item.OPTIMIZE, Item.ANALYZE, Item.CHECK, Item.REPAIR, Item.INDEXES, Item.EDIT_TABLE)));
	}

	@Test
	void postgresTableMenuOnlyHasWhatPostgresCanDo() {
		List<Item> items = TreeMenu.itemsFor(Node.TABLE, POSTGRES);
		assertTrue(items.containsAll(List.of(Item.OPTIMIZE, Item.ANALYZE)));
		assertFalse(items.contains(Item.CHECK));
		assertFalse(items.contains(Item.REPAIR));
	}

	@Test
	void browsingOnlyServerHasNoDdlOrServerTools() {
		assertEquals(List.of(Item.NEW_QUERY, Item.SEPARATOR, Item.RELOAD_DATABASES), TreeMenu.itemsFor(Node.SERVER, SQL_SERVER));
		assertEquals(List.of(Item.RELOAD_COLUMNS), TreeMenu.itemsFor(Node.COLUMN, SQL_SERVER));
		assertEquals(List.of(Item.OPEN_TABLE, Item.SEPARATOR, Item.EMPTY_TABLE, Item.DROP_TABLE, Item.SEPARATOR, Item.RELOAD_COLUMNS),
			TreeMenu.itemsFor(Node.TABLE, SQL_SERVER));
	}

	@Test
	void serverMenuOfAFullDialect() {
		assertEquals(List.of(Item.CREATE_DATABASE, Item.NEW_QUERY, Item.SEPARATOR, Item.USERS, Item.PROCESS_LIST, Item.SERVER_STATUS, Item.SERVER_VARIABLES,
			Item.SEPARATOR, Item.EXPORT, Item.IMPORT, Item.SEPARATOR, Item.RELOAD_DATABASES), TreeMenu.itemsFor(Node.SERVER, POSTGRES));
	}

	@Test
	void noMenuStartsOrEndsWithASeparatorOrHasTwoInARow() {
		for (Dialect dialect : List.of(MY_SQL, POSTGRES, SQL_SERVER, Dialects.forType(ServerType.ORACLE))) {
			for (Node node : Node.values()) {
				List<Item> items = TreeMenu.itemsFor(node, dialect);
				assertFalse(items.isEmpty());
				assertFalse(items.getFirst() == Item.SEPARATOR || items.getLast() == Item.SEPARATOR, node + " " + dialect);
				for (int i = 1; i < items.size(); i++) {
					assertFalse(items.get(i) == Item.SEPARATOR && items.get(i - 1) == Item.SEPARATOR, node + " " + dialect);
				}
			}
		}
	}
}

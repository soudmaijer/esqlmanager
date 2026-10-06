package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.connection.TreeMenu.Item;
import nl.errorsoft.esql.connection.TreeMenu.Node;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.DialectFactory;

class TreeMenuTest {
	private static final Dialect MY_SQL = DialectFactory.forType(ServerType.MY_SQL);
	private static final Dialect POSTGRES = DialectFactory.forType(ServerType.POSTGRES);
	private static final Dialect SQL_SERVER = DialectFactory.forType(ServerType.MS_SQL_SERVER);

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
		assertEquals(List.of(Item.RELOAD_DATABASES), TreeMenu.itemsFor(Node.SERVER, SQL_SERVER));
		assertEquals(List.of(Item.RELOAD_COLUMNS), TreeMenu.itemsFor(Node.COLUMN, SQL_SERVER));
		assertEquals(List.of(Item.OPEN_TABLE, Item.SEPARATOR, Item.EMPTY_TABLE, Item.DROP_TABLE, Item.SEPARATOR, Item.RELOAD_COLUMNS),
			TreeMenu.itemsFor(Node.TABLE, SQL_SERVER));
	}

	@Test
	void serverMenuOfAFullDialect() {
		assertEquals(List.of(Item.CREATE_DATABASE, Item.SEPARATOR, Item.USERS, Item.PROCESS_LIST, Item.SERVER_STATUS, Item.SERVER_VARIABLES,
			Item.SEPARATOR, Item.EXPORT, Item.IMPORT, Item.SEPARATOR, Item.RELOAD_DATABASES), TreeMenu.itemsFor(Node.SERVER, POSTGRES));
	}

	@Test
	void aConnectionHasTheServerItemsAndDisconnectAtTheEnd() {
		for (Dialect dialect : List.of(MY_SQL, POSTGRES, SQL_SERVER)) {
			List<Item> connection = TreeMenu.itemsFor(Node.CONNECTION, dialect);
			List<Item> server = TreeMenu.itemsFor(Node.SERVER, dialect);
			assertEquals(server, connection.subList(0, server.size()), String.valueOf(dialect));
			assertEquals(List.of(Item.SEPARATOR, Item.DISCONNECT), connection.subList(server.size(), connection.size()));
		}
	}

	@Test
	void aSavedProfileCanOnlyBeConnectedOrEdited() {
		assertEquals(List.of(Item.CONNECT, Item.EDIT_PROFILE), TreeMenu.itemsFor(Node.PROFILE, POSTGRES));
		assertEquals("Edit connection...", Item.EDIT_PROFILE.label(MY_SQL));
	}

	@Test
	void postgresDatabaseHoldsSchemasAndTheLabelsSaySo() {
		List<Item> database = TreeMenu.itemsFor(Node.DATABASE, POSTGRES);
		assertTrue(database.containsAll(List.of(Item.CREATE_SCHEMA, Item.RELOAD_SCHEMAS)));
		assertFalse(database.contains(Item.RELOAD_TABLES));
		assertEquals("Reload schemas", Item.RELOAD_SCHEMAS.label(POSTGRES));
		assertEquals("Reload databases", Item.RELOAD_DATABASES.label(POSTGRES));
		assertEquals(
			List.of(Item.NEW_QUERY, Item.SEPARATOR, Item.CREATE_TABLE, Item.OPEN_IN_DESIGNER, Item.SEPARATOR, Item.EXPORT, Item.IMPORT, Item.RENAME_SCHEMA,
				Item.DROP_SCHEMA,
				Item.SEPARATOR, Item.RELOAD_TABLES),
			TreeMenu.itemsFor(Node.SCHEMA, POSTGRES));
	}

	@Test
	void tablesCanBeRenamedDuplicatedAndInspectedWhereTheServerCanDdl() {
		for (Dialect dialect : List.of(MY_SQL, POSTGRES)) {
			List<Item> table = TreeMenu.itemsFor(Node.TABLE, dialect);
			assertTrue(table.containsAll(List.of(Item.RENAME_TABLE, Item.DUPLICATE_TABLE, Item.PROPERTIES)), String.valueOf(dialect));
			assertTrue(TreeMenu.itemsFor(Node.DATABASE, dialect).contains(Item.PROPERTIES));
		}
		List<Item> sqlServer = TreeMenu.itemsFor(Node.TABLE, SQL_SERVER);
		assertFalse(sqlServer.contains(Item.RENAME_TABLE) || sqlServer.contains(Item.DUPLICATE_TABLE) || sqlServer.contains(Item.PROPERTIES));
		assertFalse(TreeMenu.itemsFor(Node.DATABASE, SQL_SERVER).contains(Item.PROPERTIES));
	}

	@Test
	void onlyServersWithSchemasCanRenameThem() {
		assertTrue(TreeMenu.itemsFor(Node.SCHEMA, POSTGRES).contains(Item.RENAME_SCHEMA));
		assertFalse(TreeMenu.itemsFor(Node.SCHEMA, MY_SQL).contains(Item.RENAME_SCHEMA));
		assertEquals("Rename schema...", Item.RENAME_SCHEMA.label(POSTGRES));
	}

	@Test
	void mySqlDatabaseHoldsTables() {
		List<Item> database = TreeMenu.itemsFor(Node.DATABASE, MY_SQL);
		assertTrue(database.contains(Item.RELOAD_TABLES));
		assertFalse(database.contains(Item.CREATE_SCHEMA) || database.contains(Item.RELOAD_SCHEMAS));
		assertEquals("Reload databases", Item.RELOAD_DATABASES.label(MY_SQL));
	}

	@Test
	void noMenuStartsOrEndsWithASeparatorOrHasTwoInARow() {
		for (Dialect dialect : List.of(MY_SQL, POSTGRES, SQL_SERVER, DialectFactory.forType(ServerType.ORACLE))) {
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

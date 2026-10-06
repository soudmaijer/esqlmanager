package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import org.junit.jupiter.api.Test;

class TreeSelectionTest {
	private final Database database = new Database("shop");
	private final Schema schema = new Schema(database, "sales");
	private final Table table = new Table(schema);
	private final TableColumn column = new TableColumn(table);

	@Test
	void nothingSelectedGivesNothing() {
		assertNull(TreeSelection.database(null));
		assertNull(TreeSelection.schema(null));
		assertNull(TreeSelection.table(null));
		assertNull(TreeSelection.column(null));
	}

	@Test
	void theServerNodeHasNoDatabaseOrTable() {
		Object server = "postgres@localhost";
		assertNull(TreeSelection.database(server));
		assertNull(TreeSelection.table(server));
	}

	@Test
	void aColumnLeadsToItsTableSchemaAndDatabase() {
		assertSame(column, TreeSelection.column(column));
		assertSame(table, TreeSelection.table(column));
		assertSame(schema, TreeSelection.schema(column));
		assertSame(database, TreeSelection.database(column));
	}

	@Test
	void aNodeBelongsToTheNearestConnectionOnItsPath() {
		ConnectionNode connection = new ConnectionNode(new ConnectionProfile(), "postgres@localhost");
		assertSame(connection, TreeSelection.connection(List.of("root", connection, database, schema, table, column)));
		assertSame(connection, TreeSelection.connection(List.of("root", connection)));
	}

	@Test
	void aProfileOrNothingHasNoConnection() {
		assertNull(TreeSelection.connection(null));
		assertNull(TreeSelection.connection(List.of()));
		assertNull(TreeSelection.connection(List.of("root", new ProfileNode(new ConnectionProfile()))));
	}

	@Test
	void twoConnectionsOfOneProfileAreTwoNodes() {
		ConnectionProfile profile = new ConnectionProfile();
		assertNotEquals(new ConnectionNode(profile, "a"), new ConnectionNode(profile, "a"));
	}

	@Test
	void aDatabaseHasNoTable() {
		assertSame(database, TreeSelection.database(database));
		assertNull(TreeSelection.table(database));
		assertNull(TreeSelection.schema(database));
		assertSame(database, TreeSelection.database(schema));
	}
}

package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
	void aDatabaseHasNoTable() {
		assertSame(database, TreeSelection.database(database));
		assertNull(TreeSelection.table(database));
		assertNull(TreeSelection.schema(database));
		assertSame(database, TreeSelection.database(schema));
	}
}

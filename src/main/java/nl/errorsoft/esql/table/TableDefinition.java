package nl.errorsoft.esql.table;

import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;

/**
 * A new table: where it goes, its name, type (storage engine, null or empty when the server has none), comment and columns.
 * @param schema the schema to create it in, null for the current schema of the database
 */
public record TableDefinition(Database database, Schema schema, String name, String type, String comment, List<CreateColumn> columns) {
	public TableDefinition {
		columns = List.copyOf(columns);
	}

	/** The same table in another schema, null for the current one. */
	public TableDefinition inSchema(Schema schema) {
		return new TableDefinition(database, schema, name, type, comment, columns);
	}
}

package nl.errorsoft.esql.job;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.table.Table;

/** What an SQL script is written from (export) or run into (import): a whole database, one schema, or one table. */
public sealed interface ScriptTarget {
	/** A whole database, with every schema on servers that have them. */
	record OfDatabase(Database database) implements ScriptTarget {
		public String toString() {
			return database.getName();
		}
	}

	/** One schema of a database. */
	record OfSchema(Schema schema) implements ScriptTarget {
		public String toString() {
			return schema.getName();
		}
	}

	/** One table. */
	record OfTable(Table table) implements ScriptTarget {
		public String toString() {
			return table.getName();
		}
	}

	/** The target of a node of a tree; anything else (the server, a column) cannot be exported or imported into. */
	static ScriptTarget of(Object node) throws EsqlException {
		return switch (node) {
			case Database database -> new OfDatabase(database);
			case Schema schema -> new OfSchema(schema);
			case Table table -> new OfTable(table);
			case null -> throw new EsqlException("Select a database, schema or table.");
			default -> throw new EsqlException("'" + node + "' is not a database, schema or table.");
		};
	}
}

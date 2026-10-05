package nl.errorsoft.esql.query;

import java.util.List;

/**
 * The names auto completion offers: the tables of the current database and their columns. On servers with schemas an unqualified table is one of the
 * current schema (the first of the search path), the others are reached as {@code schema.table}.
 */
public interface SchemaNames {
	/** The tables of the current database (of its current schema), empty while they are still loading. */
	List<String> tables();

	/** The schemas of the current database, empty on servers without schemas. */
	List<String> schemas();

	/** The tables of a schema (found case-insensitively), empty for an unknown schema. */
	List<String> tables(String schema);

	/** The columns of a table, {@code table} or {@code schema.table} (found case-insensitively), empty for an unknown table. */
	List<String> columns(String table);

	/** The name quoted the way the database of the connection quotes names. */
	String quote(String name);
}

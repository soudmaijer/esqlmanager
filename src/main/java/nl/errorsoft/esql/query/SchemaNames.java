package nl.errorsoft.esql.query;

import java.util.List;

/** The names auto completion offers: the tables of the current database and their columns. */
public interface SchemaNames {
	/** The tables of the current database, empty while they are still loading. */
	List<String> tables();

	/** The columns of a table (found case-insensitively), empty for an unknown table. */
	List<String> columns(String table);

	/** The name quoted the way the database of the connection quotes names. */
	String quote(String name);
}

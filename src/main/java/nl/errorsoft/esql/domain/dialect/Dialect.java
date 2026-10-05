package nl.errorsoft.esql.domain.dialect;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.ConnectionProfile;
import nl.errorsoft.esql.domain.Database;
import nl.errorsoft.esql.domain.Table;

/**
 * Everything that differs between database servers. Callers ask the dialect
 * instead of checking which server type they are talking to.
 */
public interface Dialect
{
	/** Optional functionality that not every database has an implementation for. */
	enum Feature
	{
		DESIGNER, USER_MANAGER, CREATE_TABLE, INDEXES, IMPORT, EXPORT
	}

	int getType();

	String getDefaultPort();

	String getDefaultUsername();

	boolean supports( Feature feature );

	/**
	 * The database to put in the connection URL.
	 * @param requested a database the caller wants to connect to, empty for the profile default.
	 */
	String getConnectionDatabase( ConnectionProfile cp, String requested );

	/** Names of the databases on the server. */
	List<String> listDatabases( DatabaseConnection dbc ) throws SQLException;

	/** Makes the given database the active one for the connection. */
	void useDatabase( DatabaseConnection dbc, String database ) throws SQLException;

	/** The schema that tables are looked up in, or null when the server has no schema concept. */
	String getSchema( DatabaseConnection dbc ) throws SQLException;

	/** The tables and views of the active database. */
	Vector<Table> listTables( DatabaseConnection dbc, Database db ) throws SQLException;

	/** A query returning one page of a table, or null when the server cannot page in SQL. */
	String selectPage( String quotedTable, int skip, int show );
}

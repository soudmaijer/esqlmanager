package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.table.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.domain.CreateColumn;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.connection.ServerProcess;
import nl.errorsoft.esql.table.Table;

/**
 * Everything that differs between database servers. Callers ask the dialect
 * instead of checking which server type they are talking to.
 */
public interface Dialect
{
	/** Table maintenance commands, named after the MySQL ones. */
	enum Maintenance
	{
		OPTIMIZE, ANALYZE, CHECK, REPAIR
	}

	/** Optional functionality that not every database has an implementation for. */
	enum Feature
	{
		DESIGNER, USER_MANAGER, CREATE_TABLE, INDEXES, IMPORT, EXPORT, PROCESS_LIST, SERVER_STATUS
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

	/**
	 * A query returning one page of a table, or null when the server cannot page in SQL.
	 * @param orderBy the quoted columns that give the rows a stable order, empty when there are none.
	 */
	String selectPage( String quotedTable, String orderBy, int skip, int show );

	/** Quotes a table, column or index name. */
	String quote( String identifier );

	/** Storage engines a table can be created with, empty when the server has no such choice. */
	String[] getTableTypes();

	/** The statements that create a table, including its indexes and comment. */
	List<String> createTableSql( String table, List<CreateColumn> columns, String tableType, String comment );

	List<String> renameTableSql( String table, String newName );

	List<String> setTableTypeSql( String table, String tableType );

	List<String> setTableCommentSql( String table, String comment );

	List<String> addColumnSql( String table, CreateColumn column );

	/** Changes an existing column to the given definition, renaming it when the name differs. */
	List<String> modifyColumnSql( String table, String oldName, CreateColumn column );

	/**
	 * Adds an index on the given columns.
	 * @param name the index name, "PRIMARY" for the primary key.
	 * @param type INDEX, UNIQUE or FULLTEXT.
	 */
	List<String> addIndexSql( String table, String name, String type, List<String> columns );

	List<String> dropIndexSql( DatabaseConnection dbc, String table, String name ) throws SQLException;

	List<String> modifyIndexSql( DatabaseConnection dbc, String table, String name, String type, List<String> columns ) throws SQLException;

	/**
	 * Runs a maintenance command on a table.
	 * @return the message to show the user.
	 * @throws UnsupportedOperationException when the server has no such command.
	 */
	String maintain( DatabaseConnection dbc, Maintenance command, String table ) throws SQLException;

	/** A text value as an SQL literal. */
	String literal( String value );

	String createDatabaseSql( String database );

	/** Removes a database, also when the connection is using it. */
	void dropDatabase( DatabaseConnection dbc, String database ) throws SQLException;

	/** The statement that makes a database the active one in a script, understood by {@link #useDatabaseSql} consumers such as Import. */
	String useDatabaseSql( String database );

	/** Statements to run after the rows of a table have been loaded, such as moving auto numbering past the highest value. */
	List<String> afterDataLoadSql( DatabaseConnection dbc, String table ) throws SQLException;

	/** The CREATE TABLE statement of an existing table. */
	String createTableDdl( DatabaseConnection dbc, String table ) throws SQLException;

	/** The account and privilege management of this server. */
	UserAdmin getUserAdmin();

	/** A query returning the statistics of the server, null when there are none. */
	String getStatusQuery();

	/** A query returning the configuration settings of the server, null when there are none. */
	String getVariablesQuery();

	/** The connections that are active on the server. */
	List<ServerProcess> listProcesses( DatabaseConnection dbc ) throws SQLException;

	/** Ends a process on the server. */
	void killProcess( DatabaseConnection dbc, String processId ) throws SQLException;
}

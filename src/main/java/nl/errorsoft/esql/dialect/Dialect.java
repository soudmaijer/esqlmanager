package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.table.TableName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.table.Table;

/**
 * Everything that differs between database servers. Callers ask the dialect
 * instead of checking which server type they are talking to.
 * A dialect never runs SQL: it writes statements and reads result rows, the repositories run them.
 */
public interface Dialect {
	/** Table maintenance commands, named after the MySQL ones. */
	enum Maintenance {
		OPTIMIZE, ANALYZE, CHECK, REPAIR
	}

	/** Optional functionality that not every database has an implementation for. */
	enum Feature {
		DESIGNER, USER_MANAGER, CREATE_DATABASE, CREATE_TABLE, INDEXES, IMPORT, EXPORT, PROCESS_LIST, SERVER_STATUS, FOREIGN_KEYS,
		/** A database holds schemas, which hold the tables: the tree shows server, databases, schemas, tables. */
		SCHEMAS
	}

	int getType();

	String getDefaultPort();

	String getDefaultUsername();

	boolean supports(Feature feature);

	/**
	 * The database to put in the connection URL.
	 * @param requested a database the caller wants to connect to, empty for the profile default.
	 */
	String getConnectionDatabase(ConnectionProfile cp, String requested);

	/** How a connection makes another database the active one. */
	enum DatabaseSwitch {
		/** {@link java.sql.Connection#setCatalog}. */
		CATALOG,
		/** Connect again, to the other database. */
		RECONNECT,
		/** The database is part of the connection, there is nothing to switch. */
		NONE
	}

	/** A query whose first column is the name of a database, null when the profile's database is the only one. */
	String listDatabasesSql();

	DatabaseSwitch databaseSwitch();

	/** What the server calls a database, for menus and messages ("database"). */
	String databaseTerm();

	/** What the server calls a schema ("schema"), only meaningful when it {@link #supports} {@link Feature#SCHEMAS}. */
	String schemaTerm();

	/** A query listing the schemas of the connected database that hold user tables, null when the server has no schemas. */
	String listSchemasSql();

	String createSchemaSql(String schema);

	/** Creates a schema unless it exists, for a script that restores tables into it. */
	String createSchemaIfMissingSql(String schema);

	/** Makes unqualified names resolve to the schema (PostgreSQL sets the search_path), null when the server has no schemas. */
	String useSchemaSql(String schema);

	/** Drops a schema with everything in it. */
	String dropSchemaSql(String schema);

	/** A query returning the schema that tables are looked up in, null when the server has no schema concept. */
	String currentSchemaSql();

	/** A query listing the tables of the active database with their row counts, read with {@link #readTable}; null to use the JDBC metadata. */
	String listTablesSql();

	/** The table in the current row of {@link #listTablesSql}. */
	Table readTable(ResultSet row, Database db) throws SQLException;

	/** The table types asked from {@link java.sql.DatabaseMetaData#getTables} when there is no {@link #listTablesSql}. */
	String[] metadataTableTypes();

	/**
	 * A query returning one page of a table, or null when the server cannot page in SQL.
	 * @param orderBy the quoted columns that give the rows a stable order, empty when there are none.
	 */
	String selectPage(String quotedTable, String orderBy, int skip, int show);

	/** Quotes a table, column or index name. */
	String quote(String identifier);

	/** Quotes a table name, prefixed with its quoted schema when it has one. */
	String quote(TableName table);

	/** Storage engines a table can be created with, empty when the server has no such choice. */
	String[] getTableTypes();

	/** The statements that create a table, including its indexes and comment. */
	List<String> createTableSql(TableName table, List<CreateColumn> columns, String tableType, String comment);

	/** Whether a column can carry a comment. */
	boolean supportsColumnComments();

	List<String> renameTableSql(TableName table, String newName);

	List<String> setTableTypeSql(TableName table, String tableType);

	List<String> setTableCommentSql(TableName table, String comment);

	List<String> addColumnSql(TableName table, CreateColumn column);

	/** Changes an existing column to the given definition, renaming it when the name differs. */
	List<String> modifyColumnSql(TableName table, String oldName, CreateColumn column);

	/** The kinds of index a user can choose, the values {@link #addIndexSql} takes as type. SPATIAL is not written by any dialect, so it is not offered. */
	default List<String> indexTypes() {
		return List.of("INDEX", "UNIQUE", "FULLTEXT");
	}

	/**
	 * Adds an index on the given columns.
	 * @param name the index name, "PRIMARY" for the primary key.
	 * @param type INDEX, UNIQUE or FULLTEXT.
	 */
	List<String> addIndexSql(TableName table, String name, String type, List<String> columns);

	/**
	 * Drops an index.
	 * @param name the index name, "PRIMARY" for the primary key.
	 * @param primaryKeyName the name the server gave the primary key constraint, used when name is PRIMARY.
	 */
	List<String> dropIndexSql(TableName table, String name, String primaryKeyName);

	/** Replaces an index, see {@link #dropIndexSql} for the names. */
	List<String> modifyIndexSql(TableName table, String name, String primaryKeyName, String type, List<String> columns);

	/** The referential actions a foreign key may have, anything else is refused so no text from a model ends up in a statement. */
	List<String> REFERENTIAL_ACTIONS = List.of("NO ACTION", "CASCADE", "SET NULL", "RESTRICT", "SET DEFAULT");

	/**
	 * Adds a foreign key constraint.
	 * @param onDelete NO ACTION, CASCADE, SET NULL, RESTRICT or SET DEFAULT, empty for the server default.
	 * @param onUpdate as onDelete.
	 * @throws nl.errorsoft.esql.error.EsqlException when an action is not one of the allowed ones.
	 */
	List<String> addForeignKeySql(TableName table, String name, List<String> columns, String refTable, List<String> refColumns, String onDelete,
		String onUpdate);

	List<String> dropForeignKeySql(TableName table, String name);

	/** A query with the schema (null for the current one) and the table name as parameters, returning the storage engine of the table; null when the server has no table types. */
	String tableTypeSql();

	/**
	 * Checks that the table can hold a foreign key, before one is added.
	 * @param tableType the storage engine read with {@link #tableTypeSql}, null when there is none.
	 * @throws nl.errorsoft.esql.error.EsqlException when it can't, such as a MySQL table that is not InnoDB.
	 */
	void checkForeignKeyTable(TableName table, String tableType);

	/**
	 * The statement of a maintenance command on a table.
	 * @throws UnsupportedOperationException when the server has no such command.
	 */
	MaintenanceStatement maintenanceSql(Maintenance command, TableName table);

	/** The maintenance commands that {@link #maintenanceSql} has a statement for on this server, empty when it has none. */
	java.util.Set<Maintenance> maintenanceCommands();

	/** A text value as an SQL literal. */
	String literal(String value);

	String createDatabaseSql(String database);

	/** The choices offered when creating a database (character set, owner, ...), empty when the server has none. */
	List<DatabaseOption> createDatabaseOptions();

	/**
	 * CREATE DATABASE with the chosen options.
	 * @param options the value per {@link DatabaseOption#key()}, options that are missing or blank are left to the server default.
	 */
	String createDatabaseSql(String database, java.util.Map<String, String> options);

	/**
	 * A query that returns the properties of a database as one row, the database name is its only parameter; each column is a property named by its
	 * label. Null when the server has nothing to show.
	 */
	String databasePropertiesSql();

	/** Renames a schema, only meaningful when the server {@link #supports} {@link Feature#SCHEMAS}. */
	String renameSchemaSql(String schema, String newName);

	/** Copies the structure of a table (columns, defaults, indexes) to a new table and, when asked, its rows. */
	List<String> copyTableSql(TableName source, TableName target, boolean withData);

	/** A query with the schema (null for the current one) and the table name as parameters, returning the size of the table with its indexes in bytes; null when unknown. */
	String tableSizeSql();

	String dropDatabaseSql(String database);

	/** The database to switch to before dropping the one the connection is using, null when the server can drop it while in use. */
	String databaseToLeaveFor(String database);

	/** The statement that makes a database the active one in a script, understood by {@link #useDatabaseSql} consumers such as Import. */
	String useDatabaseSql(String database);

	/**
	 * The database a statement typed in the query tab switches to, when it is the way {@link #useDatabaseSql} writes a switch on this server
	 * ({@code USE shop} on MySQL, {@code \connect shop} or {@code \c shop} elsewhere); null for any other statement. Such a statement is not sent as it
	 * is but goes through {@code DatabaseConnection.useDatabase}, so the application knows the database in use.
	 */
	String databaseSwitchTarget(String statement);

	/** A query with the schema (null for the current one) and the table name as parameters, returning the auto numbered columns that need {@link #afterDataLoadSql}; null when none do. */
	String autoNumberedColumnsSql();

	/**
	 * Statements to run after the rows of a table have been loaded, such as moving auto numbering past the highest value.
	 * @param autoNumberedColumns the columns found with {@link #autoNumberedColumnsSql}.
	 */
	List<String> afterDataLoadSql(TableName table, List<String> autoNumberedColumns);

	/**
	 * A column of an existing table as the designer models it, read from the current row of {@link java.sql.DatabaseMetaData#getColumns}:
	 * the type in the names of {@code datatypes.xml}, the length, the default as plain text, not null and auto numbering. The primary key is not set.
	 */
	CreateColumn readColumn(ResultSet columns) throws SQLException;

	/** The name in {@code datatypes.xml} of a type as the driver reports it (PostgreSQL {@code int4} is {@code integer}). */
	String datatypeName(String nativeTypeName);

	/** A query whose second column is the CREATE TABLE statement of an existing table, null when it is built from the metadata with {@link #createTableDdl}. */
	String showCreateTableSql(TableName table);

	/** A query whose second column is the CREATE VIEW statement of an existing view, null when the server cannot give the definition. */
	String showCreateViewSql(TableName view);

	String dropTableSql(TableName table, boolean ifExists);

	String dropViewSql(TableName view, boolean ifExists);

	/** The CREATE TABLE statement made to leave an existing table alone. */
	String createTableIfNotExists(String createTableSql);

	/** Starts a transaction in a script, null when the server has no such statement for a script. */
	String beginTransactionSql();

	/** Ends the transaction {@link #beginTransactionSql} started. */
	String commitSql();

	/** Makes the server stop checking foreign keys in this session so tables can be loaded in any order, null when it cannot be done safely. */
	String disableForeignKeyChecksSql();

	/** Undoes {@link #disableForeignKeyChecksSql}. */
	String enableForeignKeyChecksSql();

	/** A column definition for CREATE TABLE, from the current row of {@link java.sql.DatabaseMetaData#getColumns}. */
	String columnDdl(ResultSet columns) throws SQLException;

	/**
	 * The CREATE TABLE statement of an existing table, from its metadata.
	 * @param columnDefinitions made with {@link #columnDdl}.
	 * @param primaryKey the primary key columns in key order.
	 */
	String createTableDdl(TableName table, List<String> columnDefinitions, List<String> primaryKey);

	/** The account and privilege management of this server. */
	UserAdmin getUserAdmin();

	/** A query returning the statistics of the server, null when there are none. */
	String getStatusQuery();

	/** A query returning the configuration settings of the server, null when there are none. */
	String getVariablesQuery();

	/** A query listing the connections that are active on the server, read with {@link #readProcess}. */
	String listProcessesSql();

	ServerProcess readProcess(ResultSet row) throws SQLException;

	/** The statement that ends a process on the server. */
	String killProcessSql(String processId);
}

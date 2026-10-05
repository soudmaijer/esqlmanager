package nl.errorsoft.esql.dialect.postgres;

import nl.errorsoft.esql.table.TableName;

import nl.errorsoft.esql.dialect.AbstractDialect;
import nl.errorsoft.esql.dialect.DatabaseOption;
import nl.errorsoft.esql.dialect.MaintenanceStatement;
import nl.errorsoft.esql.dialect.UserAdmin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.connection.ServerType;

/**
 * PostgreSQL has one database per connection, so switching database means connecting again.
 * A database holds schemas (public and others), which hold the tables; unqualified names resolve through the search_path.
 */
public class PostgresDialect extends AbstractDialect {
	/** A text default with an optional cast, as PostgreSQL reports it: 'it''s'::character varying. */
	private static final Pattern CAST_DEFAULT = Pattern.compile("^'(.*)'(::[\\w\\s\\[\\]]+)?$", Pattern.DOTALL);

	/** serial, bigserial and smallserial are integer types with a sequence. */
	@Override
	protected String identityType(String typeName) {
		return switch (typeName) {
			case "serial" -> "integer";
			case "bigserial" -> "bigint";
			case "smallserial" -> "smallint";
			default -> typeName;
		};
	}

	/** A column with a sequence default (serial) is filled by the server too. */
	@Override
	protected boolean isAutoIncrement(ResultSet rs, String defaultValue) throws SQLException {
		return super.isAutoIncrement(rs, defaultValue) || defaultValue != null && defaultValue.startsWith("nextval(");
	}

	/** The driver calls char bpchar. */
	@Override
	protected boolean isSizedText(String typeName) {
		return super.isSizedText(ddlTypeName(typeName));
	}

	@Override
	protected String ddlTypeName(String typeName) {
		return typeName.equals("bpchar") ? "char" : typeName;
	}

	@Override
	protected String plainDefault(String defaultValue) {
		Matcher literal = CAST_DEFAULT.matcher(defaultValue);
		return literal.matches() ? literal.group(1).replace("''", "'") : defaultValue;
	}

	/** Reads the PostgreSQL type names of the driver (int4, bpchar, serial) as the names the designer uses (integer, char). */
	@Override
	public String datatypeName(String nativeTypeName) {
		return switch (identityType(nativeTypeName.toLowerCase())) {
			case "int2" -> "smallint";
			case "int4" -> "integer";
			case "int8" -> "bigint";
			case "float4" -> "real";
			case "float8" -> "double precision";
			case "bool" -> "boolean";
			case "bpchar" -> "char";
			case String other -> other;
		};
	}

	private static final String DEFAULT_DATABASE = "postgres";

	public int getType() {
		return ServerType.POSTGRES;
	}

	public String getDefaultPort() {
		return "5432";
	}

	public String getDefaultUsername() {
		return "postgres";
	}

	/** Rows loaded with explicit ids leave the sequence behind, so the next insert would reuse an id. */
	public String autoNumberedColumnsSql() {
		return "SELECT column_name FROM information_schema.columns WHERE table_schema = coalesce(?::text, current_schema()::text) AND table_name = ? AND ( is_identity = 'YES' OR column_default LIKE 'nextval%' )";
	}

	public List<String> afterDataLoadSql(TableName table, List<String> autoNumberedColumns) {
		List<String> statements = new ArrayList<>();

		for (String column : autoNumberedColumns) {
			statements.add("SELECT setval(pg_get_serial_sequence(" + literal(quote(table)) + ", " + literal(column) + "), (SELECT max(" + quote(column)
				+ ") FROM " + quote(table) + "))");
		}
		return statements;
	}

	/** The definition text is built on the server, the view is named as the script will name it. */
	public String showCreateViewSql(TableName view) {
		String name = literal(quote(view));
		return "SELECT NULL, 'CREATE VIEW ' || " + name + " || ' AS ' || pg_get_viewdef(" + name + "::regclass, true)";
	}

	/** A full text index is a GIN index on the words of the columns. */
	public List<String> addIndexSql(TableName table, String name, String type, List<String> columns) {
		if (!"FULLTEXT".equalsIgnoreCase(type)) {
			return super.addIndexSql(table, name, type, columns);
		}
		List<String> quoted = columns.stream().map(this::quote).toList();
		return List.of("CREATE INDEX " + quote(name) + " ON " + quote(table) + " USING GIN (to_tsvector('simple', " + String.join(" || ' ' || ", quoted)
			+ "))");
	}

	public List<String> copyTableSql(TableName source, TableName target, boolean withData) {
		List<String> statements = new ArrayList<>();
		statements.add("CREATE TABLE " + quote(target) + " (LIKE " + quote(source) + " INCLUDING ALL)");

		if (withData) {
			statements.add("INSERT INTO " + quote(target) + " SELECT * FROM " + quote(source));
		}
		return statements;
	}

	/** PostgreSQL cannot switch database with a statement, so scripts use the psql meta command that Import understands. */
	public String useDatabaseSql(String database) {
		return "\\connect " + quote(database);
	}

	public String databaseSwitchTarget(String statement) {
		return switchTarget(statement, "\\connect", "\\c");
	}

	public String beginTransactionSql() {
		return "BEGIN";
	}

	public String commitSql() {
		return "COMMIT";
	}

	public boolean supportsColumnComments() {
		return true;
	}

	public java.util.Set<Maintenance> maintenanceCommands() {
		return java.util.EnumSet.of(Maintenance.OPTIMIZE, Maintenance.ANALYZE);
	}

	public MaintenanceStatement maintenanceSql(Maintenance command, TableName table) {
		return switch (command) {
			case OPTIMIZE -> new MaintenanceStatement("VACUUM " + quote(table), null, "Vacuumed " + table);
			case ANALYZE -> new MaintenanceStatement("ANALYZE " + quote(table), null, "Analyzed " + table);
			default -> super.maintenanceSql(command, table);
		};
	}

	/** PostgreSQL does not drop the database the connection is using, so move to another one first. */
	public String databaseToLeaveFor(String database) {
		return database.equals(DEFAULT_DATABASE) ? "template1" : DEFAULT_DATABASE;
	}

	public String getStatusQuery() {
		return "SELECT datname, numbackends, xact_commit, xact_rollback, blks_read, blks_hit, tup_returned, tup_fetched, tup_inserted, tup_updated, tup_deleted FROM pg_stat_database WHERE datname IS NOT NULL ORDER BY datname";
	}

	public String getVariablesQuery() {
		return "SELECT name, setting, unit, short_desc FROM pg_settings ORDER BY name";
	}

	public String listProcessesSql() {
		// The own backend is listed too, as SHOW PROCESSLIST does on MySQL: the list runs on a connection of its own, which on a quiet server is the
		// only one, and leaving it out showed an empty list.
		return "SELECT pid, usename, client_addr::text, datname, state, extract(epoch FROM now() - query_start)::bigint, query FROM pg_stat_activity WHERE backend_type = 'client backend' ORDER BY pid";
	}

	public ServerProcess readProcess(ResultSet rs) throws SQLException {
		return new ServerProcess(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
			"idle".equals(rs.getString(5)));
	}

	public String killProcessSql(String processId) {
		// The pid is a number, parsing it keeps anything else out of the statement.
		return "SELECT pg_terminate_backend(" + Integer.parseInt(processId) + ")";
	}

	public UserAdmin getUserAdmin() {
		return new PostgresUserAdmin(this);
	}

	public boolean supports(Feature feature) {
		return feature == Feature.DESIGNER || feature == Feature.PROCESS_LIST || feature == Feature.SERVER_STATUS || feature == Feature.USER_MANAGER
			|| feature == Feature.CREATE_DATABASE || feature == Feature.CREATE_TABLE || feature == Feature.INDEXES || feature == Feature.IMPORT
			|| feature == Feature.EXPORT
			|| feature == Feature.FOREIGN_KEYS || feature == Feature.SCHEMAS;
	}

	/** The profile's database list is a filter, so the first entry is where we connect to. */
	public String getConnectionDatabase(ConnectionProfile profile, String requested) {
		if (requested != null && requested.trim().length() > 0) {
			return requested.trim();
		}

		String first = profile.getDatabases().split(",")[0].trim();
		return first.length() > 0 ? first : DEFAULT_DATABASE;
	}

	public String listDatabasesSql() {
		return "SELECT datname FROM pg_database WHERE datallowconn AND NOT datistemplate ORDER BY datname";
	}

	public DatabaseSwitch databaseSwitch() {
		return DatabaseSwitch.RECONNECT;
	}

	/** The system schemas (catalog, information schema, TOAST and temporary ones) hold no user tables. */
	public String listSchemasSql() {
		return """
			SELECT nspname FROM pg_namespace
			WHERE nspname NOT IN ('pg_catalog', 'information_schema') AND nspname NOT LIKE 'pg\\_toast%' AND nspname NOT LIKE 'pg\\_temp%'
			ORDER BY nspname""";
	}

	public List<DatabaseOption> createDatabaseOptions() {
		return List.of(new DatabaseOption("owner", "Owner", "SELECT rolname FROM pg_roles WHERE rolname NOT LIKE 'pg\\_%' ORDER BY 1"),
			new DatabaseOption("encoding", "Encoding",
				"SELECT DISTINCT pg_encoding_to_char(i) FROM generate_series(0, 40) i WHERE pg_encoding_to_char(i) <> '' ORDER BY 1"));
	}

	/** Another encoding than the template's needs template0, which is always empty and always there. */
	protected String databaseOptionClause(String key, String value) {
		return switch (key) {
			case "owner" -> "OWNER " + quote(value);
			case "encoding" -> "ENCODING " + literal(value) + " TEMPLATE template0";
			default -> super.databaseOptionClause(key, value);
		};
	}

	public String databasePropertiesSql() {
		return "SELECT pg_get_userbyid(datdba)::text AS \"Owner\", pg_encoding_to_char(encoding)::text AS \"Encoding\", datcollate::text AS \"Collation\" FROM pg_database WHERE datname = ?";
	}

	public String tableSizeSql() {
		return "SELECT pg_total_relation_size(format('%I.%I', coalesce(?::text, current_schema()::text), ?::text)::regclass)";
	}

	public String useSchemaSql(String schema) {
		return "SET search_path TO " + quote(schema);
	}

	public String currentSchemaSql() {
		return "SELECT current_schema()";
	}

	public String[] metadataTableTypes() {
		return new String[]{"TABLE", "VIEW", "MATERIALIZED VIEW", "PARTITIONED TABLE"};
	}

	/** Without an ORDER BY the server may return the rows in any order, and an updated row moves to the end. */
	public String selectPage(String quotedTable, String orderBy, int skip, int show) {
		return "SELECT * FROM " + quotedTable + (orderBy.length() > 0 ? " ORDER BY " + orderBy : "") + " LIMIT " + show + " OFFSET " + skip;
	}
}

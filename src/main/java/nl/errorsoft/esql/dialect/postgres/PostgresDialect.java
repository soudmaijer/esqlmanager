package nl.errorsoft.esql.dialect.postgres;

import nl.errorsoft.esql.dialect.AbstractDialect;
import nl.errorsoft.esql.dialect.MaintenanceStatement;
import nl.errorsoft.esql.dialect.UserAdmin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.connection.ServerType;

/**
 * PostgreSQL has one database per connection, so switching database means connecting again.
 * Tables are looked up in the connection's current schema (normally "public").
 */
public class PostgresDialect extends AbstractDialect {
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
		return "SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND ( is_identity = 'YES' OR column_default LIKE 'nextval%' )";
	}

	public List<String> afterDataLoadSql(String table, List<String> autoNumberedColumns) {
		List<String> statements = new ArrayList<>();

		for (String column : autoNumberedColumns) {
			statements.add("SELECT setval(pg_get_serial_sequence(" + literal(quote(table)) + ", " + literal(column) + "), (SELECT max(" + quote(column)
				+ ") FROM " + quote(table) + "))");
		}
		return statements;
	}

	public java.util.Set<Maintenance> maintenanceCommands() {
		return java.util.EnumSet.of(Maintenance.OPTIMIZE, Maintenance.ANALYZE);
	}

	public MaintenanceStatement maintenanceSql(Maintenance command, String table) {
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
		return "SELECT pid, usename, client_addr::text, datname, state, extract(epoch FROM now() - query_start)::bigint, query FROM pg_stat_activity WHERE backend_type = 'client backend' AND pid <> pg_backend_pid() ORDER BY pid";
	}

	public ServerProcess readProcess(ResultSet rs) throws SQLException {
		return new ServerProcess(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7));
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
			|| feature == Feature.FOREIGN_KEYS;
	}

	/** The profile's database list is a filter, so the first entry is where we connect to. */
	public String getConnectionDatabase(ConnectionProfile cp, String requested) {
		if (requested != null && requested.trim().length() > 0) {
			return requested.trim();
		}

		String first = cp.getDatabases().split(",")[0].trim();
		return first.length() > 0 ? first : DEFAULT_DATABASE;
	}

	public String listDatabasesSql() {
		return "SELECT datname FROM pg_database WHERE datallowconn AND NOT datistemplate ORDER BY datname";
	}

	public DatabaseSwitch databaseSwitch() {
		return DatabaseSwitch.RECONNECT;
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

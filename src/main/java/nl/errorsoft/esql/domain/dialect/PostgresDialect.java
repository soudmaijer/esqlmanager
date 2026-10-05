package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.table.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.connection.ServerProcess;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.table.Table;

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
	public List<String> afterDataLoadSql(DatabaseConnection dbc, String table) throws SQLException {
		List<String> statements = new ArrayList<>();

		try (PreparedStatement ps = dbc.getConnection().prepareStatement(
			"SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND ( is_identity = 'YES' OR column_default LIKE 'nextval%' )")) {
			ps.setString(1, table);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String column = quote(rs.getString(1));
					statements.add("SELECT setval(pg_get_serial_sequence(" + literal(quote(table)) + ", " + literal(rs.getString(1))
						+ "), (SELECT max(" + column + ") FROM " + quote(table) + "))");
				}
			}
		}
		return statements;
	}

	public String maintain(DatabaseConnection dbc, Maintenance command, String table) throws SQLException {
		return switch (command) {
			case OPTIMIZE -> {
				dbc.executeUpdate("VACUUM " + quote(table));
				yield "Vacuumed " + table;
			}
			case ANALYZE -> {
				dbc.executeUpdate("ANALYZE " + quote(table));
				yield "Analyzed " + table;
			}
			default -> super.maintain(dbc, command, table);
		};
	}

	/** PostgreSQL does not drop the database the connection is using, so move to another one first. */
	public void dropDatabase(DatabaseConnection dbc, String database) throws SQLException {
		if (database.equals(dbc.getDatabase())) {
			dbc.useDatabase(database.equals(DEFAULT_DATABASE) ? "template1" : DEFAULT_DATABASE);
		}

		super.dropDatabase(dbc, database);
	}

	public String getStatusQuery() {
		return "SELECT datname, numbackends, xact_commit, xact_rollback, blks_read, blks_hit, tup_returned, tup_fetched, tup_inserted, tup_updated, tup_deleted FROM pg_stat_database WHERE datname IS NOT NULL ORDER BY datname";
	}

	public String getVariablesQuery() {
		return "SELECT name, setting, unit, short_desc FROM pg_settings ORDER BY name";
	}

	public List<ServerProcess> listProcesses(DatabaseConnection dbc) throws SQLException {
		List<ServerProcess> processes = new ArrayList<>();
		try (ResultSet rs = dbc.executeQuery(
			"SELECT pid, usename, client_addr::text, datname, state, extract(epoch FROM now() - query_start)::bigint, query FROM pg_stat_activity WHERE backend_type = 'client backend' AND pid <> pg_backend_pid() ORDER BY pid")) {

			while (rs.next()) {
				processes.add(new ServerProcess(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
					rs.getString(7)));
			}

		}
		return processes;
	}

	public void killProcess(DatabaseConnection dbc, String processId) throws SQLException {
		try (PreparedStatement ps = dbc.getConnection().prepareStatement("SELECT pg_terminate_backend(?)")) {
			ps.setInt(1, Integer.parseInt(processId));
			ps.execute();
		}
	}

	public UserAdmin getUserAdmin() {
		return new PostgresUserAdmin(this);
	}

	public boolean supports(Feature feature) {
		return feature == Feature.DESIGNER || feature == Feature.PROCESS_LIST || feature == Feature.SERVER_STATUS || feature == Feature.USER_MANAGER
			|| feature == Feature.CREATE_TABLE || feature == Feature.INDEXES || feature == Feature.IMPORT || feature == Feature.EXPORT;
	}

	/** The profile's database list is a filter, so the first entry is where we connect to. */
	public String getConnectionDatabase(ConnectionProfile cp, String requested) {
		if (requested != null && requested.trim().length() > 0) {
			return requested.trim();
		}

		String first = cp.getDatabases().split(",")[0].trim();
		return first.length() > 0 ? first : DEFAULT_DATABASE;
	}

	public List<String> listDatabases(DatabaseConnection dbc) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet rs = dbc.executeQuery("SELECT datname FROM pg_database WHERE datallowconn AND NOT datistemplate ORDER BY datname")) {

			while (rs.next()) {
				names.add(rs.getString(1));
			}

		}
		return names;
	}

	public void useDatabase(DatabaseConnection dbc, String database) throws SQLException {
		if (database.equals(dbc.getDatabase())) {
			return;
		}

		try {
			dbc.connect(dbc.getConnectionProfile(), database);
		} catch (SQLException e) {
			throw e;
		} catch (Exception e) {
			throw new SQLException(e.getMessage(), e);
		}
	}

	public String getSchema(DatabaseConnection dbc) throws SQLException {
		try (ResultSet rs = dbc.executeQuery("SELECT current_schema()")) {
			return rs.next() ? rs.getString(1) : "public";
		}
	}

	public Vector<Table> listTables(DatabaseConnection dbc, Database db) throws SQLException {
		return listTablesFromMetaData(dbc, db, null, getSchema(dbc), new String[]{"TABLE", "VIEW", "MATERIALIZED VIEW", "PARTITIONED TABLE"});
	}

	/** Without an ORDER BY the server may return the rows in any order, and an updated row moves to the end. */
	public String selectPage(String quotedTable, String orderBy, int skip, int show) {
		return "SELECT * FROM " + quotedTable + (orderBy.length() > 0 ? " ORDER BY " + orderBy : "") + " LIMIT " + show + " OFFSET " + skip;
	}
}

package nl.errorsoft.esql.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;

/** Runs statements typed by the user. */
public class QueryRepository extends AbstractRepository {
	public QueryRepository(DatabaseConnection connection) {
		super(connection);
	}

	public int update(String sql) throws SQLException {
		return executeUpdate(sql);
	}

	/**
	 * Runs any statement and lets the driver tell whether it returned rows, so that every kind of query (WITH, EXPLAIN, VALUES, a leading comment, ...)
	 * shows its result.
	 */
	public ExecutionResult execute(String sql) throws SQLException {
		try (Statement statement = connection.getConnection().createStatement()) {
			if (statement.execute(sql)) {
				try (ResultSet rs = statement.getResultSet()) {
					return new ExecutionResult.Rows(readResult(rs, false));
				}
			}
			return new ExecutionResult.Updated(statement.getUpdateCount());
		}
	}

	/** The database the statement switches to when it is this server's way of switching, null otherwise. */
	public String databaseSwitchTarget(String sql) {
		return dialect().databaseSwitchTarget(sql);
	}

	public void switchDatabase(String name) throws SQLException {
		useDatabase(name);
	}

	public void switchSchema(String name) throws SQLException {
		useSchema(name);
	}
}

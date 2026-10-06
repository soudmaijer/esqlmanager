package nl.errorsoft.esql.query;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

	/** Runs the explain of a statement and reads its rows as text. */
	public QueryPlan explain(String statement, boolean analyze) throws SQLException {
		String sql = dialect().explainSql(statement, analyze);
		try (Statement explain = connection.getConnection().createStatement(); ResultSet rs = explain.executeQuery(sql)) {
			ResultSetMetaData meta = rs.getMetaData();
			List<String> columns = new ArrayList<>();
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				columns.add(meta.getColumnLabel(i));
			}
			List<List<String>> rows = new ArrayList<>();
			while (rs.next()) {
				List<String> row = new ArrayList<>();
				for (int i = 1; i <= columns.size(); i++) {
					row.add(rs.getString(i));
				}
				rows.add(row);
			}
			return new QueryPlan(columns, rows);
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

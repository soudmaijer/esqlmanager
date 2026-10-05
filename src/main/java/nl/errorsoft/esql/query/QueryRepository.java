package nl.errorsoft.esql.query;

import java.sql.SQLException;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;

/** Runs statements typed by the user. */
public class QueryRepository extends AbstractRepository {
	public QueryRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	public int update(String sql) throws SQLException {
		return executeUpdate(sql);
	}

	public void switchDatabase(String name) throws SQLException {
		useDatabase(name);
	}
}

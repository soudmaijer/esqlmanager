package nl.errorsoft.esql.data;

import java.sql.SQLException;
import java.util.List;

import nl.errorsoft.esql.domain.dialect.Dialect;

/**
 * Base of every repository: the one place a feature runs SQL.
 * It holds the connection and gives subclasses the {@link Dialect} of that connection,
 * so a repository never needs to know which database it talks to.
 */
public abstract class AbstractRepository {
	protected final DatabaseConnection dbc;

	protected AbstractRepository(DatabaseConnection dbc) {
		this.dbc = dbc;
	}

	protected Dialect dialect() {
		return dbc.getConnectionProfile().getServerType().getDialect();
	}

	protected String quote(String identifier) {
		return dialect().quote(identifier);
	}

	protected String literal(String value) {
		return dbc.formatFieldValue(value);
	}

	protected void useDatabase(String name) throws SQLException {
		dbc.useDatabase(name);
	}

	protected int executeUpdate(String sql) throws SQLException {
		return dbc.executeUpdate(sql);
	}

	protected void executeAll(List<String> statements) throws SQLException {
		for (String statement : statements) {
			dbc.executeUpdate(statement);
		}
	}
}

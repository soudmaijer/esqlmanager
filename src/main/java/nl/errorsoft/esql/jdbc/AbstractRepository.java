package nl.errorsoft.esql.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.table.Table;

import nl.errorsoft.esql.dialect.Dialect;

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

	/** The first column of every row of a query, with the parameters bound in order. */
	protected List<String> queryStrings(String sql, String... parameters) throws SQLException {
		List<String> values = new ArrayList<>();

		try (PreparedStatement ps = dbc.getConnection().prepareStatement(sql)) {
			for (int i = 0; i < parameters.length; i++) {
				ps.setString(i + 1, parameters[i]);
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					values.add(rs.getString(1));
				}
			}
		}
		return values;
	}

	/** The tables and views of a database, with their row counts. */
	protected List<Table> listTables(Database database) throws SQLException {
		useDatabase(database.getName());
		List<Table> tables = new ArrayList<>();
		String sql = dialect().listTablesSql();

		if (sql != null) {
			try (ResultSet rs = dbc.executeQuery(sql)) {
				while (rs.next()) {
					tables.add(dialect().readTable(rs, database));
				}
			}
			return tables;
		}

		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		try (ResultSet rs = dmd.getTables(dbc.getConnection().getCatalog(), dbc.getSchema(), "%", dialect().metadataTableTypes())) {
			while (rs.next()) {
				Table table = new Table(database);
				table.setName(rs.getString("TABLE_NAME"));
				table.setType(rs.getString("TABLE_TYPE"));
				table.setComment(rs.getString("REMARKS"));
				tables.add(table);
			}
		}

		for (Table table : tables) {
			try (ResultSet counted = dbc.executeQuery("SELECT count(*) FROM " + quote(table.getName()))) {
				if (counted.next()) {
					table.setRowCount(counted.getInt(1));
				}
			} catch (SQLException e) {
				// A table we cannot count (no rights, broken view) is still listed, without a row count.
			}
		}
		return tables;
	}
}

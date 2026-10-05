package nl.errorsoft.esql.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.dialect.DatabaseOption;
import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Runs the SQL for databases and their schemas, and lists their tables. */
public class DatabaseRepository extends AbstractRepository {
	public DatabaseRepository(DatabaseConnection connection) {
		super(connection);
	}

	public List<String> listNames() throws SQLException {
		String sql = dialect().listDatabasesSql();

		if (sql == null) {
			return connection.getConnectionProfile().getSelection().databases();
		}

		List<String> names = new ArrayList<>();
		try (ResultSet rs = connection.executeQuery(sql)) {
			while (rs.next()) {
				names.add(rs.getString(1));
			}
		}
		return names;
	}

	@Override
	public List<Table> listTables(Database database) throws SQLException {
		return super.listTables(database);
	}

	/** The schemas of the database, empty on servers without schemas. */
	public List<String> listSchemaNames(Database database) throws SQLException {
		String sql = dialect().listSchemasSql();

		if (sql == null) {
			return List.of();
		}

		useDatabase(database.getName());
		List<String> names = new ArrayList<>();
		try (ResultSet rs = connection.executeQuery(sql)) {
			while (rs.next()) {
				names.add(rs.getString(1));
			}
		}
		return names;
	}

	@Override
	public List<Table> listTables(Schema schema) throws SQLException {
		return super.listTables(schema);
	}

	/** The schema unqualified names resolve to, null on servers without schemas. */
	public String currentSchema(Database database) throws SQLException {
		useDatabase(database.getName());
		return connection.getSchema();
	}

	public void createSchema(Database database, String name) throws SQLException {
		useDatabase(database.getName());
		executeUpdate(dialect().createSchemaSql(name));
	}

	public void dropSchema(Schema schema) throws SQLException {
		useDatabase(schema.getDatabase().getName());
		executeUpdate(dialect().dropSchemaSql(schema.getName()));
	}

	/** What the server calls a database. */
	public String databaseTerm() {
		return dialect().databaseTerm();
	}

	public boolean exists(String name) throws SQLException {
		return listNames().contains(name);
	}

	public void use(String name) throws SQLException {
		useDatabase(name);
	}

	public void create(String name, Map<String, String> options) throws SQLException {
		executeUpdate(dialect().createDatabaseSql(name, options));
	}

	/** The values to choose from for every option of CREATE DATABASE, by option key; empty when the server has no options. */
	public Map<String, List<String>> createDatabaseChoices() throws SQLException {
		Map<String, List<String>> choices = new LinkedHashMap<>();

		for (DatabaseOption option : dialect().createDatabaseOptions()) {
			choices.put(option.key(), queryStrings(option.choicesSql()));
		}
		return choices;
	}

	/** The properties of a database as the server reports them, in the order of its columns; empty when the server reports none. */
	public Map<String, String> describe(Database database) throws SQLException {
		String sql = dialect().databasePropertiesSql();
		Map<String, String> details = new LinkedHashMap<>();

		if (sql == null) {
			return details;
		}

		try (PreparedStatement ps = connection.getConnection().prepareStatement(sql)) {
			ps.setString(1, database.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					ResultSetMetaData columns = rs.getMetaData();

					for (int i = 1; i <= columns.getColumnCount(); i++) {
						details.put(columns.getColumnLabel(i), rs.getString(i));
					}
				}
			}
		}
		return details;
	}

	public void renameSchema(Schema schema, String newName) throws SQLException {
		useDatabase(schema.getDatabase().getName());
		executeUpdate(dialect().renameSchemaSql(schema.getName(), newName));
	}

	/** Also drops the database the connection is using, moving to another one first when the server needs that. */
	public void drop(Database database) throws SQLException {
		String leaveFor = dialect().databaseToLeaveFor(database.getName());

		if (leaveFor != null && database.getName().equals(connection.getDatabase())) {
			useDatabase(leaveFor);
		}
		executeUpdate(dialect().dropDatabaseSql(database.getName()));
	}
}

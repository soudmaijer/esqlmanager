package nl.errorsoft.esql.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Runs the SQL for databases (schemas) and lists their tables. */
public class DatabaseRepository extends AbstractRepository {
	public DatabaseRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	public List<String> listNames() throws SQLException {
		String sql = dialect().listDatabasesSql();

		if (sql == null) {
			return List.of(dbc.getConnectionProfile().getDatabases());
		}

		List<String> names = new ArrayList<>();
		try (ResultSet rs = dbc.executeQuery(sql)) {
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

	public boolean exists(String name) throws SQLException {
		return listNames().contains(name);
	}

	public void use(String name) throws SQLException {
		useDatabase(name);
	}

	public void create(String name) throws SQLException {
		executeUpdate("CREATE DATABASE " + quote(name));
	}

	/** Also drops the database the connection is using, moving to another one first when the server needs that. */
	public void drop(Database database) throws SQLException {
		String leaveFor = dialect().databaseToLeaveFor(database.getName());

		if (leaveFor != null && database.getName().equals(dbc.getDatabase())) {
			useDatabase(leaveFor);
		}
		executeUpdate(dialect().dropDatabaseSql(database.getName()));
	}
}

package nl.errorsoft.esql.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;

/** Server wide information: running processes. */
public class ServerRepository extends AbstractRepository {
	public ServerRepository(DatabaseConnection connection) {
		super(connection);
	}

	public List<ServerProcess> listProcesses() throws SQLException {
		List<ServerProcess> processes = new ArrayList<>();

		try (ResultSet rs = connection.executeQuery(dialect().listProcessesSql())) {
			while (rs.next()) {
				processes.add(dialect().readProcess(rs));
			}
		}
		return processes;
	}

	public void killProcess(String id) throws SQLException {
		connection.execute(dialect().killProcessSql(id));
	}

	public String statusQuery() {
		return dialect().getStatusQuery();
	}

	public String variablesQuery() {
		return dialect().getVariablesQuery();
	}
}

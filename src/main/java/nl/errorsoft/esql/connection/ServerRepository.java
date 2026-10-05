package nl.errorsoft.esql.connection;

import java.sql.SQLException;
import java.util.List;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;

/** Server wide information: running processes. */
public class ServerRepository extends AbstractRepository {
	public ServerRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	public List<ServerProcess> listProcesses() throws SQLException {
		return dialect().listProcesses(dbc);
	}

	public void killProcess(String id) throws SQLException {
		dialect().killProcess(dbc, id);
	}

	public String statusQuery() {
		return dialect().getStatusQuery();
	}

	public String variablesQuery() {
		return dialect().getVariablesQuery();
	}
}

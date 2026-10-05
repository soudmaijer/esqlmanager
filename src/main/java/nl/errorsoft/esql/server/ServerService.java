package nl.errorsoft.esql.server;

import java.util.List;

import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.QueryResult;
import nl.errorsoft.esql.table.TableService;

/** Application logic for what the server is doing: processes, status and variables. */
public class ServerService {
	private final ServerRepository repository;
	private final TableService tables;

	public ServerService(ServerRepository repository, TableService tables) {
		this.repository = repository;
		this.tables = tables;
	}

	public List<ServerProcess> getProcesses() throws Exception {
		return repository.listProcesses();
	}

	public void killProcess(String id) throws Exception {
		repository.killProcess(id);
	}

	public QueryResult getStatus() throws Exception {
		return tables.runCommand(repository.statusQuery());
	}

	public QueryResult getVariables() throws Exception {
		return tables.runCommand(repository.variablesQuery());
	}
}

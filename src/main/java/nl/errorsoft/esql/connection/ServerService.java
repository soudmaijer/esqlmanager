package nl.errorsoft.esql.connection;

import java.util.List;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.table.QueryResult;
import nl.errorsoft.esql.table.TableService;

/** Application logic for what the server is doing: processes, status and variables. */
public class ServerService
{
	private final ServerRepository repository;
	private final TableService tables;

	public ServerService( DatabaseConnection dbc )
	{
		this.repository = new ServerRepository( dbc );
		this.tables = new TableService( dbc );
	}

	public List<ServerProcess> getProcesses() throws Exception
	{
		return repository.listProcesses();
	}

	public void killProcess( String id ) throws Exception
	{
		repository.killProcess( id );
	}

	public QueryResult getStatus() throws Exception
	{
		return tables.runCommand( repository.statusQuery() );
	}

	public QueryResult getVariables() throws Exception
	{
		return tables.runCommand( repository.variablesQuery() );
	}
}

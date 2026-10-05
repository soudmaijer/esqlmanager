package nl.errorsoft.esql.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Runs the SQL for databases (schemas) and lists their tables. */
public class DatabaseRepository extends AbstractRepository
{
	public DatabaseRepository( DatabaseConnection dbc )
	{
		super( dbc );
	}

	public List<String> listNames() throws SQLException
	{
		return dialect().listDatabases( dbc );
	}

	public List<Table> listTables( Database database ) throws SQLException
	{
		useDatabase( database.getName() );
		return dialect().listTables( dbc, database );
	}

	public void create( String name ) throws SQLException
	{
		executeUpdate( "CREATE DATABASE "+ quote( name ) );
	}

	public void drop( Database database ) throws SQLException
	{
		dialect().dropDatabase( dbc, database.getName() );
	}
}

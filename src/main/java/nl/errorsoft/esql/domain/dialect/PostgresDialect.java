package nl.errorsoft.esql.domain.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.ConnectionProfile;
import nl.errorsoft.esql.domain.Database;
import nl.errorsoft.esql.domain.ServerType;
import nl.errorsoft.esql.domain.Table;

/**
 * PostgreSQL has one database per connection, so switching database means connecting again.
 * Tables are looked up in the connection's current schema (normally "public").
 */
public class PostgresDialect extends AbstractDialect
{
	private static final String DEFAULT_DATABASE = "postgres";

	public int getType()
	{
		return ServerType.POSTGRES;
	}

	public String getDefaultPort()
	{
		return "5432";
	}

	public String getDefaultUsername()
	{
		return "postgres";
	}

	/** The profile's database list is a filter, so the first entry is where we connect to. */
	public String getConnectionDatabase( ConnectionProfile cp, String requested )
	{
		if( requested != null && requested.trim().length() > 0 )
			return requested.trim();

		String first = cp.getDatabases().split(",")[0].trim();
		return first.length() > 0 ? first : DEFAULT_DATABASE;
	}

	public List<String> listDatabases( DatabaseConnection dbc ) throws SQLException
	{
		List<String> names = new ArrayList<String>();
		ResultSet rs = dbc.executeQuery( "SELECT datname FROM pg_database WHERE datallowconn AND NOT datistemplate ORDER BY datname" );

		while( rs.next() )
			names.add( rs.getString(1) );

		rs.close();
		return names;
	}

	public void useDatabase( DatabaseConnection dbc, String database ) throws SQLException
	{
		if( database.equals( dbc.getDatabase() ) )
			return;

		try
		{
			dbc.connect( dbc.getConnectionProfile(), database );
		}
		catch( SQLException e )
		{
			throw e;
		}
		catch( Exception e )
		{
			throw new SQLException( e.getMessage(), e );
		}
	}

	public String getSchema( DatabaseConnection dbc ) throws SQLException
	{
		ResultSet rs = dbc.executeQuery( "SELECT current_schema()" );
		String schema = rs.next() ? rs.getString(1) : "public";
		rs.close();
		return schema;
	}

	public Vector<Table> listTables( DatabaseConnection dbc, Database db ) throws SQLException
	{
		return listTablesFromMetaData( dbc, db, null, getSchema( dbc ), new String[] { "TABLE", "VIEW", "MATERIALIZED VIEW", "PARTITIONED TABLE" } );
	}

	public String selectPage( String quotedTable, int skip, int show )
	{
		return "SELECT * FROM "+ quotedTable +" LIMIT "+ show +" OFFSET "+ skip;
	}
}

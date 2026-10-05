package nl.errorsoft.esql.domain.dialect;

import java.sql.PreparedStatement;
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

	/** Rows loaded with explicit ids leave the sequence behind, so the next insert would reuse an id. */
	public List<String> afterDataLoadSql( DatabaseConnection dbc, String table ) throws SQLException
	{
		List<String> statements = new ArrayList<String>();

		try( PreparedStatement ps = dbc.getConnection().prepareStatement( "SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ? AND ( is_identity = 'YES' OR column_default LIKE 'nextval%' )" ) )
		{
			ps.setString( 1, table );

			try( ResultSet rs = ps.executeQuery() )
			{
				while( rs.next() )
				{
					String column = quote( rs.getString( 1 ) );
					statements.add( "SELECT setval(pg_get_serial_sequence(" + literal( quote( table ) ) + ", " + literal( rs.getString( 1 ) ) + "), (SELECT max(" + column + ") FROM " + quote( table ) + "))" );
				}
			}
		}
		return statements;
	}

	public String maintain( DatabaseConnection dbc, Maintenance command, String table ) throws SQLException
	{
		switch( command )
		{
			case OPTIMIZE:
				dbc.executeUpdate( "VACUUM " + quote( table ) );
				return "Vacuumed " + table;
			case ANALYZE:
				dbc.executeUpdate( "ANALYZE " + quote( table ) );
				return "Analyzed " + table;
			default:
				return super.maintain( dbc, command, table );
		}
	}

	public UserAdmin getUserAdmin()
	{
		return new PostgresUserAdmin( this );
	}

	public boolean supports( Feature feature )
	{
		return feature == Feature.DESIGNER || feature == Feature.USER_MANAGER || feature == Feature.CREATE_TABLE || feature == Feature.INDEXES || feature == Feature.IMPORT || feature == Feature.EXPORT;
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

	/** Without an ORDER BY the server may return the rows in any order, and an updated row moves to the end. */
	public String selectPage( String quotedTable, String orderBy, int skip, int show )
	{
		return "SELECT * FROM "+ quotedTable + ( orderBy.length() > 0 ? " ORDER BY "+ orderBy : "" ) +" LIMIT "+ show +" OFFSET "+ skip;
	}
}

package nl.errorsoft.esql.domain.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.ConnectionProfile;
import nl.errorsoft.esql.domain.Database;
import nl.errorsoft.esql.domain.ServerType;
import nl.errorsoft.esql.domain.Table;

/**
 * Plain JDBC behaviour that works on any database. Dialects override what is different.
 */
public abstract class AbstractDialect implements Dialect
{
	public boolean supports( Feature feature )
	{
		return false;
	}

	public String getConnectionDatabase( ConnectionProfile cp, String requested )
	{
		return requested;
	}

	public void useDatabase( DatabaseConnection dbc, String database ) throws SQLException
	{
		dbc.getConnection().setCatalog( database );
	}

	public String getSchema( DatabaseConnection dbc ) throws SQLException
	{
		return null;
	}

	public Vector<Table> listTables( DatabaseConnection dbc, Database db ) throws SQLException
	{
		return listTablesFromMetaData( dbc, db, db.getName(), getSchema( dbc ), new String[] { "TABLE", "VIEW" } );
	}

	public String selectPage( String quotedTable, int skip, int show )
	{
		return null;
	}

	/** Lists tables through DatabaseMetaData and counts the rows of each of them. */
	protected Vector<Table> listTablesFromMetaData( DatabaseConnection dbc, Database db, String catalog, String schema, String [] types ) throws SQLException
	{
		Vector<Table> tables = new Vector<Table>();
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		ResultSet rs = dmd.getTables( catalog, schema, "%", types );

		while( rs.next() )
		{
			Table table = new Table( dbc, db );
			table.setName( rs.getString("TABLE_NAME") );
			table.setType( rs.getString("TABLE_TYPE") );
			table.setComment( rs.getString("REMARKS") );
			tables.add( table );
		}
		rs.close();

		ServerType serverType = dbc.getConnectionProfile().getServerType();

		for( Table table : tables )
		{
			try
			{
				rs = dbc.executeQuery( "SELECT count(*) AS cnt FROM "+ serverType.getFieldOpenChar() + table.getName() + serverType.getFieldCloseChar() );

				if( rs.first() )
					table.setRowCount( rs.getInt("cnt") );

				rs.close();
			}
			catch( SQLException e )
			{
				// A table we cannot count (no rights, broken view) is still listed, without a row count.
			}
		}
		return tables;
	}
}

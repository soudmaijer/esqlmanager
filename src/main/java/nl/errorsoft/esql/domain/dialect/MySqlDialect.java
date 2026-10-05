package nl.errorsoft.esql.domain.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.Database;
import nl.errorsoft.esql.domain.ServerType;
import nl.errorsoft.esql.domain.Table;

public class MySqlDialect extends AbstractDialect
{
	public int getType()
	{
		return ServerType.MY_SQL;
	}

	public String getDefaultPort()
	{
		return "3306";
	}

	public String getDefaultUsername()
	{
		return "root";
	}

	/** The MySQL specific tools were written for MySQL, so it is the only server that supports all of them. */
	public boolean supports( Feature feature )
	{
		return true;
	}

	public List<String> listDatabases( DatabaseConnection dbc ) throws SQLException
	{
		List<String> names = new ArrayList<String>();
		ResultSet rs = dbc.executeQuery( "SHOW DATABASES" );

		while( rs.next() )
			names.add( rs.getString(1) );

		rs.close();
		return names;
	}

	public Vector<Table> listTables( DatabaseConnection dbc, Database db ) throws SQLException
	{
		Vector<Table> tables = new Vector<Table>();
		ResultSet rs = dbc.executeQuery( "SHOW TABLE STATUS" );

		while( rs.next() )
		{
			Table table = new Table( dbc, db );
			table.setName( rs.getString("Name") );
			table.setType( rs.getString("Engine") );
			table.setRowCount( rs.getInt("Rows") );
			table.setComment( rs.getString("Comment") );
			tables.add( table );
		}
		rs.close();
		return tables;
	}

	public String selectPage( String quotedTable, int skip, int show )
	{
		return "SELECT * FROM "+ quotedTable +" LIMIT "+ skip +","+ show;
	}
}

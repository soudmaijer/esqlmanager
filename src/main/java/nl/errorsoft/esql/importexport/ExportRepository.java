package nl.errorsoft.esql.importexport;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/** Reads what an export script is made of: table names, table definitions and rows as statements. */
public class ExportRepository extends AbstractRepository
{
	public ExportRepository( DatabaseConnection dbc )
	{
		super( dbc );
	}

	/** The tables of a database that have structure and data of their own, so no views. */
	public List<String> tableNames( Database database ) throws SQLException
	{
		useDatabase( database.getName() );
		List<String> names = new ArrayList<String>();

		for ( Table table : dialect().listTables( dbc, database ) )
			if ( !"VIEW".equalsIgnoreCase( table.getType() ) )
				names.add( table.getName() );

		return names;
	}

	public String createDatabaseSql( String database )
	{
		return dialect().createDatabaseSql( database );
	}

	public String useDatabaseSql( String database )
	{
		return dialect().useDatabaseSql( database );
	}

	public String dropTableSql( String table )
	{
		return "DROP TABLE IF EXISTS " + quote( table );
	}

	public String structureSql( String table ) throws SQLException
	{
		return dialect().createTableDdl( dbc, table );
	}

	/** Passes every row of the table to the sink as an INSERT statement, binary columns are left empty. */
	public void insertStatements( String table, Consumer<String> sink ) throws SQLException
	{
		try ( ResultSet rs = dbc.executeQuery( "SELECT * FROM " + quote( table ) ) )
		{
			ResultSetMetaData rsm = rs.getMetaData();

			while ( rs.next() )
			{
				List<String> values = new ArrayList<String>();

				for ( int d = 1; d <= rsm.getColumnCount(); d++ )
				{
					TableColumn column = new TableColumn( null );
					column.setType( rsm.getColumnType( d ) );

					if ( column.isBinary() )
						values.add( "''" );
					else if ( rs.getString( d ) != null )
						values.add( dialect().literal( rs.getString( d ) ) );
					else
						values.add( "NULL" );
				}

				sink.accept( "INSERT INTO " + quote( table ) + " VALUES(" + String.join( ",", values ) + ");" );
			}
		}
	}

	/** Statements that must follow the data, such as moving a sequence past the imported rows. */
	public List<String> afterDataStatements( String table ) throws SQLException
	{
		return dialect().afterDataLoadSql( dbc, table );
	}
}

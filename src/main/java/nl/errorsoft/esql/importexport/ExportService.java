package nl.errorsoft.esql.importexport;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.table.Table;

/** Writes databases and tables to an SQL script on its own thread and reports progress (0 to 100) or an Exception to its observers. */
public class ExportService extends Observable implements Runnable
{
	private final ExportRepository repository;
	private final Object[] exportObject;
	private final String file;
	private final boolean dumpStructure;
	private final boolean dumpData;
	private final boolean createDatabase;
	private final boolean dropTable;
	private final boolean useDatabase;

	public ExportService( DatabaseConnection dbc, Object[] exportObject, String file, boolean dumpStructure, boolean dumpData, boolean createDatabase,
		boolean dropTable, boolean useDatabase )
	{
		this.repository = new ExportRepository( dbc );
		this.exportObject = exportObject;
		this.file = file;
		this.dumpStructure = dumpStructure;
		this.dumpData = dumpData;
		this.createDatabase = createDatabase;
		this.dropTable = dropTable;
		this.useDatabase = useDatabase;
	}

	public void run()
	{
		try ( PrintWriter pw = new PrintWriter( file, StandardCharsets.UTF_8 ) )
		{
			progress( 10 );

			for ( int i = 0; i < exportObject.length; i++ )
			{
				String database;
				List<String> tables = new ArrayList<String>();

				if ( exportObject[i] instanceof Database )
				{
					Database source = ( Database ) exportObject[i];
					database = source.getName();
					tables = repository.tableNames( source );
				}
				else if ( exportObject[i] instanceof Table )
				{
					Table source = ( Table ) exportObject[i];
					database = source.getDatabase().getName();
					tables.add( source.getName() );
				}
				else
					continue;

				if ( createDatabase )
					pw.println( repository.createDatabaseSql( database ) + ";\n" );

				if ( useDatabase )
					pw.println( repository.useDatabaseSql( database ) + ";\n" );

				for ( String table : tables )
					dumpTable( pw, database, table );

				progress( ( ( 100 / exportObject.length ) * ( i + 1 ) ) - 1 );
			}
			progress( 100 );
		}
		catch ( Exception e )
		{
			setChanged();
			notifyObservers( e );
		}
	}

	private void dumpTable( PrintWriter pw, String database, String table ) throws Exception
	{
		if ( dropTable )
			pw.println( repository.dropTableSql( table ) + ";\n" );

		if ( dumpStructure )
			pw.println( repository.structureSql( table ) + ";\n" );

		if ( dumpData )
		{
			repository.insertStatements( table, pw::println );

			for ( String statement : repository.afterDataStatements( table ) )
				pw.println( statement + ";\n" );
		}
	}

	private void progress( int percent )
	{
		setChanged();
		notifyObservers( Integer.valueOf( percent ) );
	}

	public void start()
	{
		new Thread( this ).start();
	}
}

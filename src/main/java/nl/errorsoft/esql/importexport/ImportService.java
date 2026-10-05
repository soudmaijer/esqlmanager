package nl.errorsoft.esql.importexport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Observable;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;

/** Runs an SQL script on its own thread and reports progress (0 to 100) or an Exception to its observers. */
public class ImportService extends Observable implements Runnable
{
	private static final String CONNECT = "\\connect ";

	private final ImportRepository repository;
	private final Object importToDatabase;
	private final String file;

	public ImportService( DatabaseConnection dbc, Object importToDatabase, String file )
	{
		this.repository = new ImportRepository( dbc );
		this.importToDatabase = importToDatabase;
		this.file = file;
	}

	public void run()
	{
		try
		{
			progress( 10 );

			if( importToDatabase instanceof Database )
				repository.switchDatabase( ((Database)importToDatabase).getName() );

			runScript( Path.of( file ) );
			progress( 100 );
		}
		catch( Exception e )
		{
			setChanged();
			notifyObservers( e );
		}
	}

	private void runScript( Path script ) throws Exception
	{
		try( BufferedReader reader = Files.newBufferedReader( script, StandardCharsets.UTF_8 ) )
		{
			StringBuilder statement = new StringBuilder();
			String line;

			while( ( line = reader.readLine() ) != null )
			{
				// A script switches database with the psql meta command, it is not SQL a server understands.
				if( statement.length() == 0 && line.startsWith( CONNECT ) )
				{
					repository.switchDatabase( line.substring( CONNECT.length() ).replaceAll( "^[\"`]|[\"`];?$", "" ) );
					continue;
				}

				statement.append( line ).append( "\n" );

				if( line.endsWith( ";" ) )
				{
					repository.run( statement.toString() );
					statement.setLength( 0 );
				}
			}
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

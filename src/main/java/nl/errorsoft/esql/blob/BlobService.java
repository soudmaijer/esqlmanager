package nl.errorsoft.esql.blob;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Observable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableService;

/** Uploads a file into a binary cell and saves a binary cell to a file, reports progress (0 to 100) to its observers. */
public class BlobService extends Observable
{
	private static final Logger log = LogManager.getLogger( BlobService.class );

	private final BlobRepository repository;
	private final TableService tables;

	public BlobService( DatabaseConnection dbc )
	{
		this.repository = new BlobRepository( dbc );
		this.tables = new TableService( dbc );
	}

	public void upload( Table table, TableData [] row, TableData cell, String file ) throws Exception
	{
		String condition = tables.rowFilter( row );
		byte [] content = Files.readAllBytes( Path.of( file ) );
		log.debug( "Read {} bytes from {}", content.length, file );

		repository.write( table, cell.getTableColumn().getName(), condition, new ByteArrayInputStream( content ), content.length );
		progress( 100 );
	}

	public void download( Table table, TableData [] row, TableData cell, String file ) throws Exception
	{
		String condition = tables.rowFilter( row );

		try( BufferedOutputStream target = new BufferedOutputStream( new FileOutputStream( file ) ) )
		{
			if( !repository.read( table, cell.getTableColumn().getName(), condition, target ) )
				log.warn( "No row found to save to {}", file );
		}
		catch( Exception e )
		{
			log.error( "Can't save {}: {}", file, e.getMessage(), e );
		}

		progress( 100 );
	}

	private void progress( int percent )
	{
		setChanged();
		notifyObservers( Integer.valueOf( percent ) );
	}
}

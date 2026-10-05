package nl.errorsoft.esql.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.gui.*;
import java.util.*;

public class UDDataCC implements Observer
{	
	private static final Logger log = LogManager.getLogger( UDDataCC.class );

	private ConnectionWindowCC cwcc;
	private UDDataIF udif;
	private Table table;
	private TableData[] row;
	private TableData cell;

	public UDDataCC( ConnectionWindowCC cwcc )
	{	
		this.cwcc = cwcc;
	}

	public void startDownloadUI( ESQLManagerUI parent, Table table, TableData[] row, TableData cell )
	{
		this.table = table;
		this.row = row;
		this.cell = cell;
		udif = new DownloadFileUI( this, parent );
	}

	public void startUploadUI( ESQLManagerUI parent, Table table, TableData[] row, TableData cell )
	{
		this.table = table;
		this.row = row;
		this.cell = cell;
		udif = new UploadFileUI( this, parent );
	}
	
	public void downloadFile( String fileLocation )
	{
		try
		{
			UDData udd = new UDData( cwcc.getDatabaseConnection() );
			udd.addObserver( this );
			udd.downloadData( table, row, cell, fileLocation );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}		
	}

	public void uploadFile( String fileLocation )
	{
		try
		{
			UDData udd = new UDData( cwcc.getDatabaseConnection() );
			udd.addObserver( this );
			udd.uploadData(  table, row, cell, fileLocation  );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}
	
   public void update(Observable o, Object arg) 
 	{ 
   	udif.setProgressValue( ((Integer)arg).intValue() );
   }		
}
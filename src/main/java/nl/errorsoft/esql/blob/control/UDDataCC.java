package nl.errorsoft.esql.blob.control;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.blob.ui.DownloadFileUI;
import nl.errorsoft.esql.blob.ui.UDDataIF;
import nl.errorsoft.esql.blob.ui.UploadFileUI;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import nl.errorsoft.esql.table.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.blob.BlobService;
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
			BlobService udd = new BlobService( cwcc.getDatabaseConnection() );
			udd.addObserver( this );
			udd.download( table, row, cell, fileLocation );
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
			BlobService udd = new BlobService( cwcc.getDatabaseConnection() );
			udd.addObserver( this );
			udd.upload(  table, row, cell, fileLocation  );
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
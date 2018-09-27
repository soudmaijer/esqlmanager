package esql.control;

import esql.domain.*;
import esql.gui.*;
import java.util.*;

public class UDDataCC implements Observer
{	
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
	
   public void update(Observable o, Object arg) 
 	{ 
   	udif.setProgressValue( ((Integer)arg).intValue() );
   }		
}
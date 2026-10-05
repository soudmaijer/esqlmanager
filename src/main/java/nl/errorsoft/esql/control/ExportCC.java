package nl.errorsoft.esql.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.gui.*;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import javax.swing.tree.*;
import java.util.Observer;
import java.util.Observable;

public class ExportCC implements Observer
{
	private static final Logger log = LogManager.getLogger( ExportCC.class );

  	private ESQLManagerCC ecc;
  	private ConnectionWindowCC cwcc;
  	private ImportExportProgressUI ies;

   public ExportCC( ESQLManagerCC ecc ) 
   {
		this.ecc = ecc;
   }

	/*
	 * @description: starts the export selection ui
	 */
   public void startExportSelectionUI( ConnectionWindowCC cwcc )
   {
		try
		{	if( !cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getDialect().supports( Dialect.Feature.EXPORT ) )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }

   	this.cwcc = cwcc;
   	new ExportSelectionUI( this, ecc.getUI() ).setVisible( true );
   }

	/*
	 * @description: starts the export ui for the option: Export data as SQL statements
	 */
   public void startExportSQLUI()
   {
		try
		{
			DatabaseCC dbcc = new DatabaseCC( cwcc );
	   	ExportAsSQLUI iasu = new ExportAsSQLUI( ecc.getUI(), this );
	   	iasu.showDatabaseTreeView( dbcc.getDatabaseTreeView( ) );
	   }
	   catch( Exception e )
	   {
	   	log.error( e.getMessage(), e );
	   }
   }
   
   public void showTables( ExportAsSQLUI iasu, Database db )
   {
		try
		{
   		DatabaseCC dbcc = new DatabaseCC( cwcc );
   		iasu.getDatabaseTreeView().loadTables( db, dbcc.getTables( db ) );
	   }
	   catch( Exception e )
	   {
	   	log.error( e.getMessage(), e );
	   }   	
   }   

	public void exportNodesAsSQL( ExportAsSQLUI iasu, TreePath[] tpa, String file, boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase )
	{
		// open progress window...
		ies = new ImportExportProgressUI( iasu );
		
		// start export...
		try
		{
			Object [] export = new Object[tpa.length];
			
			for( int i=0; i<tpa.length; i++ )
			{
				export[i] = ((DefaultMutableTreeNode)tpa[i].getLastPathComponent()).getUserObject();
			}
			
			Export exp = new Export( cwcc.getDatabaseConnection(), export, file, dumpStructure, dumpData, createDatabase, dropTable, useDatabase );
			exp.addObserver( this );
			exp.start();
		}
		catch( Exception e )
		{
			iasu.showErrorMessage( "An error occured while importing the data! "+ e.getMessage() );
			log.error( e.getMessage(), e );
		}
	}

   public void update(Observable o, Object arg) 
 	{ 
   	if( arg instanceof Integer )
   		ies.setProgressValue( ((Integer)arg).intValue() );
   	else if( arg instanceof Exception )
   	{	log.error( ((Exception)arg).getMessage(), (Exception)arg );
   		ies.showErrorMessage( ((Exception)arg).getMessage() );
   		ies.dispose();
   	}
   }		
	
	public ImageLoader getImageLoader()
	{
		return cwcc.getImageLoader();
	}	 

	/*
	 * @description: starts the export ui for the option: Export data as CSV comma-seperated
	 */
   public void startExportCSVUI()
   {
   }
}
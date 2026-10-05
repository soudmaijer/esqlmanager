package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.gui.*;
import nl.errorsoft.esqlmanager.data.*;
import nl.errorsoft.esqlmanager.domain.*;
import javax.swing.tree.*;
import java.util.Observer;
import java.util.Observable;

public class ExportCC implements Observer
{
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
		{	if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
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
	   	e.printStackTrace();
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
	   	e.printStackTrace();
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
			e.printStackTrace();
		}
	}

   public void update(Observable o, Object arg) 
 	{ 
   	if( arg instanceof Integer )
   		ies.setProgressValue( ((Integer)arg).intValue() );
   	else if( arg instanceof Exception )
   	{	System.out.println( "\n"+ ((Exception)arg).getMessage() );
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
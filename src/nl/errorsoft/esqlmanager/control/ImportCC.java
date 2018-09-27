package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.gui.*;
import nl.errorsoft.esqlmanager.data.*;
import nl.errorsoft.esqlmanager.domain.*;
import javax.swing.tree.*;
import java.util.Observer;
import java.util.Observable;

public class ImportCC implements Observer
{
  	private ESQLManagerCC ecc;
  	private ConnectionWindowCC cwcc;
	private ImportExportProgressUI ies;
	private ImportAsSQLUI iasu;

   public ImportCC( ESQLManagerCC ecc ) 
   {
		this.ecc = ecc;
   }

	/*
	 * @description: starts the Import selection ui
	 */
   public void startImportSelectionUI( ConnectionWindowCC cwcc )
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
   	new ImportSelectionUI( this, ecc.getUI() ).setVisible( true );
   }

	/*
	 * @description: starts the Import ui for the option: Import data as SQL statements
	 */
   public void startImportSQLUI()
   {
		try
		{
			DatabaseCC dbcc = new DatabaseCC( cwcc );
	   	iasu = new ImportAsSQLUI( ecc.getUI(), this );
	   	iasu.showDatabaseTreeView( dbcc.getDatabaseTreeView() );
	   }
	   catch( Exception e )
	   {
	   	e.printStackTrace();
	   }
   }
   
   public void showTables( ImportAsSQLUI iasu, Database db )
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

	public void importNodesAsSQL( ImportAsSQLUI iasu, TreePath tpa, String file )
	{
		// check if file exist...
		if( !(new java.io.File( file ).exists()) )
		{
			iasu.showErrorMessage("File doesn`t exist!");
			return;
		}
		// open progress window...
		ies = new ImportExportProgressUI( iasu );
		
		// start export...
		try
		{
			Object node = null;
			
			if( tpa != null )
				node = ((DefaultMutableTreeNode)tpa.getLastPathComponent()).getUserObject();

			Import ie = new Import( cwcc.getDatabaseConnection(), node, file );
			ie.addObserver( this );
			ie.start();
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
   	{
   		System.out.println( "\n"+ ((Exception)arg).getMessage() );
   		ies.showErrorMessage( ((Exception)arg).getMessage() );
   		ies.dispose();
   	}
   }		
	
	public ImageLoader getImageLoader()
	{
		return cwcc.getImageLoader();
	}	 

	/*
	 * @description: starts the Import ui for the option: Import data as CSV comma-seperated
	 */
   public void startImportCSVUI()
   {
   }

}
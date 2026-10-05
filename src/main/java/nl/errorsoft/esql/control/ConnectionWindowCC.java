package nl.errorsoft.esql.control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.gui.*;
import java.util.*;

public class ConnectionWindowCC extends Thread
{
	private String statusDetail = "";
	private static final Logger log = LogManager.getLogger( ConnectionWindowCC.class );

	private ESQLManagerCC jmcc;
	private ConnectionWindow cw;
	private ConnectionWindowUI cwui;
	private TableCC tbcc;

	public ConnectionWindowCC( ESQLManagerCC jmcc, nl.errorsoft.esql.domain.ConnectionProfile cp )
	{
		this.jmcc = jmcc;
		this.cw = new ConnectionWindow( this, cp );
		this.start();
	}
	
	public void run()
	{
		// Create Frame.
		log.info( "Connecting to `"+ cw.getConnectionProfile().getServerType().getDescription() +"` @ `"+ cw.getConnectionProfile().getHost() +"` with username `"+ cw.getConnectionProfile().getUsername() +"` on port `"+ cw.getConnectionProfile().getPort() +"`" );
		cwui = new ConnectionWindowUI( this, jmcc.getUI() );
		
		// Create database connection to Server.
		jmcc.showConnectionWindow( cwui );
		jmcc.updateStatus( "Connecting...", true );
		
		try
		{
			cw.start();
			jmcc.updateStatus( "Loading databases...", true );
			showDatabaseTree();
			setStatusDetail( "" );
			jmcc.updateStatus( "Ready...", false );		
		}
		catch( Exception e )
		{
			cwui.showErrorMessage( "Can`t connect: "+ e.getMessage() );
			cwui.closeUI(false);
			jmcc.updateStatus( "Can`t connect to server...", false );
		}
	}
	
	/** Shows the server and account of this connection, followed by what happened last. */
	public void showStatusInfo()
	{
		String info = "";
		
		try
		{
			info = getDatabaseConnection().getServerDescription() +"  |  "+ cw.getConnectionProfile().getUsername() +"@"+ cw.getConnectionProfile().getHost() +":"+ cw.getConnectionProfile().getPort();
		}
		catch( Exception e )
		{
			// Not connected (yet), there is nothing to show.
		}
		
		if( info.length() > 0 && statusDetail.length() > 0 )
			info += "  |  "+ statusDetail;
		
		jmcc.setStatusInfo( info );
	}
	
	private void setStatusDetail( String detail )
	{
		statusDetail = detail;
		showStatusInfo();
	}
	
	private long millisSince( long startNanos )
	{
		return ( System.nanoTime() - startNanos ) / 1000000;
	}
	
	public void closeUI()
	{
		// Stop database connection
		try
		{
			cw.stop();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
		
		// Remove references
		jmcc.removeConnectionWindow( cwui );

		// Close Internalframe
		cwui.dispose();
	}
		
	public void showDatabaseTree()
	{
		try
		{
			// Load databases into JTree.
			jmcc.updateStatus( "Loading databases...", true );
			DatabaseCC dbcc = new DatabaseCC( this );
			cwui.showDatabaseTreeView( dbcc.getDatabaseTreeView() );
			cwui.showHelp();
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}
	
	public void createDatabase( String name )
	{
		try
		{
			jmcc.updateStatus( "Creating database...", true );
			DatabaseCC dbcc = new DatabaseCC( this );
			Database db = dbcc.createDatabase( name );
			cwui.getDatabaseTreeView().addDatabase( db );
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}

	public void dropDatabase()
	{
		try
		{
			jmcc.updateStatus( "Deleting database...", true );
			Database db = cwui.getDatabase();
			DatabaseCC dbcc = new DatabaseCC( this );
			dbcc.dropDatabase( db );
			cwui.getDatabaseTreeView().deleteDatabase( db );
			cwui.removeDataTab();
			jmcc.updateStatus( "Ready...", false );
			this.showDatabaseTree();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}
	
	public void dropTable()
	{
		try
		{
			jmcc.updateStatus( "Deleting table...", true );
			Table tb = cwui.getTable();
			TableCC dbcc = new TableCC( this );
			dbcc.dropTable( tb );
			cwui.getDatabaseTreeView().deleteTable( tb );
			jmcc.updateStatus( "Ready...", false );
			this.databaseSelected( tb.getDatabase() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}	

	public void addTableColumn( FieldProperties fp, String name, String length, String dfault, DataType dt, boolean primary, boolean unique, boolean indexed, boolean auto, boolean signed, boolean nullable )
	{
		try
		{
			jmcc.updateStatus( "Adding tablecolumn...", true );
			TableCC dbcc = new TableCC( this );
			dbcc.addTableColumn( cwui.getTable(), name, length, dfault, dt, primary, auto, signed, nullable );
			reloadSelectedTable();
			fp.dispose();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}

	public void editTableColumn( FieldProperties fp, TableColumn tbc, String name, String length, String dfault, DataType dt, boolean primary, boolean unique, boolean indexed, boolean auto, boolean signed, boolean nullable )
	{
		try
		{
			jmcc.updateStatus( "Updating tablecolumn...", true );
			TableCC dbcc = new TableCC( this );
			dbcc.editTableColumn( tbc, name, length, dfault, dt, primary, auto, signed, nullable );
			reloadSelectedTable();
			fp.dispose();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}	

	public void dropTableColumn()
	{
		try
		{
			jmcc.updateStatus( "Deleting tablecolumn...", true );
			TableColumn tb = cwui.getTableColumn();
			TableCC dbcc = new TableCC( this );
			dbcc.dropTableColumn( tb );
			cwui.getDatabaseTreeView().deleteTableColumn( tb );
			jmcc.updateStatus( "Ready...", false );
			reloadSelectedTable();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}		

	public void reloadSelectedTable()
	{
		this.tableSelected( cwui.getTable(), true );
	}

	public void reloadSelectedDatabase()
	{
		this.databaseSelected( cwui.getDatabase() );
	}
	
	/*
	 * @description: deletes all data from the selected table.
	 */
	public void flushSelectedTable()
	{
		try
		{
			jmcc.updateStatus( "Flushing table data...", true );
			TableCC tbcc = new TableCC( this );
			tbcc.flushTable(  cwui.getTable() );
			jmcc.updateStatus( "Ready...", false );
			reloadSelectedTable();
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}	
	}

	/*
	 * @description: updates the tree view with all tables in the selected database.
	 */	
	public void databaseSelected( Database database )
	{
		try
		{
			// Load tables.
			jmcc.updateStatus( "Loading tables...", true );
			
			// Show tables in tab.
			DatabaseCC dbcc = new DatabaseCC( this );
			Vector tables = dbcc.getTables( database );
			cwui.showTableListView( database.getName(), dbcc.getTableListView( tables ) );
			
			// Show tables in tree.
			cwui.getDatabaseTreeView().loadTables( database, tables );
			cwui.databaseSelected();
			setStatusDetail( database.getName() +": "+ tables.size() +" table(s)" );
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}			
	}
	
	/*
	 * @description: updates the tree view with all columns in the selected table.
	 */
	public void tableSelected( Table table, boolean addTreeColumns )
	{
		try
		{
			// 1th Show table data.
			showTableData( table );
			
			// 2nd Load tables columns in tree if not already loaded.
			if( addTreeColumns )
			{
				jmcc.updateStatus( "Fetching table data...", true );
				TableCC tbcc = new TableCC( this );
				TableColumn[] fields = table.getColumns();
				cwui.getDatabaseTreeView().loadTableColumns( table, fields );
			}

			// 3th enable buttons.
			cwui.tableSelected();
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}			
	}
	
	public void fieldSelected()
	{
		cwui.fieldSelected();
	}

	public void rootSelected()
	{
		cwui.rootSelected();
	}	
	
	public void insertNewRow()
	{
		tbcc.insertNewRow();	
	}

	public void deleteSelectedRows()
	{
		tbcc.deleteSelectedRows();	
	}	
	
	public void saveSelectedRow()
	{
		tbcc.saveSelectedRow();	
	}
	
	public void startQueryUI()
	{
		try
		{
			jmcc.updateStatus( "Starting query window...", true );
			DatabaseCC dbcc = new DatabaseCC( this );
			QueryUI qu = new QueryUI( this, jmcc.getUI(), new Syntax(), jmcc.getImageLoader(), dbcc.getDatabases(), cwui.getDatabase() );
			jmcc.updateStatus( "Ready...", false );
			qu.setVisible( true );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
		}
	}

	public void startFieldUI( boolean add, boolean edit )
	{
		try
		{
			jmcc.updateStatus( "Starting field properties interface...", true );
			FieldProperties fpu = new FieldProperties( jmcc.getUI(), this, cwui.getTableColumn(), add, edit );
			jmcc.updateStatus( "Ready...", false );
			fpu.setVisible( true );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
		}
	}	

	/*
	 * @description: runs custom SQL queries for the QueryUI (wrong place!!)
	 */	
	public void runCustomSQL( String query )
	{
		try
		{
			jmcc.updateStatus( "Executing query...", true );
			cwui.disableDataEdit();
			long start = System.nanoTime();
			
			if( query.toLowerCase().startsWith("select") || query.toLowerCase().startsWith("show") )
			{
				TableCC tcc = new TableCC( this );
				TableDataView result = tcc.executeQuery( query );
				cwui.showTableDataView( "Query results", result );
				setStatusDetail( "Query returned "+ result.getRowCount() +" row(s) in "+ millisSince( start ) +" ms" );
			}
			else
			{
				if( query.toLowerCase().startsWith("use") )
				{
					String db = query.substring( 3, query.length() );
					java.util.StringTokenizer st = new java.util.StringTokenizer( db, "; `", false );
					
					if( st.hasMoreTokens() )
						this.getDatabaseConnection().useDatabase( st.nextToken() );
				}
				else
				{
					int rows = this.getDatabaseConnection().executeUpdate( query );
					setStatusDetail( "Query affected "+ rows +" row(s) in "+ millisSince( start ) +" ms" );
				}				
			}
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}			
	}
	
	/*
	 * @description: Changes the active database for the QueryUI (wrong place!!)
	 */	
	public void changeDatabase( Database db )
	{
		try
		{		
			this.getDatabaseConnection().useDatabase( db.getName() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}				
	}

	/*
	 * @description: Let the tree generate its own events.
	 */
	public void selectTableInTree( Table table )
	{
		cwui.getDatabaseTreeView().selectTableInTree( table );
	}
		
	public void showTableData( Table table )
	{
		try
		{
			// Load the tables from the database.
			jmcc.updateStatus( "Loading table data...", true );
			long start = System.nanoTime();
			
			// Let the Table control class handle the data display creation.
			tbcc = new TableCC( this );
			cwui.showTableDataView( table.getDatabase().getName() +" : "+ table.getName(), tbcc.getTableDataView( table, 0, 50 ) );
			setStatusDetail( table.getDatabase().getName() +"."+ table.getName() +": "+ table.getRowCount() +" row(s), loaded in "+ millisSince( start ) +" ms" );
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}			
	}
	
	/*
	 * @description: 	Starts the table indexes manager
	 * 
	 * @comment:		PRO ONLY
	 */
	public void dispatchTableIndexesUI()
	{
		try
		{
			IndexesCC tcc = new IndexesCC( this, (Table)cwui.getSelectedNode().getUserObject() );
			tcc.startUI( jmcc.getUI() );		
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
		}		
	}
		
	public void dispatchUserManagerUI()
	{
		UserManagerCC umcc = new UserManagerCC( this );
		umcc.startUI( jmcc.getUI() );
	}
	
	public void dispatchProcessUI()
	{
		if( requireFeature( Dialect.Feature.PROCESS_LIST, "The process list" ) )
			new Processlist( this, jmcc.getUI() );
	}	

	/** Tells the user when the database of this connection can't do what they asked. */
	private boolean requireFeature( Dialect.Feature feature, String description )
	{
		if( cw.getConnectionProfile().getServerType().getDialect().supports( feature ) )
			return true;

		cwui.showErrorMessage( description +" is not available for "+ cw.getConnectionProfile().getServerType().getDescription() );
		return false;
	}

	public void dispatchCreateTableUI()
	{
		try
		{		
			TableCC tbcc = new TableCC( this );
			tbcc.startCreateTableUI( jmcc.getUI(), cwui.getDatabase() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}

	public void dispatchModifyTableUI()
	{
		try
		{		
			TableCC dbcc = new TableCC( this );
			dbcc.startEditTableUI( jmcc.getUI(), cwui.getDatabase(), cwui.getTable() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}		
	}	
			
	public String getTitle()
	{
		return cw.getConnectionProfile().getUsername() +"@"+ cw.getConnectionProfile().getHost();
	}
	
	public ESQLManagerUI getUI()
	{
		return jmcc.getUI();
	}
	
	public ImageLoader getImageLoader()
	{
		return jmcc.getImageLoader();
	}
	
	public nl.errorsoft.esql.data.DatabaseConnection getDatabaseConnection() throws Exception
	{
		if( !cw.getDatabaseConnection().getConnection().isClosed() )
			return cw.getDatabaseConnection();
		else 
			throw new Exception("Connection lost!");
	}
	
	public ConnectionProfile getConnectionProfile()
	{
		return cw.getConnectionProfile();
	}

	/*
	 * Server options, not every database has them.
	 */
	public void showServerStatus()
	{
		try
		{
			if( !requireFeature( Dialect.Feature.SERVER_STATUS, "Server status" ) )
				return;

			TableCC tcc = new TableCC( this );
			cwui.showTableDataView( "Server status", tcc.showServerStatus() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}
	public void showServerVariables()
	{
		try
		{
			if( !requireFeature( Dialect.Feature.SERVER_STATUS, "Server variables" ) )
				return;

			TableCC tcc = new TableCC( this );
			cwui.showTableDataView( "Server variables", tcc.showServerVariables() );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}	
	
	public void optimizeTable()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showMessage( "Optimize table: "+ cwui.getTable().getName(), tcc.optimizeTable( cwui.getTable() ) );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}

	public void analyseTable()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showMessage( "Analyze table: "+ cwui.getTable().getName(), tcc.analyseTable( cwui.getTable() ) );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}
	
	public void checkTable()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showMessage( "Check table: "+ cwui.getTable().getName(), tcc.checkTable( cwui.getTable() ) );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}		

	public void repairTable()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showMessage( "Repair table: "+ cwui.getTable().getName(), tcc.repairTable( cwui.getTable() ) );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
	}		
}
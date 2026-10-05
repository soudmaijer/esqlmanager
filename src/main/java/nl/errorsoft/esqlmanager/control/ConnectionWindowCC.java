package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.domain.*;
import nl.errorsoft.esqlmanager.gui.*;
import java.util.*;

public class ConnectionWindowCC extends Thread
{
	private ESQLManagerCC jmcc;
	private ConnectionWindow cw;
	private ConnectionWindowUI cwui;
	private TableCC tbcc;

	public ConnectionWindowCC( ESQLManagerCC jmcc, nl.errorsoft.esqlmanager.domain.ConnectionProfile cp )
	{
		this.jmcc = jmcc;
		this.cw = new ConnectionWindow( this, cp );
		this.start();
	}
	
	public void run()
	{
		// Create Frame.
		jmcc.println( "Connecting to `"+ cw.getConnectionProfile().getServerType().getDescription() +"` @ `"+ cw.getConnectionProfile().getHost() +"` with username `"+ cw.getConnectionProfile().getUsername() +"` on port `"+ cw.getConnectionProfile().getPort() +"`" );
		cwui = new ConnectionWindowUI( this, jmcc.getUI() );
		
		// Create database connection to Server.
		jmcc.showConnectionWindow( cwui );
		jmcc.updateStatus( "Connecting...", true );
		
		try
		{
			cw.start();
			jmcc.updateStatus( "Loading databases...", true );
			showDatabaseTree();
			jmcc.updateStatus( "Ready...", false );		
		}
		catch( Exception e )
		{
			cwui.showErrorMessage( "Can`t connect: "+ e.getMessage() );
			cwui.closeUI(false);
			jmcc.updateStatus( "Can`t connect to server...", false );
		}
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			e.printStackTrace();
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
			qu.show();
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			fpu.show();
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			
			if( query.toLowerCase().startsWith("select") || query.toLowerCase().startsWith("show") )
			{
				TableCC tcc = new TableCC( this );
				cwui.showTableDataView( "Query results", tcc.executeQuery( query ) );
			}
			else
			{
				if( query.toLowerCase().startsWith("use") )
				{
					String db = query.substring( 3, query.length() );
					java.util.StringTokenizer st = new java.util.StringTokenizer( db, "; `", false );
					
					if( st.hasMoreTokens() )
						this.getDatabaseConnection().getConnection().setCatalog( st.nextToken() );
				}
				else
				{
					this.getDatabaseConnection().executeUpdate( query );
				}				
			}
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			this.getDatabaseConnection().getConnection().setCatalog( db.getName() );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			
			// Let the Table control class handle the data display creation.
			tbcc = new TableCC( this );
			cwui.showTableDataView( table.getDatabase().getName() +" : "+ table.getName(), tbcc.getTableDataView( table, 0, 50 ) );
			jmcc.updateStatus( "Ready...", false );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			if( jmcc.checkPro() ) return;
			IndexesCC tcc = new IndexesCC( this, (Table)cwui.getSelectedNode().getUserObject() );
			tcc.startUI( jmcc.getUI() );		
		}
		catch( Exception e )
		{
			e.printStackTrace();
			cwui.showErrorMessage( e.getMessage() );
		}		
	}
		
	public void dispatchUserManagerUI()
	{
		if( jmcc.checkPro() ) return;
		UserManagerCC umcc = new UserManagerCC( this );
		umcc.startUI( jmcc.getUI() );
	}
	
	public void dispatchProcessUI()
	{
		if( jmcc.checkPro() ) return;
		Processlist pl = new Processlist( this, jmcc.getUI() );
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
			e.printStackTrace();
			cwui.showErrorMessage( e.getMessage() );
			jmcc.updateStatus( "Error...", false );
		}
	}

	public void dispatchModifyTableUI()
	{
		try
		{		
			if( jmcc.checkPro() ) return;
			TableCC dbcc = new TableCC( this );
			dbcc.startEditTableUI( jmcc.getUI(), cwui.getDatabase(), cwui.getTable() );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
	
	public nl.errorsoft.esqlmanager.data.DatabaseConnection getDatabaseConnection() throws Exception
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
	 * MySQL specific options.
	 */
	public void showMySQLStatus()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showTableDataView( "MySQL Status", tcc.showMySQLStatus() );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	public void showMySQLVariables()
	{
		try
		{
			TableCC tcc = new TableCC( this );
			cwui.showTableDataView( "MySQL Variables", tcc.showMySQLVariables() );
		}
		catch( Exception e )
		{
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}		
}
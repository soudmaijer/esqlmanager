package esql.control;

import esql.gui.*;
import esql.data.*;
import esql.domain.*;
import java.util.Vector;

public class TableCC 
{
	private TableDataView tdv;
  	private ConnectionWindowCC cwcc;

   public TableCC( ConnectionWindowCC cwcc ) 
   {	this.cwcc = cwcc;
   }
   
	public void startCreateTableUI( ESQLManagerUI eu, Database d ) throws Exception
	{
		// Dit stuk code moet naar CreateTableCC!
		try
		{	if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }

		eu.updateStatus("Starting create table interface...", true );
		CreateTable ct = new CreateTable( eu, cwcc, this, d, null );
		ct.show();
		eu.updateStatus("Ready...", false );
		
	}

	public void startEditTableUI( ESQLManagerUI eu, Database d, Table t ) throws Exception
	{
		// Dit stuk code moet naar CreateTableCC!
		try
		{	if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
	   
		eu.updateStatus("Starting modify table interface...", true );
		if( t.getColumns() == null )
			t.setColumns( t.getColumns( t ) );
		CreateTable ct = new CreateTable( eu, cwcc, this, d, t );
		eu.updateStatus("Ready...", false );
		ct.show();
	}
	
	public void dispatchDownloadFileUI( Table table, TableData [] rowData, TableData cellData )	
	{
		UDDataCC udcc = new UDDataCC( cwcc );
		udcc.startDownloadUI( cwcc.getUI(), table, rowData, cellData );
	}
   
	public void dispatchUploadFileUI( Table table, TableData [] rowData, TableData cellData )	
	{
		UDDataCC udcc = new UDDataCC( cwcc );
		udcc.startUploadUI( cwcc.getUI(), table, rowData, cellData );
	}
   
   public TableDataView getTableDataView( Table table, int skip, int show ) throws Exception
   {
		TableData [][] tdata = table.getData( table, skip, show );
		tdv = new TableDataView( this );
		tdv.loadData( table, table.getColumns(), tdata );
		return tdv;
	}
	
	public TableColumn[] getColumns( Table table ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), table.getDatabase() );
		return t.getColumns( table );
	}
	
	public void showTableData( Table table, int skip, int show ) throws Exception
	{
		TableData [][] tdata = table.getData( table, skip, show );
		tdv.loadData( table, table.getColumns(), tdata );
	}
	
	public TableDataView executeQuery( String query ) throws Exception
	{
		Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
		TableData [][] tdaq = tempTable.executeQuery( query );
		TableDataView tdvq = new TableDataView( this );
		tdvq.loadData( tempTable, tempTable.getColumns(), tdaq );
		return tdvq;		
	}

	public void dropTableColumn( TableColumn tb ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), tb.getTable().getDatabase() );
		t.dropTableColumn( tb );
	}

	public void dropTable( Table tb ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), tb.getDatabase() );
		t.dropTable( tb );
	}

	public void flushTable( Table tb ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), tb.getDatabase() );
		t.flushTable( tb );
	}
	
	public void executeUpdate( String query ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), null );
		t.executeUpdate( query );
	}		
	
	public void addTableColumn( Table tb, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), null );
		t.addTableColumn( tb, name, length, defaultValue, dt, primary, auto, unsigned, nullable );
	}		
	
	public void editTableColumn( TableColumn tbc, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), null );
		t.editTableColumn( tbc, name, length, defaultValue, dt, primary, auto, unsigned, nullable );
	}
		
	public void insertNewRow()
	{
		tdv.insertNewRow();
	}
	
	public void deleteSelectedRows()
	{
		tdv.deleteSelectedRows();
	}
	
	public void saveSelectedRow()
	{
		tdv.saveSelectedRow();
	}	
	
	public ImageLoader getImageLoader()
	{
		return cwcc.getImageLoader();
	}
	
	public void insertRow( Table table, TableData [] rowData ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), table.getDatabase() );
		t.insertRow( table, rowData );
	}
	
	public void dataChanged( Table table, TableData [] rowData, TableData cellData, Object newValue ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), table.getDatabase() );
		t.dataChanged( table, rowData, cellData, newValue );
	}

	public void deleteRow( Table table, TableData [] rowData ) throws Exception
	{
		Table t = new Table( cwcc.getDatabaseConnection(), table.getDatabase() );
		t.deleteRow( table, rowData );
	}
	
	/*
	 * MySQL specific options.
	 */	
	public TableDataView showMySQLStatus() throws Exception
	{
	  	return runMySQLCommand( "SHOW STATUS;" );				
	} 
	
	public TableDataView showMySQLVariables() throws Exception
	{
	  	return runMySQLCommand( "SHOW VARIABLES;" );				
	} 
	
	public String optimizeTable( Table table ) throws Exception
	{
		Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
		return tempTable.optimizeTable( table );
	}

	public String analyseTable( Table table ) throws Exception
	{
		Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
		return tempTable.analyseTable( table );
	}

	public String checkTable( Table table ) throws Exception
	{
		Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
		return tempTable.checkTable( table );
	}		

	public String repairTable( Table table ) throws Exception
	{
		Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
		return tempTable.repairTable( table );
	}	
	
	public TableDataView runMySQLCommand( String query ) throws Exception
	{
  		TableDataView tdvq = new TableDataView( this );
   	Table tempTable = new Table( cwcc.getDatabaseConnection(), null );
   	TableData [][] tdaq = tempTable.runMySQLCommand( query );
   	tdvq.loadData( tempTable, tempTable.getColumns(), tdaq );
   	return tdvq;				
	} 
	
}

//Source file: d:\\roseoutput\\esql\\esql\\table\\Table.java

package nl.errorsoft.esql.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Vector;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.dialect.Dialect;

public class Table 
{
	private static final Logger log = LogManager.getLogger( Table.class );

   private String name;
   private String type;
   private String comment;
   private int rows = 0;
   private DatabaseConnection dbc;
   private Database db;
   private TableColumn tca[];
   private TableIndex tia[];

   /**
    * @roseuid 3E05A70D006B
    */
   public Table( DatabaseConnection dbc, Database db ) 
   {
   	this.db = db;
   	this.dbc = dbc;
   }
 
 	// Columns
  	public void setColumns( TableColumn [] tca )
   {
   	this.tca = tca;
   }
   
   public TableColumn [] getColumns()
   {
   	return this.tca;
   }
   
   public TableColumn [] getColumns( Table tb ) throws Exception
   {
		Vector v = new Vector();
		tb.setColumns( new TableColumn[0] );
		dbc.useDatabase( tb.getDatabase().getName() );
		
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		ResultSet rs = dmd.getColumns( dbc.getConnection().getCatalog(), dbc.getSchema(), tb.getName() , "%" );
	
		// Get column name.
		while( rs.next() )
		{
			TableColumn temp = new TableColumn( tb );
			temp.setNativeTypeName( rs.getString("TYPE_NAME") );
			temp.setName( rs.getString("COLUMN_NAME") );
			temp.setSize( rs.getInt("COLUMN_SIZE") );
			temp.setNullable( rs.getBoolean("NULLABLE") );
			temp.setDefault( rs.getString("COLUMN_DEF") );
			v.add( temp );			
		}
		
		rs.close();
		rs = dmd.getPrimaryKeys( dbc.getConnection().getCatalog(), dbc.getSchema(), tb.getName() );

		while( rs.next() )
		{
			for( int i=0; i<v.size(); i++ )
			{
				TableColumn temp = (TableColumn)v.elementAt(i);
								
				if( temp.getName().equalsIgnoreCase( rs.getString("COLUMN_NAME") ) )
				{
					temp.setPrimary( true );
					break;
				}
			}
		}
		rs.close();

		return (TableColumn[])v.toArray(new TableColumn[v.size()]);
	}

   // Indexes
   public void setIndexes( TableIndex [] tia )
   {
   	this.tia = tia;
   }
   
   public TableIndex [] getIndexes()
   {
   	if( tia == null )
   		return new TableIndex[0];
   	return this.tia;
   }

   public TableColumn getTableColumn( String name )
   {
   	if( tca != null )
   	{
	   	for( int i=0; i<tca.length; i++ )
	   		if( tca[i].getName().equals( name ) )
	   			return tca[i];
	   }
   	return null;
   }
   
   public TableIndex getTableIndex( String name )
   {
   	if( tia != null )
   	{
	   	for( int i=0; i<tia.length; i++ )
	   		if( tia[i].getName().equals( name ) )
	   			return tia[i];
	   }
		return null;
   }
	
   public TableIndex [] getIndexes( Table tb )
   {
		try
		{
			Vector v = new Vector();
			tb.setIndexes( new TableIndex[0] );
			dbc.useDatabase( tb.getDatabase().getName() );
			
			DatabaseMetaData dmd = dbc.getConnection().getMetaData();
			String primaryKeyName = primaryKeyName( dmd, tb );
			ResultSet rs = dmd.getIndexInfo( dbc.getConnection().getCatalog(), dbc.getSchema(), tb.getName(), false, false );
	
			while( rs.next() )
			{
				// Some drivers add a statistics row without an index name.
				if( rs.getString("INDEX_NAME") == null )
					continue;

				TableIndex ti = new TableIndex(tb);
				// The primary key is always shown as PRIMARY, whatever name the server gave it.
				String indexName = rs.getString("INDEX_NAME");
				ti.setName( indexName != null && indexName.equals( primaryKeyName ) ? "PRIMARY" : indexName );
	
				if( !rs.getBoolean("NON_UNIQUE") )
					ti.setUnique( true );
					
				if( rs.getString("TYPE").toUpperCase().equals("FULLTEXT") )
					ti.setFulltext( true );
				
				// Kan Nullpointer op sql server opleveren!
				TableColumn tc = tb.getTableColumn( rs.getString("COLUMN_NAME") );
				
				// If null
				if( tc != null )
				{
					tc.setHasUniqueIndex( ti.isUnique() );
					tc.setIndexed( true );
					tc.setIndexPosition( rs.getInt("ORDINAL_POSITION") );
					
					if( tb.getTableIndex( ti.getName() ) == null )
					{	
						ti.addTableColumn( tc );
						v.add( ti );
						tb.setIndexes( (TableIndex[])v.toArray(new TableIndex[v.size()]) );
					}
					else
					{	tb.getTableIndex( ti.getName() ).addTableColumn( tc );
					}
				}
			}
	
			rs.close();
			return (TableIndex[])v.toArray(new TableIndex[v.size()]);
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
		}
		return new TableIndex[0];
	}	
	
	private String primaryKeyName( DatabaseMetaData dmd, Table tb ) throws SQLException
	{
		ResultSet rs = dmd.getPrimaryKeys( dbc.getConnection().getCatalog(), dbc.getSchema(), tb.getName() );

		try
		{
			return rs.next() ? rs.getString("PK_NAME") : null;
		}
		finally
		{
			rs.close();
		}
	}

	public TableData [][] getData( Table tb, int skip, int show ) throws Exception
	{
		Vector rows = new Vector();
		TableData [] rowData = null;
		TableColumn [] tcatemp = tb.getColumns( tb );
		tb.setColumns( tcatemp );
		tb.setIndexes( tb.getIndexes( tb ) );
		
		// Set the active database.
		dbc.useDatabase( tb.getDatabase().getName() );
		
		// Count the total number of records.
		ResultSet rs = dbc.executeQuery("SELECT count(*) FROM "+ quote( tb.getName() ) );
		
		if( rs.first() )
			tb.setRowCount( rs.getInt(1) );
		
		rs.close();
		
		// Let the server do the paging when the dialect can, otherwise skip rows in the result set.
		String quotedTable = quote( tb.getName() );
		java.util.List<String> keyColumns = new java.util.ArrayList<String>();
		for( TableColumn column : tcatemp )
			if( column.isPrimary() )
				keyColumns.add( quote( column.getName() ) );
		String pageQuery = dbc.getConnectionProfile().getServerType().getDialect().selectPage( quotedTable, String.join( ", ", keyColumns ), skip, show );
		
		if( pageQuery != null )
		{
			rs = dbc.executeQuery( pageQuery );
		}
		else
		{	rs = dbc.executeQuery("SELECT * FROM "+ quotedTable );
		
			if( skip > 0 )
				rs.absolute( skip );
			else
				rs.beforeFirst();
		}
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		for( int i=0; i<tcatemp.length; i++ )
		{
			tcatemp[i].setTypeName( rsmd.getColumnTypeName(i+1) );
			tcatemp[i].setClassName( rsmd.getColumnClassName(i+1) );
			tcatemp[i].setWritable( rsmd.isWritable(i+1) );
			tcatemp[i].setAutoIncrement( rsmd.isAutoIncrement(i+1) );			
			tcatemp[i].setSigned( rsmd.isSigned(i+1) );	
			tcatemp[i].setType( rsmd.getColumnType(i+1) );	
		}
		
		// Get the data.
		while( rs.next() )
		{
			rowData = new TableData[ tb.getColumns().length ];
			
			for( int i=0; i<rowData.length; i++ )
			{
				TableData temp = new TableData();
				temp.setTableColumn( tca[i] );
				
				if( !tca[i].isBinary() )
					temp.setData( rs.getObject(i+1) );
					
				rowData[i] = temp;
			}
			
			rows.add( rowData );
		}
		rs.close();

		// Return data in requested format.
		TableData [][] tda = new TableData[ rows.size() ][ tcatemp.length ];
		
		for( int i=0; i<rows.size(); i++ )
		{
			for( int j=0; j<tcatemp.length; j++ )
			{
				tda[i][j] = ((TableData[])rows.get(i))[j];
			}
		}
		tb.setColumns( tcatemp );
		return tda;
	}

	public void dropTable( Table tb ) throws Exception
	{
		dbc.executeUpdate("DROP TABLE "+ quote( tb.getName() ) );
	}		

	public void flushTable( Table tb ) throws Exception
	{
		dbc.executeUpdate("DELETE FROM "+ quote( tb.getName() ) );
	}	

	public void dropTableColumn( TableColumn tb ) throws Exception
	{
		dbc.executeUpdate("ALTER TABLE "+ quote( tb.getTable().getName() ) +" DROP "+ quote( tb.getName() ) );
	}

	public void modifyTable( Table tb, String tableName, String tableType, String tableComment ) throws Exception
	{
		if( !tb.getName().equalsIgnoreCase( tableName ) )
		{	executeAll( getDialect().renameTableSql( tb.getName(), tableName ) );
			tb.setName( tableName );
		}
		if( tableType != null && !tableType.equalsIgnoreCase( tb.getType() ) )
		{	executeAll( getDialect().setTableTypeSql( tb.getName(), tableType ) );
			tb.setType( tableType );
		}
		if( !tableComment.equals( tb.getComment() == null ? "" : tb.getComment() ) )
		{	executeAll( getDialect().setTableCommentSql( tb.getName(), tableComment ) );
			tb.setComment( tableComment );
		}
	}

	public void executeUpdate( String query ) throws Exception
	{
		dbc.executeUpdate( query );
	}
	
	public TableData [][] executeQuery( String query ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( query );
		ResultSetMetaData rsm = rs.getMetaData();
		Vector vecRows = new Vector();
		Vector vecRowData = null;
		tca = new TableColumn[ rsm.getColumnCount() ];
		db = new Database( dbc );
		db.setName( dbc.getConnection().getCatalog() );
		
		for( int i=0; i<tca.length; i++ )
		{
			Table t = new Table( dbc, db );
			t.setName( rsm.getTableName( i+1 ) );
			nl.errorsoft.esql.domain.TableColumn tc = new nl.errorsoft.esql.domain.TableColumn( t );
			tc.setName( rsm.getColumnName( i+1 ) );
			tca[i] = tc;
		}

		rs.beforeFirst();
		
		while( rs.next() )
		{
			vecRowData = new Vector();
			
			for( int i=0; i<tca.length; i++ )
			{
				vecRowData.add( rs.getObject( i+1 ) );
			}
			vecRows.add( vecRowData );
		}
		rs.close();

		
		this.rows = vecRows.size();
		TableData [][] tda = new TableData[ rows ][ tca.length ];
		
		for( int i=0; i<rows; i++ )
		{
			vecRowData = (Vector)vecRows.get(i);
			
			for( int j=0; j<tca.length; j++ )
			{
				TableData td = new TableData();
				td.setData( vecRowData.get(j) );
				td.setTableColumn( tca[j] );
				tda[i][j] = td;
			}
		}
		
		return tda;
	}	
	
	/**
	 * The condition that selects the given row: its key columns when it has any,
	 * otherwise all of its columns that are not binary.
	 */
	public String rowFilter( TableData [] rowData ) throws Exception
	{
		java.util.List<String> keys = new java.util.ArrayList<String>();
		java.util.List<String> columns = new java.util.ArrayList<String>();

		for( TableData cell : rowData )
		{
			TableColumn column = cell.getTableColumn();
			String name = quote( column.getName() );

			if( column.isPrimary() || column.hasUniqueIndex() )
				keys.add( name +"="+ sqlValue( cell ) );
			else if( !column.isBinary() )
				columns.add( cell.isNull() ? name +" IS NULL" : name +"="+ sqlValue( cell ) );
		}

		java.util.List<String> conditions = keys.isEmpty() ? columns : keys;

		if( conditions.isEmpty() )
			throw new Exception("The row can't be identified, it has no key and no column to compare.");

		return String.join( " AND ", conditions );
	}

	/** The value of a cell as it goes into a statement, an empty cell is NULL and not the text "null". */
	private String sqlValue( TableData cell )
	{
		return cell.isNull() ? "NULL" : dbc.formatFieldValue( cell.getData() );
	}

	public void insertRow( Table tb, TableData [] rowData ) throws Exception
	{
		String sqlColumnName = "";
		String sqlColumnData = "";
		
		for( int i=0; i<rowData.length; i++ )
		{
			sqlColumnName += quote( rowData[i].getTableColumn().getName() );
			sqlColumnData += sqlValue( rowData[i] );

			if( i<rowData.length-1 )
			{
				sqlColumnName += ",";
				sqlColumnData += ",";
			}
		}
		
		dbc.useDatabase( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("INSERT INTO "+ quote( tb.getName() ) 
										+ " ("+  sqlColumnName +")"
										+ " VALUES ("+ sqlColumnData +")");
		
		tb.setRowCount( tb.getRowCount() + 1 );
	}
	
	public int dataChanged( Table tb, TableData [] rowData, TableData cellData, Object newValue ) throws Exception
	{
		if( cellData.getData().equals( newValue.toString() ) )
			return 0;
		else if( cellData.getTableColumn().isBinary() )
			throw new Exception("Editing of binary data is not supported yet!");

		String sqlWhere = rowFilter( rowData );
		dbc.useDatabase( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("UPDATE "+ quote( tb.getName() ) 
										+" SET "+ quote( cellData.getTableColumn().getName() ) +"="+ dbc.formatFieldValue( newValue.toString() )
										+" WHERE "+ sqlWhere );
		return i;
	}

	public void deleteRow( Table tb, TableData [] rowData ) throws Exception
	{
		String sqlWhere = rowFilter( rowData );

		dbc.useDatabase( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("DELETE FROM "+ quote( tb.getName() ) +" WHERE "+ sqlWhere );
		tb.setRowCount( tb.getRowCount() - 1 );
	}
	
	/*
	 * @description: creates a new field to an existing table.
	 */
	public void addTableColumn( Table tb, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		CreateColumn column = createColumn( name, length, defaultValue, dt, auto, unsigned, nullable );
		column.primary = primary;

		executeAll( getDialect().addColumnSql( tb.getName(), column ) );
	}

	/*
	 * @description: modifies a field of an existing table.
	 */
	public void editTableColumn( TableColumn tc, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		String table = tc.getTable().getName();
		CreateColumn column = createColumn( name, length, defaultValue, dt, auto, unsigned, nullable );

		executeAll( getDialect().modifyColumnSql( table, tc.getName(), column ) );

		if( tc.isPrimary() && !primary )
			executeAll( getDialect().dropIndexSql( dbc, table, "PRIMARY" ) );
		if( !tc.isPrimary() && primary )
			executeAll( getDialect().addIndexSql( table, "PRIMARY", "INDEX", java.util.Arrays.asList( name ) ) );
	}

	private CreateColumn createColumn( String name, String length, String defaultValue, DataType dt, boolean auto, boolean unsigned, boolean nullable )
	{
		CreateColumn column = new CreateColumn( name );
		column.type = dt;
		column.length = length;
		column.defaultval = defaultValue;
		column.unsigned = dt.unsigned && unsigned;
		column.notnull = dt.notnull && !nullable;
		column.autoincrement = auto;
		return column;
	}

	public void addIndex( Table tb, TableIndex ti, TableColumn tc[], String type ) throws Exception
	{
		if( tc == null || tc.length <=0 )
			return;

		dbc.useDatabase( tb.getDatabase().getName() );
		executeAll( getDialect().addIndexSql( tb.getName(), ti.getName(), indexType( type ), columnNames( tc ) ) );
		tb.setIndexes( tb.getIndexes( tb ) );
	}

	public void modifyIndex( Table tb, TableIndex ti, TableColumn tc[], String type ) throws Exception
	{
		if( tc == null || tc.length <=0 )
			return;

		dbc.useDatabase( tb.getDatabase().getName() );
		executeAll( getDialect().modifyIndexSql( dbc, tb.getName(), ti.getName(), indexType( type ), columnNames( tc ) ) );
		tb.setIndexes( tb.getIndexes( tb ) );
	}

	public void dropIndex( Table tb, TableIndex ti ) throws Exception
	{
		dbc.useDatabase( tb.getDatabase().getName() );
		executeAll( getDialect().dropIndexSql( dbc, tb.getName(), ti.getName() ) );
		tb.setIndexes( tb.getIndexes( tb ) );
	}

	private String indexType( String type )
	{
		return type == null || type.length() <= 0 ? "INDEX" : type;
	}

	private java.util.List<String> columnNames( TableColumn tc[] )
	{
		java.util.List<String> names = new java.util.ArrayList<String>();

		for( TableColumn column : tc )
			names.add( column.getName() );

		return names;
	}

	private String quote( String identifier )
	{
		return getDialect().quote( identifier );
	}

	private nl.errorsoft.esql.domain.dialect.Dialect getDialect()
	{
		return dbc.getConnectionProfile().getServerType().getDialect();
	}

	private void executeAll( java.util.List<String> statements ) throws Exception
	{
		for( String statement : statements )
			dbc.executeUpdate( statement );
	}

	public TableData [][] runMySQLCommand( String query ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( query );
		ResultSetMetaData rsm = rs.getMetaData();
		Vector vecRows = new Vector();
		Vector vecRowData = null;
		tca = new TableColumn[ rsm.getColumnCount() ];
		db = new Database( dbc );
		db.setName( dbc.getConnection().getCatalog() );
		
		for( int i=0; i<tca.length; i++ )
		{
			Table t = new Table( dbc, db );
			t.setName( rsm.getTableName( i+1 ) );
			nl.errorsoft.esql.domain.TableColumn tc = new nl.errorsoft.esql.domain.TableColumn( t );
			tc.setName( rsm.getColumnName( i+1 ) );
			tc.setWritable( false );
			tca[i] = tc;
		}

		rs.beforeFirst();
		
		while( rs.next() )
		{
			vecRowData = new Vector();
			
			for( int i=0; i<tca.length; i++ )
			{
				vecRowData.add( rs.getObject( i+1 ) );
			}
			
			vecRows.add( vecRowData );
		}
		
		rs.close();		

		this.rows = vecRows.size();
		TableData [][] tda = new TableData[ rows ][ tca.length ];
		
		for( int i=0; i<rows; i++ )
		{
			vecRowData = (Vector)vecRows.get(i);
			
			for( int j=0; j<tca.length; j++ )
			{
				TableData td = new TableData();
				td.setData( vecRowData.get(j) );
				td.setTableColumn( tca[j] );
				tda[i][j] = td;
			}
		}
		
		return tda;
	}	
	
	public void setType( String type )
	{
		this.type = type;
	}
	
	public String getType()
	{
		return this.type;
	}
	
	public void setRowCount( int rows )
	{
		this.rows = rows;
	}	

	public int getRowCount()
	{
		return this.rows;
	}

	public void setComment( String comment )
	{
		this.comment = comment;
	}	

	public String getComment()
	{
		return this.comment;
	}	

	public void setName( String name )
	{
		this.name = name;
	}
	
	public String getName()
	{
		return this.name;
	}   
	
	public String toString()
	{
		return name;
	}
		
	public Database getDatabase()
	{
		return db;
	}
	
	/*
	 * Maintenance, what each command does depends on the database.
	 */
	public String optimizeTable( Table table ) throws Exception
	{
		return getDialect().maintain( dbc, Dialect.Maintenance.OPTIMIZE, table.getName() );
	}

	public String analyseTable( Table table ) throws Exception
	{
		return getDialect().maintain( dbc, Dialect.Maintenance.ANALYZE, table.getName() );
	}

	public String checkTable( Table table ) throws Exception
	{
		return getDialect().maintain( dbc, Dialect.Maintenance.CHECK, table.getName() );
	}

	public String repairTable( Table table ) throws Exception
	{
		return getDialect().maintain( dbc, Dialect.Maintenance.REPAIR, table.getName() );
	}	 		
}

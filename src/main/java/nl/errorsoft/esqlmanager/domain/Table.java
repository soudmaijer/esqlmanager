//Source file: d:\\roseoutput\\esql\\esql\\table\\Table.java

package nl.errorsoft.esqlmanager.domain;

import java.sql.*;
import java.util.Vector;
import nl.errorsoft.esqlmanager.data.*;

public class Table 
{
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
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		ResultSet rs = dmd.getColumns( dbc.getConnection().getCatalog(), null, tb.getName() , "%" );
	
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
		rs = dmd.getPrimaryKeys( dbc.getConnection().getCatalog(), null, tb.getName() );

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
			dbc.getConnection().setCatalog( tb.getDatabase().getName() );
			
			DatabaseMetaData dmd = dbc.getConnection().getMetaData();
			ResultSet rs = dmd.getIndexInfo( dbc.getConnection().getCatalog(), null, tb.getName(), true, false );
	
			while( rs.next() )
			{
				TableIndex ti = new TableIndex(tb);
				ti.setName( rs.getString("INDEX_NAME") );
	
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
			e.printStackTrace();
		}
		return new TableIndex[0];
	}	
	
	public TableData [][] getData( Table tb, int skip, int show ) throws Exception
	{
		Vector rows = new Vector();
		TableData [] rowData = null;
		TableColumn [] tcatemp = tb.getColumns( tb );
		tb.setColumns( tcatemp );
		tb.setIndexes( tb.getIndexes( tb ) );
		
		// Set the active database.
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		
		// Count the total number of records.
		ResultSet rs = dbc.executeQuery("SELECT count(*) FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
		
		if( rs.first() )
			tb.setRowCount( rs.getInt(1) );
		
		rs.close();
		
		// For speed enhancement LIMIT the MySQL query result at server side.
		if( dbc.getConnectionProfile().getServerType().getType() == ServerType.MY_SQL )
		{
			rs = dbc.executeQuery("SELECT * FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" LIMIT "+ skip +","+ show );
		}
		// For speed enhancement TOP the Postgres query result at server side.
		else if( dbc.getConnectionProfile().getServerType().getType() == ServerType.POSTGRES )
		{
			rs = dbc.executeQuery("SELECT * FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" LIMIT "+ show +" OFFSET "+ skip );
		}
		// JDBC Default LOW performance code.
		else
		{	rs = dbc.executeQuery("SELECT * FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
		
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
		dbc.executeUpdate("DROP TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
	}		

	public void flushTable( Table tb ) throws Exception
	{
		dbc.executeUpdate("DELETE FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
	}	

	public void dropTableColumn( TableColumn tb ) throws Exception
	{
		dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getTable().getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" DROP "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
	}

	public void modifyTable( Table tb, String tableName, String tableType, String tableComment ) throws Exception
	{
		String newtype = "";
		String newname = "";
		String newcomment = "";
		
		if( !tb.getName().equalsIgnoreCase( tableName ) )
		{	dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" RENAME "+ tableName );
			tb.setName( tableName );
		}
		if( !tb.getType().equalsIgnoreCase( tableType ) )
		{	dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" TYPE="+ tableType );
			tb.setType( tableType );
		}
		if( !tb.getComment().equalsIgnoreCase( tableComment ) );
		{
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" COMMENT ="+ dbc.getConnectionProfile().getServerType().getDataOpenChar() + tableComment + dbc.getConnectionProfile().getServerType().getDataCloseChar() );
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
			nl.errorsoft.esqlmanager.domain.TableColumn tc = new nl.errorsoft.esqlmanager.domain.TableColumn( t );
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
	
	public void insertRow( Table tb, TableData [] rowData ) throws Exception
	{
		String sqlColumnName = "";
		String sqlColumnData = "";
		
		for( int i=0; i<rowData.length; i++ )
		{
			sqlColumnName += dbc.getConnectionProfile().getServerType().getFieldOpenChar() + rowData[i].getTableColumn().getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar();
			sqlColumnData += dbc.formatFieldValue( rowData[i].getData() );

			if( i<rowData.length-1 )
			{
				sqlColumnName += ",";
				sqlColumnData += ",";
			}
		}
		
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("INSERT INTO "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() 
										+ " ("+  sqlColumnName +")"
										+ " VALUES ("+ sqlColumnData +")");
		
		tb.setRowCount( tb.getRowCount() + 1 );
	}
	
	public int dataChanged( Table tb, TableData [] rowData, TableData cellData, Object newValue ) throws Exception
	{
		String sqlWhere = "";
		boolean keySearch = false;
		
		if( cellData.getData().equals( newValue.toString() ) )
			return 0;
		else if( cellData.getTableColumn().isBinary() )
			throw new Exception("Editing of binary data is not supported yet!");

		for( int i=0; i<rowData.length; i++ )
		{
			if( rowData[i].getTableColumn().isPrimary() || rowData[i].getTableColumn().hasUniqueIndex() )
			{
				if( i<rowData.length-1 && sqlWhere.length() > 0 )
					sqlWhere += " AND ";				
					
				sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() );
				keySearch = true;
			}
			else if( !keySearch )
			{
				if( !rowData[i].getTableColumn().isBinary() )
				{
					if( rowData[i].isNull() )
						sqlWhere += rowData[i].getTableColumn().getName() +" IS NULL";
					else
						sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() );
										
					if( i<rowData.length-1 )
						sqlWhere += " AND ";
				}
			}
		}
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("UPDATE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() 
										+" SET "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + cellData.getTableColumn().getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +"="+ dbc.formatFieldValue( newValue.toString() )
										+" WHERE "+ sqlWhere );
		return i;
	}

	public void deleteRow( Table tb, TableData [] rowData ) throws Exception
	{
		String sqlWhere = "";
		boolean keySearch = false;
		
		for( int i=0; i<rowData.length; i++ )
		{
			if( rowData[i].getTableColumn().isPrimary() || rowData[i].getTableColumn().hasUniqueIndex() )
			{
				if( i<rowData.length-1 && sqlWhere.length() > 0 )
					sqlWhere += " AND ";				

				sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				keySearch = true;
			}
			else if( !keySearch )
			{
				if( !rowData[i].getTableColumn().isBinary() )
				{	sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				
					if( i<rowData.length-1 )
						sqlWhere += " AND ";
				}
			}
		}

		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		int i = dbc.executeUpdate("DELETE FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" WHERE "+ sqlWhere );
		tb.setRowCount( tb.getRowCount() - 1 );
	}
	
	/*
	 * @description: creates a new field to an existing table.
	 */
	public void addTableColumn( Table tb, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		String def = "";
		
		def = dt.getName();
		
		if( length.trim().length() > 0 )
			def += "("+ length +")";
		if( dt.unsigned && unsigned )
			def += " UNSIGNED";
		if( defaultValue.trim().length() > 0 )
			def += " DEFAULT "+ dbc.getConnectionProfile().getServerType().getDataOpenChar()+ defaultValue + dbc.getConnectionProfile().getServerType().getDataCloseChar();
		if( dt.notnull && !nullable )
			def += " NOT NULL";
		if( auto )
			def += " AUTO_INCREMENT";
		if( primary )
			def += " PRIMARY KEY";
			
		dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" ADD "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + name + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" "+ def  );
	}

	/*
	 * @description: modifies a field of an existing table.
	 */
	public void editTableColumn( TableColumn tc, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned, boolean nullable ) throws Exception
	{
		String def = "";
		String query = "";
		
		def = dt.getName();
		
		if( length.trim().length() > 0 )
			def += "("+ length +")";
		if( dt.unsigned && unsigned )
			def += " UNSIGNED";
		if( defaultValue.trim().length() > 0 )
			def += " DEFAULT "+ dbc.getConnectionProfile().getServerType().getDataOpenChar()+ defaultValue + dbc.getConnectionProfile().getServerType().getDataCloseChar();
		if( dt.notnull && !nullable )
			def += " NOT NULL";
		if( auto )
			def += " AUTO_INCREMENT";
			
		query = "ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tc.getTable().getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() 
				+ " CHANGE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tc.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar()
				+ " "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + name + dbc.getConnectionProfile().getServerType().getFieldCloseChar()
				+ " "+ def;
		
		if( tc.isPrimary() && !primary )
			query += ", DROP PRIMARY KEY";
		if( !tc.isPrimary() && primary )
			query += ", ADD PRIMARY KEY ("+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + name + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +")";
	
		dbc.executeUpdate( query );
	}	
	
	public void addIndex( Table tb, TableIndex ti, TableColumn tc[], String type ) throws Exception
	{
		if( tc == null || tc.length <=0 )
			return;

		if( type == null || type.length() <= 0 )
			type = "INDEX";
		
		String cols = "(";
			
		for( int i=0; i<tc.length; i++ )
		{
			cols += tc[i].getName();
			
			if( i < tc.length-1 )
				cols += ",";
		}
		
		cols = cols + ")";
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		
		if( ti.getName().equals("PRIMARY") )
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" ADD PRIMARY KEY "+ cols );
		else
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" ADD "+ type +" "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + ti.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" "+ cols );
			
		tb.setIndexes( tb.getIndexes( tb ) );		
	}	

	public void modifyIndex( Table tb, TableIndex ti, TableColumn tc[], String type ) throws Exception
	{
		if( tc == null || tc.length <=0 )
			return;

		if( type == null || type.length() <= 0 )
			type = "INDEX";
		
		String cols = "(";
			
		for( int i=0; i<tc.length; i++ )
		{
			cols += tc[i].getName();
			
			if( i < tc.length-1 )
				cols += ",";
		}
		
		cols = cols + ")";
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		
		if( ti.getName().equals("PRIMARY") )
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" DROP PRIMARY KEY, ADD PRIMARY KEY " + cols );
		else
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" DROP INDEX "+ ti.getName() +", ADD "+ type +" "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + ti.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" "+ cols );
			
		tb.setIndexes( tb.getIndexes( tb ) );		
	}	

	public void dropIndex( Table tb, TableIndex ti ) throws Exception
	{
		dbc.getConnection().setCatalog( tb.getDatabase().getName() );

		if( ti.getName().equals("PRIMARY") )
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" DROP PRIMARY KEY" );
		else
			dbc.executeUpdate("ALTER TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" DROP INDEX "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + ti.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
		tb.setIndexes( tb.getIndexes( tb ) );
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
			nl.errorsoft.esqlmanager.domain.TableColumn tc = new nl.errorsoft.esqlmanager.domain.TableColumn( t );
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
	 * MySQL specific options
	 */
	public String optimizeTable( Table table ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( "OPTIMIZE TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + table.getName() + dbc.getConnectionProfile().getServerType().getFieldOpenChar() );
		String message = "";
		
		if( rs.first() )
			message = rs.getString("Msg_Text");
		
		rs.close();
		return message;
	}	 

	public String analyseTable( Table table ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( "ANALYZE TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + table.getName() + dbc.getConnectionProfile().getServerType().getFieldOpenChar() );
		String message = "";
		
		if( rs.first() )
			message = rs.getString("Msg_Text");
		
		rs.close();
		return message;
	}	 	
	
	public String checkTable( Table table ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( "CHECK TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + table.getName() + dbc.getConnectionProfile().getServerType().getFieldOpenChar() );
		String message = "";
		
		if( rs.first() )
			message = rs.getString("Msg_Text");
		
		rs.close();
		return message;
	}	 	

	public String repairTable( Table table ) throws Exception
	{
		ResultSet rs = dbc.executeQuery( "REPAIR TABLE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + table.getName() + dbc.getConnectionProfile().getServerType().getFieldOpenChar() );
		String message = "";
		
		if( rs.first() )
			message = rs.getString("Msg_Text");
		
		rs.close();
		return message;
	}	 		
}

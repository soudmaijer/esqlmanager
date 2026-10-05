package nl.errorsoft.esql.table;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.CreateColumn;
import nl.errorsoft.esql.domain.DataType;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.domain.dialect.Dialect;

/**
 * The only place that runs SQL for tables, their columns, indexes and rows.
 * What differs per database is asked from the {@link Dialect}.
 */
public class TableRepository extends AbstractRepository
{
	private static final Logger log = LogManager.getLogger( TableRepository.class );

	public TableRepository( DatabaseConnection dbc )
	{
		super( dbc );
	}

	// Structure

	public TableColumn[] loadColumns( Table table ) throws SQLException
	{
		useDatabaseOf( table );
		List<TableColumn> columns = new ArrayList<TableColumn>();
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();

		try ( ResultSet rs = dmd.getColumns( dbc.getConnection().getCatalog(), dbc.getSchema(), table.getName(), "%" ) )
		{
			while ( rs.next() )
			{
				TableColumn column = new TableColumn( table );
				column.setNativeTypeName( rs.getString( "TYPE_NAME" ) );
				column.setName( rs.getString( "COLUMN_NAME" ) );
				column.setSize( rs.getInt( "COLUMN_SIZE" ) );
				column.setNullable( rs.getBoolean( "NULLABLE" ) );
				column.setDefault( rs.getString( "COLUMN_DEF" ) );
				columns.add( column );
			}
		}

		try ( ResultSet rs = dmd.getPrimaryKeys( dbc.getConnection().getCatalog(), dbc.getSchema(), table.getName() ) )
		{
			while ( rs.next() )
			{
				for ( TableColumn column : columns )
				{
					if ( column.getName().equalsIgnoreCase( rs.getString( "COLUMN_NAME" ) ) )
					{
						column.setPrimary( true );
						break;
					}
				}
			}
		}

		return columns.toArray( new TableColumn[columns.size()] );
	}

	/** Loads the indexes of the table, its columns must be loaded first. */
	public TableIndex[] loadIndexes( Table table )
	{
		List<TableIndex> indexes = new ArrayList<TableIndex>();

		try
		{
			useDatabaseOf( table );
			DatabaseMetaData dmd = dbc.getConnection().getMetaData();
			String primaryKeyName = primaryKeyName( dmd, table );

			try ( ResultSet rs = dmd.getIndexInfo( dbc.getConnection().getCatalog(), dbc.getSchema(), table.getName(), false, false ) )
			{
				while ( rs.next() )
				{
					// Some drivers add a statistics row without an index name.
					String indexName = rs.getString( "INDEX_NAME" );
					if ( indexName == null )
						continue;

					// The primary key is always shown as PRIMARY, whatever name the server gave it.
					String name = indexName.equals( primaryKeyName ) ? "PRIMARY" : indexName;
					TableColumn column = table.getTableColumn( rs.getString( "COLUMN_NAME" ) );

					// Some drivers report columns that are not in the column list.
					if ( column == null )
						continue;

					TableIndex index = find( indexes, name );

					if ( index == null )
					{
						index = new TableIndex( table );
						index.setName( name );
						index.setUnique( !rs.getBoolean( "NON_UNIQUE" ) );
						index.setFulltext( "FULLTEXT".equalsIgnoreCase( rs.getString( "TYPE" ) ) );
						indexes.add( index );
					}

					column.setHasUniqueIndex( index.isUnique() );
					column.setIndexed( true );
					column.setIndexPosition( rs.getInt( "ORDINAL_POSITION" ) );
					index.addTableColumn( column );
				}
			}
		}
		catch ( Exception e )
		{
			log.error( e.getMessage(), e );
		}

		return indexes.toArray( new TableIndex[indexes.size()] );
	}

	private TableIndex find( List<TableIndex> indexes, String name )
	{
		for ( TableIndex index : indexes )
			if ( index.getName().equals( name ) )
				return index;
		return null;
	}

	private String primaryKeyName( DatabaseMetaData dmd, Table table ) throws SQLException
	{
		try ( ResultSet rs = dmd.getPrimaryKeys( dbc.getConnection().getCatalog(), dbc.getSchema(), table.getName() ) )
		{
			return rs.next() ? rs.getString( "PK_NAME" ) : null;
		}
	}

	public boolean exists( Database database, String name ) throws SQLException
	{
		useDatabase( database.getName() );

		try ( ResultSet rs = dbc.getConnection().getMetaData().getTables( dbc.getConnection().getCatalog(), dialect().getSchema( dbc ), name,
			new String[]{"TABLE"} ) )
		{
			return rs.next();
		}
	}

	public void create( Database database, String name, List<CreateColumn> columns, String type, String comment ) throws Exception
	{
		useDatabase( database.getName() );
		executeAll( dialect().createTableSql( name, columns, type, comment ) );
	}

	public void dropTable( Table table ) throws Exception
	{
		executeUpdate( "DROP TABLE " + quote( table.getName() ) );
	}

	public void flushTable( Table table ) throws Exception
	{
		executeUpdate( "DELETE FROM " + quote( table.getName() ) );
	}

	public void renameTable( Table table, String newName ) throws Exception
	{
		executeAll( dialect().renameTableSql( table.getName(), newName ) );
	}

	public void setTableType( Table table, String type ) throws Exception
	{
		executeAll( dialect().setTableTypeSql( table.getName(), type ) );
	}

	public void setTableComment( Table table, String comment ) throws Exception
	{
		executeAll( dialect().setTableCommentSql( table.getName(), comment ) );
	}

	public String maintain( Table table, Dialect.Maintenance maintenance ) throws Exception
	{
		return dialect().maintain( dbc, maintenance, table.getName() );
	}

	// Columns

	public void addColumn( Table table, CreateColumn column ) throws Exception
	{
		executeAll( dialect().addColumnSql( table.getName(), column ) );
	}

	public void modifyColumn( TableColumn old, CreateColumn column ) throws Exception
	{
		executeAll( dialect().modifyColumnSql( old.getTable().getName(), old.getName(), column ) );
	}

	public void dropColumn( TableColumn column ) throws Exception
	{
		executeUpdate( "ALTER TABLE " + quote( column.getTable().getName() ) + " DROP " + quote( column.getName() ) );
	}

	// Indexes

	public void addIndex( Table table, String name, String type, List<String> columns ) throws Exception
	{
		useDatabaseOf( table );
		executeAll( dialect().addIndexSql( table.getName(), name, type, columns ) );
	}

	public void modifyIndex( Table table, String name, String type, List<String> columns ) throws Exception
	{
		useDatabaseOf( table );
		executeAll( dialect().modifyIndexSql( dbc, table.getName(), name, type, columns ) );
	}

	public void dropIndex( Table table, String name ) throws Exception
	{
		useDatabaseOf( table );
		executeAll( dialect().dropIndexSql( dbc, table.getName(), name ) );
	}

	// Rows

	/** Reads one page of rows and sets the row count and the column metadata on the table. The columns must be loaded first. */
	public TableData[][] readPage( Table table, int skip, int show ) throws Exception
	{
		useDatabaseOf( table );
		TableColumn[] columns = table.getColumns();
		String quotedTable = quote( table.getName() );

		try ( ResultSet rs = dbc.executeQuery( "SELECT count(*) FROM " + quotedTable ) )
		{
			if ( rs.first() )
				table.setRowCount( rs.getInt( 1 ) );
		}

		// Let the server do the paging when the dialect can, otherwise skip rows in the result set.
		List<String> keyColumns = new ArrayList<String>();
		for ( TableColumn column : columns )
			if ( column.isPrimary() )
				keyColumns.add( quote( column.getName() ) );
		String pageQuery = dialect().selectPage( quotedTable, String.join( ", ", keyColumns ), skip, show );

		try ( ResultSet rs = pageQuery != null ? dbc.executeQuery( pageQuery ) : dbc.executeQuery( "SELECT * FROM " + quotedTable ) )
		{
			if ( pageQuery == null )
			{
				if ( skip > 0 )
					rs.absolute( skip );
				else
					rs.beforeFirst();
			}

			ResultSetMetaData rsmd = rs.getMetaData();

			for ( int i = 0; i < columns.length; i++ )
			{
				columns[i].setTypeName( rsmd.getColumnTypeName( i + 1 ) );
				columns[i].setClassName( rsmd.getColumnClassName( i + 1 ) );
				columns[i].setWritable( rsmd.isWritable( i + 1 ) );
				columns[i].setAutoIncrement( rsmd.isAutoIncrement( i + 1 ) );
				columns[i].setSigned( rsmd.isSigned( i + 1 ) );
				columns[i].setType( rsmd.getColumnType( i + 1 ) );
			}

			List<TableData[]> rows = new ArrayList<TableData[]>();

			while ( rs.next() )
			{
				TableData[] row = new TableData[columns.length];

				for ( int i = 0; i < row.length; i++ )
				{
					row[i] = new TableData();
					row[i].setTableColumn( columns[i] );

					if ( !columns[i].isBinary() )
						row[i].setData( rs.getObject( i + 1 ) );
				}

				rows.add( row );
			}

			return rows.toArray( new TableData[rows.size()][] );
		}
	}

	public void insertRow( Table table, TableData[] row ) throws Exception
	{
		List<String> names = new ArrayList<String>();
		List<String> values = new ArrayList<String>();

		for ( TableData cell : row )
		{
			names.add( quote( cell.getTableColumn().getName() ) );
			values.add( sqlValue( cell ) );
		}

		useDatabaseOf( table );
		executeUpdate( "INSERT INTO " + quote( table.getName() )
			+ " (" + String.join( ",", names ) + ") VALUES (" + String.join( ",", values ) + ")" );
	}

	public int updateCell( Table table, TableData[] row, TableData cell, String newValue ) throws Exception
	{
		String where = rowFilter( row );

		useDatabaseOf( table );
		return executeUpdate( "UPDATE " + quote( table.getName() )
			+ " SET " + quote( cell.getTableColumn().getName() ) + "=" + literal( newValue )
			+ " WHERE " + where );
	}

	public void deleteRow( Table table, TableData[] row ) throws Exception
	{
		String where = rowFilter( row );

		useDatabaseOf( table );
		executeUpdate( "DELETE FROM " + quote( table.getName() ) + " WHERE " + where );
	}

	/**
	 * The condition that selects the given row: its key columns when it has any,
	 * otherwise all of its columns that are not binary.
	 */
	public String rowFilter( TableData[] row ) throws Exception
	{
		List<String> keys = new ArrayList<String>();
		List<String> columns = new ArrayList<String>();

		for ( TableData cell : row )
		{
			TableColumn column = cell.getTableColumn();
			String name = quote( column.getName() );

			if ( column.isPrimary() || column.hasUniqueIndex() )
				keys.add( name + "=" + sqlValue( cell ) );
			else if ( !column.isBinary() )
				columns.add( cell.isNull() ? name + " IS NULL" : name + "=" + sqlValue( cell ) );
		}

		List<String> conditions = keys.isEmpty() ? columns : keys;

		if ( conditions.isEmpty() )
			throw new Exception( "The row can't be identified, it has no key and no column to compare." );

		return String.join( " AND ", conditions );
	}

	// Free queries

	public int run( String sql ) throws Exception
	{
		return executeUpdate( sql );
	}

	/** Runs a query and returns its result with one column object per result column. */
	public QueryResult query( String sql, boolean readOnly ) throws Exception
	{
		try ( ResultSet rs = dbc.executeQuery( sql ) )
		{
			ResultSetMetaData rsmd = rs.getMetaData();
			Database database = new Database( dbc.getConnection().getCatalog() );

			Table result = new Table( database );
			TableColumn[] columns = new TableColumn[rsmd.getColumnCount()];

			for ( int i = 0; i < columns.length; i++ )
			{
				Table source = new Table( database );
				source.setName( rsmd.getTableName( i + 1 ) );
				columns[i] = new TableColumn( source );
				columns[i].setName( rsmd.getColumnName( i + 1 ) );

				if ( readOnly )
					columns[i].setWritable( false );
			}

			result.setColumns( columns );
			rs.beforeFirst();
			List<TableData[]> rows = new ArrayList<TableData[]>();

			while ( rs.next() )
			{
				TableData[] row = new TableData[columns.length];

				for ( int i = 0; i < row.length; i++ )
				{
					row[i] = new TableData();
					row[i].setData( rs.getObject( i + 1 ) );
					row[i].setTableColumn( columns[i] );
				}

				rows.add( row );
			}

			result.setRowCount( rows.size() );
			return new QueryResult( result, rows.toArray( new TableData[rows.size()][] ) );
		}
	}

	// Plumbing

	private void useDatabaseOf( Table table ) throws SQLException
	{
		useDatabase( table.getDatabase().getName() );
	}

	/** The value of a cell as it goes into a statement, an empty cell is NULL and not the text "null". */
	private String sqlValue( TableData cell )
	{
		return cell.isNull() ? "NULL" : literal( cell.getData() );
	}
}

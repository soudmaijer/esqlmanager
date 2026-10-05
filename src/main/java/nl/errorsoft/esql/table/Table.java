package nl.errorsoft.esql.table;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;

/** A table or view: plain data, loaded and changed through the table service. */
public class Table 
{
	private String name;
	private String type;
	private String comment;
	private int rows = 0;
	private Database db;
	private TableColumn tca[];
	private TableIndex tia[];

	public Table( Database db ) 
	{
		this.db = db;
	}

	public void setColumns( TableColumn [] tca )
	{
		this.tca = tca;
	}

	public TableColumn [] getColumns()
	{
		return this.tca;
	}

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
}

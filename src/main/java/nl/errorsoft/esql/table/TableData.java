//Source file: d:\\roseoutput\\esql\\esql\\table\\TableRow.java

package nl.errorsoft.esql.table;

import nl.errorsoft.esql.data.*;

import nl.errorsoft.esql.domain.*;

public class TableData
{
	private Object data;
	private boolean newRow = false;
	private boolean nullData = true;
	private nl.errorsoft.esql.table.TableColumn tc;

	/**
	* @roseuid 3E05A70B0344
	*/
	public TableData()
	{
	}

	public void setTableColumn( nl.errorsoft.esql.table.TableColumn tc )
	{
		this.tc = tc;
	}

	public TableColumn getTableColumn()
	{
		return tc;
	}

	public void setData( Object data )
	{
		if ( data != null )
			nullData = false;
		this.data = data;
	}

	public void setNewRow( boolean newRow )
	{
		this.newRow = newRow;
	}

	public boolean isNewRow()
	{
		return newRow;
	}

	public String getData()
	{
		if ( data == null )
			return "null";
		else
			return data.toString();
	}

	public Object getNativeData()
	{
		if ( data == null )
			return "null";
		else
			return data;
	}

	public String toString()
	{
		if ( tc.isBinary() )
			return "[BINARY]";
		else if ( data == null )
			return "null";
		else
			return data.toString();
	}

	public boolean isNull()
	{
		return nullData;
	}
}

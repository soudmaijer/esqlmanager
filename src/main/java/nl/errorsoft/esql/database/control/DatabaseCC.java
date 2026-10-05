//Source file: d:\\roseoutput\\esql\\esql\\database\\DatabaseCC.java

package nl.errorsoft.esql.database.control;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
import nl.errorsoft.esql.ui.ImageLoader;

import nl.errorsoft.esql.table.*;
import nl.errorsoft.esql.table.ui.*;

import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.database.*;
import java.util.Vector;

public class DatabaseCC
{
	private ConnectionWindowCC cwcc;

	/**
	* @roseuid 3E05A70C031D
	*/
	public DatabaseCC( ConnectionWindowCC cwcc )
	{
		this.cwcc = cwcc;
	}

	public java.util.Vector getDatabases() throws Exception
	{
		return new Vector<Database>( service().getDatabases() );
	}

	public Database createDatabase( String name ) throws Exception
	{
		return service().createDatabase( name );
	}

	public void dropDatabase( Database data ) throws Exception
	{
		service().dropDatabase( data );
	}

	public java.util.Vector getTables( Database database ) throws Exception
	{
		return new Vector<Table>( service().getTables( database ) );
	}

	private DatabaseService service() throws Exception
	{
		return new DatabaseService( cwcc.getDatabaseConnection() );
	}

	public DatabaseTreeView getDatabaseTreeView() throws Exception
	{
		DatabaseTreeView dbtv = new DatabaseTreeView( this, cwcc.getTitle() );
		dbtv.loadDatabases( getDatabases() );
		return dbtv;
	}

	public TableListView getTableListView( Vector tables ) throws Exception
	{
		TableListView tlv = new TableListView( this );
		tlv.loadDatabases( tables );
		return tlv;
	}

	public void tableSelected( Table table )
	{
		cwcc.selectTableInTree( table );
	}

	public ImageLoader getImageLoader()
	{
		return cwcc.getImageLoader();
	}
}

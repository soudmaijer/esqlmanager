//Source file: d:\\roseoutput\\esql\\esql\\database\\DatabaseCC.java

package esql.control;

import esql.domain.*;
import esql.gui.*;
import esql.data.*;
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
  		Database db = new Database( cwcc.getDatabaseConnection() );
  		return db.getDatabases();
	}
	
	public Database createDatabase( String name ) throws Exception
   {
  		Database db = new Database( cwcc.getDatabaseConnection() );
  		return db.createDatabase( name );
	}	

   public void dropDatabase( Database data ) throws Exception
   {
  		Database db = new Database( cwcc.getDatabaseConnection() );
  		db.dropDatabase( data );
	}	
   public java.util.Vector getTables( Database database ) throws Exception
   {
  		Database db = new Database( cwcc.getDatabaseConnection() );
  		return db.getTables( database );
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

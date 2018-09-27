package esql.control;

import esql.domain.*;
import esql.gui.*;

import java.util.Vector;

public class UserManagerCC
{	
	private ConnectionWindowCC cwcc;
	private UserManager um;
	
	public UserManagerCC ( ConnectionWindowCC cwcc )
	{
		this.cwcc = cwcc;
		this.um = new UserManager( cwcc );
	}
	
	public void startUI( ESQLManagerUI emui )
	{
		try
		{
			if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	
			emui.updateStatus("Starting usermanager...", true );
			UserManagerUI up = new UserManagerUI( emui, cwcc );
			emui.updateStatus("Ready...", false );
			up.show();
		}
		catch( Exception e )
		{
		}
	}
	
	public Vector getUserAccounts ()
	{
		return um.getUserAccounts();
	}
	
	public String createUserAccount ( User u, boolean grant)
	{
		return um.addUser( u, grant );
	}
	
	public String editUserAccount( User nw, User old)
	{
		return um.editUserAccount(nw, old);
	}
	
	public String deleteUserAccount( User u )
	{
		return um.deleteUserAccount( u );
	}	
	
   public void showTables( UserManagerUI um, Database db )
   {
		try
		{
   		DatabaseCC dbcc = new DatabaseCC( cwcc );
   		um.getDatabaseTreeView().loadTables( db, dbcc.getTables( db ) );
	   }
	   catch( Exception e )
	   {
	   	e.printStackTrace();
	   }   	
   } 
  
   public void showFields( UserManagerUI um, Table tb )
   {
		try
		{
   		TableCC tbcc = new TableCC( cwcc );
   		um.getDatabaseTreeView().loadTableColumns( tb, tbcc.getColumns(tb) );
	   }
	   catch( Exception e )
	   {
	   	e.printStackTrace();
	   }   	
   }    
     
	public void initTree( UserManagerUI umui )
   {
		try
		{
			DatabaseCC dbcc = new DatabaseCC( cwcc );
	   	umui.showDatabaseTreeView( dbcc.getDatabaseTreeView() );
	   }
	   catch( Exception e )
	   {
	   	e.printStackTrace();
	   }
   }
   
   public boolean updateGrants( User u, Vector grants )
   {	
   	return um.updateGrants(u, grants);
   }
}
package esql.control;

import esql.domain.*;
import esql.gui.*;
import esql.data.*;
import java.util.Vector;

public class CreateTableCC
{	private ConnectionWindowCC cwcc;
	
	public CreateTableCC ( ConnectionWindowCC cwcc )
	{	
		try
		{	if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
		this.cwcc = cwcc;
	}
	
	/* 
	 	List all databases, so user can choose database to create table on
	*/
	public Vector getDatabases ()
	{	try
		{
			DatabaseCC dbc = new DatabaseCC(cwcc);
			return dbc.getDatabases();
		}
		catch(Exception e)
		{	e.printStackTrace();
			return null;
		}
	}
	
	/* 
	 	List all tabletypes
	 	TODO make this work for every DBMS
    */
	public String [] getTableTypes ()
	{	return cwcc.getConnectionProfile().getServerType().getMySQLTableTypes();
	}
	
	/* 
	 	List all datatypes
    */
	public DataType [] getDatatypes ()
	{	return cwcc.getConnectionProfile().getServerType().getDataTypes();
	}
	
	/*
		Save and close, moet in de domein class!!!!!!!!!
	*/
	public void createTable ( String name, String database, String comment, String type, CreateTable ct, Vector columns )
	{	if(name.trim().length() == 0)
		{	ct.showErrorMessage("Tablename missing. You must enter a tablename in order to create a table.");
			return;
		}
		if(columns.size() == 0)
		{	ct.showErrorMessage("You didn't add any columns to the table. Please add some fields to the table prior to generating it.");
			return;
		}
		String query = "CREATE TABLE `" + name + "` ";
		query = query + " (";
		for(int i = 0; i < columns.size(); i++)
		{	if(i != 0)	{	query = query + ", ";	}
			CreateColumn f = (CreateColumn)columns.get(i);
			query = query + " `" + f.name + "` " ;
			query = query + f.type;
			if(!f.length.trim().equals("")) { query = query + " (" + f.length + ")";  }
			if(f.unsigned){ query = query + " UNSIGNED"; }
			if(!f.defaultval.trim().equals("")) {query = query + " DEFAULT '" + f.defaultval + "'"; }
			if(f.notnull){ query = query + " NOT NULL"; }
			if(f.zerofill){ query = query + " ZEROFILL"; }
			if(f.binary){ query = query + " BINARY"; }
			if(f.autoincrement){ query = query + " AUTO_INCREMENT"; }
			if(f.primary){ query = query + ", PRIMARY KEY(`" + f.name + "`)"; }
			if(f.unique){ query = query + ", UNIQUE(`" + f.name + "`)"; }
			if(f.index){ query = query + ", INDEX(`" + f.name + "`)"; }
		}	
		query = query + ") TYPE=" + type;
		if(!comment.trim().equals(""))
		{	query = query + " COMMENT='" + comment + "'";
		}	
		
		try
		{	cwcc.getDatabaseConnection().executeUpdate(query);
			ct.dispose();
			cwcc.reloadSelectedDatabase();
		}
		catch(Exception e)
		{	ct.showErrorMessage(e.getMessage());
		}
	}
	
	public void modifyTable( CreateTable ct, Table t, String tableName, String tableType, String tableComment )
	{
		try
		{	
			t.modifyTable( t, tableName, tableType, tableComment );
			ct.dispose();
			//cwcc.reloadSelectedDatabase();
		}
		catch(Exception e)
		{	ct.showErrorMessage(e.getMessage());
		}
	}
	
	/*
		Close
	*/
	public void closeDialog( CreateTable ct )
	{	ct.dispose();
	}
}
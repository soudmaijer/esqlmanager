//Source file: d:\\roseoutput\\esql\\esql\\database\\Database.java

package nl.errorsoft.esql.domain;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import java.util.*;
import java.sql.*;

public class Database
{
   private String name = "";
   private DatabaseConnection dbc;
   private String [] requestDatabases;
   
   /**
    * @roseuid 3E05A70C03A9
    */
   public Database( DatabaseConnection dbc ) 
   {
		this.dbc = dbc;
		StringTokenizer st = new StringTokenizer( dbc.getConnectionProfile().getDatabases(), ",", false );
		requestDatabases = new String[ st.countTokens() ];
		
		if( st.countTokens() > 0 )		
		{
			int i=0;
			
			while( st.hasMoreTokens() )
			{
				requestDatabases[i] = st.nextToken().trim();
				i++;
			}
		}		
	}
	
	public Database createDatabase( String name ) throws Exception
	{
		dbc.executeUpdate( "CREATE DATABASE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + name + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
		Database temp = new Database( dbc );
		temp.setName( name );
		return temp;
	}

	public void dropDatabase( Database db ) throws Exception
	{
		dbc.executeUpdate( "DROP DATABASE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + db.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
	}	
	
	public Vector getDatabases() throws Exception
	{
		Vector v = new Vector();
		Vector results = new Vector();
		
		for( String databaseName : dbc.getConnectionProfile().getServerType().getDialect().listDatabases( dbc ) )
		{
			Database temp = new Database( dbc );
			temp.setName( databaseName );
			v.add( temp );
		}
		
		if( requestDatabases.length > 0 )		
		{
			for( int i=0; i<v.size(); i++ )
			{
				Database d = (Database)v.get(i);
				
				for( int j=0; j<requestDatabases.length; j++)
				{
					if( d.getName().equalsIgnoreCase( requestDatabases[j] ) )
					{
						results.add( d );
						break;
					}
				}
			}
			return results;
		}
		return v;
	}
	
	public Vector getTables( Database db ) throws Exception
	{
		dbc.useDatabase( db.getName() );
		return dbc.getConnectionProfile().getServerType().getDialect().listTables( dbc, db );
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
}

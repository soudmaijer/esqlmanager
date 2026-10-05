//Source file: d:\\roseoutput\\esql\\esql\\database\\Database.java

package nl.errorsoft.esql.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import java.util.*;
import java.sql.*;

public class Database
{
	private static final Logger log = LogManager.getLogger( Database.class );

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
		dbc.executeUpdate( "CREATE DATABASE "+ quote( name ) );
		Database temp = new Database( dbc );
		temp.setName( name );
		return temp;
	}

	public void dropDatabase( Database db ) throws Exception
	{
		dbc.getConnectionProfile().getServerType().getDialect().dropDatabase( dbc, db.getName() );
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
		
		log.info( "Found {} database(s) on the server", v.size() );
		
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
			log.info( "Showing {} database(s) matching the profile filter: {}", results.size(), String.join( ", ", requestDatabases ) );
			return results;
		}
		return v;
	}
	
	public Vector getTables( Database db ) throws Exception
	{
		dbc.useDatabase( db.getName() );
		Vector tables = dbc.getConnectionProfile().getServerType().getDialect().listTables( dbc, db );
		log.info( "Database {}: {} table(s)", db.getName(), tables.size() );
		return tables;
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

	private String quote( String identifier )
	{
		return dbc.getConnectionProfile().getServerType().getDialect().quote( identifier );
	}
}
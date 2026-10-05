//Source file: d:\\roseoutput\\esql\\esql\\database\\Database.java

package nl.errorsoft.esqlmanager.domain;

import nl.errorsoft.esqlmanager.data.*;
import nl.errorsoft.esqlmanager.domain.*;
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
	
	/*
	 * @description: Needs to be implemented for all databases types!
	 */
	public Vector getDatabases() throws Exception
	{
		Vector v = new Vector();
		Vector results = new Vector();
		ResultSet rs = null;

		// Postgres.
		if( dbc.getConnectionProfile().getServerType().getType() == ServerType.POSTGRES )
		{
			Database temp = new Database( dbc );
			temp.setName( dbc.getConnectionProfile().getDatabases() );
			v.add( temp );
			return v;
		}		
		// Oracle implementation.
		else if( dbc.getConnectionProfile().getServerType().getType() == ServerType.ORACLE )
		{
			Database temp = new Database( dbc );
			temp.setName( dbc.getConnectionProfile().getDatabases() );
			v.add( temp );
			return v;
		}
		// SQL Server implementation.
		else if( dbc.getConnectionProfile().getServerType().getType() == ServerType.MS_SQL_SERVER )
		{
			CallableStatement cs = dbc.getConnection().prepareCall("{call sp_databases}", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY );
			rs = cs.executeQuery();
		}
		// MySQL implementation.
		else if( dbc.getConnectionProfile().getServerType().getType() == ServerType.MY_SQL )
		{
			rs = dbc.executeQuery("SHOW DATABASES");
		}
		
		while( rs.next() )
		{
			Database temp = new Database( dbc );
			temp.setName( rs.getString(1) );
			v.add( temp );
		}
		
		rs.close();
		
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
		Vector v = new Vector();
		dbc.getConnection().setCatalog( db.getName() );
		/*
		 * MySQL specific implementation, we use this because we want
		 * specific MySQL table information.
		 */
		if( dbc.getConnectionProfile().getServerType().getType() == ServerType.MY_SQL )
		{
			ResultSet rs = dbc.executeQuery( "SHOW TABLE STATUS" );
			
			while( rs.next() )
			{
				Table temp = new Table( dbc, db );
				temp.setName( rs.getString("Name") );
				temp.setType( rs.getString("Type") );
				temp.setRowCount( rs.getInt("Rows") );
				temp.setComment( rs.getString("Comment") );
				v.add( temp );			
			}
			
			rs.close();
		}
		/*
		 * Default JDBC implementation.
		 */
		else
		{
			DatabaseMetaData dmd = dbc.getConnection().getMetaData();
			ResultSet rs = dmd.getTables( db.getName(), null, null, null );
			
			while( rs.next() )
			{
				String type = rs.getString("TABLE_TYPE");
				
				if( type.equalsIgnoreCase("TABLE") || type.equalsIgnoreCase("VIEW") )
				{
					Table temp = new Table( dbc, db );
					temp.setName( rs.getString("TABLE_NAME") );
					temp.setType( type );
					
					try
					{
						temp.setComment( rs.getString("REMARKS") );
					}
					catch( Exception e )
					{
					}
					
					v.add( temp );
				}
			}
			
			rs.close();
			
			for( int i=0; i<v.size(); i++ )
			{
				try
				{
					rs = dbc.executeQuery("SELECT count(*) AS cnt FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + ((Table)v.get(i)).getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() );
					
					if( rs.first() )
						((Table)v.get(i)).setRowCount( rs.getInt("cnt") );
				
					rs.close();
				}
				catch( Exception e )
				{
					//e.printStackTrace();
				}
			}			
		}
		
		return v;
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

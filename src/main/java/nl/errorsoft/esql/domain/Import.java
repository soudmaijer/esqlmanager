package nl.errorsoft.esql.domain;

import java.sql.*;
import nl.errorsoft.esql.data.*;
import java.util.*;
import java.io.*;

public class Import extends Observable implements Runnable
{
	private DatabaseConnection dbc;
	private Object importToDatabase;
	private String file;
	
	public Import( DatabaseConnection dbc, Object importToDatabase, String file )
	{
		this.dbc = dbc;
		this.importToDatabase = importToDatabase;
		this.file = file;
	}
	
	public void run()
	{
		try
		{
			setChanged();
			notifyObservers( new Integer(10) );
			String db = "";
			
			if( importToDatabase != null && importToDatabase instanceof Database )
			{
				db = ((Database)importToDatabase).getName();
				dbc.useDatabase( db );
			}
				
			try( BufferedReader f = new BufferedReader( new InputStreamReader( new FileInputStream( file ), java.nio.charset.StandardCharsets.UTF_8 ) ) )
			{
				String s = "";
				String sql = "";
				
				while( ( s = f.readLine() ) != null )
				{
					// A script switches database with the psql meta command, it is not SQL a server understands.
					if( sql.length() == 0 && s.startsWith( "\\connect " ) )
					{
						dbc.useDatabase( s.substring( "\\connect ".length() ).replaceAll( "^[\"`]|[\"`];?$", "" ) );
						continue;
					}
					
					sql += s +"\n";
					
					if( s.endsWith(";") )
					{	
						dbc.executeUpdate( sql );
						sql = "";
					}
				}
			}
	
	 		setChanged();
	 		notifyObservers( new Integer(100) );			
		}
		catch( Exception e )
		{
			setChanged();
			notifyObservers( e );		
		}
	}
	
	public void start()
	{
		Thread t = new Thread( this );
		t.start();
	}
}

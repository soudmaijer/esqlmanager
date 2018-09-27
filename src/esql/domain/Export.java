package esql.domain;

import java.sql.*;
import esql.data.*;
import java.util.*;
import java.io.*;

public class Export extends Observable implements Runnable
{
	private DatabaseConnection dbc;
	private Object [] exportObject;
	private String file;
	private boolean dumpStructure;
	private boolean dumpData;
	private boolean createDatabase;
	private boolean dropTable;
	private boolean useDatabase;
	
	public Export( DatabaseConnection dbc, Object [] exportObject, String file, boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase )
	{
		this.dbc = dbc;
		this.exportObject = exportObject;
		this.file = file;
		this.dumpStructure = dumpStructure;
		this.dumpData = dumpData;
		this.createDatabase = createDatabase;
		this.dropTable = dropTable;
		this.useDatabase = useDatabase;	
	}
	
	public void run()
	{
		try
		{
			setChanged();
			notifyObservers( new Integer(10) );
			PrintWriter pw = new PrintWriter( new FileOutputStream( new File( file ) ) );
		
			for( int i=0; i<exportObject.length; i++ )
			{
				if( exportObject[i] instanceof Database )
				{
					Database to = (Database)exportObject[i];
					
					if( createDatabase )
						pw.println( "CREATE DATABASE IF NOT EXISTS `"+ to.getName() +"`;\n" );
					
					if( useDatabase )
						pw.println( "USE `"+ to.getName() +"`;\n" );
	
					dbc.getConnection().setCatalog( to.getName() );
					ResultSet rs = dbc.executeQuery("SHOW TABLES FROM `"+ to.getName() +"`");
					
					while( rs.next() )
					{
						if( dropTable )
							pw.println( "DROP TABLE IF EXISTS `"+ rs.getString(1) +"`;\n" );
					
						if( dumpStructure )
						{
							ResultSet show =  dbc.executeQuery("SHOW CREATE TABLE `"+ rs.getString(1) +"`");
							show.first();
							pw.println( show.getString(2) +";\n" );
							show.close();
						}
					
						if( dumpData )
						{
							ResultSet show = dbc.executeQuery("SELECT * FROM `"+ rs.getString(1) +"`" );
	
							while( show.next() )
							{
								ResultSetMetaData rsm = show.getMetaData();
								pw.print( "INSERT INTO `"+ rs.getString(1) +"` VALUES(" );
								
								for( int d=1; d<rsm.getColumnCount()+1; d++ )
								{	
									TableColumn temp = new TableColumn(null);
									temp.setType( rsm.getColumnType(d) );
								
									if( temp.isBinary() )
										pw.print("\"\"");
									else if( show.getString(d) != null )
										pw.print( "\""+ show.getString(d).replaceAll("\\\\", "\\\\\\\\").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r").replaceAll("\"", "\\\\\"") +"\"" );
									else
										pw.print( "\"null\"" );
	
									if( d < rsm.getColumnCount() )
										pw.print(",");
								}	
								
								pw.println( ");" );
							}
						}		
					}
				}
				else if( exportObject[i] instanceof Table )
				{
					Table to = (Table)exportObject[i];
					String db = to.getDatabase().getName();
					dbc.getConnection().setCatalog( db );
	
					if( createDatabase )
						pw.println( "CREATE DATABASE IF NOT EXISTS `"+ db +"`;\n" );
					
					if( useDatabase )
						pw.println( "USE `"+ db +"`;\n" );
					
					if( dropTable )
						pw.println( "DROP TABLE IF EXISTS `"+ to.getName() +"`;\n" );
	
					if( dumpStructure )
					{
						ResultSet show = dbc.executeQuery("SHOW CREATE TABLE `"+ to.getName() +"`");
						show.first();
						pw.println( show.getString(2) +";\n" );
						show.close();
					}
	
					if( dumpData )
					{
						ResultSet show = dbc.executeQuery("SELECT * FROM `"+ to.getName() +"`" );
	
						while( show.next() )
						{
							ResultSetMetaData rsm = show.getMetaData();
							pw.print( "INSERT INTO `"+ to.getName() +"` VALUES(" );
							
							for( int d=1; d<rsm.getColumnCount()+1; d++ )
							{	
								TableColumn temp = new TableColumn(null);
								temp.setType( rsm.getColumnType(d) );
								
								if( temp.isBinary() )
									pw.print("\"\"");
								else if( show.getString(d) != null )
									pw.print( "\""+ show.getString(d).replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r").replaceAll("\"", "\\\\\"") +"\"" );
								else
									pw.print( "\"null\"" );
								
								if( d < rsm.getColumnCount() )
									pw.print(",");
							}	
							
							pw.println( ");" );
						}
					}								
				}
				
				setChanged();
	 			notifyObservers( new Integer( ((100/exportObject.length)*(i+1))-1 ) );
			}
			pw.close();
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

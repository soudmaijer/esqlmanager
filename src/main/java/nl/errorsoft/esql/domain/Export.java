package nl.errorsoft.esql.domain;

import java.sql.*;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.dialect.Dialect;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

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
		try( PrintWriter pw = new PrintWriter( file, StandardCharsets.UTF_8 ) )
		{
			setChanged();
			notifyObservers( new Integer(10) );
		
			for( int i=0; i<exportObject.length; i++ )
			{
				String database;
				List<String> tables = new ArrayList<String>();
				
				if( exportObject[i] instanceof Database )
				{
					Database to = (Database)exportObject[i];
					database = to.getName();
					dbc.useDatabase( database );
					
					for( Table table : getDialect().listTables( dbc, to ) )
					{
						// A view has no structure or data of its own to dump.
						if( !"VIEW".equalsIgnoreCase( table.getType() ) )
							tables.add( table.getName() );
					}
				}
				else if( exportObject[i] instanceof Table )
				{
					Table to = (Table)exportObject[i];
					database = to.getDatabase().getName();
					dbc.useDatabase( database );
					tables.add( to.getName() );
				}
				else
					continue;
				
				if( createDatabase )
					pw.println( getDialect().createDatabaseSql( database ) +";\n" );
				
				if( useDatabase )
					pw.println( getDialect().useDatabaseSql( database ) +";\n" );

				for( String table : tables )
					dumpTable( pw, table );
				
				setChanged();
	 			notifyObservers( new Integer( ((100/exportObject.length)*(i+1))-1 ) );
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
	
	private void dumpTable( PrintWriter pw, String table ) throws SQLException
	{
		Dialect dialect = getDialect();
		
		if( dropTable )
			pw.println( "DROP TABLE IF EXISTS "+ dialect.quote( table ) +";\n" );
		
		if( dumpStructure )
			pw.println( dialect.createTableDdl( dbc, table ) +";\n" );
		
		if( dumpData )
		{
			ResultSet rs = dbc.executeQuery( "SELECT * FROM "+ dialect.quote( table ) );
			ResultSetMetaData rsm = rs.getMetaData();
			
			while( rs.next() )
			{
				pw.print( "INSERT INTO "+ dialect.quote( table ) +" VALUES(" );
				
				for( int d=1; d<=rsm.getColumnCount(); d++ )
				{	
					TableColumn temp = new TableColumn(null);
					temp.setType( rsm.getColumnType(d) );
					
					if( temp.isBinary() )
						pw.print( "''" );
					else if( rs.getString(d) != null )
						pw.print( dialect.literal( rs.getString(d) ) );
					else
						pw.print( "NULL" );
					
					if( d < rsm.getColumnCount() )
						pw.print(",");
				}	
				
				pw.println( ");" );
			}
			rs.close();
		}
	}
	
	private Dialect getDialect()
	{
		return dbc.getConnectionProfile().getServerType().getDialect();
	}
	
	public void start()
	{
		Thread t = new Thread( this );
		t.start();
	}
}


package nl.errorsoft.esql.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import nl.errorsoft.esql.data.*;
import java.util.*;
import java.io.*;

public class UDData extends Observable
{
	private static final Logger log = LogManager.getLogger( UDData.class );

	private DatabaseConnection dbc;
	
	public UDData( DatabaseConnection dbc )
	{
		this.dbc = dbc;
	}

	public void uploadData( Table tb, TableData [] rowData, TableData tc, String file ) throws Exception
	{
		String sqlWhere = tb.rowFilter( rowData );

		byte [] temp = java.nio.file.Files.readAllBytes( new File(file).toPath() );
		log.debug( "Read {} bytes from {}", temp.length, file );

		dbc.useDatabase( tb.getDatabase().getName() );
		try( ByteArrayInputStream bai = new ByteArrayInputStream( temp );
			java.sql.PreparedStatement pstmt = dbc.getConnection().prepareStatement("UPDATE "+ dbc.getConnectionProfile().getServerType().getDialect().quote( tb.getName() ) +" SET "+ dbc.getConnectionProfile().getServerType().getDialect().quote( tc.getTableColumn().getName() ) +" = ? WHERE "+ sqlWhere ) )
		{
			pstmt.setBinaryStream(1, bai, temp.length);
			pstmt.execute();
		}
 		setChanged();
 		notifyObservers( Integer.valueOf(100) );		
	}	
	
	public void downloadData( Table tb, TableData [] rowData, TableData tc, String file ) throws Exception
	{
		String sqlWhere = tb.rowFilter( rowData );

		dbc.useDatabase( tb.getDatabase().getName() );
		java.sql.ResultSet rs = dbc.executeQuery("SELECT * FROM "+ dbc.getConnectionProfile().getServerType().getDialect().quote( tb.getName() ) +" WHERE "+ sqlWhere );
		BufferedInputStream bis = null;
		
		if( rs.first() )
		{	bis = new BufferedInputStream( rs.getBinaryStream( tc.getTableColumn().getName() ) );
		}		
		
 		try
 		{
	    	try( BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(file))) )
	    	{	bis.transferTo( bos );
	    	}
	    	finally
	    	{	bis.close();
	    	}
	 	}
	 	catch( Exception e )
	 	{
	 		log.error( "Can't save {}: {}", file, e.getMessage(), e );
	 	}
 		
 		setChanged();
 		notifyObservers( Integer.valueOf(100) );
	}	
}

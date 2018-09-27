
package nl.errorsoft.esqlmanager.domain;

import java.sql.*;
import nl.errorsoft.esqlmanager.data.*;
import java.util.*;
import java.io.*;

public class UDData extends Observable
{
	private DatabaseConnection dbc;
	
	public UDData( DatabaseConnection dbc )
	{
		this.dbc = dbc;
	}

	public void uploadData( Table tb, TableData [] rowData, TableData tc, String file ) throws Exception
	{
		String sqlWhere = "";
		boolean keySearch = false;
		
		for( int i=0; i<rowData.length; i++ )
		{
			if( rowData[i].getTableColumn().isPrimary() || rowData[i].getTableColumn().hasUniqueIndex() )
			{
				if( i<rowData.length-1 && sqlWhere.length() > 0 )
					sqlWhere += " AND ";				

				sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				keySearch = true;
			}
			else if( !keySearch )
			{
				if( !rowData[i].getTableColumn().isBinary() )
				{	sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				
					if( i<rowData.length-1 )
						sqlWhere += " AND ";
				}
			}
		}

		FileInputStream fis = new FileInputStream(new File(file));
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		int aByte;
		
		while( (aByte=fis.read()) != -1 )
			outStream.write( aByte );

		byte [] temp = outStream.toByteArray();
		System.out.println( temp.length );
		ByteArrayInputStream bai = new ByteArrayInputStream( temp );

		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		java.sql.PreparedStatement pstmt = dbc.getConnection().prepareStatement("UPDATE "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" SET "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tc.getTableColumn().getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" = ? WHERE "+ sqlWhere );
		pstmt.setBinaryStream(1, bai, temp.length);
		pstmt.execute();
		pstmt.close();
		fis.close();
		bai.close();
 		setChanged();
 		notifyObservers( new Integer(100) );		
	}	
	
	public void downloadData( Table tb, TableData [] rowData, TableData tc, String file ) throws Exception
	{
		String sqlWhere = "";
		boolean keySearch = false;
		
		for( int i=0; i<rowData.length; i++ )
		{
			if( rowData[i].getTableColumn().isPrimary() || rowData[i].getTableColumn().hasUniqueIndex() )
			{
				if( i<rowData.length-1 && sqlWhere.length() > 0 )
					sqlWhere += " AND ";				

				sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				keySearch = true;
			}
			else if( !keySearch )
			{
				if( !rowData[i].getTableColumn().isBinary() )
				{	sqlWhere += rowData[i].getTableColumn().getName() +"="+ dbc.formatFieldValue( rowData[i].getData() ) +"";
				
					if( i<rowData.length-1 )
						sqlWhere += " AND ";
				}
			}
		}

		dbc.getConnection().setCatalog( tb.getDatabase().getName() );
		java.sql.ResultSet rs = dbc.executeQuery("SELECT * FROM "+ dbc.getConnectionProfile().getServerType().getFieldOpenChar() + tb.getName() + dbc.getConnectionProfile().getServerType().getFieldCloseChar() +" WHERE "+ sqlWhere );
		BufferedInputStream bis = null;
		
		if( rs.first() )
		{	bis = new BufferedInputStream( rs.getBinaryStream( tc.getTableColumn().getName() ) );
		}		
		
 		try
 		{
	    	BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(file)));
    
	    	int aByte;
	    	while ((aByte = bis.read()) != -1) 
	    	{ bos.write(aByte);
	    	}
    
	    	bos.flush();
	    	bos.close();
	    	bis.close();
	 	}
	 	catch( Exception e )
	 	{
	 	}
 		
 		setChanged();
 		notifyObservers( new Integer(100) );
	}	
}

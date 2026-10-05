package nl.errorsoft.esql.blob;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.errorsoft.esql.data.AbstractRepository;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Reads and writes the binary content of one cell, the row is selected by a ready made condition. */
public class BlobRepository extends AbstractRepository
{
	public BlobRepository( DatabaseConnection dbc )
	{
		super( dbc );
	}

	public void write( Table table, String column, String rowCondition, InputStream content, int length ) throws SQLException
	{
		useDatabase( table.getDatabase().getName() );

		try( PreparedStatement statement = dbc.getConnection().prepareStatement(
			"UPDATE "+ quote( table.getName() ) +" SET "+ quote( column ) +" = ? WHERE "+ rowCondition ) )
		{
			statement.setBinaryStream( 1, content, length );
			statement.execute();
		}
	}

	/** Copies the content to the stream, returns false when no row matched. */
	public boolean read( Table table, String column, String rowCondition, OutputStream target ) throws Exception
	{
		useDatabase( table.getDatabase().getName() );

		try( ResultSet rs = dbc.executeQuery( "SELECT * FROM "+ quote( table.getName() ) +" WHERE "+ rowCondition ) )
		{
			if( !rs.first() )
				return false;

			try( InputStream content = rs.getBinaryStream( column ) )
			{
				content.transferTo( target );
			}
			return true;
		}
	}
}

package esql.data;

import esql.domain.*;
import java.sql.*;
import java.io.*;

public class DatabaseConnection
{
	private String driver	= "";
	private String url		= "";
	private String database = "";

	private ConnectionProfile cp;
	private Connection connection;
	private Statement statement;
	private PreparedStatement preparedStatement;

	// Connection types.
	private int connectionType = -1;

	public DatabaseConnection()
	{
	}

	// Set the database url.
	public void setUrl( String url )
	{
		this.url = url;
	}

	// Set the database driver.
	public void setDriver( String driver )
	{
		this.driver = driver;
	}
	
	public void setConnectionProfile( ConnectionProfile cp )
	{
		this.cp = cp;
	}	
	
	public ConnectionProfile getConnectionProfile()
	{
		return cp;
	}

	// Connect to given database.
	public void connect( ConnectionProfile _cp, String database ) throws Exception, SQLException
	{
		this.cp = _cp;
		this.database 	= database;
		this. url = cp.getServerType().getConnectionURL( cp );
		
		Class.forName( cp.getServerType().getDriverName() ).newInstance();
		connection = java.sql.DriverManager.getConnection( url, cp.getUsername(), cp.getPassword() );
	}

	// Execute a query.
	public java.sql.ResultSet executeQuery( String query ) throws java.sql.SQLException
	{
		System.out.println( query );
		statement = connection.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );
		return statement.executeQuery( query );
	}

	public int executeUpdate( String query ) throws java.sql.SQLException
	{
		System.out.print( query );
		statement = connection.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE );
		int i = statement.executeUpdate( query );
		System.out.println( " [ "+ i +" row(s) updated ] " );
		return i;
	}

	// Returns the active database connection.
	public Connection getConnection()
	{
		return connection;
	}

	// Returns the current statement.
	public Statement getStatement()
	{
		return statement;
	}

	// Closes the active statement and connection.
	public void close()
	{
		try
		{ if (this.statement != null) this.statement.close();
		}
		catch( java.sql.SQLException sql )
		{
		}
		
		try
		{ if (this.preparedStatement != null) this.preparedStatement.close();
		}
		catch( java.sql.SQLException sql )
		{
		}

		try
		{	connection.close();
		}
		catch( Exception sql )
		{
		}
	}
	

	public String formatFieldValue( String in )
	{
		in = in.replaceAll("\\\\", "\\\\\\\\"); 
		in = in.replaceAll("'", "\\\\'");
		
		return this.getConnectionProfile().getServerType().getDataOpenChar() + in + this.getConnectionProfile().getServerType().getDataOpenChar();
	}	
}

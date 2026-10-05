package nl.errorsoft.esqlmanager.domain;

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.data.*;

public class ConnectionWindow
{
	private ConnectionWindowCC cwcc;
	private ConnectionProfile cp;
	private DatabaseConnection db;
	
	public ConnectionWindow( ConnectionWindowCC cwcc, ConnectionProfile cp )
	{
		this.cwcc = cwcc;
		this.cp = cp;
	}
	
	public void start() throws Exception
	{
		db = new DatabaseConnection();
		
		if( cp.getServerType().getType() == ServerType.POSTGRES )
			db.connect( cp, cp.getDatabases() );
		else
			db.connect( cp, "" );
	}
	
	public void stop() throws Exception
	{
		db.close();
	}
	
	public DatabaseConnection getDatabaseConnection()
	{
		return this.db;
	}

	public ConnectionProfile getConnectionProfile()
	{
		return cp;
	}	
}
package nl.errorsoft.esql.domain;

/** A connection that is active on the server, as shown in the process list. */
public class ServerProcess
{
	private final String id;
	private final String user;
	private final String host;
	private final String database;
	private final String command;
	private final String time;
	private final String info;

	public ServerProcess( String id, String user, String host, String database, String command, String time, String info )
	{
		this.id = id;
		this.user = user;
		this.host = host;
		this.database = database;
		this.command = command;
		this.time = time;
		this.info = info;
	}

	public String getId()
	{
		return id;
	}

	public String getUser()
	{
		return user;
	}

	public String getHost()
	{
		return host;
	}

	public String getDatabase()
	{
		return database;
	}

	public String getCommand()
	{
		return command;
	}

	/** Seconds the process has been in its current state. */
	public String getTime()
	{
		return time;
	}

	public String getInfo()
	{
		return info;
	}
}

package nl.errorsoft.esql.user;

/** A database account: a user at a host on MySQL, a role on PostgreSQL (which has no host). */
public class DatabaseUser
{
	private final String name;
	private final String host;

	public DatabaseUser( String name, String host )
	{
		this.name = name;
		this.host = host;
	}

	public String getName()
	{
		return name;
	}

	/** The host the account may connect from, null when the server has no such concept. */
	public String getHost()
	{
		return host;
	}

	public String toString()
	{
		return host == null ? name : name + "@" + host;
	}
}

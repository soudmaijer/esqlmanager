package nl.errorsoft.esql.user;

/** The object a set of privileges applies to: the whole server, a database or a table. */
public class GrantTarget
{
	public enum Scope
	{
		GLOBAL, DATABASE, TABLE
	}

	private final Scope scope;
	private final String database;
	private final String table;

	private GrantTarget( Scope scope, String database, String table )
	{
		this.scope = scope;
		this.database = database;
		this.table = table;
	}

	public static GrantTarget global()
	{
		return new GrantTarget( Scope.GLOBAL, null, null );
	}

	public static GrantTarget database( String database )
	{
		return new GrantTarget( Scope.DATABASE, database, null );
	}

	public static GrantTarget table( String database, String table )
	{
		return new GrantTarget( Scope.TABLE, database, table );
	}

	public Scope getScope()
	{
		return scope;
	}

	public String getDatabase()
	{
		return database;
	}

	public String getTable()
	{
		return table;
	}

	public String toString()
	{
		switch ( scope )
		{
			case GLOBAL :
				return "Global";
			case DATABASE :
				return database;
			default :
				return table;
		}
	}
}

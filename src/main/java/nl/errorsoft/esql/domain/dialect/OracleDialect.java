package nl.errorsoft.esql.domain.dialect;

import java.util.ArrayList;
import java.util.List;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;

/** Oracle has no databases to browse, the "database" of the profile is the SID to connect to. */
public class OracleDialect extends AbstractDialect
{
	public int getType()
	{
		return ServerType.ORACLE;
	}

	public String getDefaultPort()
	{
		return "1521";
	}

	public String getDefaultUsername()
	{
		return "system";
	}

	public String getConnectionDatabase( ConnectionProfile cp, String requested )
	{
		return cp.getDatabases();
	}

	public List<String> listDatabases( DatabaseConnection dbc )
	{
		List<String> names = new ArrayList<String>();
		names.add( dbc.getConnectionProfile().getDatabases() );
		return names;
	}

	public void useDatabase( DatabaseConnection dbc, String database )
	{
		// The SID is part of the connection, there is nothing to switch.
	}
}

package nl.errorsoft.esql.dialect.oracle;

import nl.errorsoft.esql.dialect.AbstractDialect;

import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;

/** Oracle has no databases to browse, the "database" of the profile is the SID to connect to. */
public class OracleDialect extends AbstractDialect {
	public int getType() {
		return ServerType.ORACLE;
	}

	public String getDefaultPort() {
		return "1521";
	}

	public String getDefaultUsername() {
		return "system";
	}

	public String getConnectionDatabase(ConnectionProfile cp, String requested) {
		return cp.getDatabases();
	}

	/** The SID of the profile is the only database. */
	public String listDatabasesSql() {
		return null;
	}

	/** The SID is part of the connection, there is nothing to switch. */
	public DatabaseSwitch databaseSwitch() {
		return DatabaseSwitch.NONE;
	}
}

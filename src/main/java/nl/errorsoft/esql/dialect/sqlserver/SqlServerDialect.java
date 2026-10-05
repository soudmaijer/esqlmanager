package nl.errorsoft.esql.dialect.sqlserver;

import nl.errorsoft.esql.dialect.AbstractDialect;

import nl.errorsoft.esql.connection.ServerType;

public class SqlServerDialect extends AbstractDialect {
	public int getType() {
		return ServerType.MS_SQL_SERVER;
	}

	public String getDefaultPort() {
		return "1433";
	}

	public String getDefaultUsername() {
		return "sa";
	}

	public String listDatabasesSql() {
		return "EXEC sp_databases";
	}
}

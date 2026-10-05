package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.dialect.mysql.MySqlDialect;
import nl.errorsoft.esql.dialect.oracle.OracleDialect;
import nl.errorsoft.esql.dialect.postgres.PostgresDialect;
import nl.errorsoft.esql.dialect.sqlserver.SqlServerDialect;

import nl.errorsoft.esql.connection.ServerType;

public class DialectFactory {
	private static final Dialect MY_SQL = new MySqlDialect();
	private static final Dialect POSTGRES = new PostgresDialect();
	private static final Dialect SQL_SERVER = new SqlServerDialect();
	private static final Dialect ORACLE = new OracleDialect();

	public static Dialect forType(int serverType) {
		return switch (serverType) {
			case ServerType.MY_SQL -> MY_SQL;
			case ServerType.POSTGRES -> POSTGRES;
			case ServerType.MS_SQL_SERVER -> SQL_SERVER;
			case ServerType.ORACLE -> ORACLE;
			default -> throw new IllegalArgumentException("Unknown server type: " + serverType);
		};
	}
}

package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.connection.ServerType;

public class Dialects {
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

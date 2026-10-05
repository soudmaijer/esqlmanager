package nl.errorsoft.esql.user;

/** The object a set of privileges applies to: the whole server, a database or a table. */
public record GrantTarget(Scope scope, String database, String table) {
	public enum Scope {
		GLOBAL, DATABASE, TABLE
	}

	public static GrantTarget global() {
		return new GrantTarget(Scope.GLOBAL, null, null);
	}

	public static GrantTarget database(String database) {
		return new GrantTarget(Scope.DATABASE, database, null);
	}

	public static GrantTarget table(String database, String table) {
		return new GrantTarget(Scope.TABLE, database, table);
	}

	@Override
	public String toString() {
		return switch (scope) {
			case GLOBAL -> "Global";
			case DATABASE -> database;
			default -> table;
		};
	}
}

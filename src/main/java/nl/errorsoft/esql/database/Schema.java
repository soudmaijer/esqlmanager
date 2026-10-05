package nl.errorsoft.esql.database;

/** A schema of a database on servers that have them (PostgreSQL): plain data, it holds tables. */
public class Schema {
	private final Database database;
	private final String name;

	public Schema(Database database, String name) {
		this.database = database;
		this.name = name;
	}

	public Database getDatabase() {
		return database;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}
}

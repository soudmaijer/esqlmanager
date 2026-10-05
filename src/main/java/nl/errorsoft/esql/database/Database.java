package nl.errorsoft.esql.database;

/** A database (schema) on a server: plain data, loaded through the database service. */
public class Database {
	private String name = "";

	public Database() {
	}

	public Database(String name) {
		this.name = name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String toString() {
		return name;
	}
}

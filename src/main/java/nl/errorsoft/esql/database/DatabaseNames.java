package nl.errorsoft.esql.database;

import java.util.Collection;

/**
 * The rule for the name of a new database, shared by the Create dialog (which checks the names it knows while the user types) and
 * {@link DatabaseService#createDatabase}, which checks the server and stays the authority. Names are compared exactly, as the server compares quoted
 * names.
 */
public final class DatabaseNames {
	private DatabaseNames() {
	}

	/**
	 * What is wrong with the name, null when it can be used.
	 * @param term what the server calls a database ("database")
	 * @param existing the names that are taken
	 */
	public static String problem(String name, String term, Collection<String> existing) {
		if (name == null || name.isBlank()) {
			return "Enter a name for the " + term + ".";
		}
		return existing.contains(name.trim()) ? taken(name.trim(), term) : null;
	}

	public static String taken(String name, String term) {
		return "A " + term + " named '" + name + "' exists already.";
	}
}

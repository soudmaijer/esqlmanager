package nl.errorsoft.esql.dialect;

/**
 * A choice the user has when creating a database, such as the character set. The repository runs {@code choicesSql} to get the values to choose from.
 * @param key names the option in the map handed to {@link Dialect#createDatabaseSql(String, java.util.Map)}
 * @param label the text next to the choice ("Character set")
 * @param choicesSql a query whose first column holds the possible values
 * @param narrowedBy the key of another option whose value is the start (followed by an underscore) of the values to offer, null for all values
 */
public record DatabaseOption(String key, String label, String choicesSql, String narrowedBy) {
	public DatabaseOption(String key, String label, String choicesSql) {
		this(key, label, choicesSql, null);
	}
}

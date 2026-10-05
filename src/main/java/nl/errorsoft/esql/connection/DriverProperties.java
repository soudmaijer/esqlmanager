package nl.errorsoft.esql.connection;

/**
 * What the user can change of a database driver: the connection URL, the driver class, the quote characters of identifiers and strings, and a jar of
 * their own to load the driver from (empty for the bundled or downloaded driver).
 */
public record DriverProperties(String url, String className, String identifierOpen, String identifierClose, String stringOpen, String stringClose,
	String jar) {
}

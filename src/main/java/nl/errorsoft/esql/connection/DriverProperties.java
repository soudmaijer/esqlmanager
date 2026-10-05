package nl.errorsoft.esql.connection;

/** What the user can change of a database driver: the connection URL, the driver class and the quote characters of identifiers and strings. */
public record DriverProperties(String url, String className, String identifierOpen, String identifierClose, String stringOpen, String stringClose) {
}

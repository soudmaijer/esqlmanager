package nl.errorsoft.esql.database;

import java.util.Map;

/**
 * What the server says about a database, for the Properties window.
 * @param details the server specific properties in display order (character set and collation, or owner and encoding)
 * @param tableCount the tables and views in it, on servers with schemas those of every schema
 */
public record DatabaseProperties(String name, Map<String, String> details, int tableCount) {
}

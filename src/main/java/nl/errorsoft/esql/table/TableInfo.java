package nl.errorsoft.esql.table;

/**
 * What is known about a table, for the Properties window.
 * @param schema null on servers without schemas
 * @param type the storage engine, or TABLE / VIEW on servers without table types
 * @param sizeBytes the size of the table with its indexes, null when the server cannot tell
 */
public record TableInfo(String name, String database, String schema, String type, String comment, long rowCount, int columnCount, Long sizeBytes) {
}

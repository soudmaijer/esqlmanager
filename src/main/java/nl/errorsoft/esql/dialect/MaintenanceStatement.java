package nl.errorsoft.esql.dialect;

/**
 * A table maintenance statement and how to tell the user it ran.
 * @param resultColumn the column of the first result row that holds the server's message, null when the statement returns no rows.
 * @param message what to tell the user when there is no result column.
 */
public record MaintenanceStatement(String sql, String resultColumn, String message) {
}

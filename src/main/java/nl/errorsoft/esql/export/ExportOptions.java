package nl.errorsoft.esql.export;

/** What an export script contains. */
public record ExportOptions(boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase) {
}

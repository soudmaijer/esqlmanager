package nl.errorsoft.esql.importexport;

/** What an export script contains. */
public record ExportOptions(boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase) {
}

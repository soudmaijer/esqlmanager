package nl.errorsoft.esql.export;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * What an export script contains and how it is written.
 * @param dropIfExists DROP TABLE IF EXISTS instead of DROP TABLE
 * @param createIfNotExists CREATE TABLE IF NOT EXISTS instead of CREATE TABLE
 * @param charset the encoding of the file
 * @param rowsPerInsert 1 writes an INSERT per row, more rows share one INSERT
 * @param includeViews also write the definition of the views of a database or schema (with structure)
 * @param useTransaction wrap the script of each database in BEGIN and COMMIT where the server has such statements
 * @param disableForeignKeyChecks switch foreign key checking off while the script runs where the server can do that safely
 */
public record ExportOptions(boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase, boolean dropIfExists,
	boolean createIfNotExists, Charset charset, int rowsPerInsert, boolean includeViews, boolean useTransaction, boolean disableForeignKeyChecks) {

	public ExportOptions {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		if (rowsPerInsert < 1) {
			throw new IllegalArgumentException("rowsPerInsert must be at least 1");
		}
	}

	/** The classic script: DROP TABLE IF EXISTS, one INSERT per row, UTF-8, no views and no transaction. */
	public ExportOptions(boolean dumpStructure, boolean dumpData, boolean createDatabase, boolean dropTable, boolean useDatabase) {
		this(dumpStructure, dumpData, createDatabase, dropTable, useDatabase, true, false, StandardCharsets.UTF_8, 1, false, false, false);
	}
}

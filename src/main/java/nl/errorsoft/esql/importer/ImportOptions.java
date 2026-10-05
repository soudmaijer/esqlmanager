package nl.errorsoft.esql.importer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * How a script is run.
 * @param stopOnError true stops at the first statement that fails, false runs on and reports the failed statements at the end
 * @param singleTransaction run all statements in one transaction that is rolled back when the import stops. A transaction cannot continue after an
 *        error, so it always stops at the first one. MySQL commits implicitly at every DDL statement, so only the data of such a script is rolled back.
 * @param charset the encoding of the file
 */
public record ImportOptions(boolean stopOnError, boolean singleTransaction, Charset charset) {
	/** Stop at the first error, no transaction, UTF-8. */
	public static final ImportOptions DEFAULT = new ImportOptions(true, false, StandardCharsets.UTF_8);

	public ImportOptions {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
	}

	/** Whether the first failing statement ends the import. */
	public boolean stopsOnFirstError() {
		return stopOnError || singleTransaction;
	}
}

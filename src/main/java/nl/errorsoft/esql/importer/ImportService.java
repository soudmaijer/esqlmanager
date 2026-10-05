package nl.errorsoft.esql.importer;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.job.Cancellation;
import nl.errorsoft.esql.job.JobCancelledException;
import nl.errorsoft.esql.job.ProgressListener;
import nl.errorsoft.esql.job.ScriptTarget;
import nl.errorsoft.esql.table.Table;

/**
 * Runs an SQL script (gzip compressed when the name ends in .gz) on its own thread and reports progress (0 to 100), the number of statements done, or
 * an Exception to its listener. {@link #cancel} stops it before the next statement; with a single transaction everything is rolled back then.
 */
public class ImportService implements Runnable {
	private static final Logger log = LogManager.getLogger(ImportService.class);
	private static final String CONNECT = "\\connect ";
	private static final long STATUS_INTERVAL_NANOS = 100_000_000L;
	private static final int PREVIEW_LENGTH = 80;

	private ProgressListener listener = ProgressListener.NONE;
	private final Cancellation cancellation = new Cancellation();
	private final ImportRepository repository;
	/** Where the script runs; null for the database the connection uses. */
	private final ScriptTarget target;
	private final String file;
	private final ImportOptions options;
	private final List<String> failures = new ArrayList<>();
	private int statementsDone;
	private long lastStatus;
	private boolean rolledBack;

	public ImportService(ImportRepository repository, ScriptTarget target, String file, ImportOptions options) {
		this.repository = repository;
		this.target = target;
		this.file = file;
		this.options = options;
	}

	public ImportService(ImportRepository repository, ScriptTarget target, String file) {
		this(repository, target, file, ImportOptions.DEFAULT);
	}

	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}

	/** Asks the job to stop before the next statement. Safe to call from any thread. */
	public void cancel() {
		cancellation.cancel();
	}

	/** The statements that failed when the import continued after errors: number, start of the statement and the message. */
	public List<String> getFailures() {
		return failures;
	}

	public void run() {
		try {
			progress(10);

			Path script = Path.of(file);

			switch (target) {
				case null -> runScript(script);
				case ScriptTarget.OfDatabase(Database database) -> {
					repository.switchDatabase(database.getName());
					runScript(script);
				}
				case ScriptTarget.OfSchema(Schema schema) -> runInSchema(schema);
				case ScriptTarget.OfTable(Table table) -> {
					repository.switchDatabase(table.getDatabase().getName());
					if (table.getSchema() == null) {
						runScript(script);
					} else {
						runInSchema(table.getSchema());
					}
				}
			}
			progress(100);
			finish();
		} catch (JobCancelledException e) {
			listener.cancelled(cancelledSummary());
		} catch (Exception e) {
			listener.failed(e);
		}
	}

	private void finish() {
		if (failures.isEmpty()) {
			listener.finished("Ran " + statementsDone + " statement(s)", List.of());
		} else {
			listener.finished("Ran " + (statementsDone - failures.size()) + " statement(s), " + failures.size() + " failed", failures);
		}
	}

	private String cancelledSummary() {
		String done = "Cancelled after " + statementsDone + " statement(s). ";

		if (rolledBack) {
			return done + "The transaction was rolled back.";
		}
		return done + "The statements already run were not undone.";
	}

	/** Unqualified names of the script go to the schema; afterwards the connection resolves names the way it did before. */
	private void runInSchema(Schema schema) throws Exception {
		repository.switchDatabase(schema.getDatabase().getName());
		String previous = repository.currentSchema();
		repository.switchSchema(schema.getName());

		try {
			runScript(Path.of(file));
		} finally {
			if (previous != null) {
				repository.switchSchema(previous);
			}
		}
	}

	private void runScript(Path script) throws Exception {
		if (!options.singleTransaction()) {
			readStatements(script);
			return;
		}

		repository.beginTransaction();
		try {
			readStatements(script);
			repository.commit();
		} catch (Exception e) {
			repository.rollback();
			rolledBack = true;
			throw e;
		} finally {
			repository.endTransaction();
		}
	}

	private void readStatements(Path script) throws Exception {
		long size = Math.max(1, Files.size(script));

		try (CountingInputStream counted = new CountingInputStream(Files.newInputStream(script));
			BufferedReader reader = new BufferedReader(new InputStreamReader(unzipped(script, counted), options.charset()))) {
			StringBuilder statement = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				// A script switches database with the psql meta command, it is not SQL a server understands.
				if (statement.length() == 0 && line.startsWith(CONNECT)) {
					if (options.singleTransaction()) {
						throw new EsqlException("A script that switches database cannot run in a single transaction.");
					}
					repository.switchDatabase(line.substring(CONNECT.length()).replaceAll("^[\"`]|[\"`];?$", ""));
					continue;
				}

				statement.append(line).append("\n");

				if (line.endsWith(";")) {
					runStatement(statement.toString());
					statement.setLength(0);
					reportProgress((int) (10 + 85 * counted.count() / size));
				}
			}
		}
		reportStatus();
	}

	private static InputStream unzipped(Path script, InputStream in) throws IOException {
		return script.toString().toLowerCase().endsWith(".gz") ? new GZIPInputStream(in) : in;
	}

	private void runStatement(String statement) throws Exception {
		cancellation.check();
		int number = statementsDone + 1;

		try {
			repository.run(statement);
			statementsDone = number;
		} catch (Exception e) {
			String problem = "Statement " + number + " (" + preview(statement) + ") failed: " + e.getMessage();

			if (options.stopsOnFirstError()) {
				throw new EsqlException(problem, e);
			}
			log.warn(problem);
			failures.add(problem);
			statementsDone = number;
		}
	}

	private static String preview(String statement) {
		String oneLine = statement.strip().replaceAll("\\s+", " ");
		return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "...";
	}

	/** Progress and the statement count go to the listener at most a few times a second, a script has thousands of statements. */
	private void reportProgress(int percent) {
		long now = System.nanoTime();

		if (now - lastStatus >= STATUS_INTERVAL_NANOS) {
			lastStatus = now;
			progress(Math.min(percent, 99));
			reportStatus();
		}
	}

	private void reportStatus() {
		listener.status(statementsDone + " statement(s) done" + (failures.isEmpty() ? "" : ", " + failures.size() + " failed"));
	}

	private void progress(int percent) {
		listener.progressed(percent);
	}

	public void start() {
		Thread.ofVirtual().name("import").start(this);
	}

	/** Counts the bytes read from the file, so the progress follows the position in the file whatever the encoding. */
	private static final class CountingInputStream extends FilterInputStream {
		private long count;

		CountingInputStream(InputStream in) {
			super(in);
		}

		long count() {
			return count;
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b >= 0) {
				count++;
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			int n = super.read(buffer, offset, length);
			if (n > 0) {
				count += n;
			}
			return n;
		}
	}
}

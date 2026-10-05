package nl.errorsoft.esql.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.job.ProgressListener;

/** Runs an SQL script on its own thread and reports progress (0 to 100) or an Exception to its listener. */
public class ImportService implements Runnable {
	private static final String CONNECT = "\\connect ";

	private ProgressListener listener = ProgressListener.NONE;
	private final ImportRepository repository;
	private final Object importToDatabase;
	private final String file;

	public ImportService(ImportRepository repository, Object importToDatabase, String file) {
		this.repository = repository;
		this.importToDatabase = importToDatabase;
		this.file = file;
	}

	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}

	public void run() {
		try {
			progress(10);

			if (importToDatabase instanceof Database database) {
				repository.switchDatabase(database.getName());
			} else if (importToDatabase instanceof Schema schema) {
				repository.switchDatabase(schema.getDatabase().getName());
			}

			runScript(Path.of(file));
			progress(100);
		} catch (Exception e) {
			listener.failed(e);
		}
	}

	private void runScript(Path script) throws Exception {
		try (BufferedReader reader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
			StringBuilder statement = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				// A script switches database with the psql meta command, it is not SQL a server understands.
				if (statement.length() == 0 && line.startsWith(CONNECT)) {
					repository.switchDatabase(line.substring(CONNECT.length()).replaceAll("^[\"`]|[\"`];?$", ""));
					continue;
				}

				statement.append(line).append("\n");

				if (line.endsWith(";")) {
					repository.run(statement.toString());
					statement.setLength(0);
				}
			}
		}
	}

	private void progress(int percent) {
		listener.progressed(percent);
	}

	public void start() {
		Thread.ofVirtual().name("import").start(this);
	}
}

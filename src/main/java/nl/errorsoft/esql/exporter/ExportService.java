package nl.errorsoft.esql.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.job.Cancellation;
import nl.errorsoft.esql.job.JobCancelledException;
import nl.errorsoft.esql.job.ProgressListener;
import nl.errorsoft.esql.job.ScriptTarget;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableName;

/**
 * Writes databases and tables to an SQL script on its own thread and reports progress (0 to 100), the table it is on, or an Exception to its listener.
 * A file name ending in .gz gets a gzip compressed script. {@link #cancel} stops the job between two rows; the partial file is deleted.
 */
public class ExportService implements Runnable {
	private static final Logger log = LogManager.getLogger(ExportService.class);

	private ProgressListener listener = ProgressListener.NONE;
	private final Cancellation cancellation = new Cancellation();
	private final ExportRepository repository;
	private final List<ScriptTarget> targets;
	private final String file;
	private final ExportOptions options;
	private int tablesDone;
	private int tablesTotal;

	/** What one exported object contributes: the database it is in, its tables and, when asked for, its views. */
	private record Part(String database, List<TableName> tables, List<TableName> views) {
	}

	public ExportService(ExportRepository repository, List<ScriptTarget> targets, String file, ExportOptions options) {
		this.repository = repository;
		this.targets = List.copyOf(targets);
		this.file = file;
		this.options = options;
	}

	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}

	/** Asks the job to stop at the next table or row. Safe to call from any thread. */
	public void cancel() {
		cancellation.cancel();
	}

	public void run() {
		try {
			write();
			progress(100);
			listener.finished("Exported " + tablesDone + " table(s) to " + file, List.of());
		} catch (JobCancelledException e) {
			deletePartialFile();
			listener.cancelled("Cancelled after " + tablesDone + " of " + tablesTotal + " table(s). The partial file was deleted.");
		} catch (Exception e) {
			listener.failed(e);
		}
	}

	private void write() throws Exception {
		try (PrintWriter writer = open()) {
			progress(10);
			List<Part> parts = parts();
			tablesTotal = parts.stream().mapToInt(part -> part.tables().size()).sum();

			for (Part part : parts) {
				writePart(writer, part);
			}

			if (writer.checkError()) {
				throw new IOException("The file " + file + " could not be written");
			}
		}
	}

	private List<Part> parts() throws Exception {
		List<Part> parts = new ArrayList<>();
		boolean views = options.includeViews() && options.dumpStructure();

		for (ScriptTarget target : targets) {
			cancellation.check();

			parts.add(switch (target) {
				case ScriptTarget.OfDatabase(Database source) -> new Part(source.getName(), repository.tableNames(source),
					views ? repository.viewNames(source) : List.of());
				case ScriptTarget.OfSchema(Schema source) -> new Part(source.getDatabase().getName(), repository.tableNames(source),
					views ? repository.viewNames(source) : List.of());
				case ScriptTarget.OfTable(Table source) -> new Part(source.getDatabase().getName(), List.of(source.qualifiedName()), List.of());
			});
		}
		return parts;
	}

	private void writePart(PrintWriter writer, Part part) throws Exception {
		if (options.createDatabase()) {
			writer.println(repository.createDatabaseSql(part.database()) + ";\n");
		}

		if (options.useDatabase()) {
			writer.println(repository.useDatabaseSql(part.database()) + ";\n");
		}

		if (options.dumpStructure()) {
			// A table is restored into its own schema, which may not exist on the server the script is run on.
			for (String schema : new LinkedHashSet<>(part.tables().stream().map(TableName::schema).filter(Objects::nonNull).toList())) {
				writer.println(repository.createSchemaSql(schema) + ";\n");
			}
		}

		boolean transaction = options.useTransaction() && repository.beginSql() != null;
		boolean foreignKeys = options.disableForeignKeyChecks() && repository.disableForeignKeyChecksSql() != null;

		if (transaction) {
			writer.println(repository.beginSql() + ";\n");
		}
		if (foreignKeys) {
			writer.println(repository.disableForeignKeyChecksSql() + ";\n");
		}

		for (TableName table : part.tables()) {
			cancellation.check();
			listener.status("Exporting " + table);
			dumpTable(writer, table);
			tablesDone++;
			progress(10 + 85 * tablesDone / Math.max(1, tablesTotal));
		}

		for (TableName view : part.views()) {
			cancellation.check();
			dumpView(writer, view);
		}

		if (foreignKeys) {
			writer.println(repository.enableForeignKeyChecksSql() + ";\n");
		}
		if (transaction) {
			writer.println(repository.commitSql() + ";\n");
		}
	}

	private void dumpTable(PrintWriter writer, TableName table) throws Exception {
		if (options.dropTable()) {
			writer.println(repository.dropTableSql(table, options.dropIfExists()) + ";\n");
		}

		if (options.dumpStructure()) {
			writer.println(repository.structureSql(table, options.createIfNotExists()) + ";\n");
		}

		if (options.dumpData()) {
			repository.insertStatements(table, options.rowsPerInsert(), statement -> {
				cancellation.check();
				writer.println(statement);
			});

			for (String statement : repository.afterDataStatements(table)) {
				writer.println(statement + ";\n");
			}
		}
	}

	private void dumpView(PrintWriter writer, TableName view) throws Exception {
		String definition = repository.viewSql(view);

		if (definition == null) {
			log.warn("The definition of view {} cannot be exported on this server", view);
			return;
		}
		if (options.dropTable()) {
			writer.println(repository.dropViewSql(view, options.dropIfExists()) + ";\n");
		}
		writer.println(definition + ";\n");
	}

	/** The file is written as UTF-8 unless another encoding was chosen, compressed when the name ends in .gz. */
	private PrintWriter open() throws IOException {
		OutputStream out = Files.newOutputStream(Path.of(file));

		try {
			if (file.toLowerCase().endsWith(".gz")) {
				out = new GZIPOutputStream(out);
			}
		} catch (IOException e) {
			out.close();
			throw e;
		}
		return new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, options.charset())));
	}

	private void deletePartialFile() {
		try {
			Files.deleteIfExists(Path.of(file));
		} catch (IOException e) {
			log.warn("The partial file {} could not be deleted: {}", file, e.getMessage());
		}
	}

	private void progress(int percent) {
		listener.progressed(percent);
	}

	public void start() {
		Thread.ofVirtual().name("export").start(this);
	}
}

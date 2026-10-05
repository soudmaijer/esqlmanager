package nl.errorsoft.esql.export;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.job.ProgressListener;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableName;

/** Writes databases and tables to an SQL script on its own thread and reports progress (0 to 100) or an Exception to its listener. */
public class ExportService implements Runnable {
	private ProgressListener listener = ProgressListener.NONE;
	private final ExportRepository repository;
	private final Object[] exportObject;
	private final String file;
	private final ExportOptions options;

	public ExportService(ExportRepository repository, Object[] exportObject, String file, ExportOptions options) {
		this.repository = repository;
		this.exportObject = exportObject;
		this.file = file;
		this.options = options;
	}

	public void setListener(ProgressListener listener) {
		this.listener = listener;
	}

	public void run() {
		try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
			progress(10);

			for (int i = 0; i < exportObject.length; i++) {
				String database;
				List<TableName> tables = new ArrayList<>();

				if (exportObject[i] instanceof Database source) {
					database = source.getName();
					tables = repository.tableNames(source);
				} else if (exportObject[i] instanceof Schema source) {
					database = source.getDatabase().getName();
					tables = repository.tableNames(source);
				} else if (exportObject[i] instanceof Table source) {
					database = source.getDatabase().getName();
					tables.add(source.qualifiedName());
				} else {
					continue;
				}

				if (options.createDatabase()) {
					pw.println(repository.createDatabaseSql(database) + ";\n");
				}

				if (options.useDatabase()) {
					pw.println(repository.useDatabaseSql(database) + ";\n");
				}

				for (TableName table : tables) {
					dumpTable(pw, database, table);
				}

				progress(((100 / exportObject.length) * (i + 1)) - 1);
			}
			progress(100);
		} catch (Exception e) {
			listener.failed(e);
		}
	}

	private void dumpTable(PrintWriter pw, String database, TableName table) throws Exception {
		if (options.dropTable()) {
			pw.println(repository.dropTableSql(table) + ";\n");
		}

		if (options.dumpStructure()) {
			pw.println(repository.structureSql(table) + ";\n");
		}

		if (options.dumpData()) {
			repository.insertStatements(table, pw::println);

			for (String statement : repository.afterDataStatements(table)) {
				pw.println(statement + ";\n");
			}
		}
	}

	private void progress(int percent) {
		listener.progressed(percent);
	}

	public void start() {
		Thread.ofVirtual().name("export").start(this);
	}
}

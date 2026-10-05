package nl.errorsoft.esql.query.control;

import java.awt.Component;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.query.QueryService;
import nl.errorsoft.esql.query.SchemaNames;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.TableCC;
import nl.errorsoft.esql.table.ui.TableDataView;

/**
 * The controller of one query tab: runs the statements of the editor and gives the completion the names of the current database. The names are loaded
 * once per tab, the tables in the background when the database is chosen, the columns of a table the first time they are asked for.
 */
public class QueryCC implements SchemaNames {
	private static final Logger log = LogManager.getLogger(QueryCC.class);

	private final ConnectionWindowCC cwcc;
	private volatile Map<String, Table> tables = Map.of();
	private String database = "";
	private final Map<String, List<String>> columns = new ConcurrentHashMap<>();

	/** The outcome of a run: a result for every statement that returned rows, in order, and whether every statement succeeded. */
	public record RunResult(List<StatementResult> results, boolean succeeded) {
	}

	/** The rows of one statement, with what is shown below them: when it ran, on which database, how many rows and how long it took. */
	public record StatementResult(String sql, TableDataView view, LocalTime ranAt, String database, long millis) {
	}

	public QueryCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public List<Database> databases() throws Exception {
		return cwcc.getContext().databases().getDatabases();
	}

	/** Makes the database the current one and loads its table names for the completion, off the event thread. */
	public void use(Database database) {
		try {
			cwcc.getContext().databases().use(database);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwcc.getUI(), "Change database", e);
			return;
		}

		this.database = database.getName();
		tables = Map.of();
		columns.clear();
		Thread.ofVirtual().name("completion-tables").start(() -> {
			try {
				Map<String, Table> loaded = new ConcurrentHashMap<>();

				for (Table table : cwcc.getContext().databases().getTables(database)) {
					loaded.put(table.getName().toLowerCase(), table);
				}
				tables = loaded;
			} catch (Exception e) {
				// Completion then offers keywords only, running statements still works.
				log.warn("Could not load the tables of {} for completion: {}", database.getName(), e.getMessage());
			}
		});
	}

	/**
	 * Runs the statements in order and stops at the first that fails. The outcome of each goes to the log, the total or the failing statement to the status
	 * bar.
	 */
	public RunResult run(List<String> statements, Component parent) {
		QueryService queries;

		try {
			queries = cwcc.getContext().queries();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent, "Run query", e);
			return new RunResult(List.of(), false);
		}

		List<StatementResult> results = new ArrayList<>();
		TableDataView view = null;
		long start = System.nanoTime();

		for (int i = 0; i < statements.size(); i++) {
			String sql = statements.get(i);
			String which = statements.size() > 1 ? "Statement " + (i + 1) + " of " + statements.size() : "Statement";

			try {
				long started = System.nanoTime();
				LocalTime ranAt = LocalTime.now().withNano(0);

				if (queries.returnsRows(sql)) {
					view = new TableCC(cwcc).executeQuery(sql);
					long millis = millisSince(started);
					results.add(new StatementResult(sql.strip(), view, ranAt, currentDatabase(), millis));
					log.info("{}: {} row(s) in {} ms", which, view.getRowCount(), millis);
				} else if (queries.isUse(sql)) {
					queries.use(sql);
					log.info("{}: database changed", which);
				} else {
					int rows = queries.update(sql);
					log.info("{}: {} row(s) affected in {} ms", which, rows, millisSince(started));
				}
			} catch (Exception e) {
				cwcc.setStatusDetail(which + " failed: " + firstLine(sql));
				ApplicationContext.get().errors().report(parent, which + " (" + firstLine(sql) + ")", e);
				return new RunResult(results, false);
			}
		}

		String summary = statements.size() == 1 && view != null
			? "Query returned " + view.getRowCount() + " row(s) in " + millisSince(start) + " ms"
			: statements.size() + " statement(s) executed in " + millisSince(start) + " ms";
		cwcc.setStatusDetail(summary);
		return new RunResult(results, true);
	}

	private String currentDatabase() {
		return database;
	}

	@Override
	public List<String> tables() {
		List<String> names = new ArrayList<>();

		for (Table table : tables.values()) {
			names.add(table.getName());
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	@Override
	public List<String> columns(String tableName) {
		Table table = tables.get(tableName.toLowerCase());

		if (table == null) {
			return List.of();
		}

		return columns.computeIfAbsent(table.getName().toLowerCase(), key -> {
			try {
				List<String> names = new ArrayList<>();

				for (TableColumn column : cwcc.getContext().tables().loadColumns(table)) {
					names.add(column.getName());
				}
				return names;
			} catch (Exception e) {
				// Completion offers no columns for this table, the editor keeps working.
				log.warn("Could not load the columns of {} for completion: {}", table.getName(), e.getMessage());
				return List.of();
			}
		});
	}

	@Override
	public String quote(String name) {
		return cwcc.getConnectionProfile().getServerType().getDialect().quote(name);
	}

	private static String firstLine(String sql) {
		String line = sql.strip().lines().findFirst().orElse("");
		return line.length() > 60 ? line.substring(0, 60) + "..." : line;
	}

	private static long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}
}

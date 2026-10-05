package nl.errorsoft.esql.query.control;

import java.awt.Component;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.query.QueryService;
import nl.errorsoft.esql.query.SchemaNames;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.TableController;
import nl.errorsoft.esql.table.ui.TableDataTab;

/**
 * The controller of one query tab: runs the statements of the editor and gives the completion the names of the current database (of all its schemas). The names are loaded
 * once per tab, the tables in the background when the database is chosen, the columns of a table the first time they are asked for.
 */
public class QueryController implements SchemaNames {
	private static final Logger log = LogManager.getLogger(QueryController.class);

	private final ConnectionWindowController connectionWindowController;
	/** The tables per schema (lower case schema, "" on servers without schemas, to lower case table name). */
	private volatile Map<String, Map<String, Table>> tables = Map.of();
	private volatile List<String> schemaNames = List.of();
	/** The schema unqualified names resolve to, lower case; "" on servers without schemas. */
	private volatile String currentSchema = "";
	private String database = "";
	/** The schema chosen in the tab, null until one is (and on servers without schemas). */
	private volatile String chosenSchema;
	private BiConsumer<List<String>, String> schemaListener = (schemas, current) -> {
	};
	private final Map<String, List<String>> columns = new ConcurrentHashMap<>();

	/** The outcome of a run: a result for every statement that returned rows, in order, and whether every statement succeeded. */
	public record RunResult(List<StatementResult> results, boolean succeeded) {
	}

	/** The rows of one statement, with what is shown below them: when it ran, on which database, how many rows and how long it took. */
	public record StatementResult(String sql, TableDataTab view, LocalTime ranAt, String database, long millis) {
	}

	public QueryController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	/** Whether the server has schemas, so the tab offers a schema next to the database. */
	public boolean hasSchemas() {
		return dialect().supports(Dialect.Feature.SCHEMAS);
	}

	/** What the server calls a schema, for the label of the picker. */
	public String schemaTerm() {
		return dialect().schemaTerm();
	}

	/** Told on the event thread, after a database is chosen, which schemas it has and which one names resolve to. */
	public void setSchemaListener(BiConsumer<List<String>, String> listener) {
		this.schemaListener = listener;
	}

	public List<Database> databases() throws Exception {
		return connectionWindowController.getContext().databases().getDatabases();
	}

	/** Makes the database the current one and loads its table names for the completion, off the event thread. */
	public void use(Database database) {
		try {
			connectionWindowController.getContext().databases().use(database);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getMainWindow(), "Change database", e);
			return;
		}

		this.database = database.getName();
		chosenSchema = null;
		tables = Map.of();
		schemaNames = List.of();
		columns.clear();
		Thread.ofVirtual().name("completion-tables").start(() -> {
			try {
				DatabaseService service = connectionWindowController.getContext().databases();
				Map<String, Map<String, Table>> loaded = new ConcurrentHashMap<>();
				List<Schema> schemas = service.getSchemas(database);

				if (schemas.isEmpty()) {
					loaded.put("", byName(service.getTables(database)));
				} else {
					for (Schema schema : schemas) {
						loaded.put(schema.getName().toLowerCase(), byName(service.getTables(schema)));
					}
				}
				String current = service.currentSchema(database);
				currentSchema = current == null || schemas.isEmpty() ? "" : current.toLowerCase();
				schemaNames = schemas.stream().map(Schema::getName).toList();
				tables = loaded;
				List<String> names = schemaNames;
				SwingUtilities.invokeLater(() -> schemaListener.accept(names, current));
			} catch (Exception e) {
				// Completion then offers keywords only, running statements still works.
				log.warn("Could not load the tables of {} for completion: {}", database.getName(), e.getMessage());
			}
		});
	}

	/** Makes unqualified names resolve to the schema, for the statements and the completion. */
	public void useSchema(String schema) {
		try {
			connectionWindowController.getContext().queries().useSchema(database, schema);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getMainWindow(), "Change " + schemaTerm(), e);
			return;
		}
		chosenSchema = schema;
		currentSchema = schema.toLowerCase();
	}

	/**
	 * Runs the statements in order and stops at the first that fails. The outcome of each goes to the log, the total or the failing statement to the status
	 * bar.
	 */
	public RunResult run(List<String> statements, Component parent) {
		QueryService queries;

		try {
			queries = connectionWindowController.getContext().queries();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent, "Run query", e);
			return new RunResult(List.of(), false);
		}

		try {
			// The connection is shared with the tree and other tabs, which may have moved it to another database or schema.
			if (chosenSchema != null) {
				queries.useSchema(database, chosenSchema);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent, "Change " + schemaTerm(), e);
			return new RunResult(List.of(), false);
		}

		List<StatementResult> results = new ArrayList<>();
		TableDataTab view = null;
		long start = System.nanoTime();

		for (int i = 0; i < statements.size(); i++) {
			String sql = statements.get(i);
			String which = statements.size() > 1 ? "Statement " + (i + 1) + " of " + statements.size() : "Statement";

			try {
				long started = System.nanoTime();
				LocalTime ranAt = LocalTime.now().withNano(0);

				if (queries.returnsRows(sql)) {
					view = new TableController(connectionWindowController).executeQuery(sql);
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
				connectionWindowController.setStatusDetail(parent, which + " failed: " + firstLine(sql));
				ApplicationContext.get().errors().report(parent, which + " (" + firstLine(sql) + ")", e);
				return new RunResult(results, false);
			}
		}

		String summary = statements.size() == 1 && view != null
			? "Query returned " + view.getRowCount() + " row(s) in " + millisSince(start) + " ms"
			: statements.size() + " statement(s) executed in " + millisSince(start) + " ms";
		connectionWindowController.setStatusDetail(parent, summary);
		return new RunResult(results, true);
	}

	private String currentDatabase() {
		String schema = chosenSchema;
		return schema == null ? database : database + "." + schema;
	}

	private static Map<String, Table> byName(List<Table> list) {
		Map<String, Table> named = new ConcurrentHashMap<>();

		for (Table table : list) {
			named.put(table.getName().toLowerCase(), table);
		}
		return named;
	}

	@Override
	public List<String> tables() {
		return tables(currentSchema);
	}

	@Override
	public List<String> schemas() {
		return schemaNames;
	}

	@Override
	public List<String> tables(String schema) {
		List<String> names = new ArrayList<>();

		for (Table table : tables.getOrDefault(schema.toLowerCase(), Map.of()).values()) {
			names.add(table.getName());
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	/** {@code schema.table} is looked up in that schema, a plain name in the current schema. */
	@Override
	public List<String> columns(String tableName) {
		String name = tableName.toLowerCase();
		int dot = name.lastIndexOf('.');
		Map<String, Table> schema = tables.getOrDefault(dot < 0 ? currentSchema : name.substring(0, dot), Map.of());
		Table table = schema.get(dot < 0 ? name : name.substring(dot + 1));

		if (table == null) {
			return List.of();
		}

		return columns.computeIfAbsent(table.qualifiedName().toString().toLowerCase(), key -> {
			try {
				List<String> names = new ArrayList<>();

				for (TableColumn column : connectionWindowController.getContext().tables().loadColumns(table)) {
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
		return dialect().quote(name);
	}

	private Dialect dialect() {
		return connectionWindowController.getConnectionProfile().getServerType().getDialect();
	}

	private static String firstLine(String sql) {
		String line = sql.strip().lines().findFirst().orElse("");
		return line.length() > 60 ? line.substring(0, 60) + "..." : line;
	}

	private static long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}
}

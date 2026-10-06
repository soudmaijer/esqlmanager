package nl.errorsoft.esql.query.control;

import java.awt.Component;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.query.ExecutionResult;
import nl.errorsoft.esql.query.QueryPlan;
import nl.errorsoft.esql.query.plan.Plan;
import nl.errorsoft.esql.query.plan.PlanParser;
import nl.errorsoft.esql.query.QueryService;
import nl.errorsoft.esql.query.SchemaNames;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.TableController;
import nl.errorsoft.esql.table.QueryResult;

/**
 * The controller of one query tab: runs the statements of the editor and gives the completion the names of the current database (of all its schemas). The names are loaded
 * once per tab in the background: the tables when the database is chosen, the columns of a table the first time they are asked for (the completion
 * offers none until they arrive).
 */
public class QueryController implements SchemaNames {
	private static final Logger log = LogManager.getLogger(QueryController.class);

	private final ConnectionWindowController connectionWindowController;
	/** The tables per schema (lower case schema, "" on servers without schemas, to lower case table name). */
	private volatile Map<String, Map<String, Table>> tables = Map.of();
	private volatile List<String> schemaNames = List.of();
	/** The schema unqualified names resolve to, lower case; "" on servers without schemas. */
	private volatile String currentSchema = "";
	private volatile String database = "";
	/** The schema chosen in the tab, null until one is (and on servers without schemas). */
	private volatile String chosenSchema;
	/** The schema a new tab is asked to start in (a tab opened on a table), used once when the schemas are listed. */
	private volatile String startSchema;
	private BiConsumer<List<String>, String> schemaListener = (schemas, current) -> {
	};
	private final Map<String, List<String>> columns = new ConcurrentHashMap<>();

	/** The outcome of a run: a result for every statement that returned rows, in order, and whether every statement succeeded. */
	public record RunResult(List<StatementResult> results, boolean succeeded) {
	}

	/** The rows of one statement, with what is shown below them: when it ran, on which database, how many rows and how long it took. */
	public record StatementResult(String sql, QueryResult result, LocalTime ranAt, String database, long millis) {
		public int rowCount() {
			return result.table().getRowCount();
		}
	}

	/**
	 * The plan of one statement as the server gave it ({@code raw}) and parsed ({@code plan}, null when its shape is unknown), with when it was asked, on
	 * which database and how long it took; {@code analyzed} when the statement was run.
	 */
	public record ExplainResult(String sql, QueryPlan raw, Plan plan, boolean analyzed, LocalTime ranAt, String database, long millis) {
	}

	public QueryController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	/** Whether the server has schemas, so the tab offers a schema next to the database. */
	public boolean hasSchemas() {
		return dialect().supports(Dialect.Feature.SCHEMAS);
	}

	/** Whether the server can show the plan of a statement, so the tab offers Explain and Explain analyze. */
	public boolean supportsExplain() {
		return dialect().supports(Dialect.Feature.EXPLAIN);
	}

	/** What the server calls a database, for the label of the picker. */
	public String databaseTerm() {
		return dialect().databaseTerm();
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

	/** The database a new tab starts on: the one selected in the tree, else the one the connection uses now. */
	public Database startDatabase(List<Database> databases, Database selected) {
		String connected = null;
		try {
			connected = connectionWindowController.getDatabaseConnection().getDatabase();
		} catch (Exception e) {
			// Without a connection the tab starts on the first database, running a statement reports the problem.
		}
		return startDatabase(databases, selected, connected);
	}

	/**
	 * The database a new tab starts on: the selected one, else the one the connection uses, else the first of the list (the profile's filter is applied
	 * to it already). Null only for an empty list.
	 */
	public static Database startDatabase(List<Database> databases, Database selected, String connected) {
		for (String name : java.util.Arrays.asList(selected == null ? null : selected.getName(), connected)) {
			if (name != null) {
				for (Database database : databases) {
					if (database.getName().equals(name)) {
						return database;
					}
				}
			}
		}
		return databases.isEmpty() ? null : databases.getFirst();
	}

	/**
	 * Makes the database the current one off the event thread (on PostgreSQL that connects again), then loads its table names for the completion.
	 * {@code always} runs on the event thread when the switch is done or failed.
	 */
	public void use(Database database, Runnable always) {
		chosenSchema = null;
		tables = Map.of();
		schemaNames = List.of();
		columns.clear();
		connectionWindowController.inBackground("Change " + databaseTerm(), "Changing " + databaseTerm() + "...", () -> {
			connectionWindowController.getContext().databases().use(database);
			return database.getName();
		}, name -> {
			this.database = name;
			loadTables(database);
		}, always);
	}

	private void loadTables(Database database) {
		Thread.ofVirtual().name("completion-tables").start(() -> {
			try (var context = connectionWindowController.logContext()) {
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
				String start = startSchema;
				startSchema = null;
				String shown = start != null && names.contains(start) ? start : current;
				SwingUtilities.invokeLater(() -> {
					schemaListener.accept(names, shown);
					if (!java.util.Objects.equals(shown, current)) {
						useSchema(shown, () -> {
							// The picker shows the schema already.
						});
					}
				});
			} catch (Exception e) {
				// Completion then offers keywords only, running statements still works.
				log.warn("Could not load the tables of {} for completion: {}", database.getName(), e.getMessage());
			}
		});
	}

	/** Starts the tab in this schema instead of the current one, once its database's schemas are listed. Call before the tab is built. */
	public void startInSchema(String schema) {
		this.startSchema = schema;
	}

	/** Makes unqualified names resolve to the schema, for the statements and the completion, off the event thread; then {@code always} on it. */
	public void useSchema(String schema, Runnable always) {
		String in = database;
		connectionWindowController.inBackground("Change " + schemaTerm(), "Changing " + schemaTerm() + "...", () -> {
			connectionWindowController.getContext().queries().useSchema(in, schema);
			return schema;
		}, chosen -> {
			chosenSchema = chosen;
			currentSchema = chosen.toLowerCase();
		}, always);
	}

	/** The controller of the grid a result is shown in, so that its rows can be edited like the rows of a table. */
	public TableController tableController() {
		return new TableController(connectionWindowController);
	}

	/**
	 * Runs the statements in order on a virtual thread and stops at the first that fails. The outcome of each goes to the log and a result tab, "Running...", the failing
	 * statement or the count of statements without rows to the status bar; {@code done} gets the results on the event thread.
	 */
	public void run(List<String> statements, Component parent, Consumer<RunResult> done) {
		connectionWindowController.setStatusDetail(parent, "Running " + (statements.size() == 1 ? "statement" : statements.size() + " statements") + "...");
		Thread.ofVirtual().name("query").start(() -> {
			try (var context = connectionWindowController.logContext()) {
				RunResult result = run(statements, parent);
				SwingUtilities.invokeLater(() -> done.accept(result));
			}
		});
	}

	/** Runs the statements on the calling thread (not the event thread); errors and the status are shown on the event thread. */
	private RunResult run(List<String> statements, Component parent) {
		QueryService queries;

		try {
			queries = connectionWindowController.getContext().queries();
		} catch (Exception e) {
			report(parent, "Run query", e);
			return new RunResult(List.of(), false);
		}

		try {
			// The connection is shared with the tree and other tabs, which may have moved it to another database or schema.
			if (chosenSchema != null) {
				queries.useSchema(database, chosenSchema);
			}
		} catch (Exception e) {
			report(parent, "Change " + schemaTerm(), e);
			return new RunResult(List.of(), false);
		}

		List<StatementResult> results = new ArrayList<>();
		long start = System.nanoTime();

		for (int i = 0; i < statements.size(); i++) {
			String sql = statements.get(i);
			String which = statements.size() > 1 ? "Statement " + (i + 1) + " of " + statements.size() : "Statement";

			try {
				long started = System.nanoTime();
				LocalTime ranAt = LocalTime.now().withNano(0);

				switch (queries.execute(sql)) {
					case ExecutionResult.Rows rows -> {
						long millis = millisSince(started);
						StatementResult result = new StatementResult(sql.strip(), rows.result(), ranAt, currentDatabase(), millis);
						results.add(result);
						log.info("{}: {} row(s) in {} ms", which, result.rowCount(), millis);
					}
					case ExecutionResult.DatabaseChanged changed -> log.info("{}: {} changed to {}", which, databaseTerm(), changed.database());
					case ExecutionResult.Updated updated -> log.info("{}: {} row(s) affected in {} ms", which, updated.count(), millisSince(started));
				}
			} catch (Exception e) {
				String status = which + " failed: " + firstLine(sql);
				SwingUtilities.invokeLater(() -> connectionWindowController.setStatusDetail(parent, status));
				report(parent, which + " (" + firstLine(sql) + ")", e);
				return new RunResult(results, false);
			}
		}

		String summary = summary(statements.size(), results.size(), millisSince(start));
		SwingUtilities.invokeLater(() -> connectionWindowController.setStatusDetail(parent, summary));
		return new RunResult(results, true);
	}

	/**
	 * Asks the server for the plan of one statement on a virtual thread, in the database and schema of the tab; with {@code analyze} the statement runs.
	 * {@code done} gets the plan on the event thread, a failure is reported and {@code done} gets null.
	 */
	public void explain(String sql, boolean analyze, Component parent, Consumer<ExplainResult> done) {
		String action = analyze ? "Explain analyze" : "Explain";
		connectionWindowController.setStatusDetail(parent, (analyze ? "Analyzing" : "Explaining") + " statement...");
		Thread.ofVirtual().name("explain").start(() -> {
			try (var context = connectionWindowController.logContext()) {
				ExplainResult result = null;
				String status = "";
				try {
					QueryService queries = connectionWindowController.getContext().queries();
					// The connection is shared with the tree and other tabs, which may have moved it to another database or schema.
					if (chosenSchema != null) {
						queries.useSchema(database, chosenSchema);
					}
					long started = System.nanoTime();
					LocalTime ranAt = LocalTime.now().withNano(0);
					QueryPlan plan = queries.explain(sql, analyze);
					long millis = millisSince(started);
					result = new ExplainResult(sql.strip(), plan, parse(plan), analyze, ranAt, currentDatabase(), millis);
					log.info("{}: plan of {} line(s) in {} ms", action, plan.rows().size(), millis);
				} catch (Exception e) {
					status = action + " failed: " + firstLine(sql);
					report(parent, action + " (" + firstLine(sql) + ")", e);
				}
				ExplainResult outcome = result;
				String shown = status;
				SwingUtilities.invokeLater(() -> {
					connectionWindowController.setStatusDetail(parent, shown);
					done.accept(outcome);
				});
			}
		});
	}

	private static Plan parse(QueryPlan raw) {
		try {
			return PlanParser.parse(raw.text());
		} catch (RuntimeException e) {
			// A plan of a shape the parser does not know is still shown as text.
			log.warn("The plan could not be read: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * What the bar below the query tab says after a successful run. A statement with rows has its own line on its result tab (time, database, rows,
	 * duration), so the bar does not repeat it and stays empty when every statement had rows; otherwise it says how many statements ran.
	 */
	static String summary(int statements, int withRows, long millis) {
		return withRows == statements ? "" : statements + " statement(s) executed in " + millis + " ms";
	}

	private static void report(Component parent, String action, Exception e) {
		SwingUtilities.invokeLater(() -> ApplicationContext.get().errors().report(parent, action, e));
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

		String key = table.qualifiedName().toString().toLowerCase();
		List<String> known = columns.get(key);

		if (known == null && columns.putIfAbsent(key, List.of()) == null) {
			loadColumns(table, key);
		}
		// Until the columns arrive the completion offers none, the next completion has them.
		return known == null ? List.of() : known;
	}

	/** Loads the columns of a table off the event thread; a result for a database that is no longer chosen is dropped. */
	private void loadColumns(Table table, String key) {
		Map<String, Map<String, Table>> loadedFor = tables;
		Thread.ofVirtual().name("completion-columns").start(() -> {
			try (var context = connectionWindowController.logContext()) {
				List<String> names = new ArrayList<>();

				for (TableColumn column : connectionWindowController.getContext().tables().loadColumns(table)) {
					names.add(column.getName());
				}
				if (tables == loadedFor) {
					columns.put(key, List.copyOf(names));
				}
			} catch (Exception e) {
				// Completion offers no columns for this table, the editor keeps working.
				log.warn("Could not load the columns of {} for completion: {}", table.getName(), e.getMessage());
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

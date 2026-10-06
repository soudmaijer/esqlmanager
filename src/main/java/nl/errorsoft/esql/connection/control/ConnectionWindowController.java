package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.table.ColumnOptions;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.TableEditorController;
import nl.errorsoft.esql.table.control.IndexesController;
import nl.errorsoft.esql.table.control.TableController;
import nl.errorsoft.esql.table.ui.dialog.ColumnPropertiesDialog;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.StatusContext;

import nl.errorsoft.esql.connection.ConnectionContext;

import nl.errorsoft.esql.query.control.QueryController;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseInfo;
import nl.errorsoft.esql.database.ui.dialog.CreateDatabaseDialog;
import nl.errorsoft.esql.table.TableInfo;
import nl.errorsoft.esql.table.ui.dialog.DuplicateTableDialog;
import nl.errorsoft.esql.ui.util.ByteSize;
import nl.errorsoft.esql.ui.dialog.PropertiesDialog;
import nl.errorsoft.esql.ui.util.Validation;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ConnectionSession;
import nl.errorsoft.esql.connection.ui.ConnectionView;
import nl.errorsoft.esql.connection.ConnectionNode;
import nl.errorsoft.esql.connection.ui.WorkFrames;
import nl.errorsoft.esql.server.control.ProcessListController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.designer.ui.diagram.ModelFactory;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.user.control.UserManagerController;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.connection.TreeSelection;
import nl.errorsoft.esql.table.ui.TableListTab;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import javax.swing.SwingUtilities;

public class ConnectionWindowController {
	/** The key of the log context with the profile name of the connection, shown before the log lines in the output panel. */
	public static final String LOG_CONNECTION = "connection";
	private String statusDetail = "";
	private static final Logger log = LogManager.getLogger(ConnectionWindowController.class);
	private final ReentrantLock treeLoads = new ReentrantLock(); // One tree load at a time, see inTree

	private MainController mainController;
	private ConnectionSession session;
	private ConnectionNode node;
	private WorkFrames view;
	/** Set once the connection is made and its databases are listed. */
	private volatile boolean connected;

	/** Adds the connection to the explorer at once and connects in the background; its branch is filled when the databases are listed. */
	public ConnectionWindowController(MainController mainController, nl.errorsoft.esql.connection.ConnectionProfile profile) {
		this.mainController = mainController;
		this.session = new ConnectionSession(this, profile);
		SwingUtilities.invokeLater(this::open);
	}

	/** Shows the connection in the explorer on the event thread, then connects and lists the databases on a virtual thread. */
	private void open() {
		ConnectionProfile profile = session.getConnectionProfile();
		node = new ConnectionNode(profile, getTitle());
		view = new WorkFrames(this, mainController.getMainWindow(), node);
		mainController.addConnection(this);
		// Logged after the connection has its tab in the output panel, and in its name: the event thread may still carry another connection's.
		try (var context = logContext()) {
			log.info("Connecting to {} on {}:{} as {}", profile.getServerType().getDescription(), profile.getHost(), profile.getPort(),
				profile.getUsername());
		}
		mainController.updateStatus("Connecting...", true);

		Thread.ofVirtual().name("connect").start(() -> {
			try (var context = logContext()) {
				session.start();
				SwingUtilities.invokeLater(() -> mainController.updateStatus("Loading databases...", true));
				List<Database> databases = getContext().databases().getDatabases();
				SwingUtilities.invokeLater(() -> {
					connected = true;
					showDatabaseTree(databases);
					setStatusDetail("");
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					closeWindow();
					mainController.updateStatus("Cannot connect to server...", true);
					try (var context = logContext()) {
						ApplicationContext.get().errors().report("Connect to " + profile.getName(), e);
					}
				});
			}
		});
	}

	/**
	 * Puts the profile name of this connection on the log lines of the current thread until closed, so the output panel shows which connection did what.
	 * For work on a thread of its own.
	 */
	public CloseableThreadContext.Instance logContext() {
		return CloseableThreadContext.put(LOG_CONNECTION, session.getConnectionProfile().getName());
	}

	/**
	 * This connection became the one the user works with (its window came to the front, a node of it was selected): the status bar shows it, and what the
	 * event thread logs from now on is marked with its name.
	 */
	public void activate() {
		ThreadContext.put(LOG_CONNECTION, session.getConnectionProfile().getName());
		mainController.getMainWindow().getOutput().showConnection(getTitle());
	}

	/** Whether the connection is made and its databases are listed, for the status bar. */
	public boolean isConnected() {
		return connected;
	}

	/** The server, the account and {@code where} (database and schema, may be empty) of this connection, for the right of the status bar. */
	public String statusInfo(String where) {
		try {
			return StatusContext.info(getDatabaseConnection().getServerDescription(), session.getConnectionProfile().getUsername() + "@"
				+ session.getConnectionProfile().getHost() + ":" + session.getConnectionProfile().getPort(), where);
		} catch (Exception e) {
			// Not connected (yet), there is nothing to show.
			return "";
		}
	}

	/** Something this connection shows changed (a database or schema, a message): the status bar is described again. */
	public void showStatusInfo() {
		mainController.showConnectionState();
	}

	/** What the connection did in the tree (schemas listed, a schema created) belongs to no tab, it goes to the output panel only. */
	public void setStatusDetail(String detail) {
		statusDetail = detail;
		if (!detail.isEmpty()) {
			log.info(detail);
		}
		showStatusInfo();
	}

	/** A message about the table view tab (a table loaded, a table list shown), shown in the status bar while that tab is in front. */
	public void setViewStatus(String detail) {
		statusDetail = detail;
		view.setStatus(detail);
		showStatusInfo();
	}

	/** A message about one tab (a query tab), shown in the status bar while that tab is in front. */
	public void setStatusDetail(java.awt.Component tab, String detail) {
		statusDetail = detail;
		view.setStatus(tab, detail);
		showStatusInfo();
	}

	private long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}

	/** Database work started by a user action, run off the event thread. */
	public interface Work<T> {
		T run() throws Exception;
	}

	/** What is done with the result of {@link Work} on the event thread. */
	public interface Outcome<T> {
		void accept(T result) throws Exception;
	}

	/**
	 * Runs the work of a user action on a virtual thread while the status light says what is loading, then hands the result to {@code done} on the event
	 * thread. A failure of either is reported once as the action, with this connection window as the parent.
	 */
	public <T> void inBackground(String action, String status, Work<T> work, Outcome<T> done) {
		inBackground(action, status, work, done, () -> {
		});
	}

	/**
	 * As {@link #inBackground(String, String, Work, Outcome)}, and runs {@code always} on the event thread afterwards, also after a failure: to enable again
	 * the controls that were disabled while the work ran.
	 */
	public <T> void inBackground(String action, String status, Work<T> work, Outcome<T> done, Runnable always) {
		mainController.updateStatus(status, true);
		Thread.ofVirtual().name(action).start(() -> {
			try (var context = logContext()) {
				T result = work.run();
				SwingUtilities.invokeLater(() -> {
					mainController.showConnectionState();
					try (var log = logContext()) {
						done.accept(result);
					} catch (Exception e) {
						reportFailure(action, e);
					} finally {
						always.run();
					}
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					mainController.showConnectionState();
					try {
						reportFailure(action, e);
					} finally {
						always.run();
					}
				});
			}
		});
	}

	/** Reports a failure of this connection: the error is logged in the tab of the connection in the output panel. */
	private void reportFailure(String action, Exception e) {
		try (var log = logContext()) {
			ApplicationContext.get().errors().report(parent(), action, e);
		}
	}

	/**
	 * Disconnects after asking: editors with unsaved changes ask first, then the user confirms. Every work window of the connection closes, its designers
	 * stay open without it.
	 */
	public void disconnect() {
		if (!view.confirmCloseEditors()) {
			return;
		}
		int windows = view.frameCount();
		String closing = windows == 0 ? "" : " " + windows + (windows == 1 ? " tab will close." : " tabs will close.");
		if (Dialogs.confirm(parent(), "Disconnect", "Disconnect from " + getTitle() + "?" + closing, "Disconnect")) {
			closeWindow();
		}
	}

	/** Asks every editor of this connection with unsaved changes whether to discard them; false when the user keeps one. */
	public boolean confirmCloseEditors() {
		return view.confirmCloseEditors();
	}

	/** Closes the connection and its windows without asking, and takes it out of the explorer. */
	public void closeWindow() {
		try {
			ApplicationContext.get().release(session.getDatabaseConnection());
			session.stop();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Disconnect", e);
		}

		view.closeAll();
		mainController.getMainWindow().detachDesigners(this);
		mainController.removeConnection(this);
		// What the event thread logs from now on belongs to no connection, until another one is activated.
		if (getTitle().equals(ThreadContext.get(LOG_CONNECTION))) {
			ThreadContext.remove(LOG_CONNECTION);
		}
	}

	/** Lists the databases again in the background and shows them in the tree. */
	public void showDatabaseTree() {
		DatabaseController databaseController = new DatabaseController(this);
		inBackground("Load databases", "Loading databases...", databaseController::getDatabases, this::showDatabaseTree);
	}

	/** Shows the databases listed on another thread in the explorer, on the event thread. */
	private void showDatabaseTree(List<Database> databases) {
		view.branch().loadDatabases(databases);
		mainController.showConnectionState();
	}

	/** Asks for the name and the options of the new database (character set, owner, ...) and creates it. */
	public void startCreateDatabase() {
		try {
			CreateDatabaseDialog.Request request = CreateDatabaseDialog.ask(parent(), dialect().databaseTerm(), dialect().createDatabaseOptions(),
				getContext().databases().createDatabaseChoices(), view.branch().databaseNames());

			if (request != null) {
				createDatabase(request.name(), request.options());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Create " + dialect().databaseTerm(), e);
		}
	}

	private void createDatabase(String name, Map<String, String> options) {
		try {
			mainController.updateStatus("Creating " + dialect().databaseTerm() + "...", true);
			Database db = getContext().databases().createDatabase(name, options);
			view.branch().addDatabase(db);
			setStatusDetail(dialect().databaseTerm() + " " + name + " created");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Create " + dialect().databaseTerm(), e);
		}
	}

	public void dropDatabase() {
		Database db = selectedDatabase("Drop " + dialect().databaseTerm());
		if (db == null) {
			return;
		}

		try {
			mainController.updateStatus("Deleting database...", true);
			DatabaseController databaseController = new DatabaseController(this);
			databaseController.dropDatabase(db);
			view.branch().deleteDatabase(db);
			view.removeViews(viewKey(db, ""));
			mainController.showConnectionState();
			this.showDatabaseTree();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop database", e);
		}
	}

	public void dropTable() {
		Table table = selectedTable("Drop table");
		if (table != null) {
			dropTables(List.of(table), () -> {
			});
		}
	}

	/** Drops the tables (asked beforehand) in the background, takes them out of the tree, then runs {@code dropped} on the event thread. */
	public void dropTables(List<Table> tables, Runnable dropped) {
		TableController tableController = new TableController(this);
		inBackground("Drop table", "Deleting table...", () -> {
			for (Table table : tables) {
				tableController.dropTable(table);
			}
			return tables;
		}, done -> {
			done.forEach(view.branch()::deleteTable);
			setStatusDetail(done.size() == 1 ? "Table " + done.getFirst().getName() + " dropped" : done.size() + " tables dropped");
			dropped.run();
		});
	}

	public void addTableColumn(ColumnPropertiesDialog fieldPropertiesDialog, ColumnOptions options) {
		Table table = selectedTable("Add column");
		if (table == null) {
			return;
		}

		try {
			mainController.updateStatus("Adding tablecolumn...", true);
			TableController tableController = new TableController(this);
			tableController.addTableColumn(table, options.toColumn());
			reloadSelectedTable();
			fieldPropertiesDialog.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Add table column", e);
		}
	}

	public void editTableColumn(ColumnPropertiesDialog fieldPropertiesDialog, TableColumn tableColumn, ColumnOptions options) {
		try {
			mainController.updateStatus("Updating tablecolumn...", true);
			TableController tableController = new TableController(this);
			tableController.editTableColumn(tableColumn, options.toColumn());
			reloadSelectedTable();
			fieldPropertiesDialog.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Edit table column", e);
		}
	}

	public void dropTableColumn() {
		TableColumn column = selectedColumn("Drop column");
		if (column == null) {
			return;
		}

		try {
			mainController.updateStatus("Deleting tablecolumn...", true);
			TableController tableController = new TableController(this);
			tableController.dropTableColumn(column);
			view.branch().deleteTableColumn(column);
			mainController.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop column", e);
		}
	}

	public void reloadSelectedTable() {
		Table table = selectedTable("Reload columns");
		if (table != null) {
			this.loadColumns(table);
		}
	}

	/** Reloads the tables of the selected schema (or of the schema of the selected table), otherwise the content of the selected database. */
	public void reloadSelectedDatabase() {
		Schema schema = view.getSchema();

		if (schema != null && hasSchemas()) {
			this.loadSchema(schema);
		} else {
			Database database = selectedDatabase("Reload");
			if (database != null) {
				this.loadDatabase(database);
			}
		}
	}

	/** The database selected in the tree (or the one of the selected node); without one the user is asked to select it and the result is null. */
	private Database selectedDatabase(String action) {
		Database database = view.getDatabase();
		if (database == null) {
			Dialogs.warn(parent(), action, "Select a " + dialect().databaseTerm() + " in the tree first.");
		}
		return database;
	}

	/** The table selected in the tree (or the table of the selected column); without one the user is asked to select it and the result is null. */
	private Table selectedTable(String action) {
		Table table = view.getTable();
		if (table == null) {
			Dialogs.warn(parent(), action, "Select a table in the tree first.");
		}
		return table;
	}

	/** The column selected in the tree; without one the user is asked to select it and the result is null. */
	private TableColumn selectedColumn(String action) {
		TableColumn column = view.getTableColumn();
		if (column == null) {
			Dialogs.warn(parent(), action, "Select a column in the tree first.");
		}
		return column;
	}

	/** The schema selected in the tree; without one the user is asked to select it and the result is null. */
	private Schema selectedSchema(String action) {
		Schema schema = view.getSchema();
		if (schema == null) {
			Dialogs.warn(parent(), action, "Select a " + dialect().schemaTerm() + " in the tree first.");
		}
		return schema;
	}

	/** Whether a database of this server holds schemas, which hold the tables. */
	private boolean hasSchemas() {
		return dialect().supports(Dialect.Feature.SCHEMAS);
	}

	public Dialect dialect() {
		return session.getConnectionProfile().getServerType().getDialect();
	}

	public void startCreateSchema() {
		String name = Dialogs.input(parent(), "Create " + dialect().schemaTerm(), "&Name:", "Create");

		if (name != null) {
			createSchema(name);
		}
	}

	public void renameSchema() {
		String term = dialect().schemaTerm();
		Schema schema = selectedSchema("Rename " + term);
		if (schema == null) {
			return;
		}
		String name = Dialogs.input(parent(), "Rename " + term, "&New name:", "Rename", schema.getName(),
			value -> Validation.first(Validation.required("a name", value), value.equals(schema.getName()) ? "Enter another name." : null));

		if (name == null) {
			return;
		}

		try {
			mainController.updateStatus("Renaming " + term + "...", true);
			new DatabaseController(this).renameSchema(schema, name);
			loadDatabase(schema.getDatabase());
			setStatusDetail(schema.getDatabase().getName() + ": " + term + " " + schema.getName() + " renamed to " + name);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Rename " + term, e);
		}
	}

	public void createSchema(String name) {
		Database database = selectedDatabase("Create " + dialect().schemaTerm());
		if (database == null) {
			return;
		}

		try {
			mainController.updateStatus("Creating " + dialect().schemaTerm() + "...", true);
			Schema schema = new DatabaseController(this).createSchema(database, name);
			loadDatabase(database);
			setStatusDetail(database.getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " created");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Create " + dialect().schemaTerm(), e);
		}
	}

	public void dropSchema() {
		Schema schema = selectedSchema("Drop " + dialect().schemaTerm());
		if (schema == null) {
			return;
		}

		try {
			mainController.updateStatus("Dropping " + dialect().schemaTerm() + "...", true);
			new DatabaseController(this).dropSchema(schema);
			view.branch().deleteSchema(schema);
			view.removeViews(viewKey(schema.getDatabase(), schema.getName() + "."));
			setStatusDetail(schema.getDatabase().getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " dropped");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop " + dialect().schemaTerm(), e);
		}
	}

	/** Shows the tables of the schema or database a table is in, after the list has changed. */
	private void reloadTablesOf(Table table) {
		if (table.getSchema() != null && hasSchemas()) {
			loadSchema(table.getSchema());
		} else {
			loadDatabase(table.getDatabase());
		}
	}

	/** The message for a name the table service refuses, so a dialog can keep itself open. */
	private String tableNameProblem(Table table, String name) {
		try {
			return getContext().tables().newNameProblem(table, name);
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public void renameSelectedTable() {
		Table table = selectedTable("Rename table");
		if (table == null) {
			return;
		}
		String name = Dialogs.input(parent(), "Rename table", "&New name:", "Rename", table.getName(), value -> tableNameProblem(table, value));

		if (name == null) {
			return;
		}

		try {
			mainController.updateStatus("Renaming table...", true);
			String oldName = table.getName();
			getContext().tables().renameTable(table, name);
			reloadTablesOf(table);
			setStatusDetail("Table " + oldName + " renamed to " + name);
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Rename table", e);
		}
	}

	public void duplicateSelectedTable() {
		Table table = selectedTable("Duplicate table");
		if (table == null) {
			return;
		}
		DuplicateTableDialog.Request request = DuplicateTableDialog.ask(parent(), table.getName() + "_copy", value -> tableNameProblem(table, value));

		if (request == null) {
			return;
		}

		try {
			mainController.updateStatus("Duplicating table...", true);
			Table copy = getContext().tables().duplicateTable(table, request.name(), request.withData());
			reloadTablesOf(table);
			setStatusDetail("Table " + table.getName() + " duplicated as " + copy.getName() + (request.withData() ? " with its data" : ""));
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Duplicate table", e);
		}
	}

	/** The read-only Properties window of the selected table or database. */
	public void showProperties() {
		try {
			if (selectedObject() instanceof Table table) {
				showTableProperties(table);
			} else if (selectedObject() instanceof Database database) {
				showDatabaseProperties(database);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Properties", e);
		}
	}

	private void showTableProperties(Table table) throws Exception {
		TableInfo info = getContext().tables().describe(table);
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("Name", info.name());
		properties.put(capitalized(dialect().databaseTerm()), info.database());
		if (info.schema() != null) {
			properties.put(capitalized(dialect().schemaTerm()), info.schema());
		}
		properties.put(dialect().getTableTypes().length > 0 ? "Engine" : "Type", info.type());
		properties.put("Comment", info.comment());
		properties.put("Rows", String.valueOf(info.rowCount()));
		properties.put("Columns", String.valueOf(info.columnCount()));
		if (info.sizeBytes() != null) {
			properties.put("Size", ByteSize.format(info.sizeBytes()));
		}
		PropertiesDialog.show(parent(), "Properties of " + info.name(), properties);
	}

	private void showDatabaseProperties(Database database) throws Exception {
		DatabaseInfo info = getContext().databases().properties(database);
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("Name", info.name());
		properties.putAll(info.details());
		properties.put("Tables", String.valueOf(info.tableCount()));
		PropertiesDialog.show(parent(), "Properties of " + info.name(), properties);
	}

	private static String capitalized(String word) {
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	/*
	 * @description: deletes all data from the selected table.
	 */
	public void flushSelectedTable() {
		Table table = selectedTable("Empty table");
		if (table == null) {
			return;
		}

		try {
			mainController.updateStatus("Flushing table data...", true);
			TableController tableController = new TableController(this);
			tableController.flushTable(table);
			mainController.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Empty table", e);
		}
	}

	/**
	 * Loads the children of a node the user expanded in the explorer: the schemas or tables of a database (on PostgreSQL this connects to it), the tables of
	 * a schema, the columns of a table. {@code always} runs on the event thread afterwards, also after a failure, which is reported.
	 */
	public void loadChildren(Object node, Runnable always) {
		switch (node) {
			case Database database -> loadDatabase(database, always);
			case Schema schema -> loadSchema(schema, always);
			case Table table -> loadColumns(table, always);
			case null, default -> always.run();
		}
	}

	/** Shows the schemas (on servers with schemas) or tables of a database in the tree, loaded in the background. */
	public void loadDatabase(Database database) {
		loadDatabase(database, () -> {
		});
	}

	private void loadDatabase(Database database, Runnable always) {
		DatabaseController databaseController = new DatabaseController(this);
		if (hasSchemas()) {
			String schemas = dialect().schemaTerm() + "s";
			inTree("Load " + schemas, "Loading " + schemas + "...", () -> databaseController.getSchemas(database), list -> {
				view.branch().loadSchemas(database, list);
				setStatusDetail(database.getName() + ": " + list.size() + " " + dialect().schemaTerm() + "(s)");
			}, always);
			return;
		}
		inTree("Load tables", "Loading tables...", () -> databaseController.getTables(database), tables -> {
			view.branch().loadTables(database, tables);
			setStatusDetail(database.getName() + ": " + tables.size() + " table(s)");
		}, always);
	}

	/** Shows the tables of a schema in the tree, loaded in the background. */
	public void loadSchema(Schema schema) {
		loadSchema(schema, () -> {
		});
	}

	private void loadSchema(Schema schema, Runnable always) {
		DatabaseController databaseController = new DatabaseController(this);
		inTree("Load tables", "Loading tables...", () -> databaseController.getTables(schema), tables -> {
			view.branch().loadTables(schema, tables);
			setStatusDetail(schema.getDatabase().getName() + "." + schema.getName() + ": " + tables.size() + " table(s)");
		}, always);
	}

	/** Loads the columns of the table in the background and shows them under it in the tree. The data opens on a double click, see openTable. */
	public void loadColumns(Table table) {
		loadColumns(table, () -> {
		});
	}

	private void loadColumns(Table table, Runnable always) {
		TableController tableController = new TableController(this);
		inTree("Load table", "Fetching table columns...", () -> tableController.getColumns(table), columns -> view.branch().loadTableColumns(table, columns),
			always);
	}

	/**
	 * As {@link #inBackground(String, String, Work, Outcome, Runnable)}, with the work of one load at a time per connection: the tree loads share the
	 * connection, and on PostgreSQL loading a database reconnects, so expanding several nodes quickly must not let their loads run into each other.
	 */
	private <T> void inTree(String action, String status, Work<T> work, Outcome<T> done, Runnable always) {
		inBackground(action, status, () -> {
			treeLoads.lock();
			try {
				return work.run();
			} finally {
				treeLoads.unlock();
			}
		}, done, always);
	}

	/** Opens the list of the tables of a database or schema in a tab and puts that tab in front (Show tables). */
	public void openTableList(Object node) {
		Database database = TreeSelection.database(node);
		if (database == null) {
			selectedDatabase("Show tables");
			return;
		}
		Schema schema = node instanceof Schema s && hasSchemas() ? s : null;
		String title = schema == null ? database.getName() : database.getName() + "." + schema.getName();
		DatabaseController databaseController = new DatabaseController(this);

		inTree("Show tables", "Loading tables...", () -> schema == null ? databaseController.getTables(database) : databaseController.getTables(schema),
			tables -> {
				TableListTab tab = new TableListTab(this, database, schema);
				tab.loadTables(tables);
				view.showTableListTab(viewKey(database, schema == null ? "" : schema.getName() + "."), title, tab);
				setViewStatus(title + ": " + tables.size() + " table(s)");
			}, () -> {
			});
	}

	/** Shows columns loaded in the background under the table in the tree, on the event thread. */
	public void showTableColumns(Table table, TableColumn[] columns) {
		view.branch().loadTableColumns(table, columns);
	}

	/** Double click on a table: opens its data in a tab and puts that tab in front. */
	public void openTable(Table table) {
		if (table == null) {
			selectedTable("Open table");
			return;
		}
		showTableData(table);
	}

	/** Opens a new query tab on the database selected in the tree; without a selection on the database the connection uses. */
	public void startQueryTab() {
		startQueryTab(view.getDatabase(), view.getSchema());
	}

	/** Opens a new query tab on a database (and schema, may be null); without a database on the one the connection uses. */
	public void startQueryTab(Database selected, Schema schema) {
		QueryController controller = new QueryController(this);
		if (schema != null && hasSchemas()) {
			controller.startInSchema(schema.getName());
		}
		inBackground("Open query", "Listing databases...", controller::databases,
			databases -> view.showQueryTab(new QueryTab(controller, databases, selected)));
	}

	/** Opens a new query tab on the database (and schema) of a table, with a query on its first rows. */
	public void startQueryTab(Table table) {
		QueryController controller = new QueryController(this);
		if (table.getSchema() != null && hasSchemas()) {
			controller.startInSchema(table.getSchema().getName());
		}
		String quoted = dialect().quote(table.qualifiedName());
		// LIMIT only on servers that page in SQL; the others get the whole table.
		String sql = "SELECT * FROM " + quoted + (dialect().selectPage(quoted, "", 0, 100) != null ? " LIMIT 100" : "") + ";";
		inBackground("Open query", "Listing databases...", controller::databases, databases -> {
			QueryTab tab = new QueryTab(controller, databases, table.getDatabase());
			tab.getEditor().setText(sql);
			view.showQueryTab(tab);
		});
	}

	public void showColumnPropertiesDialog(boolean add, boolean edit) {
		if (edit ? selectedColumn("Edit column") == null : selectedTable("Add column") == null) {
			return;
		}

		try {
			mainController.updateStatus("Starting field properties interface...", true);
			ColumnPropertiesDialog fieldPropertiesDialog = new ColumnPropertiesDialog(mainController.getMainWindow(), this, view.getTableColumn(),
				add, edit);
			mainController.showConnectionState();
			fieldPropertiesDialog.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load columns", e);
		}
	}

	/*
	 * @description: Let the tree generate its own events.
	 */
	public void selectTableInTree(Table table) {
		view.branch().selectTable(table);
	}

	/** Loads the first page of the table in the background and shows it in the view tab. */
	public void showTableData(Table table) {
		long start = System.nanoTime();
		TableController controller = new TableController(this);
		String place = table.getSchema() != null && hasSchemas()
			? table.getDatabase().getName() + "." + table.getSchema().getName()
			: table.getDatabase().getName();

		inBackground("Load table data", "Loading table data...", () -> controller.loadPage(table, 0, 50), rows -> {
			String name = table.getSchema() != null && hasSchemas() ? table.getSchema().getName() + "." + table.getName() : table.getName();
			view.showTableDataTab(viewKey(table.getDatabase(), name), table.getName(), controller.newTableDataTab(table, rows));
			setViewStatus(place + "." + table.getName() + ": " + table.getRowCount() + " row(s), loaded in " + millisSince(start) + " ms");
		});
	}

	/*
	 * @description: 	Starts the table indexes manager
	 *
	 */
	public void showIndexesTab() {
		Table table = selectedTable("Indexes");
		if (table != null) {
			showIndexesTab(table);
		}
	}

	public void showIndexesTab(Table table) {
		try {
			IndexesController indexesController = new IndexesController(this, table);
			indexesController.showTab();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load indexes", e);
		}
	}

	public void showUserManagerDialog() {
		UserManagerController userManagerController = new UserManagerController(this);
		userManagerController.showDialog(mainController.getMainWindow());
	}

	/** Export and import of this connection, the same windows as in the Tools menu. */
	public void showExportDialog() {
		if (requireFeature(Dialect.Feature.EXPORT, "Export")) {
			new nl.errorsoft.esql.exporter.control.ExportController(mainController).startExport(this);
		}
	}

	public void showImportDialog() {
		if (requireFeature(Dialect.Feature.IMPORT, "Import")) {
			new nl.errorsoft.esql.importer.control.ImportController(mainController).startImport(this);
		}
	}

	public void showProcessListDialog() {
		if (requireFeature(Dialect.Feature.PROCESS_LIST, "The process list")) {
			new ProcessListController(getConnectionProfile(), mainController.getMainWindow()).start();
		}
	}

	/** Tells the user when the database of this connection can't do what they asked. */
	public boolean requireFeature(Dialect.Feature feature, String description) {
		if (session.getConnectionProfile().getServerType().getDialect().supports(feature)) {
			return true;
		}

		Dialogs.info(parent(), description,
			description + " is not available for " + session.getConnectionProfile().getServerType().getDescription() + ".");
		return false;
	}

	public void showCreateTableTab() {
		Database database = selectedDatabase("Create table");
		if (database != null) {
			showCreateTableTab(database, view.getSchema());
		}
	}

	/** Opens the editor for a new table in the database, in the schema on servers with schemas (null: the current one). */
	public void showCreateTableTab(Database database, Schema schema) {
		try {
			new TableEditorController(this).startCreateTable(database, hasSchemas() ? schema : null);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Create table", e);
		}
	}

	/** Reads the tables and foreign keys of the selected database (the selected schema, or the current one) and opens them in the designer, arranged automatically. */
	public void openDatabaseInDesigner() {
		if (!requireFeature(Dialect.Feature.DESIGNER, "The designer")) {
			return;
		}

		Database database = selectedDatabase("Open in designer");
		if (database == null) {
			return;
		}

		try {
			Schema schema = hasSchemas() && selectedObject() instanceof Schema selected ? selected : null;
			mainController.updateStatus("Reading database structure...", true);
			DesignedDatabase designed = schema != null ? getContext().designer().reverseEngineer(schema) : getContext().designer().reverseEngineer(database);
			Model model = ModelFactory.fromDatabase(designed, session.getConnectionProfile().getServerType().getDataTypes());
			model.setServerType(session.getConnectionProfile().getServerType());
			setStatusDetail((schema != null ? database.getName() + "." + schema.getName() : database.getName()) + ": " + designed.tables().size()
				+ " table(s) opened in the designer");
			mainController.showConnectionState();
			new DesignerWindow(mainController.getMainWindow(), this, model);
		} catch (Exception e) {
			mainController.showConnectionState();
			ApplicationContext.get().errors().report(parent(), "Open in designer", e);
		}
	}

	public void showEditTableTab() {
		Table table = selectedTable("Edit table");
		if (table != null) {
			showEditTableTab(table);
		}
	}

	public void showEditTableTab(Table table) {
		try {
			new TableEditorController(this).startEditTable(table.getDatabase(), table);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Modify table", e);
		}
	}

	/** The name of the profile: two connections to the same server as the same user are told apart by it. */
	public String getTitle() {
		return session.getConnectionProfile().getName();
	}

	/** What is selected in the tree (a database, schema, table, ...), null when nothing is. */
	public Object selectedObject() {
		return view.selectedObject();
	}

	/**
	 * The key of the window with the table list ({@code name} empty) or the table data of a table of a database; all windows of a database (or of a schema,
	 * {@code name} "schema.") share the key prefix, see {@link ConnectionView#removeViews}.
	 */
	private static String viewKey(Database database, String name) {
		return "view:" + database.getName() + "/" + name;
	}

	/** The node of this connection in the explorer. */
	public ConnectionNode getNode() {
		return node;
	}

	/** Where this connection shows its tree, tabs and messages. */
	public ConnectionView getView() {
		return view;
	}

	/** The parent of the messages and dialogs of this connection. */
	private java.awt.Component parent() {
		return view.dialogParent();
	}

	public MainWindow getMainWindow() {
		return mainController.getMainWindow();
	}

	/** The services of this connection, created on first use. Fails when the connection is lost. */
	public ConnectionContext getContext() throws Exception {
		return ApplicationContext.get().connection(getDatabaseConnection());
	}

	public nl.errorsoft.esql.jdbc.DatabaseConnection getDatabaseConnection() throws Exception {
		if (!session.getDatabaseConnection().getConnection().isClosed()) {
			return session.getDatabaseConnection();
		} else {
			throw new Exception("Connection lost!");
		}
	}

	public ConnectionProfile getConnectionProfile() {
		return session.getConnectionProfile();
	}

	/*
	 * Server options, not every database has them.
	 */
	public void showServerStatus() {
		try {
			if (!requireFeature(Dialect.Feature.SERVER_STATUS, "Server status")) {
				return;
			}

			TableController tableController = new TableController(this);
			view.showTableDataTab("server:status", "Server status", tableController.showServerStatus());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server status", e);
		}
	}
	public void showServerVariables() {
		try {
			if (!requireFeature(Dialect.Feature.SERVER_STATUS, "Server variables")) {
				return;
			}

			TableController tableController = new TableController(this);
			view.showTableDataTab("server:variables", "Server variables", tableController.showServerVariables());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server variables", e);
		}
	}

	public void optimizeTable() {
		maintainSelectedTable(Dialect.Maintenance.OPTIMIZE);
	}

	public void analyzeTable() {
		maintainSelectedTable(Dialect.Maintenance.ANALYZE);
	}

	public void checkTable() {
		maintainSelectedTable(Dialect.Maintenance.CHECK);
	}

	public void repairTable() {
		maintainSelectedTable(Dialect.Maintenance.REPAIR);
	}

	/** "Optimize table", the words of a maintenance command for its menu item, button and report. */
	public static String maintenanceLabel(Dialect.Maintenance command) {
		String word = command.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(word.charAt(0)) + word.substring(1) + " table";
	}

	private void maintainSelectedTable(Dialect.Maintenance command) {
		Table table = selectedTable(maintenanceLabel(command));
		if (table != null) {
			maintainTables(command, List.of(table));
		}
	}

	/** Runs a maintenance command on the tables in the background, one after the other, and shows the server's report. */
	public void maintainTables(Dialect.Maintenance command, List<Table> tables) {
		String action = maintenanceLabel(command);
		TableController controller = new TableController(this);
		inBackground(action, action + "...", () -> {
			StringBuilder report = new StringBuilder();
			for (Table table : tables) {
				String result = switch (command) {
					case OPTIMIZE -> controller.optimizeTable(table);
					case ANALYZE -> controller.analyzeTable(table);
					case CHECK -> controller.checkTable(table);
					case REPAIR -> controller.repairTable(table);
				};
				report.append(tables.size() > 1 ? table.getName() + ": " : "").append(result).append(tables.size() > 1 ? "\n" : "");
			}
			return report.toString().strip();
		}, report -> Dialogs.info(parent(), tables.size() == 1 ? action + ": " + tables.getFirst().getName() : action, report));
	}
}

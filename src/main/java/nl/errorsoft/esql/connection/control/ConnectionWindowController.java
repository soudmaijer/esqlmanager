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
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.server.control.ProcessListController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.designer.ui.diagram.ModelFactory;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.user.control.UserManagerController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import java.util.*;
import javax.swing.SwingUtilities;

public class ConnectionWindowController {
	private String statusDetail = "";
	private static final Logger log = LogManager.getLogger(ConnectionWindowController.class);

	private MainController mainController;
	private ConnectionSession session;
	private ConnectionWindow connectionWindow;
	private ConnectionView view;
	private TableController tableController;

	/** Opens the connection window at once and connects in the background; the tree is filled when the databases are listed. */
	public ConnectionWindowController(MainController mainController, nl.errorsoft.esql.connection.ConnectionProfile profile) {
		this.mainController = mainController;
		this.session = new ConnectionSession(this, profile);
		SwingUtilities.invokeLater(this::open);
	}

	/** Shows the window on the event thread, then connects and lists the databases on a virtual thread. */
	private void open() {
		ConnectionProfile profile = session.getConnectionProfile();
		log.info("Opening a connection window for {} on {}:{} as {}", profile.getServerType().getDescription(), profile.getHost(), profile.getPort(),
			profile.getUsername());
		connectionWindow = new ConnectionWindow(this, mainController.getMainWindow());
		view = connectionWindow;
		mainController.showConnectionWindow(connectionWindow);
		mainController.updateStatus("Connecting...", true);

		Thread.ofVirtual().name("connect").start(() -> {
			try {
				session.start();
				SwingUtilities.invokeLater(() -> mainController.updateStatus("Loading databases...", true));
				List<Database> databases = getContext().databases().getDatabases();
				SwingUtilities.invokeLater(() -> {
					showDatabaseTree(databases);
					setStatusDetail("");
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					connectionWindow.closeWindow(false);
					mainController.updateStatus("Cannot connect to server...", true);
					ApplicationContext.get().errors().report("Connect to " + profile.getName(), e);
				});
			}
		});
	}

	/** Shows the server and account of this connection in the status bar of the application. */
	public void showStatusInfo() {
		String info = "";

		try {
			info = getDatabaseConnection().getServerDescription() + "  |  " + session.getConnectionProfile().getUsername() + "@"
				+ session.getConnectionProfile().getHost() + ":" + session.getConnectionProfile().getPort();
		} catch (Exception e) {
			// Not connected (yet), there is nothing to show.
		}

		mainController.setStatusInfo(info);
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
			try {
				T result = work.run();
				SwingUtilities.invokeLater(() -> {
					mainController.showConnectionState();
					try {
						done.accept(result);
					} catch (Exception e) {
						ApplicationContext.get().errors().report(parent(), action, e);
					} finally {
						always.run();
					}
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					mainController.showConnectionState();
					try {
						ApplicationContext.get().errors().report(parent(), action, e);
					} finally {
						always.run();
					}
				});
			}
		});
	}

	/** Closes the designers opened from this connection; false when the user keeps one open. */
	public boolean closeDesigners() {
		return mainController.getMainWindow().closeDesigners(connectionWindow);
	}

	public void closeWindow() {
		// Stop database connection
		try {
			ApplicationContext.get().release(session.getDatabaseConnection());
			session.stop();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Disconnect", e);
		}

		// Remove references
		mainController.removeConnectionWindow(connectionWindow);

		// Close Internalframe
		connectionWindow.dispose();
	}

	/** Lists the databases again in the background and shows them in the tree. */
	public void showDatabaseTree() {
		DatabaseController databaseController = new DatabaseController(this);
		inBackground("Load databases", "Loading databases...", databaseController::getDatabases, this::showDatabaseTree);
	}

	/** Shows the databases listed on another thread in the tree, on the event thread. */
	private void showDatabaseTree(List<Database> databases) {
		view.showDatabaseTree(new DatabaseController(this).databaseTree(databases));
		mainController.showConnectionState();
	}

	/** Asks for the name and the options of the new database (character set, owner, ...) and creates it. */
	public void startCreateDatabase() {
		try {
			CreateDatabaseDialog.Request request = CreateDatabaseDialog.ask(parent(), dialect().databaseTerm(), dialect().createDatabaseOptions(),
				getContext().databases().createDatabaseChoices(), view.getDatabaseTree().databaseNames());

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
			view.getDatabaseTree().addDatabase(db);
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
			view.getDatabaseTree().deleteDatabase(db);
			view.removeDataTab();
			mainController.showConnectionState();
			this.showDatabaseTree();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop database", e);
		}
	}

	public void dropTable() {
		Table table = selectedTable("Drop table");
		if (table == null) {
			return;
		}

		try {
			mainController.updateStatus("Deleting table...", true);
			TableController tableController = new TableController(this);
			tableController.dropTable(table);
			view.getDatabaseTree().deleteTable(table);
			mainController.showConnectionState();
			if (table.getSchema() != null && hasSchemas()) {
				this.schemaSelected(table.getSchema());
			} else {
				this.databaseSelected(table.getDatabase());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop table", e);
		}
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
			view.getDatabaseTree().deleteTableColumn(column);
			mainController.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop column", e);
		}
	}

	public void reloadSelectedTable() {
		Table table = selectedTable("Reload columns");
		if (table != null) {
			this.tableSelected(table, true);
		}
	}

	/** Reloads the tables of the selected schema (or of the schema of the selected table), otherwise the content of the selected database. */
	public void reloadSelectedDatabase() {
		Schema schema = view.getSchema();

		if (schema != null && hasSchemas()) {
			this.schemaSelected(schema);
		} else {
			Database database = selectedDatabase("Reload");
			if (database != null) {
				this.databaseSelected(database);
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
			databaseSelected(schema.getDatabase());
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
			databaseSelected(database);
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
			view.getDatabaseTree().deleteSchema(schema);
			view.removeDataTab();
			setStatusDetail(schema.getDatabase().getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " dropped");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Drop " + dialect().schemaTerm(), e);
		}
	}

	/** Shows the tables of the schema or database a table is in, after the list has changed. */
	private void reloadTablesOf(Table table) {
		if (table.getSchema() != null && hasSchemas()) {
			schemaSelected(table.getSchema());
		} else {
			databaseSelected(table.getDatabase());
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

	/*
	 * @description: shows the tables of the selected database in the tree. The tab with the table list opens on a double click, see openDatabase.
	 */
	public void databaseSelected(Database database) {
		if (hasSchemas()) {
			loadSchemas(database);
			return;
		}

		try {
			mainController.updateStatus("Loading tables...", true);

			DatabaseController databaseController = new DatabaseController(this);
			java.util.List<Table> tables = databaseController.getTables(database);
			view.getDatabaseTree().loadTables(database, tables);
			view.databaseSelected();
			setStatusDetail(database.getName() + ": " + tables.size() + " table(s)");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load tables", e);
		}
	}

	/** Shows the schemas of a database in the tree; on PostgreSQL this connects to that database. */
	private void loadSchemas(Database database) {
		String schemas = dialect().schemaTerm() + "s";

		try {
			mainController.updateStatus("Loading " + schemas + "...", true);
			java.util.List<Schema> list = new DatabaseController(this).getSchemas(database);
			view.getDatabaseTree().loadSchemas(database, list);
			view.databaseSelected();
			setStatusDetail(database.getName() + ": " + list.size() + " " + dialect().schemaTerm() + "(s)");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load " + schemas, e);
		}
	}

	/** Shows the tables of a schema in the tree. */
	public void schemaSelected(Schema schema) {
		try {
			mainController.updateStatus("Loading tables...", true);
			java.util.List<Table> tables = new DatabaseController(this).getTables(schema);
			view.getDatabaseTree().loadTables(schema, tables);
			view.databaseSelected();
			setStatusDetail(schema.getDatabase().getName() + "." + schema.getName() + ": " + tables.size() + " table(s)");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load tables", e);
		}
	}

	/** Double click on a database: opens the table list in a tab and puts that tab in front. */
	public void openDatabase(Database database) {
		if (database == null) {
			selectedDatabase("Open " + dialect().databaseTerm());
			return;
		}

		try {
			mainController.updateStatus("Loading tables...", true);

			DatabaseController databaseController = new DatabaseController(this);
			java.util.List<Table> tables = databaseController.getTables(database);
			view.showTableListTab(database.getName(), databaseController.getTableListTab(tables));
			setViewStatus(database.getName() + ": " + tables.size() + " table(s)");
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Open database", e);
		}
	}

	/*
	 * @description: loads the columns of the selected table in the tree. The data opens on a double click, see openTable.
	 */
	public void tableSelected(Table table, boolean addTreeColumns) {
		try {
			if (addTreeColumns) {
				mainController.updateStatus("Fetching table columns...", true);
				TableColumn[] fields = new TableController(this).getColumns(table);
				view.getDatabaseTree().loadTableColumns(table, fields);
			}

			view.tableSelected();
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Load table", e);
		}
	}

	/** Shows columns loaded in the background under the table in the tree, on the event thread. */
	public void showTableColumns(Table table, TableColumn[] columns) {
		view.getDatabaseTree().loadTableColumns(table, columns);
		view.tableSelected();
	}

	/** Double click on a table: opens its data in a tab and puts that tab in front. */
	public void openTable(Table table) {
		if (table == null) {
			selectedTable("Open table");
			return;
		}
		showTableData(table);
	}

	public void fieldSelected() {
		view.fieldSelected();
	}

	public void rootSelected() {
		view.rootSelected();
	}

	public void insertNewRow() {
		tableController.insertNewRow();
	}

	public void deleteSelectedRows() {
		tableController.deleteSelectedRows();
	}

	public void saveSelectedRow() {
		tableController.saveSelectedRow();
	}

	/** Opens a new query tab on the database selected in the tree; without a selection on the database the connection uses. */
	public void startQueryTab() {
		QueryController controller = new QueryController(this);
		Database selected = view.getDatabase();
		inBackground("Open query", "Listing databases...", controller::databases,
			databases -> view.showQueryTab(new QueryTab(controller, databases, selected)));
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
		view.getDatabaseTree().selectTableInTree(table);
	}

	/** Loads the first page of the table in the background and shows it in the view tab. */
	public void showTableData(Table table) {
		long start = System.nanoTime();
		TableController controller = new TableController(this);
		String place = table.getSchema() != null && hasSchemas()
			? table.getDatabase().getName() + "." + table.getSchema().getName()
			: table.getDatabase().getName();

		inBackground("Load table data", "Loading table data...", () -> controller.loadPage(table, 0, 50), rows -> {
			tableController = controller;
			view.showTableDataTab(place + " : " + table.getName(), controller.newTableDataTab(table, rows));
			setViewStatus(place + "." + table.getName() + ": " + table.getRowCount() + " row(s), loaded in " + millisSince(start) + " ms");
		});
	}

	/*
	 * @description: 	Starts the table indexes manager
	 *
	 */
	public void showIndexesTab() {
		Table table = selectedTable("Indexes");
		if (table == null) {
			return;
		}

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
		if (database == null) {
			return;
		}

		try {
			new TableEditorController(this).startCreateTable(database, hasSchemas() ? view.getSchema() : null);
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
			setStatusDetail((schema != null ? database.getName() + "." + schema.getName() : database.getName()) + ": " + designed.tables().size()
				+ " table(s) opened in the designer");
			mainController.showConnectionState();
			new DesignerWindow(mainController.getMainWindow(), connectionWindow, model);
		} catch (Exception e) {
			mainController.showConnectionState();
			ApplicationContext.get().errors().report(parent(), "Open in designer", e);
		}
	}

	public void showEditTableTab() {
		Table table = selectedTable("Edit table");
		if (table == null) {
			return;
		}

		try {
			new TableEditorController(this).startEditTable(table.getDatabase(), table);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(parent(), "Modify table", e);
		}
	}

	public String getTitle() {
		return session.getConnectionProfile().getUsername() + "@" + session.getConnectionProfile().getHost();
	}

	/** What is selected in the tree (a database, schema, table, ...), null when nothing is. */
	public Object selectedObject() {
		return view.selectedObject();
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
			view.showTableDataTab("Server status", tableController.showServerStatus());
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
			view.showTableDataTab("Server variables", tableController.showServerVariables());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server variables", e);
		}
	}

	public void optimizeTable() {
		maintainSelectedTable("Optimize table", TableController::optimizeTable);
	}

	public void analyzeTable() {
		maintainSelectedTable("Analyze table", TableController::analyzeTable);
	}

	public void checkTable() {
		maintainSelectedTable("Check table", TableController::checkTable);
	}

	public void repairTable() {
		maintainSelectedTable("Repair table", TableController::repairTable);
	}

	/** A maintenance command on a table that returns the server's report. */
	private interface Maintenance {
		String run(TableController controller, Table table) throws Exception;
	}

	/** Runs a maintenance command on the selected table in the background and shows the server's report. */
	private void maintainSelectedTable(String action, Maintenance maintenance) {
		Table table = selectedTable(action);
		if (table == null) {
			return;
		}

		TableController controller = new TableController(this);
		inBackground(action, action + "...", () -> maintenance.run(controller, table),
			report -> Dialogs.info(parent(), action + ": " + table.getName(), report));
	}
}

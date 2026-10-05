package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.CreateTableCC;
import nl.errorsoft.esql.table.control.IndexesCC;
import nl.errorsoft.esql.table.control.TableCC;
import nl.errorsoft.esql.table.ui.dialog.FieldPropertiesDialog;
import nl.errorsoft.esql.table.ui.TableDataTab;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

import nl.errorsoft.esql.connection.ConnectionContext;

import nl.errorsoft.esql.query.control.QueryCC;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseInfo;
import nl.errorsoft.esql.database.ui.dialog.CreateDatabaseDialog;
import nl.errorsoft.esql.table.TableInfo;
import nl.errorsoft.esql.table.ui.dialog.DuplicateTableDialog;
import nl.errorsoft.esql.ui.util.ByteSize;
import nl.errorsoft.esql.ui.dialog.PropertiesDialog;
import nl.errorsoft.esql.ui.util.Validation;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ConnectionSession;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.server.control.ProcesslistCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.designer.ui.diagram.ModelFactory;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.user.control.UserManagerCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import java.util.*;

public class ConnectionWindowCC extends Thread {
	private String statusDetail = "";
	private static final Logger log = LogManager.getLogger(ConnectionWindowCC.class);

	private ESQLManagerCC jmcc;
	private ConnectionSession cw;
	private ConnectionWindow connectionWindow;
	private TableCC tbcc;

	public ConnectionWindowCC(ESQLManagerCC jmcc, nl.errorsoft.esql.connection.ConnectionProfile cp) {
		this.jmcc = jmcc;
		this.cw = new ConnectionSession(this, cp);
		this.start();
	}

	public void run() {
		// Create Frame.
		log.info("Connecting to `" + cw.getConnectionProfile().getServerType().getDescription() + "` @ `" + cw.getConnectionProfile().getHost()
			+ "` with username `" + cw.getConnectionProfile().getUsername() + "` on port `" + cw.getConnectionProfile().getPort() + "`");
		connectionWindow = new ConnectionWindow(this, jmcc.getUI());

		// Create database connection to Server.
		jmcc.showConnectionWindow(connectionWindow);
		jmcc.updateStatus("Connecting...", true);

		try {
			cw.start();
			jmcc.updateStatus("Loading databases...", true);
			showDatabaseTree();
			setStatusDetail("");
			jmcc.showConnectionState();
		} catch (Exception e) {
			connectionWindow.closeUI(false);
			jmcc.updateStatus("Cannot connect to server...", true);
			ApplicationContext.get().errors().report("Connect to " + cw.getConnectionProfile().getName(), e);
		}
	}

	/** Shows the server and account of this connection in the status bar of the application. */
	public void showStatusInfo() {
		String info = "";

		try {
			info = getDatabaseConnection().getServerDescription() + "  |  " + cw.getConnectionProfile().getUsername() + "@"
				+ cw.getConnectionProfile().getHost() + ":" + cw.getConnectionProfile().getPort();
		} catch (Exception e) {
			// Not connected (yet), there is nothing to show.
		}

		jmcc.setStatusInfo(info);
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
		connectionWindow.setStatus(detail);
		showStatusInfo();
	}

	/** A message about one tab (a query tab), shown in the status bar while that tab is in front. */
	public void setStatusDetail(java.awt.Component tab, String detail) {
		statusDetail = detail;
		connectionWindow.setStatus(tab, detail);
		showStatusInfo();
	}

	private long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}

	/** Closes the designers opened from this connection; false when the user keeps one open. */
	public boolean closeDesigners() {
		return jmcc.getUI().closeDesigners(connectionWindow);
	}

	public void closeUI() {
		// Stop database connection
		try {
			ApplicationContext.get().release(cw.getDatabaseConnection());
			cw.stop();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Disconnect", e);
		}

		// Remove references
		jmcc.removeConnectionWindow(connectionWindow);

		// Close Internalframe
		connectionWindow.dispose();
	}

	public void showDatabaseTree() {
		try {
			// Load databases into JTree.
			jmcc.updateStatus("Loading databases...", true);
			DatabaseCC dbcc = new DatabaseCC(this);
			connectionWindow.showDatabaseTree(dbcc.getDatabaseTree());
			connectionWindow.showHelp();
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load databases", e);
		}
	}

	/** Asks for the name and the options of the new database (character set, owner, ...) and creates it. */
	public void startCreateDatabase() {
		try {
			CreateDatabaseDialog.Request request = CreateDatabaseDialog.ask(connectionWindow, dialect().databaseTerm(), dialect().createDatabaseOptions(),
				getContext().databases().createDatabaseChoices(), connectionWindow.getDatabaseTree().databaseNames());

			if (request != null) {
				createDatabase(request.name(), request.options());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Create " + dialect().databaseTerm(), e);
		}
	}

	private void createDatabase(String name, Map<String, String> options) {
		try {
			jmcc.updateStatus("Creating " + dialect().databaseTerm() + "...", true);
			Database db = getContext().databases().createDatabase(name, options);
			connectionWindow.getDatabaseTree().addDatabase(db);
			setStatusDetail(dialect().databaseTerm() + " " + name + " created");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Create " + dialect().databaseTerm(), e);
		}
	}

	public void dropDatabase() {
		try {
			jmcc.updateStatus("Deleting database...", true);
			Database db = connectionWindow.getDatabase();
			DatabaseCC dbcc = new DatabaseCC(this);
			dbcc.dropDatabase(db);
			connectionWindow.getDatabaseTree().deleteDatabase(db);
			connectionWindow.removeDataTab();
			jmcc.showConnectionState();
			this.showDatabaseTree();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Drop database", e);
		}
	}

	public void dropTable() {
		try {
			jmcc.updateStatus("Deleting table...", true);
			Table tb = connectionWindow.getTable();
			TableCC dbcc = new TableCC(this);
			dbcc.dropTable(tb);
			connectionWindow.getDatabaseTree().deleteTable(tb);
			jmcc.showConnectionState();
			if (tb.getSchema() != null && hasSchemas()) {
				this.schemaSelected(tb.getSchema());
			} else {
				this.databaseSelected(tb.getDatabase());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Drop table", e);
		}
	}

	public void addTableColumn(FieldPropertiesDialog fp, CreateColumn column) {
		try {
			jmcc.updateStatus("Adding tablecolumn...", true);
			TableCC dbcc = new TableCC(this);
			dbcc.addTableColumn(connectionWindow.getTable(), column);
			reloadSelectedTable();
			fp.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Add table column", e);
		}
	}

	public void editTableColumn(FieldPropertiesDialog fp, TableColumn tbc, CreateColumn column) {
		try {
			jmcc.updateStatus("Updating tablecolumn...", true);
			TableCC dbcc = new TableCC(this);
			dbcc.editTableColumn(tbc, column);
			reloadSelectedTable();
			fp.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Edit table column", e);
		}
	}

	public void dropTableColumn() {
		try {
			jmcc.updateStatus("Deleting tablecolumn...", true);
			TableColumn tb = connectionWindow.getTableColumn();
			TableCC dbcc = new TableCC(this);
			dbcc.dropTableColumn(tb);
			connectionWindow.getDatabaseTree().deleteTableColumn(tb);
			jmcc.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Drop column", e);
		}
	}

	public void reloadSelectedTable() {
		this.tableSelected(connectionWindow.getTable(), true);
	}

	/** Reloads the tables of the selected schema (or of the schema of the selected table), otherwise the content of the selected database. */
	public void reloadSelectedDatabase() {
		Schema schema = connectionWindow.getSchema();

		if (schema != null && hasSchemas()) {
			this.schemaSelected(schema);
		} else {
			this.databaseSelected(connectionWindow.getDatabase());
		}
	}

	/** Whether a database of this server holds schemas, which hold the tables. */
	private boolean hasSchemas() {
		return dialect().supports(Dialect.Feature.SCHEMAS);
	}

	public Dialect dialect() {
		return cw.getConnectionProfile().getServerType().getDialect();
	}

	public void startCreateSchema() {
		String name = Dialogs.input(connectionWindow, "Create " + dialect().schemaTerm(), "&Name:", "Create");

		if (name != null) {
			createSchema(name);
		}
	}

	public void renameSchema() {
		Schema schema = connectionWindow.getSchema();
		String term = dialect().schemaTerm();
		String name = Dialogs.input(connectionWindow, "Rename " + term, "&New name:", "Rename", schema.getName(),
			value -> Validation.first(Validation.required("a name", value), value.equals(schema.getName()) ? "Enter another name." : null));

		if (name == null) {
			return;
		}

		try {
			jmcc.updateStatus("Renaming " + term + "...", true);
			new DatabaseCC(this).renameSchema(schema, name);
			databaseSelected(schema.getDatabase());
			setStatusDetail(schema.getDatabase().getName() + ": " + term + " " + schema.getName() + " renamed to " + name);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Rename " + term, e);
		}
	}

	public void createSchema(String name) {
		try {
			Database database = connectionWindow.getDatabase();
			jmcc.updateStatus("Creating " + dialect().schemaTerm() + "...", true);
			Schema schema = new DatabaseCC(this).createSchema(database, name);
			databaseSelected(database);
			setStatusDetail(database.getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " created");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Create " + dialect().schemaTerm(), e);
		}
	}

	public void dropSchema() {
		try {
			Schema schema = connectionWindow.getSchema();
			jmcc.updateStatus("Dropping " + dialect().schemaTerm() + "...", true);
			new DatabaseCC(this).dropSchema(schema);
			connectionWindow.getDatabaseTree().deleteSchema(schema);
			connectionWindow.removeDataTab();
			setStatusDetail(schema.getDatabase().getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " dropped");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Drop " + dialect().schemaTerm(), e);
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
		Table table = connectionWindow.getTable();
		String name = Dialogs.input(connectionWindow, "Rename table", "&New name:", "Rename", table.getName(), value -> tableNameProblem(table, value));

		if (name == null) {
			return;
		}

		try {
			jmcc.updateStatus("Renaming table...", true);
			String oldName = table.getName();
			getContext().tables().renameTable(table, name);
			reloadTablesOf(table);
			setStatusDetail("Table " + oldName + " renamed to " + name);
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Rename table", e);
		}
	}

	public void duplicateSelectedTable() {
		Table table = connectionWindow.getTable();
		DuplicateTableDialog.Request request = DuplicateTableDialog.ask(connectionWindow, table.getName() + "_copy", value -> tableNameProblem(table, value));

		if (request == null) {
			return;
		}

		try {
			jmcc.updateStatus("Duplicating table...", true);
			Table copy = getContext().tables().duplicateTable(table, request.name(), request.withData());
			reloadTablesOf(table);
			setStatusDetail("Table " + table.getName() + " duplicated as " + copy.getName() + (request.withData() ? " with its data" : ""));
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Duplicate table", e);
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
			ApplicationContext.get().errors().report(connectionWindow, "Properties", e);
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
		PropertiesDialog.show(connectionWindow, "Properties of " + info.name(), properties);
	}

	private void showDatabaseProperties(Database database) throws Exception {
		DatabaseInfo info = getContext().databases().properties(database);
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("Name", info.name());
		properties.putAll(info.details());
		properties.put("Tables", String.valueOf(info.tableCount()));
		PropertiesDialog.show(connectionWindow, "Properties of " + info.name(), properties);
	}

	private static String capitalized(String word) {
		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}

	/*
	 * @description: deletes all data from the selected table.
	 */
	public void flushSelectedTable() {
		try {
			jmcc.updateStatus("Flushing table data...", true);
			TableCC tbcc = new TableCC(this);
			tbcc.flushTable(connectionWindow.getTable());
			jmcc.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Empty table", e);
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
			jmcc.updateStatus("Loading tables...", true);

			DatabaseCC dbcc = new DatabaseCC(this);
			java.util.List<Table> tables = dbcc.getTables(database);
			connectionWindow.getDatabaseTree().loadTables(database, tables);
			connectionWindow.databaseSelected();
			setStatusDetail(database.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load tables", e);
		}
	}

	/** Shows the schemas of a database in the tree; on PostgreSQL this connects to that database. */
	private void loadSchemas(Database database) {
		String schemas = dialect().schemaTerm() + "s";

		try {
			jmcc.updateStatus("Loading " + schemas + "...", true);
			java.util.List<Schema> list = new DatabaseCC(this).getSchemas(database);
			connectionWindow.getDatabaseTree().loadSchemas(database, list);
			connectionWindow.databaseSelected();
			setStatusDetail(database.getName() + ": " + list.size() + " " + dialect().schemaTerm() + "(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load " + schemas, e);
		}
	}

	/** Shows the tables of a schema in the tree. */
	public void schemaSelected(Schema schema) {
		try {
			jmcc.updateStatus("Loading tables...", true);
			java.util.List<Table> tables = new DatabaseCC(this).getTables(schema);
			connectionWindow.getDatabaseTree().loadTables(schema, tables);
			connectionWindow.databaseSelected();
			setStatusDetail(schema.getDatabase().getName() + "." + schema.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load tables", e);
		}
	}

	/** Double click on a database: opens the table list in a tab and puts that tab in front. */
	public void openDatabase(Database database) {
		try {
			jmcc.updateStatus("Loading tables...", true);

			DatabaseCC dbcc = new DatabaseCC(this);
			java.util.List<Table> tables = dbcc.getTables(database);
			connectionWindow.showTableListTab(database.getName(), dbcc.getTableListTab(tables));
			setViewStatus(database.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Open database", e);
		}
	}

	/*
	 * @description: loads the columns of the selected table in the tree. The data opens on a double click, see openTable.
	 */
	public void tableSelected(Table table, boolean addTreeColumns) {
		try {
			if (addTreeColumns) {
				jmcc.updateStatus("Fetching table columns...", true);
				TableColumn[] fields = new TableCC(this).getColumns(table);
				connectionWindow.getDatabaseTree().loadTableColumns(table, fields);
			}

			connectionWindow.tableSelected();
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load table", e);
		}
	}

	/** Double click on a table: opens its data in a tab and puts that tab in front. */
	public void openTable(Table table) {
		showTableData(table);
	}

	public void fieldSelected() {
		connectionWindow.fieldSelected();
	}

	public void rootSelected() {
		connectionWindow.rootSelected();
	}

	public void insertNewRow() {
		tbcc.insertNewRow();
	}

	public void deleteSelectedRows() {
		tbcc.deleteSelectedRows();
	}

	public void saveSelectedRow() {
		tbcc.saveSelectedRow();
	}

	/** Opens a new query tab on the database selected in the tree. */
	public void startQueryTab() {
		try {
			QueryCC controller = new QueryCC(this);
			connectionWindow.showQueryTab(new QueryTab(controller, controller.databases(), connectionWindow.getDatabase()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Run query", e);
		}
	}

	public void startFieldUI(boolean add, boolean edit) {
		try {
			jmcc.updateStatus("Starting field properties interface...", true);
			FieldPropertiesDialog fpu = new FieldPropertiesDialog(jmcc.getUI(), this, connectionWindow.getTableColumn(), add, edit);
			jmcc.showConnectionState();
			fpu.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load columns", e);
		}
	}

	/*
	 * @description: Let the tree generate its own events.
	 */
	public void selectTableInTree(Table table) {
		connectionWindow.getDatabaseTree().selectTableInTree(table);
	}

	public void showTableData(Table table) {
		try {
			// Load the tables from the database.
			jmcc.updateStatus("Loading table data...", true);
			long start = System.nanoTime();

			// Let the Table control class handle the data display creation.
			tbcc = new TableCC(this);
			String place = table.getSchema() != null && hasSchemas()
				? table.getDatabase().getName() + "." + table.getSchema().getName()
				: table.getDatabase().getName();
			connectionWindow.showTableDataTab(place + " : " + table.getName(), tbcc.getTableDataTab(table, 0, 50));
			setViewStatus(place + "." + table.getName() + ": " + table.getRowCount() + " row(s), loaded in " + millisSince(start) + " ms");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load table data", e);
		}
	}

	/*
	 * @description: 	Starts the table indexes manager
	 *
	 */
	public void dispatchTableIndexesUI() {
		try {
			IndexesCC tcc = new IndexesCC(this, (Table) connectionWindow.getSelectedNode().getUserObject());
			tcc.startUI();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Load indexes", e);
		}
	}

	public void dispatchUserManagerUI() {
		UserManagerCC umcc = new UserManagerCC(this);
		umcc.startUI(jmcc.getUI());
	}

	/** Export and import of this connection, the same windows as in the Tools menu. */
	public void dispatchExportUI() {
		if (requireFeature(Dialect.Feature.EXPORT, "Export")) {
			new nl.errorsoft.esql.export.control.ExportCC(jmcc).startExport(this);
		}
	}

	public void dispatchImportUI() {
		if (requireFeature(Dialect.Feature.IMPORT, "Import")) {
			new nl.errorsoft.esql.importer.control.ImportCC(jmcc).startImport(this);
		}
	}

	public void dispatchProcessUI() {
		if (requireFeature(Dialect.Feature.PROCESS_LIST, "The process list")) {
			new ProcesslistCC(getConnectionProfile(), jmcc.getUI()).start();
		}
	}

	/** Tells the user when the database of this connection can't do what they asked. */
	public boolean requireFeature(Dialect.Feature feature, String description) {
		if (cw.getConnectionProfile().getServerType().getDialect().supports(feature)) {
			return true;
		}

		Dialogs.info(connectionWindow, description, description + " is not available for " + cw.getConnectionProfile().getServerType().getDescription() + ".");
		return false;
	}

	public void dispatchCreateTableUI() {
		try {
			new CreateTableCC(this).startCreateTable(connectionWindow.getDatabase(), hasSchemas() ? connectionWindow.getSchema() : null);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Create table", e);
		}
	}

	/** Reads the tables and foreign keys of the selected database (the selected schema, or the current one) and opens them in the designer, arranged automatically. */
	public void openDatabaseInDesigner() {
		if (!requireFeature(Dialect.Feature.DESIGNER, "The designer")) {
			return;
		}

		try {
			Database database = connectionWindow.getDatabase();
			Schema schema = hasSchemas() && connectionWindow.getSelectedNode().getUserObject() instanceof Schema selected ? selected : null;
			jmcc.updateStatus("Reading database structure...", true);
			DesignedDatabase designed = schema != null ? getContext().designer().reverseEngineer(schema) : getContext().designer().reverseEngineer(database);
			Model model = ModelFactory.fromDatabase(designed, cw.getConnectionProfile().getServerType().getDataTypes());
			setStatusDetail((schema != null ? database.getName() + "." + schema.getName() : database.getName()) + ": " + designed.tables().size()
				+ " table(s) opened in the designer");
			jmcc.showConnectionState();
			new DesignerWindow(jmcc.getUI(), connectionWindow, model);
		} catch (Exception e) {
			jmcc.showConnectionState();
			ApplicationContext.get().errors().report(connectionWindow, "Open in designer", e);
		}
	}

	public void dispatchModifyTableUI() {
		try {
			new CreateTableCC(this).startEditTable(connectionWindow.getDatabase(), connectionWindow.getTable());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindow, "Modify table", e);
		}
	}

	public String getTitle() {
		return cw.getConnectionProfile().getUsername() + "@" + cw.getConnectionProfile().getHost();
	}

	/** The connection window, the parent of messages and the owner of the tabs. */
	/** What is selected in the tree (a database, schema, table, ...), null when nothing is. */
	public Object selectedObject() {
		return connectionWindow.getSelectedNode() == null ? null : connectionWindow.getSelectedNode().getUserObject();
	}

	public ConnectionWindow getWindow() {
		return connectionWindow;
	}

	public MainWindow getUI() {
		return jmcc.getUI();
	}

	/** The services of this connection, created on first use. Fails when the connection is lost. */
	public ConnectionContext getContext() throws Exception {
		return ApplicationContext.get().connection(getDatabaseConnection());
	}

	public nl.errorsoft.esql.jdbc.DatabaseConnection getDatabaseConnection() throws Exception {
		if (!cw.getDatabaseConnection().getConnection().isClosed()) {
			return cw.getDatabaseConnection();
		} else {
			throw new Exception("Connection lost!");
		}
	}

	public ConnectionProfile getConnectionProfile() {
		return cw.getConnectionProfile();
	}

	/*
	 * Server options, not every database has them.
	 */
	public void showServerStatus() {
		try {
			if (!requireFeature(Dialect.Feature.SERVER_STATUS, "Server status")) {
				return;
			}

			TableCC tcc = new TableCC(this);
			connectionWindow.showTableDataTab("Server status", tcc.showServerStatus());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server status", e);
		}
	}
	public void showServerVariables() {
		try {
			if (!requireFeature(Dialect.Feature.SERVER_STATUS, "Server variables")) {
				return;
			}

			TableCC tcc = new TableCC(this);
			connectionWindow.showTableDataTab("Server variables", tcc.showServerVariables());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server variables", e);
		}
	}

	public void optimizeTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(connectionWindow, "Optimize table: " + connectionWindow.getTable().getName(), tcc.optimizeTable(connectionWindow.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Optimize table", e);
		}
	}

	public void analyseTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(connectionWindow, "Analyze table: " + connectionWindow.getTable().getName(), tcc.analyseTable(connectionWindow.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Analyze table", e);
		}
	}

	public void checkTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(connectionWindow, "Check table: " + connectionWindow.getTable().getName(), tcc.checkTable(connectionWindow.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Check table", e);
		}
	}

	public void repairTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(connectionWindow, "Repair table: " + connectionWindow.getTable().getName(), tcc.repairTable(connectionWindow.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Repair table", e);
		}
	}
}

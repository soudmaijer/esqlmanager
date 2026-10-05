package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.CreateTableCC;
import nl.errorsoft.esql.table.control.IndexesCC;
import nl.errorsoft.esql.table.control.TableCC;
import nl.errorsoft.esql.table.ui.FieldProperties;
import nl.errorsoft.esql.table.ui.TableDataView;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

import nl.errorsoft.esql.connection.ConnectionContext;

import nl.errorsoft.esql.query.control.QueryCC;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseProperties;
import nl.errorsoft.esql.database.ui.CreateDatabaseForm;
import nl.errorsoft.esql.table.TableInfo;
import nl.errorsoft.esql.table.ui.DuplicateTableForm;
import nl.errorsoft.esql.ui.util.ByteSize;
import nl.errorsoft.esql.ui.util.PropertiesDialog;
import nl.errorsoft.esql.ui.util.Validation;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ConnectionWindow;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.server.control.ProcesslistCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.DBCreator;
import nl.errorsoft.esql.designer.ui.diagram.ModelFactory;
import nl.errorsoft.esql.query.ui.QueryUI;
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
	private ConnectionWindow cw;
	private ConnectionWindowUI cwui;
	private TableCC tbcc;

	public ConnectionWindowCC(ESQLManagerCC jmcc, nl.errorsoft.esql.connection.ConnectionProfile cp) {
		this.jmcc = jmcc;
		this.cw = new ConnectionWindow(this, cp);
		this.start();
	}

	public void run() {
		// Create Frame.
		log.info("Connecting to `" + cw.getConnectionProfile().getServerType().getDescription() + "` @ `" + cw.getConnectionProfile().getHost()
			+ "` with username `" + cw.getConnectionProfile().getUsername() + "` on port `" + cw.getConnectionProfile().getPort() + "`");
		cwui = new ConnectionWindowUI(this, jmcc.getUI());

		// Create database connection to Server.
		jmcc.showConnectionWindow(cwui);
		jmcc.updateStatus("Connecting...", true);

		try {
			cw.start();
			jmcc.updateStatus("Loading databases...", true);
			showDatabaseTree();
			setStatusDetail("");
			jmcc.showConnectionState();
		} catch (Exception e) {
			cwui.closeUI(false);
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
		cwui.setStatus(detail);
		showStatusInfo();
	}

	/** A message about one tab (a query tab), shown in the status bar while that tab is in front. */
	public void setStatusDetail(java.awt.Component tab, String detail) {
		statusDetail = detail;
		cwui.setStatus(tab, detail);
		showStatusInfo();
	}

	private long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}

	/** Closes the designers opened from this connection; false when the user keeps one open. */
	public boolean closeDesigners() {
		return jmcc.getUI().closeDesigners(cwui);
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
		jmcc.removeConnectionWindow(cwui);

		// Close Internalframe
		cwui.dispose();
	}

	public void showDatabaseTree() {
		try {
			// Load databases into JTree.
			jmcc.updateStatus("Loading databases...", true);
			DatabaseCC dbcc = new DatabaseCC(this);
			cwui.showDatabaseTreeView(dbcc.getDatabaseTreeView());
			cwui.showHelp();
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load databases", e);
		}
	}

	/** Asks for the name and the options of the new database (character set, owner, ...) and creates it. */
	public void startCreateDatabase() {
		try {
			CreateDatabaseForm.Request request = CreateDatabaseForm.ask(cwui, dialect().databaseTerm(), dialect().createDatabaseOptions(),
				getContext().databases().createDatabaseChoices(), cwui.getDatabaseTreeView().databaseNames());

			if (request != null) {
				createDatabase(request.name(), request.options());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create " + dialect().databaseTerm(), e);
		}
	}

	private void createDatabase(String name, Map<String, String> options) {
		try {
			jmcc.updateStatus("Creating " + dialect().databaseTerm() + "...", true);
			Database db = getContext().databases().createDatabase(name, options);
			cwui.getDatabaseTreeView().addDatabase(db);
			setStatusDetail(dialect().databaseTerm() + " " + name + " created");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create " + dialect().databaseTerm(), e);
		}
	}

	public void dropDatabase() {
		try {
			jmcc.updateStatus("Deleting database...", true);
			Database db = cwui.getDatabase();
			DatabaseCC dbcc = new DatabaseCC(this);
			dbcc.dropDatabase(db);
			cwui.getDatabaseTreeView().deleteDatabase(db);
			cwui.removeDataTab();
			jmcc.showConnectionState();
			this.showDatabaseTree();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Drop database", e);
		}
	}

	public void dropTable() {
		try {
			jmcc.updateStatus("Deleting table...", true);
			Table tb = cwui.getTable();
			TableCC dbcc = new TableCC(this);
			dbcc.dropTable(tb);
			cwui.getDatabaseTreeView().deleteTable(tb);
			jmcc.showConnectionState();
			if (tb.getSchema() != null && hasSchemas()) {
				this.schemaSelected(tb.getSchema());
			} else {
				this.databaseSelected(tb.getDatabase());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Drop table", e);
		}
	}

	public void addTableColumn(FieldProperties fp, String name, String length, String dfault, DataType dt, boolean primary, boolean unique, boolean indexed,
		boolean auto, boolean signed, boolean nullable) {
		try {
			jmcc.updateStatus("Adding tablecolumn...", true);
			TableCC dbcc = new TableCC(this);
			dbcc.addTableColumn(cwui.getTable(), name, length, dfault, dt, primary, auto, signed, nullable);
			reloadSelectedTable();
			fp.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Add table column", e);
		}
	}

	public void editTableColumn(FieldProperties fp, TableColumn tbc, String name, String length, String dfault, DataType dt, boolean primary, boolean unique,
		boolean indexed, boolean auto, boolean signed, boolean nullable) {
		try {
			jmcc.updateStatus("Updating tablecolumn...", true);
			TableCC dbcc = new TableCC(this);
			dbcc.editTableColumn(tbc, name, length, dfault, dt, primary, auto, signed, nullable);
			reloadSelectedTable();
			fp.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Edit table column", e);
		}
	}

	public void dropTableColumn() {
		try {
			jmcc.updateStatus("Deleting tablecolumn...", true);
			TableColumn tb = cwui.getTableColumn();
			TableCC dbcc = new TableCC(this);
			dbcc.dropTableColumn(tb);
			cwui.getDatabaseTreeView().deleteTableColumn(tb);
			jmcc.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Drop column", e);
		}
	}

	public void reloadSelectedTable() {
		this.tableSelected(cwui.getTable(), true);
	}

	/** Reloads the tables of the selected schema (or of the schema of the selected table), otherwise the content of the selected database. */
	public void reloadSelectedDatabase() {
		Schema schema = cwui.getSchema();

		if (schema != null && hasSchemas()) {
			this.schemaSelected(schema);
		} else {
			this.databaseSelected(cwui.getDatabase());
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
		String name = Dialogs.input(cwui, "Create " + dialect().schemaTerm(), "&Name:", "Create");

		if (name != null) {
			createSchema(name);
		}
	}

	public void renameSchema() {
		Schema schema = cwui.getSchema();
		String term = dialect().schemaTerm();
		String name = Dialogs.input(cwui, "Rename " + term, "&New name:", "Rename", schema.getName(),
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
			ApplicationContext.get().errors().report(cwui, "Rename " + term, e);
		}
	}

	public void createSchema(String name) {
		try {
			Database database = cwui.getDatabase();
			jmcc.updateStatus("Creating " + dialect().schemaTerm() + "...", true);
			Schema schema = new DatabaseCC(this).createSchema(database, name);
			databaseSelected(database);
			setStatusDetail(database.getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " created");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create " + dialect().schemaTerm(), e);
		}
	}

	public void dropSchema() {
		try {
			Schema schema = cwui.getSchema();
			jmcc.updateStatus("Dropping " + dialect().schemaTerm() + "...", true);
			new DatabaseCC(this).dropSchema(schema);
			cwui.getDatabaseTreeView().deleteSchema(schema);
			cwui.removeDataTab();
			setStatusDetail(schema.getDatabase().getName() + ": " + dialect().schemaTerm() + " " + schema.getName() + " dropped");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Drop " + dialect().schemaTerm(), e);
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
		Table table = cwui.getTable();
		String name = Dialogs.input(cwui, "Rename table", "&New name:", "Rename", table.getName(), value -> tableNameProblem(table, value));

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
			ApplicationContext.get().errors().report(cwui, "Rename table", e);
		}
	}

	public void duplicateSelectedTable() {
		Table table = cwui.getTable();
		DuplicateTableForm.Request request = DuplicateTableForm.ask(cwui, table.getName() + "_copy", value -> tableNameProblem(table, value));

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
			ApplicationContext.get().errors().report(cwui, "Duplicate table", e);
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
			ApplicationContext.get().errors().report(cwui, "Properties", e);
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
		PropertiesDialog.show(cwui, "Properties of " + info.name(), properties);
	}

	private void showDatabaseProperties(Database database) throws Exception {
		DatabaseProperties info = getContext().databases().properties(database);
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put("Name", info.name());
		properties.putAll(info.details());
		properties.put("Tables", String.valueOf(info.tableCount()));
		PropertiesDialog.show(cwui, "Properties of " + info.name(), properties);
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
			tbcc.flushTable(cwui.getTable());
			jmcc.showConnectionState();
			reloadSelectedTable();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Empty table", e);
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
			cwui.getDatabaseTreeView().loadTables(database, tables);
			cwui.databaseSelected();
			setStatusDetail(database.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load tables", e);
		}
	}

	/** Shows the schemas of a database in the tree; on PostgreSQL this connects to that database. */
	private void loadSchemas(Database database) {
		String schemas = dialect().schemaTerm() + "s";

		try {
			jmcc.updateStatus("Loading " + schemas + "...", true);
			java.util.List<Schema> list = new DatabaseCC(this).getSchemas(database);
			cwui.getDatabaseTreeView().loadSchemas(database, list);
			cwui.databaseSelected();
			setStatusDetail(database.getName() + ": " + list.size() + " " + dialect().schemaTerm() + "(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load " + schemas, e);
		}
	}

	/** Shows the tables of a schema in the tree. */
	public void schemaSelected(Schema schema) {
		try {
			jmcc.updateStatus("Loading tables...", true);
			java.util.List<Table> tables = new DatabaseCC(this).getTables(schema);
			cwui.getDatabaseTreeView().loadTables(schema, tables);
			cwui.databaseSelected();
			setStatusDetail(schema.getDatabase().getName() + "." + schema.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load tables", e);
		}
	}

	/** Double click on a database: opens the table list in a tab and puts that tab in front. */
	public void openDatabase(Database database) {
		try {
			jmcc.updateStatus("Loading tables...", true);

			DatabaseCC dbcc = new DatabaseCC(this);
			java.util.List<Table> tables = dbcc.getTables(database);
			cwui.showTableListView(database.getName(), dbcc.getTableListView(tables));
			setViewStatus(database.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Open database", e);
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
				cwui.getDatabaseTreeView().loadTableColumns(table, fields);
			}

			cwui.tableSelected();
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load table", e);
		}
	}

	/** Double click on a table: opens its data in a tab and puts that tab in front. */
	public void openTable(Table table) {
		showTableData(table);
	}

	public void fieldSelected() {
		cwui.fieldSelected();
	}

	public void rootSelected() {
		cwui.rootSelected();
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
	public void startQueryUI() {
		try {
			QueryCC controller = new QueryCC(this);
			cwui.showQueryTab(new QueryUI(controller, controller.databases(), cwui.getDatabase()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Run query", e);
		}
	}

	public void startFieldUI(boolean add, boolean edit) {
		try {
			jmcc.updateStatus("Starting field properties interface...", true);
			FieldProperties fpu = new FieldProperties(jmcc.getUI(), this, cwui.getTableColumn(), add, edit);
			jmcc.showConnectionState();
			fpu.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load columns", e);
		}
	}

	/*
	 * @description: Let the tree generate its own events.
	 */
	public void selectTableInTree(Table table) {
		cwui.getDatabaseTreeView().selectTableInTree(table);
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
			cwui.showTableDataView(place + " : " + table.getName(), tbcc.getTableDataView(table, 0, 50));
			setViewStatus(place + "." + table.getName() + ": " + table.getRowCount() + " row(s), loaded in " + millisSince(start) + " ms");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load table data", e);
		}
	}

	/*
	 * @description: 	Starts the table indexes manager
	 *
	 */
	public void dispatchTableIndexesUI() {
		try {
			IndexesCC tcc = new IndexesCC(this, (Table) cwui.getSelectedNode().getUserObject());
			tcc.startUI();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Load indexes", e);
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

		Dialogs.info(cwui, description, description + " is not available for " + cw.getConnectionProfile().getServerType().getDescription() + ".");
		return false;
	}

	public void dispatchCreateTableUI() {
		try {
			new CreateTableCC(this).startCreateTable(cwui.getDatabase(), hasSchemas() ? cwui.getSchema() : null);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create table", e);
		}
	}

	/** Reads the tables and foreign keys of the selected database (the selected schema, or the current one) and opens them in the designer, arranged automatically. */
	public void openDatabaseInDesigner() {
		if (!requireFeature(Dialect.Feature.DESIGNER, "The designer")) {
			return;
		}

		try {
			Database database = cwui.getDatabase();
			Schema schema = hasSchemas() && cwui.getSelectedNode().getUserObject() instanceof Schema selected ? selected : null;
			jmcc.updateStatus("Reading database structure...", true);
			DesignedDatabase designed = schema != null ? getContext().designer().reverseEngineer(schema) : getContext().designer().reverseEngineer(database);
			Model model = ModelFactory.fromDatabase(designed, cw.getConnectionProfile().getServerType().getDataTypes());
			setStatusDetail((schema != null ? database.getName() + "." + schema.getName() : database.getName()) + ": " + designed.tables().size()
				+ " table(s) opened in the designer");
			jmcc.showConnectionState();
			new DBCreator(jmcc.getUI(), cwui, model);
		} catch (Exception e) {
			jmcc.showConnectionState();
			ApplicationContext.get().errors().report(cwui, "Open in designer", e);
		}
	}

	public void dispatchModifyTableUI() {
		try {
			new CreateTableCC(this).startEditTable(cwui.getDatabase(), cwui.getTable());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Modify table", e);
		}
	}

	public String getTitle() {
		return cw.getConnectionProfile().getUsername() + "@" + cw.getConnectionProfile().getHost();
	}

	/** The connection window, the parent of messages and the owner of the tabs. */
	/** What is selected in the tree (a database, schema, table, ...), null when nothing is. */
	public Object selectedObject() {
		return cwui.getSelectedNode() == null ? null : cwui.getSelectedNode().getUserObject();
	}

	public ConnectionWindowUI getWindow() {
		return cwui;
	}

	public ESQLManagerUI getUI() {
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
			cwui.showTableDataView("Server status", tcc.showServerStatus());
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
			cwui.showTableDataView("Server variables", tcc.showServerVariables());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Show server variables", e);
		}
	}

	public void optimizeTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(cwui, "Optimize table: " + cwui.getTable().getName(), tcc.optimizeTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Optimize table", e);
		}
	}

	public void analyseTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(cwui, "Analyze table: " + cwui.getTable().getName(), tcc.analyseTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Analyze table", e);
		}
	}

	public void checkTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(cwui, "Check table: " + cwui.getTable().getName(), tcc.checkTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Check table", e);
		}
	}

	public void repairTable() {
		try {
			TableCC tcc = new TableCC(this);
			Dialogs.info(cwui, "Repair table: " + cwui.getTable().getName(), tcc.repairTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Repair table", e);
		}
	}
}

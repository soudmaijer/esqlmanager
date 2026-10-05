package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.IndexesCC;
import nl.errorsoft.esql.table.control.TableCC;
import nl.errorsoft.esql.table.ui.FieldProperties;
import nl.errorsoft.esql.table.ui.TableDataView;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

import nl.errorsoft.esql.connection.ConnectionContext;

import nl.errorsoft.esql.query.QueryService;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ConnectionWindow;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.server.ui.Processlist;
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
			jmcc.updateStatus("Can`t connect to server...", true);
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

	/** What this connection did last is shown in the status bar of its own window. */
	private void setStatusDetail(String detail) {
		statusDetail = detail;
		cwui.setStatus(detail);
		showStatusInfo();
	}

	private long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1000000;
	}

	public void closeUI() {
		// Stop database connection
		try {
			ApplicationContext.get().release(cw.getDatabaseConnection());
			cw.stop();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Close", e);
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
			ApplicationContext.get().errors().report(cwui, "Database tree", e);
		}
	}

	public void createDatabase(String name) {
		try {
			jmcc.updateStatus("Creating database...", true);
			DatabaseCC dbcc = new DatabaseCC(this);
			Database db = dbcc.createDatabase(name);
			cwui.getDatabaseTreeView().addDatabase(db);
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create database", e);
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
			this.databaseSelected(tb.getDatabase());
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
			ApplicationContext.get().errors().report(cwui, "Drop table column", e);
		}
	}

	public void reloadSelectedTable() {
		this.tableSelected(cwui.getTable(), true);
	}

	public void reloadSelectedDatabase() {
		this.databaseSelected(cwui.getDatabase());
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
			ApplicationContext.get().errors().report(cwui, "Flush selected table", e);
		}
	}

	/*
	 * @description: updates the tree view with all tables in the selected database.
	 */
	public void databaseSelected(Database database) {
		try {
			// Load tables.
			jmcc.updateStatus("Loading tables...", true);

			// Show tables in tab.
			DatabaseCC dbcc = new DatabaseCC(this);
			Vector tables = dbcc.getTables(database);
			cwui.showTableListView(database.getName(), dbcc.getTableListView(tables));

			// Show tables in tree.
			cwui.getDatabaseTreeView().loadTables(database, tables);
			cwui.databaseSelected();
			setStatusDetail(database.getName() + ": " + tables.size() + " table(s)");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Database selected", e);
		}
	}

	/*
	 * @description: updates the tree view with all columns in the selected table.
	 */
	public void tableSelected(Table table, boolean addTreeColumns) {
		try {
			// 1th Show table data.
			showTableData(table);

			// 2nd Load tables columns in tree if not already loaded.
			if (addTreeColumns) {
				jmcc.updateStatus("Fetching table data...", true);
				TableCC tbcc = new TableCC(this);
				TableColumn[] fields = table.getColumns();
				cwui.getDatabaseTreeView().loadTableColumns(table, fields);
			}

			// 3th enable buttons.
			cwui.tableSelected();
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Table selected", e);
		}
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

	public void startQueryUI() {
		try {
			jmcc.updateStatus("Starting query window...", true);
			DatabaseCC dbcc = new DatabaseCC(this);
			QueryUI qu = new QueryUI(this, jmcc.getUI(), ApplicationContext.get().imageLoader(), dbcc.getDatabases(), cwui.getDatabase());
			jmcc.showConnectionState();
			qu.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Query", e);
		}
	}

	public void startFieldUI(boolean add, boolean edit) {
		try {
			jmcc.updateStatus("Starting field properties interface...", true);
			FieldProperties fpu = new FieldProperties(jmcc.getUI(), this, cwui.getTableColumn(), add, edit);
			jmcc.showConnectionState();
			fpu.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Field", e);
		}
	}

	/*
	 * @description: runs custom SQL queries for the QueryUI (wrong place!!)
	 */
	public void runCustomSQL(String query) {
		try {
			jmcc.updateStatus("Executing query...", true);
			cwui.disableDataEdit();
			long start = System.nanoTime();

			if (getContext().queries().returnsRows(query)) {
				TableCC tcc = new TableCC(this);
				TableDataView result = tcc.executeQuery(query);
				cwui.showTableDataView("Query results", result);
				setStatusDetail("Query returned " + result.getRowCount() + " row(s) in " + millisSince(start) + " ms");
			} else {
				QueryService queries = getContext().queries();

				if (queries.isUse(query)) {
					queries.use(query);
				} else {
					int rows = queries.update(query);
					setStatusDetail("Query affected " + rows + " row(s) in " + millisSince(start) + " ms");
				}
			}
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Run custom sql", e);
		}
	}

	/*
	 * @description: Changes the active database for the QueryUI (wrong place!!)
	 */
	public void changeDatabase(Database db) {
		try {
			getContext().databases().use(db);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Change database", e);
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
			cwui.showTableDataView(table.getDatabase().getName() + " : " + table.getName(), tbcc.getTableDataView(table, 0, 50));
			setStatusDetail(
				table.getDatabase().getName() + "." + table.getName() + ": " + table.getRowCount() + " row(s), loaded in " + millisSince(start) + " ms");
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Table data", e);
		}
	}

	/*
	 * @description: 	Starts the table indexes manager
	 *
	 */
	public void dispatchTableIndexesUI() {
		try {
			IndexesCC tcc = new IndexesCC(this, (Table) cwui.getSelectedNode().getUserObject());
			tcc.startUI(jmcc.getUI());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Table indexes", e);
		}
	}

	public void dispatchUserManagerUI() {
		UserManagerCC umcc = new UserManagerCC(this);
		umcc.startUI(jmcc.getUI());
	}

	public void dispatchProcessUI() {
		if (requireFeature(Dialect.Feature.PROCESS_LIST, "The process list")) {
			new Processlist(this, jmcc.getUI());
		}
	}

	/** Tells the user when the database of this connection can't do what they asked. */
	public boolean requireFeature(Dialect.Feature feature, String description) {
		if (cw.getConnectionProfile().getServerType().getDialect().supports(feature)) {
			return true;
		}

		cwui.showErrorMessage(description + " is not available for " + cw.getConnectionProfile().getServerType().getDescription());
		return false;
	}

	public void dispatchCreateTableUI() {
		try {
			TableCC tbcc = new TableCC(this);
			tbcc.startCreateTableUI(jmcc.getUI(), cwui.getDatabase());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Create table", e);
		}
	}

	/** Reads the tables and foreign keys of the selected database and opens them in the designer, arranged automatically. */
	public void openDatabaseInDesigner() {
		if (!requireFeature(Dialect.Feature.DESIGNER, "The designer")) {
			return;
		}

		try {
			Database database = cwui.getDatabase();
			jmcc.updateStatus("Reading database structure...", true);
			DesignedDatabase designed = getContext().designer().reverseEngineer(database);
			Model model = ModelFactory.fromDatabase(designed, cw.getConnectionProfile().getServerType().getDataTypes());
			setStatusDetail(database.getName() + ": " + designed.tables().size() + " table(s) opened in the designer");
			jmcc.showConnectionState();
			new DBCreator(jmcc.getUI(), cwui, model);
		} catch (Exception e) {
			jmcc.showConnectionState();
			ApplicationContext.get().errors().report(cwui, "Open in designer", e);
		}
	}

	public void dispatchModifyTableUI() {
		try {
			TableCC dbcc = new TableCC(this);
			dbcc.startEditTableUI(jmcc.getUI(), cwui.getDatabase(), cwui.getTable());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwui, "Modify table", e);
		}
	}

	public String getTitle() {
		return cw.getConnectionProfile().getUsername() + "@" + cw.getConnectionProfile().getHost();
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
			cwui.showMessage("Optimize table: " + cwui.getTable().getName(), tcc.optimizeTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Optimize table", e);
		}
	}

	public void analyseTable() {
		try {
			TableCC tcc = new TableCC(this);
			cwui.showMessage("Analyze table: " + cwui.getTable().getName(), tcc.analyseTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Analyse table", e);
		}
	}

	public void checkTable() {
		try {
			TableCC tcc = new TableCC(this);
			cwui.showMessage("Check table: " + cwui.getTable().getName(), tcc.checkTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Check table", e);
		}
	}

	public void repairTable() {
		try {
			TableCC tcc = new TableCC(this);
			cwui.showMessage("Repair table: " + cwui.getTable().getName(), tcc.repairTable(cwui.getTable()));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Repair table", e);
		}
	}
}

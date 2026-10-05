package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.ui.TableEditor;

import java.util.ArrayList;
import java.util.List;

/** Opens the table editor as a tab of the connection window ("New table", "Edit orders") and saves what it holds. */
public class CreateTableCC {
	private static final String NEW_TABLE = "New table";

	private ConnectionWindowCC cwcc;

	public CreateTableCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public void startCreateTable(Database database) {
		if (!cwcc.requireFeature(Dialect.Feature.CREATE_TABLE, "Creating and modifying tables") || window().selectEditorTab(NEW_TABLE)) {
			return;
		}

		window().showEditorTab(NEW_TABLE, NEW_TABLE, new TableEditor(this, NEW_TABLE, database, null));
	}

	public void startEditTable(Database database, Table table) throws Exception {
		String key = "edit:" + table.getDatabase().getName() + "." + table.getName();

		if (!cwcc.requireFeature(Dialect.Feature.CREATE_TABLE, "Creating and modifying tables") || window().selectEditorTab(key)) {
			return;
		}

		if (table.getColumns() == null) {
			cwcc.getContext().tables().loadColumns(table);
		}
		String title = "Edit " + table.getName();
		window().showEditorTab(key, title, new TableEditor(this, title, database, table));
	}

	/*
	 	List all databases, so user can choose database to create table on
	*/
	public List<Database> getDatabases() {
		try {
			DatabaseCC dbc = new DatabaseCC(cwcc);
			return dbc.getDatabases();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Load databases", e);
			return List.of();
		}
	}

	/*
	 	List all tabletypes, empty when the server has no such choice
	*/
	public String[] getTableTypes() {
		return cwcc.getConnectionProfile().getServerType().getDialect().getTableTypes();
	}

	/*
	 	List all datatypes
	*/
	public DataType[] getDatatypes() {
		return cwcc.getConnectionProfile().getServerType().getDataTypes();
	}

	public void createTable(String name, String database, String comment, String type, TableEditor editor, List<CreateColumn> columns) {
		if (name.trim().length() == 0) {
			Dialogs.error(window(), "Create table", "Enter a table name.");
			return;
		}
		if (columns.isEmpty()) {
			Dialogs.error(window(), "Create table", "Add at least one column.");
			return;
		}
		try {
			cwcc.getContext().tables().createTable(new Database(database), name, new ArrayList<>(columns), type, comment);
			window().removeTab(editor);
			cwcc.reloadSelectedDatabase();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Create table", e);
		}
	}

	public void modifyTable(TableEditor editor, Table t, String tableName, String tableType, String tableComment) {
		try {
			cwcc.getContext().tables().modifyTable(t, tableName, tableType, tableComment);
			window().removeTab(editor);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Modify table", e);
		}
	}

	/** Cancel: closes the tab, asking first when something changed. */
	public void cancel(TableEditor editor) {
		window().closeTab(editor);
	}

	private ConnectionWindowUI window() {
		return cwcc.getWindow();
	}
}

package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.table.ui.TableEditorTab;

import java.util.ArrayList;
import java.util.List;

/** Opens the table editor as a tab of the connection window ("New table", "Edit orders") and saves what it holds. */
public class CreateTableCC {
	private static final String NEW_TABLE = "New table";

	private ConnectionWindowCC cwcc;
	/** The schema a new table goes in, null for the current schema of the chosen database. */
	private Schema schema;

	public CreateTableCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	/** @param schema the schema to create the table in, null for the current one. */
	public void startCreateTable(Database database, Schema schema) {
		this.schema = schema;
		if (!cwcc.requireFeature(Dialect.Feature.CREATE_TABLE, "Creating and modifying tables") || window().selectEditorTab(NEW_TABLE)) {
			return;
		}

		window().showEditorTab(NEW_TABLE, NEW_TABLE, new TableEditorTab(this, NEW_TABLE, database, null));
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
		window().showEditorTab(key, title, new TableEditorTab(this, title, database, table));
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

	/** True when the server has schemas between databases and tables. */
	public boolean supportsSchemas() {
		return cwcc.getConnectionProfile().getServerType().getDialect().supports(Dialect.Feature.SCHEMAS);
	}

	/** The name of the schema a new table goes in, null for the current schema of the database. */
	public String targetSchemaName() {
		return schema == null ? null : schema.getName();
	}

	/** Whether the server keeps a comment per column, so the editor offers the field. */
	public boolean supportsColumnComments() {
		return cwcc.dialect().supportsColumnComments();
	}

	/**
	 * The statements Save would run, for the SQL preview: the CREATE TABLE of a new table (in the schema this editor was opened for) or the changes of an
	 * existing one. A problem is returned as a comment line.
	 */
	public List<String> previewStatements(Table existing, String database, String name, String comment, String type, List<CreateColumn> columns) {
		try {
			TableService tables = cwcc.getContext().tables();

			if (existing != null) {
				return tables.modifyStatements(existing, name, type, comment);
			}
			Schema target = schema != null && schema.getDatabase().getName().equals(database) ? schema : null;
			return tables.createStatements(target, name, columns, type, comment);
		} catch (Exception e) {
			return List.of("-- " + e.getMessage());
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

	public void createTable(String name, String database, String comment, String type, TableEditorTab editor, List<CreateColumn> columns) {
		if (name.trim().length() == 0) {
			Dialogs.error(window(), "Create table", "Enter a table name.");
			return;
		}
		if (columns.isEmpty()) {
			Dialogs.error(window(), "Create table", "Add at least one column.");
			return;
		}
		try {
			if (schema != null && schema.getDatabase().getName().equals(database)) {
				cwcc.getContext().tables().createTable(schema, name, new ArrayList<>(columns), type, comment);
			} else {
				cwcc.getContext().tables().createTable(new Database(database), name, new ArrayList<>(columns), type, comment);
			}
			window().removeTab(editor);
			cwcc.reloadSelectedDatabase();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Create table", e);
		}
	}

	public void modifyTable(TableEditorTab editor, Table t, String tableName, String tableType, String tableComment) {
		try {
			cwcc.getContext().tables().modifyTable(t, tableName, tableType, tableComment);
			window().removeTab(editor);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Modify table", e);
		}
	}

	/** Cancel: closes the tab, asking first when something changed. */
	public void cancel(TableEditorTab editor) {
		window().closeTab(editor);
	}

	private ConnectionWindow window() {
		return cwcc.getWindow();
	}
}

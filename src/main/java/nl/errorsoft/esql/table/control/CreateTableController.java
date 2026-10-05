package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableDefinition;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.table.ui.TableEditorTab;

import java.util.ArrayList;
import java.util.List;

/** Opens the table editor as a tab of the connection window ("New table", "Edit orders") and saves what it holds. */
public class CreateTableController {
	private static final String NEW_TABLE = "New table";

	private ConnectionWindowController connectionWindowController;
	/** The schema a new table goes in, null for the current schema of the chosen database. */
	private Schema schema;

	public CreateTableController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	/** @param schema the schema to create the table in, null for the current one. */
	public void startCreateTable(Database database, Schema schema) {
		this.schema = schema;
		if (!connectionWindowController.requireFeature(Dialect.Feature.CREATE_TABLE, "Creating and modifying tables") || window().selectEditorTab(NEW_TABLE)) {
			return;
		}

		window().showEditorTab(NEW_TABLE, NEW_TABLE, new TableEditorTab(this, NEW_TABLE, database, null));
	}

	/** Opens the editor of an existing table, after loading its columns in the background when they are not known yet. */
	public void startEditTable(Database database, Table table) {
		String key = "edit:" + table.getDatabase().getName() + "." + table.getName();

		if (!connectionWindowController.requireFeature(Dialect.Feature.CREATE_TABLE, "Creating and modifying tables") || window().selectEditorTab(key)) {
			return;
		}

		String title = "Edit " + table.getName();
		connectionWindowController.inBackground("Modify table", "Loading columns...",
			() -> table.getColumns() != null ? table.getColumns() : connectionWindowController.getContext().tables().loadColumns(table), columns -> {
				// Opened twice while loading: the first tab stays.
				if (!window().selectEditorTab(key)) {
					window().showEditorTab(key, title, new TableEditorTab(this, title, database, table));
				}
			});
	}

	/*
	 	List all databases, so user can choose database to create table on
	*/
	public List<Database> getDatabases() {
		try {
			return connectionWindowController.getContext().databases().getDatabases();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Load databases", e);
			return List.of();
		}
	}

	/** True when the server has schemas between databases and tables. */
	public boolean supportsSchemas() {
		return connectionWindowController.dialect().supports(Dialect.Feature.SCHEMAS);
	}

	/** The name of the schema a new table goes in, null for the current schema of the database. */
	public String targetSchemaName() {
		return schema == null ? null : schema.getName();
	}

	/** Whether the server keeps a comment per column, so the editor offers the field. */
	public boolean supportsColumnComments() {
		return connectionWindowController.dialect().supportsColumnComments();
	}

	/**
	 * The statements Save would run, for the SQL preview: the CREATE TABLE of a new table (in the schema this editor was opened for) or the changes of an
	 * existing one. A problem is returned as a comment line.
	 */
	public List<String> previewStatements(Table existing, TableDefinition definition) {
		try {
			TableService tables = connectionWindowController.getContext().tables();

			if (existing != null) {
				return tables.modifyStatements(existing, definition.name(), definition.type(), definition.comment());
			}
			return tables.createStatements(inTargetSchema(definition));
		} catch (Exception e) {
			return List.of("-- " + e.getMessage());
		}
	}

	/*
	 	List all tabletypes, empty when the server has no such choice
	*/
	public String[] getTableTypes() {
		return connectionWindowController.dialect().getTableTypes();
	}

	/*
	 	List all datatypes
	*/
	public DataType[] getDatatypes() {
		return connectionWindowController.getConnectionProfile().getServerType().getDataTypes();
	}

	/** The definition in the schema this editor was opened for, when the table goes in that schema's database. */
	private TableDefinition inTargetSchema(TableDefinition definition) {
		boolean sameDatabase = schema != null && schema.getDatabase().getName().equals(definition.database().getName());
		return definition.inSchema(sameDatabase ? schema : null);
	}

	public void createTable(TableDefinition definition, TableEditorTab editor) {
		if (definition.name().trim().length() == 0) {
			Dialogs.error(window(), "Create table", "Enter a table name.");
			return;
		}
		if (definition.columns().isEmpty()) {
			Dialogs.error(window(), "Create table", "Add at least one column.");
			return;
		}
		try {
			connectionWindowController.getContext().tables().createTable(inTargetSchema(definition));
			window().removeTab(editor);
			connectionWindowController.reloadSelectedDatabase();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(window(), "Create table", e);
		}
	}

	public void modifyTable(TableEditorTab editor, Table t, String tableName, String tableType, String tableComment) {
		try {
			connectionWindowController.getContext().tables().modifyTable(t, tableName, tableType, tableComment);
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
		return connectionWindowController.getWindow();
	}
}

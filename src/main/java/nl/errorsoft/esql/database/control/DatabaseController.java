package nl.errorsoft.esql.database.control;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.ui.TableListTab;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.ui.DatabaseTree;
import nl.errorsoft.esql.dialect.Dialect;

public class DatabaseController {
	private ConnectionWindowController connectionWindowController;

	public DatabaseController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	public java.util.List<Database> getDatabases() throws Exception {
		return service().getDatabases();
	}

	public Database createDatabase(String name) throws Exception {
		return service().createDatabase(name);
	}

	public void dropDatabase(Database data) throws Exception {
		service().dropDatabase(data);
	}

	public java.util.List<Table> getTables(Database database) throws Exception {
		return service().getTables(database);
	}

	public java.util.List<Schema> getSchemas(Database database) throws Exception {
		return service().getSchemas(database);
	}

	public java.util.List<Table> getTables(Schema schema) throws Exception {
		return service().getTables(schema);
	}

	public Schema createSchema(Database database, String name) throws Exception {
		return service().createSchema(database, name);
	}

	public Schema renameSchema(Schema schema, String newName) throws Exception {
		return service().renameSchema(schema, newName);
	}

	public void dropSchema(Schema schema) throws Exception {
		service().dropSchema(schema);
	}

	private DatabaseService service() throws Exception {
		return connectionWindowController.getContext().databases();
	}

	public DatabaseTree getDatabaseTree() throws Exception {
		return databaseTree(getDatabases());
	}

	/** A tree of the given databases, which were listed beforehand (on another thread). */
	public DatabaseTree databaseTree(java.util.List<Database> databases) {
		DatabaseTree databaseTree = new DatabaseTree(connectionWindowController.getTitle(),
			connectionWindowController.getConnectionProfile().getServerType().iconName());
		databaseTree.loadDatabases(databases);
		return databaseTree;
	}

	/**
	 * Loads what a node of a dialog's tree holds when it is opened: the schemas of a database on servers with schemas, otherwise its tables, the tables of a
	 * schema. Nothing for other nodes.
	 */
	public void loadChildren(DatabaseTree tree, Object node) throws Exception {
		if (node instanceof Database database) {
			if (connectionWindowController.getConnectionProfile().getServerType().getDialect().supports(Dialect.Feature.SCHEMAS)) {
				tree.loadSchemas(database, getSchemas(database));
			} else {
				tree.loadTables(database, getTables(database));
			}
		} else if (node instanceof Schema schema) {
			tree.loadTables(schema, getTables(schema));
		}
	}

	/** Opens a dialog's tree at the node selected in the connection window (a database, schema or table), so that the dialog starts from it. */
	public void selectInTree(DatabaseTree tree, Object node) throws Exception {
		switch (node) {
			case Database database -> tree.selectDatabase(database);
			case Schema schema -> {
				loadChildren(tree, schema.getDatabase());
				tree.selectSchema(schema);
			}
			case Table table -> {
				if (table.getSchema() != null) {
					selectInTree(tree, table.getSchema());
				} else {
					loadChildren(tree, table.getDatabase());
				}
				tree.selectTableInTree(table);
			}
			case null, default -> {
				// Nothing selected, or a node (server, column) the dialog does not start from.
			}
		}
	}

	public TableListTab getTableListTab(java.util.List<Table> tables) throws Exception {
		TableListTab tableListTab = new TableListTab(this);
		tableListTab.loadDatabases(tables);
		return tableListTab;
	}

	public void tableSelected(Table table) {
		connectionWindowController.selectTableInTree(table);
	}

}

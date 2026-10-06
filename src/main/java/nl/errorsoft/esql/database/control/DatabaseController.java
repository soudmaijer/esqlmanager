package nl.errorsoft.esql.database.control;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.Table;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.connection.ConnectionNode;
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

	/** A tree of the given databases, which were listed beforehand (on another thread). */
	public DatabaseTree databaseTree(java.util.List<Database> databases) {
		DatabaseTree databaseTree = new DatabaseTree(
			new ConnectionNode(connectionWindowController.getConnectionProfile(), connectionWindowController.getTitle()),
			connectionWindowController.getConnectionProfile().getServerType().iconName());
		databaseTree.loadDatabases(databases);
		return databaseTree;
	}

	/** What a node of a dialog's tree holds, read beforehand: the node and its schemas or tables. */
	public record Children(Object node, java.util.List<?> items) {
	}

	/** What a dialog's tree opens with, read beforehand: the databases, the children on the way to the selected node, and that node. */
	public record TreeStart(java.util.List<Database> databases, java.util.List<Children> children, Object selected) {
	}

	/**
	 * Reads what a node of a dialog's tree holds when it is opened: the schemas of a database on servers with schemas, otherwise its tables, the tables of a
	 * schema; nothing for other nodes. Database work, not for the event thread.
	 */
	public Children children(Object node) throws Exception {
		return switch (node) {
			case Database database when hasSchemas() -> new Children(node, getSchemas(database));
			case Database database -> new Children(node, getTables(database));
			case Schema schema -> new Children(node, getTables(schema));
			case null, default -> new Children(node, java.util.List.of());
		};
	}

	/** Puts children read by {@link #children} in a dialog's tree, on the event thread. */
	@SuppressWarnings("unchecked")
	public void showChildren(DatabaseTree tree, Children children) {
		switch (children.node()) {
			case Database database when hasSchemas() -> tree.loadSchemas(database, (java.util.List<Schema>) children.items());
			case Database database -> tree.loadTables(database, (java.util.List<Table>) children.items());
			case Schema schema -> tree.loadTables(schema, (java.util.List<Table>) children.items());
			case null, default -> {
				// Other nodes have nothing to load.
			}
		}
	}

	/**
	 * Reads the databases of a dialog's tree and what it needs to open at the node selected in the explorer (a database, schema or table). Database
	 * work, not for the event thread.
	 */
	public TreeStart treeStart(Object selected) throws Exception {
		java.util.List<Children> children = new java.util.ArrayList<>();

		switch (selected) {
			case Schema schema -> children.add(children(schema.getDatabase()));
			case Table table when table.getSchema() != null -> {
				children.add(children(table.getDatabase()));
				children.add(children(table.getSchema()));
			}
			case Table table -> children.add(children(table.getDatabase()));
			case null, default -> {
				// A database needs nothing more, other nodes are not started from.
			}
		}
		return new TreeStart(getDatabases(), children, selected);
	}

	/** Opens a dialog's tree at the node selected in the explorer, so that the dialog starts from it; on the event thread. */
	public void selectInTree(DatabaseTree tree, TreeStart start) {
		for (Children children : start.children()) {
			showChildren(tree, children);
		}
		switch (start.selected()) {
			case Database database -> tree.selectDatabase(database);
			case Schema schema -> tree.selectSchema(schema);
			case Table table -> tree.selectTableInTree(table);
			case null, default -> {
				// Nothing selected, or a node (server, column) the dialog does not start from.
			}
		}
	}

	private boolean hasSchemas() {
		return connectionWindowController.getConnectionProfile().getServerType().getDialect().supports(Dialect.Feature.SCHEMAS);
	}

}

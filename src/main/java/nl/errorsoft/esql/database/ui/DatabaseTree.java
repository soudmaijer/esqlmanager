package nl.errorsoft.esql.database.ui;

import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/**
 * A tree with the databases, tables and columns of one connection; on servers with schemas a database holds schemas, which hold the tables. The nodes
 * below the root are kept by a {@link ConnectionBranch}, which only shows what the controller loads: {@code DatabaseController} and
 * {@code ConnectionWindowController} fetch the data through the services and call these methods.
 */
public class DatabaseTree extends JTree {
	private final ConnectionBranch branch;

	/**
	 * @param root what the root node shows, the server (a {@code ConnectionNode} or a title)
	 * @param serverIcon the icon of the root node (see {@code ServerType.iconName})
	 */
	public DatabaseTree(Object root, String serverIcon) {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
		setModel(new DefaultTreeModel(rootNode, false));
		setCellRenderer(new DatabaseTreeCellRenderer(ApplicationContext.get().imageLoader(), serverIcon));
		this.branch = new ConnectionBranch(this, rootNode);
	}

	/** The nodes of the connection, below the root. */
	public ConnectionBranch getBranch() {
		return branch;
	}

	public void loadDatabases(List<Database> databases) {
		branch.loadDatabases(databases);
	}

	/** The names of the databases in the tree. */
	public List<String> databaseNames() {
		return branch.databaseNames();
	}

	public void addDatabase(Database db) {
		branch.addDatabase(db);
	}

	public void deleteDatabase(Database database) {
		branch.deleteDatabase(database);
	}

	public void deleteSchema(Schema schema) {
		branch.deleteSchema(schema);
	}

	public void deleteTable(Table table) {
		branch.deleteTable(table);
	}

	public void deleteTableColumn(TableColumn column) {
		branch.deleteTableColumn(column);
	}

	public void loadTables(Database database, List<Table> tables) {
		branch.loadTables(database, tables);
	}

	public void loadSchemas(Database database, List<Schema> schemas) {
		branch.loadSchemas(database, schemas);
	}

	public void loadTables(Schema schema, List<Table> tables) {
		branch.loadTables(schema, tables);
	}

	public void loadTableColumns(Table table, TableColumn[] columns) {
		branch.loadTableColumns(table, columns);
	}

	public void selectDatabase(Database database) {
		branch.selectDatabase(database);
	}

	public void selectSchema(Schema schema) {
		branch.selectSchema(schema);
	}

	public void selectTableInTree(Table table) {
		branch.selectTable(table);
	}
}

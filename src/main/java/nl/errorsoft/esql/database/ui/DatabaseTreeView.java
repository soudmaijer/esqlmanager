package nl.errorsoft.esql.database.ui;

import java.util.List;
import java.util.Optional;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/**
 * The tree of databases, tables and columns of a connection; on servers with schemas a database holds schemas, which hold the tables. It only shows what the controller loads: {@code DatabaseCC} and {@code ConnectionWindowCC}
 * fetch the data through the services and call these methods.
 */
public class DatabaseTreeView extends JTree {
	private final DefaultMutableTreeNode rootNode;
	private final DefaultTreeModel dtm;

	/** @param serverIcon the icon of the root node, the server (see {@code ServerType.iconName}) */
	public DatabaseTreeView(String title, String serverIcon) {
		this.rootNode = new DefaultMutableTreeNode(title);
		this.dtm = new DefaultTreeModel(rootNode, false);
		setModel(dtm);
		setCellRenderer(new DatabaseTreeViewCellRenderer(ApplicationContext.get().imageLoader(), serverIcon));
	}

	public void loadDatabases(List<Database> databases) {
		rootNode.removeAllChildren();
		databases.forEach(db -> rootNode.add(new DefaultMutableTreeNode(db)));
		dtm.reload();
	}

	public void addDatabase(Database db) {
		rootNode.add(new DefaultMutableTreeNode(db));
		dtm.reload(rootNode);
	}

	public void deleteDatabase(Database database) {
		databaseNode(database.getName()).ifPresent(node -> remove(rootNode, node));
	}

	public void deleteSchema(Schema schema) {
		schemaNode(schema).ifPresent(node -> remove((DefaultMutableTreeNode) node.getParent(), node));
	}

	public void deleteTable(Table table) {
		tableNode(table).ifPresent(node -> remove((DefaultMutableTreeNode) node.getParent(), node));
	}

	public void deleteTableColumn(TableColumn column) {
		tableNode(column.getTable()).flatMap(tableNode -> child(tableNode, column.getName(), TableColumn.class))
			.ifPresent(node -> remove((DefaultMutableTreeNode) node.getParent(), node));
	}

	public void loadTables(Database database, List<Table> tables) {
		databaseNode(database.getName()).ifPresent(node -> replaceChildren(node, tables));
	}

	public void loadSchemas(Database database, List<Schema> schemas) {
		databaseNode(database.getName()).ifPresent(node -> replaceChildren(node, schemas));
	}

	public void loadTables(Schema schema, List<Table> tables) {
		schemaNode(schema).ifPresent(node -> replaceChildren(node, tables));
	}

	public void loadTableColumns(Table table, TableColumn[] columns) {
		tableNode(table).ifPresent(node -> replaceChildren(node, List.of(columns)));
	}

	public void selectTableInTree(Table table) {
		tableNode(table).ifPresent(node -> setSelectionPath(new TreePath(node.getPath())));
	}

	private void replaceChildren(DefaultMutableTreeNode node, List<?> children) {
		node.removeAllChildren();
		children.forEach(child -> node.add(new DefaultMutableTreeNode(child)));
		dtm.reload(node);
		TreePath path = new TreePath(node.getPath());
		scrollPathToVisible(path);
		expandPath(path);
		setSelectionPath(path);
	}

	private void remove(DefaultMutableTreeNode parent, DefaultMutableTreeNode node) {
		parent.remove(node);
		dtm.reload(parent);
	}

	private Optional<DefaultMutableTreeNode> databaseNode(String name) {
		return child(rootNode, name, Database.class);
	}

	private Optional<DefaultMutableTreeNode> schemaNode(Schema schema) {
		return databaseNode(schema.getDatabase().getName()).flatMap(db -> child(db, schema.getName(), Schema.class));
	}

	/** Under its schema when the server has schemas and that schema is shown, otherwise under its database. */
	private Optional<DefaultMutableTreeNode> tableNode(Table table) {
		Optional<DefaultMutableTreeNode> parent = table.getSchema() == null ? Optional.empty() : schemaNode(table.getSchema());

		if (parent.isEmpty()) {
			parent = databaseNode(table.getDatabase().getName());
		}
		return parent.flatMap(node -> child(node, table.getName(), Table.class));
	}

	/** The child of {@code parent} of the given type whose name ({@code toString}) is {@code name}, ignoring case. */
	private static Optional<DefaultMutableTreeNode> child(DefaultMutableTreeNode parent, String name, Class<?> type) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);
			if (type.isInstance(node.getUserObject()) && node.getUserObject().toString().equalsIgnoreCase(name)) {
				return Optional.of(node);
			}
		}
		return Optional.empty();
	}
}

package nl.errorsoft.esql.database.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/**
 * The databases, schemas, tables and columns of one connection below a node of a tree: the whole tree of a {@link DatabaseTree}, or the branch of one
 * connection in a tree that holds several. It only shows what the controller loads and only changes the nodes below its root.
 */
public class ConnectionBranch {
	private final JTree tree;
	private final DefaultMutableTreeNode root;

	/** @param root a node of {@code tree}, whose model must be a {@link DefaultTreeModel} */
	public ConnectionBranch(JTree tree, DefaultMutableTreeNode root) {
		this.tree = tree;
		this.root = root;
	}

	public DefaultMutableTreeNode getRoot() {
		return root;
	}

	private DefaultTreeModel model() {
		return (DefaultTreeModel) tree.getModel();
	}

	public void loadDatabases(List<Database> databases) {
		root.removeAllChildren();
		databases.forEach(db -> root.add(new DefaultMutableTreeNode(db)));
		model().nodeStructureChanged(root);
		tree.expandPath(new TreePath(root.getPath()));
	}

	/** The names of the databases in the branch. */
	public List<String> databaseNames() {
		List<String> names = new ArrayList<>();

		for (int i = 0; i < root.getChildCount(); i++) {
			names.add(((DefaultMutableTreeNode) root.getChildAt(i)).getUserObject().toString());
		}
		return names;
	}

	public void addDatabase(Database db) {
		root.add(new DefaultMutableTreeNode(db));
		model().reload(root);
	}

	public void deleteDatabase(Database database) {
		databaseNode(database.getName()).ifPresent(node -> remove(root, node));
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

	public void selectDatabase(Database database) {
		databaseNode(database.getName()).ifPresent(this::select);
	}

	public void selectSchema(Schema schema) {
		schemaNode(schema).ifPresent(this::select);
	}

	public void selectTable(Table table) {
		tableNode(table).ifPresent(this::select);
	}

	private void select(DefaultMutableTreeNode node) {
		tree.setSelectionPath(new TreePath(node.getPath()));
	}

	/**
	 * Shows the new children of a node and expands it. The selection only moves when it was on the node or below it (the user asked for this load): a
	 * load that finishes in the background must not take the selection away from another node or another connection.
	 */
	private void replaceChildren(DefaultMutableTreeNode node, List<?> children) {
		TreePath path = new TreePath(node.getPath());
		TreePath selected = tree.getSelectionPath();
		boolean selectionInside = selected != null && path.isDescendant(selected);

		node.removeAllChildren();
		children.forEach(child -> node.add(new DefaultMutableTreeNode(child)));
		model().reload(node);
		tree.expandPath(path);
		if (selectionInside) {
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
		}
	}

	private void remove(DefaultMutableTreeNode parent, DefaultMutableTreeNode node) {
		parent.remove(node);
		model().reload(parent);
	}

	private Optional<DefaultMutableTreeNode> databaseNode(String name) {
		return child(root, name, Database.class);
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

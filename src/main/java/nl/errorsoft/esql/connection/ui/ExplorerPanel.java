package nl.errorsoft.esql.connection.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.ConnectionNode;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ProfileNode;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.TreeSelection;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.ui.ConnectionBranch;
import nl.errorsoft.esql.database.ui.DatabaseTreeCellRenderer;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.util.ToolbarButtons;

/**
 * The explorer on the left of the main window: one tree with every open connection (its databases, schemas, tables and columns below it) and the saved
 * profiles that are not connected, in grey. The toolbar and the context menus act on the selected node, through the controller of its connection.
 */
public class ExplorerPanel extends JPanel {
	private final MainController mainController;
	private final ImageLoader images = ApplicationContext.get().imageLoader();
	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Connections");
	private final DefaultTreeModel model = new DefaultTreeModel(root);
	private final JTree tree = new JTree(model);
	private final Map<ConnectionNode, ConnectionWindowController> connections = new IdentityHashMap<>();

	private final JButton refreshButton = button("pc", "Reload databases");
	private final JButton queryButton = button("imgRunQuery", "New query");
	private final JButton usersButton = button("imgUserManager", "User manager");
	private final JButton designerButton = button("imgDesigner", "Open in designer");
	private final JButton createTableButton = button("imgCreateTable", "Create table");
	private final JButton dropTableButton = button("imgDropTable", "Drop table");
	private final JButton addFieldButton = button("imgAddField", "Add field");
	private final JButton dropFieldButton = button("imgDeleteField", "Drop field");

	public ExplorerPanel(MainController mainController) {
		super(new BorderLayout());
		this.mainController = mainController;

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new DatabaseTreeCellRenderer(images, null));
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.addTreeSelectionListener(e -> {
			updateButtons();
			if (e.isAddedPath()) {
				nodeSelected((DefaultMutableTreeNode) e.getPath().getLastPathComponent());
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			// The popup trigger comes on press on macOS (also ctrl-click) and Linux, on release on Windows.
			@Override
			public void mousePressed(MouseEvent e) {
				showContextMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				showContextMenu(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
					doubleClicked(e);
				}
			}
		});

		JToolBar toolbar = ToolbarButtons.toolbar();
		toolbar.add(refreshButton);
		toolbar.add(queryButton);
		toolbar.add(usersButton);
		toolbar.add(ToolbarButtons.separator());
		toolbar.add(designerButton);
		toolbar.add(createTableButton);
		toolbar.add(dropTableButton);
		toolbar.add(ToolbarButtons.separator());
		toolbar.add(addFieldButton);
		toolbar.add(dropFieldButton);
		ToolbarButtons.style(refreshButton, queryButton, usersButton, designerButton, createTableButton, dropTableButton, addFieldButton, dropFieldButton);
		toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));

		refreshButton.addActionListener(e -> act(ConnectionWindowController::showDatabaseTree));
		queryButton.addActionListener(e -> act(ConnectionWindowController::startQueryTab));
		usersButton.addActionListener(e -> act(ConnectionWindowController::showUserManagerDialog));
		designerButton.addActionListener(e -> act(ConnectionWindowController::openDatabaseInDesigner));
		createTableButton.addActionListener(e -> act(ConnectionWindowController::showCreateTableTab));
		dropTableButton.addActionListener(e -> act(this::dropTable));
		addFieldButton.addActionListener(e -> act(c -> c.showColumnPropertiesDialog(true, false)));
		dropFieldButton.addActionListener(e -> act(this::dropField));

		JScrollPane scroll = new JScrollPane(tree);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UIManager.getColor("Tree.background"));
		add(toolbar, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		updateButtons();
	}

	private JButton button(String icon, String tooltip) {
		JButton button = new JButton(images.getIcon(icon));
		button.setToolTipText(tooltip);
		button.getAccessibleContext().setAccessibleName(tooltip);
		return button;
	}

	/** Adds an open connection below the other connections and selects it; its databases are loaded into the returned branch. */
	public ConnectionBranch addConnection(ConnectionNode node, ConnectionWindowController controller) {
		DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
		model.insertNodeInto(treeNode, root, connections.size());
		connections.put(node, controller);
		tree.setSelectionPath(new TreePath(treeNode.getPath()));
		return new ConnectionBranch(tree, treeNode);
	}

	public void removeConnection(ConnectionNode node) {
		connections.remove(node);
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			if (child.getUserObject() == node) {
				model.removeNodeFromParent(child);
				break;
			}
		}
		updateButtons();
	}

	/** Shows the saved profiles that are not connected below the connections. */
	public void showProfiles(List<ConnectionProfile> profiles) {
		for (int i = root.getChildCount() - 1; i >= 0; i--) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			if (child.getUserObject() instanceof ProfileNode) {
				model.removeNodeFromParent(child);
			}
		}
		for (ConnectionProfile profile : profiles) {
			model.insertNodeInto(new DefaultMutableTreeNode(new ProfileNode(profile)), root, root.getChildCount());
		}
	}

	/** The selected node when it belongs to this connection (the connection node itself or a node below it), else null. */
	public Object selectedObject(ConnectionNode connection) {
		TreePath path = tree.getSelectionPath();
		if (path == null || TreeSelection.connection(userObjects(path)) != connection) {
			return null;
		}
		return ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
	}

	/** The connection of the selected node, null when nothing or a saved profile is selected. */
	public ConnectionWindowController selectedConnection() {
		TreePath path = tree.getSelectionPath();
		return path == null ? null : connections.get(TreeSelection.connection(userObjects(path)));
	}

	private static List<Object> userObjects(TreePath path) {
		List<Object> objects = new ArrayList<>();
		for (Object node : path.getPath()) {
			objects.add(((DefaultMutableTreeNode) node).getUserObject());
		}
		return objects;
	}

	/** Selecting a database, schema or table loads what is below it; the status bar shows the connection of the node. */
	private void nodeSelected(DefaultMutableTreeNode node) {
		ConnectionWindowController controller = selectedConnection();
		if (controller == null) {
			return;
		}
		switch (node.getUserObject()) {
			case Database database -> controller.databaseSelected(database);
			case Schema schema -> controller.schemaSelected(schema);
			case Table table -> controller.tableSelected(table, node.getChildCount() == 0);
			default -> {
				// The connection and columns have nothing to load.
			}
		}
		controller.showStatusInfo();
	}

	/** The toolbar offers what fits the selected node. */
	private void updateButtons() {
		ConnectionWindowController controller = selectedConnection();
		TreePath path = tree.getSelectionPath();
		Object selected = path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		boolean connected = controller != null;
		boolean inDatabase = selected instanceof Database || selected instanceof Schema;
		boolean onTable = selected instanceof Table || selected instanceof TableColumn;

		refreshButton.setEnabled(connected);
		queryButton.setEnabled(connected);
		usersButton.setEnabled(connected && controller.dialect().supports(Dialect.Feature.USER_MANAGER));
		designerButton.setEnabled(connected && inDatabase);
		createTableButton.setEnabled(connected && (inDatabase || onTable));
		dropTableButton.setEnabled(connected && onTable);
		addFieldButton.setEnabled(connected && onTable);
		dropFieldButton.setEnabled(connected && selected instanceof TableColumn);
	}

	private interface Action {
		void run(ConnectionWindowController controller);
	}

	private void act(Action action) {
		ConnectionWindowController controller = selectedConnection();
		if (controller != null) {
			action.run(controller);
		}
	}

	/** A double click connects a saved profile and opens a database or table in a window. */
	private void doubleClicked(MouseEvent e) {
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path == null) {
			return;
		}
		Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		ConnectionWindowController controller = connections.get(TreeSelection.connection(userObjects(path)));

		switch (object) {
			case ProfileNode profile -> mainController.connect(profile.profile());
			case Database database when controller != null -> controller.openDatabase(database);
			case Table table when controller != null -> controller.openTable(table);
			default -> {
				// Other nodes expand or collapse, as the tree does by itself.
			}
		}
	}

	/** Selects the node under the cursor and shows its context menu. */
	private void showContextMenu(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path == null) {
			return;
		}
		tree.setSelectionPath(path);
		Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		JPopupMenu menu = contextMenu(object, selectedConnection());
		if (menu != null) {
			menu.show(tree, e.getX(), e.getY());
		}
	}

	/** The context menu of a node: a title with its name, then only the items the server supports. */
	private JPopupMenu contextMenu(Object node, ConnectionWindowController controller) {
		TreeMenu.Node kind;
		String title;
		String icon;
		Dialect dialect;

		if (node instanceof ProfileNode profile) {
			kind = TreeMenu.Node.PROFILE;
			title = profile.toString();
			icon = profile.profile().getServerType().iconName();
			dialect = profile.profile().getServerType().getDialect();
		} else if (controller == null) {
			return null;
		} else {
			dialect = controller.dialect();
			switch (node) {
				case Database database -> {
					kind = TreeMenu.Node.DATABASE;
					title = database.getName();
					icon = "db_select_20x20";
				}
				case Schema schema -> {
					kind = TreeMenu.Node.SCHEMA;
					title = schema.getName();
					icon = "sc_select_20x20";
				}
				case Table table -> {
					kind = TreeMenu.Node.TABLE;
					title = table.getName();
					icon = "tb_select_20x20";
				}
				case TableColumn column -> {
					kind = TreeMenu.Node.COLUMN;
					title = column.getName();
					icon = "fd_select_20x20";
				}
				default -> {
					kind = TreeMenu.Node.CONNECTION;
					title = controller.getTitle();
					icon = controller.getConnectionProfile().getServerType().iconName();
				}
			}
		}

		JPopupMenu menu = new JPopupMenu();
		JLabel label = new JLabel(title, images.getIcon(icon), SwingConstants.LEFT);
		label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		menu.add(label);
		menu.addSeparator();

		for (TreeMenu.Item item : TreeMenu.itemsFor(kind, dialect)) {
			if (item == TreeMenu.Item.SEPARATOR) {
				menu.addSeparator();
			} else {
				JMenuItem menuItem = new JMenuItem(item.label(dialect));
				String itemIcon = menuIcon(item);

				if (itemIcon != null) {
					menuItem.setIcon(images.getIcon(itemIcon));
				}
				menuItem.addActionListener(e -> perform(item, node, controller));
				menu.add(menuItem);
			}
		}
		return menu;
	}

	/** Runs an item of the context menu on the node that was right clicked (it is selected first). */
	private void perform(TreeMenu.Item item, Object node, ConnectionWindowController c) {
		if (node instanceof ProfileNode profile) {
			switch (item) {
				case CONNECT -> mainController.connect(profile.profile());
				case EDIT_PROFILE -> mainController.showConnectionProfileDialog(profile.profile().getName());
				default -> {
					// A profile has no other items.
				}
			}
			return;
		}

		switch (item) {
			case CREATE_DATABASE -> c.startCreateDatabase();
			case NEW_QUERY -> c.startQueryTab();
			case USERS -> c.showUserManagerDialog();
			case PROCESS_LIST -> c.showProcessListDialog();
			case SERVER_STATUS -> c.showServerStatus();
			case SERVER_VARIABLES -> c.showServerVariables();
			case EXPORT -> c.showExportDialog();
			case IMPORT -> c.showImportDialog();
			case RELOAD_DATABASES -> c.showDatabaseTree();
			case OPEN_DATABASE -> c.openDatabase(c.getView().getDatabase());
			case CREATE_TABLE -> c.showCreateTableTab();
			case OPEN_IN_DESIGNER -> c.openDatabaseInDesigner();
			case DROP_DATABASE -> {
				Database database = c.getView().getDatabase();
				String term = c.dialect().databaseTerm();
				String name = database != null ? term + " '" + database.getName() + "'" : "the selected " + term;

				if (Dialogs.confirmDestructive(this, "Drop " + term, "Drop " + name + " and all its tables? This cannot be undone.", "Drop")) {
					c.dropDatabase();
				}
			}
			case RELOAD_TABLES, RELOAD_SCHEMAS -> c.reloadSelectedDatabase();
			case CREATE_SCHEMA -> c.startCreateSchema();
			case RENAME_SCHEMA -> c.renameSchema();
			case DROP_SCHEMA -> {
				Schema schema = c.getView().getSchema();
				String term = c.dialect().schemaTerm();

				if (Dialogs.confirmDestructive(this, "Drop " + term,
					"Drop " + term + " '" + (schema == null ? "" : schema.getName()) + "' and everything in it? This cannot be undone.", "Drop")) {
					c.dropSchema();
				}
			}
			case RENAME_TABLE -> c.renameSelectedTable();
			case DUPLICATE_TABLE -> c.duplicateSelectedTable();
			case PROPERTIES -> c.showProperties();
			case OPEN_TABLE -> c.openTable(c.getView().getTable());
			case EDIT_TABLE -> c.showEditTableTab();
			case INDEXES -> c.showIndexesTab();
			case ADD_FIELD -> c.showColumnPropertiesDialog(true, false);
			case EMPTY_TABLE -> {
				if (Dialogs.confirmDestructive(this, "Empty table", "Delete all rows from " + tableName(c.getView().getTable()) + "? This cannot be undone.",
					"Empty")) {
					c.flushSelectedTable();
				}
			}
			case DROP_TABLE -> dropTable(c);
			case OPTIMIZE -> c.optimizeTable();
			case ANALYZE -> c.analyzeTable();
			case CHECK -> c.checkTable();
			case REPAIR -> c.repairTable();
			case RELOAD_COLUMNS -> c.reloadSelectedTable();
			case EDIT_FIELD -> c.showColumnPropertiesDialog(false, true);
			case DROP_FIELD -> dropField(c);
			case DISCONNECT -> c.disconnect();
			case CONNECT, EDIT_PROFILE, SEPARATOR -> {
				// Items of saved profiles, handled above.
			}
		}
	}

	private void dropTable(ConnectionWindowController c) {
		if (Dialogs.confirmDestructive(this, "Drop table", "Drop table " + tableName(c.getView().getTable()) + "? All its data will be lost.", "Drop")) {
			c.dropTable();
		}
	}

	private void dropField(ConnectionWindowController c) {
		TableColumn column = c.getView().getTableColumn();
		String name = column != null ? "column '" + column.getName() + "' from table " + tableName(column.getTable()) : "the selected column";

		if (Dialogs.confirmDestructive(this, "Drop column", "Drop " + name + "? This cannot be undone.", "Drop")) {
			c.dropTableColumn();
		}
	}

	/** The registered Lucide icon of an item, null when there is none that fits. */
	private static String menuIcon(TreeMenu.Item item) {
		return switch (item) {
			case CONNECT -> "imgConnect";
			case DISCONNECT -> "imgDisconnect";
			case EDIT_PROFILE -> "des_properties";
			case CREATE_DATABASE -> "add_database";
			case DROP_DATABASE -> "imgDropDatabase";
			case NEW_QUERY -> "imgRunQuery";
			case USERS -> "imgUserManager";
			case OPEN_IN_DESIGNER -> "imgDesigner";
			case CREATE_TABLE -> "imgCreateTable";
			case DROP_TABLE -> "imgDropTable";
			case OPEN_DATABASE, OPEN_TABLE -> "imgOpen";
			case INDEXES -> "imgHasIndex";
			case ADD_FIELD -> "imgAddField";
			case DROP_FIELD -> "imgDeleteField";
			case EDIT_TABLE, EDIT_FIELD -> "des_properties";
			case EXPORT, IMPORT -> "imgSave";
			case RELOAD_DATABASES, RELOAD_SCHEMAS, RELOAD_TABLES, RELOAD_COLUMNS -> "imgRun";
			default -> null;
		};
	}

	/** 'database.table' (or 'database.schema.table') for the questions before a table is changed. */
	private static String tableName(Table table) {
		if (table == null) {
			return "the selected table";
		}
		String schema = table.getSchema() != null ? table.getSchema().getName() + "." : "";
		return "'" + table.getDatabase().getName() + "." + schema + table.getName() + "'";
	}
}

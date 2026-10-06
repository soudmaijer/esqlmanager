package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.ui.util.MouseClicks;

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
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.ConnectionNode;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.NodeLoads;
import nl.errorsoft.esql.connection.ProfileNode;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.TreeSelection;
import nl.errorsoft.esql.app.StatusContext;
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
	private static final String TREE = "tree";
	private static final String EMPTY = "empty";
	private final JPanel cards = new JPanel(new java.awt.CardLayout()); // The tree, or the hint when it is empty

	private final List<Runnable> selectionListeners = new ArrayList<>();
	private final NodeLoads loads = new NodeLoads();
	private final JButton connectButton = button("imgConnectSmall", "Connect");
	private final JButton queryButton = button("imgRunQuery", "New query");
	private final JButton usersButton = button("imgUserManager", "User manager");
	private final JButton designerButton = button("imgDesigner", "Open in designer");

	public ExplorerPanel(MainController mainController) {
		super(new BorderLayout());
		this.mainController = mainController;

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		// A double click is handled in doubleClicked: a table opens without expanding, other nodes still expand or collapse.
		tree.setToggleClickCount(0);
		tree.setCellRenderer(new DatabaseTreeCellRenderer(images, null));
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			// A saved profile has a handle like a connection: expanding it connects. The connection takes its place in the tree at once.
			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
				if (node.getUserObject() instanceof ProfileNode profile) {
					mainController.connect(profile.profile());
					throw new ExpandVetoException(event, "Connecting");
				}
				if (ConnectionBranch.needsLoading(node)) {
					loadChildren(event.getPath());
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) {
				// Nothing to do.
			}
		});
		tree.addTreeSelectionListener(e -> {
			// Selecting only selects: what is below a node loads when it is expanded, a view opens on a double click, Enter or a menu item.
			updateButtons();
			ConnectionWindowController controller = selectedConnection();
			if (e.isAddedPath() && controller != null) {
				controller.activate();
			}
			// A selection the user made (the tree has the focus) is the context of the status bar; one made by the application only refreshes it.
			if (tree.hasFocus()) {
				mainController.contextTouched(StatusContext.Source.EXPLORER);
			} else {
				mainController.showConnectionState();
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
				if (MouseClicks.isDoubleClick(e)) {
					doubleClicked(e);
				}
			}
		});

		// Enter opens what a double click on a table opens, and the table list of a database or schema.
		tree.getInputMap(JTree.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "openSelected");
		tree.getActionMap().put("openSelected", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				openSelected();
			}
		});

		JToolBar toolbar = ToolbarButtons.toolbar();
		toolbar.add(connectButton);
		toolbar.add(queryButton);
		toolbar.add(usersButton);
		toolbar.add(ToolbarButtons.separator());
		toolbar.add(designerButton);
		ToolbarButtons.style(connectButton, queryButton, usersButton, designerButton);
		toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
			toolbar.getBorder()));

		connectButton.addActionListener(e -> connectSelected());
		queryButton.addActionListener(e -> startQuery());
		usersButton.addActionListener(e -> act(ConnectionWindowController::showUserManagerDialog));
		designerButton.addActionListener(e -> act(ConnectionWindowController::openDatabaseInDesigner));

		JScrollPane scroll = new JScrollPane(tree);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UIManager.getColor("Tree.background"));
		cards.add(scroll, TREE);
		cards.add(emptyHint(), EMPTY);
		add(toolbar, BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
		showTreeOrHint();
		updateButtons();
	}

	/** Without connections and saved profiles the explorer says how to start, with a button for the first profile. */
	private JPanel emptyHint() {
		JLabel text = new JLabel("<html><center>No connections yet.<br>Create a profile to connect to a server.</center></html>", SwingConstants.CENTER);
		text.setForeground(UIManager.getColor("Label.disabledForeground"));
		JButton create = new JButton("New connection...", images.getIcon("imgConnect"));
		create.addActionListener(e -> mainController.showConnectionProfileDialog());
		JPanel hint = new JPanel(new java.awt.GridBagLayout());
		hint.setBackground(UIManager.getColor("Tree.background"));
		java.awt.GridBagConstraints cell = new java.awt.GridBagConstraints();
		cell.gridx = 0;
		cell.insets = new java.awt.Insets(6, 12, 6, 12);
		hint.add(text, cell);
		hint.add(create, cell);
		return hint;
	}

	private void showTreeOrHint() {
		((java.awt.CardLayout) cards.getLayout()).show(cards, root.getChildCount() == 0 ? EMPTY : TREE);
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
		showTreeOrHint();
		return new ConnectionBranch(tree, treeNode, true);
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
		showTreeOrHint();
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
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ProfileNode(profile));
			// The child only makes the tree draw an expand handle, expanding connects instead (see the will expand listener).
			node.add(new DefaultMutableTreeNode("Connecting..."));
			model.insertNodeInto(node, root, root.getChildCount());
		}
		showTreeOrHint();
		updateButtons();
	}

	/** The selected node when it belongs to this connection (the connection node itself or a node below it), else null. */
	public Object selectedObject(ConnectionNode connection) {
		TreePath path = tree.getSelectionPath();
		if (path == null || TreeSelection.connection(userObjects(path)) != connection) {
			return null;
		}
		return ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
	}

	/** The object of the selected node (a profile, database, schema, table, ...), null when nothing is selected. */
	public Object selectedObject() {
		TreePath path = tree.getSelectionPath();
		return path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
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

	/**
	 * A database, schema or table was expanded while its children were not loaded yet: the controller of its connection loads them (one load at a time per
	 * connection). The "Loading..." child shows meanwhile; after a failure, which the controller reports, the node collapses again. Expanding the node again
	 * while its load runs does not start another.
	 */
	private void loadChildren(TreePath path) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		ConnectionWindowController controller = connections.get(TreeSelection.connection(userObjects(path)));
		if (controller == null || !loads.start(node)) {
			return;
		}
		controller.loadChildren(node.getUserObject(), () -> {
			loads.finish(node);
			if (ConnectionBranch.needsLoading(node)) {
				tree.collapsePath(path);
			}
		});
	}

	/** The toolbar offers what fits the selected node: hidden what the server never offers, disabled (saying what is needed) what the node cannot do. */
	private void updateButtons() {
		ConnectionWindowController controller = selectedConnection();
		Dialect dialect = controller == null ? null : controller.dialect();

		show(queryButton, TreeMenu.Item.NEW_QUERY, dialect);
		show(usersButton, TreeMenu.Item.USERS, dialect);
		show(designerButton, TreeMenu.Item.OPEN_IN_DESIGNER, dialect);
		selectionListeners.forEach(Runnable::run);
	}

	private void show(javax.swing.JComponent component, TreeMenu.Item item, Dialect dialect) {
		component.setVisible(dialect == null || TreeMenu.supported(item, dialect));
		ToolbarButtons.setAvailable(component, missing(item));
	}

	/**
	 * What the selected node lacks for an item of the tree menus, null when it can run ({@link TreeMenu#missing}); for the menu bar, so that it agrees with
	 * the toolbar and the context menus.
	 */
	public String missing(TreeMenu.Item item) {
		TreePath path = tree.getSelectionPath();
		ConnectionWindowController controller = selectedConnection();
		return TreeMenu.missing(item, path == null ? null : userObjects(path), controller == null ? null : controller.dialect());
	}

	/** Whether the server of the selected connection offers the item; true when no connection is selected (the item is then disabled, not hidden). */
	public boolean supported(TreeMenu.Item item) {
		ConnectionWindowController controller = selectedConnection();
		return controller == null || TreeMenu.supported(item, controller.dialect());
	}

	/** Opens a query tab on the selected database, the same as the toolbar button. */
	public void startQuery() {
		if (missing(TreeMenu.Item.NEW_QUERY) == null) {
			act(ConnectionWindowController::startQueryTab);
		}
	}

	/** Called whenever what the toolbar offers may have changed: the selection, or a connection that opens or closes. */
	public void addStateListener(Runnable listener) {
		selectionListeners.add(listener);
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

	/** Connects the selected saved profile; with anything else selected it opens the connection dialog for a new connection. */
	private void connectSelected() {
		TreePath path = tree.getSelectionPath();
		Object selected = path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (selected instanceof ProfileNode profile) {
			mainController.connect(profile.profile());
		} else {
			mainController.showConnectionProfileDialog();
		}
	}

	/** A double click connects a saved profile and opens a table (without expanding it); other nodes expand or collapse, expanding loads them. */
	private void doubleClicked(MouseEvent e) {
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path == null) {
			return;
		}
		Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		ConnectionWindowController controller = connections.get(TreeSelection.connection(userObjects(path)));

		switch (object) {
			case ProfileNode profile -> mainController.connect(profile.profile());
			case Table table when controller != null -> controller.openTable(table);
			default -> toggle(path);
		}
	}

	/** Enter: a table opens its data, a database or schema its table list, a saved profile connects. */
	private void openSelected() {
		TreePath path = tree.getSelectionPath();
		ConnectionWindowController controller = selectedConnection();
		Object object = path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		switch (object) {
			case ProfileNode profile -> mainController.connect(profile.profile());
			case Table table when controller != null -> controller.openTable(table);
			case Database database when controller != null -> controller.openTableList(database);
			case Schema schema when controller != null -> controller.openTableList(schema);
			case null, default -> {
				// Nothing to open.
			}
		}
	}

	private void toggle(TreePath path) {
		if (tree.isExpanded(path)) {
			tree.collapsePath(path);
		} else {
			tree.expandPath(path);
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
			case OPEN_DATABASE -> c.openTableList(node);
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

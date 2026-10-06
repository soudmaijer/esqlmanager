package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.ui.util.ToolbarButtons;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;

import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.TreeSelection;
import nl.errorsoft.esql.database.ui.DatabaseTree;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.component.EditorTab;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.table.ui.TableListTab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ConnectionWindow extends JInternalFrame implements ActionListener, MouseListener {
	// Components.
	private nl.errorsoft.esql.connection.control.ConnectionWindowController connectionWindowController;
	private ImageLoader imgLoader;
	private DatabaseTree databaseTree;
	private DefaultMutableTreeNode selectedNode;
	private JScrollPane treeScroll; // Scroll pane for the tree
	private JSplitPane treeSplit; // Tree and the tabs
	private JTabbedPane tabbedPane; // Contains the tabs
	private static final String READY = "Ready"; // Only sizes the status bar
	private JLabel status;
	private JPanel navigation;
	private Component viewTab; // The table data or table list, shown in front of the help
	private int queryTabs; // Query tabs opened so far, for their numbers
	private static final String EDITOR_KEY = "ConnectionWindow.editorKey"; // Finds an editor tab again
	private static final String STATUS_KEY = "ConnectionWindow.status"; // The status message of a tab

	// Internal toolbar.
	private JToolBar toolbar;
	private JButton refreshTreeButton;
	private JButton dropTableButton;
	private JButton createTableButton;
	private JButton userManagerButton;
	private JButton designerButton;
	private JButton runQueryButton;
	private JButton newRowButton;
	private JButton updateRowButton;
	private JButton deleteRowButton;
	private JButton addFieldButton;
	private JButton deleteFieldButton;

	public ConnectionWindow(nl.errorsoft.esql.connection.control.ConnectionWindowController connectionWindowController, MainWindow mainWindow) {
		// Windowconstructor
		this.connectionWindowController = connectionWindowController;
		this.setTitle(connectionWindowController.getTitle());
		this.setFrameIcon(ApplicationContext.get().imageLoader().getIcon(connectionWindowController.getConnectionProfile().getServerType().iconName()));
		this.setClosable(true);

		// Set internal variables
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		this.addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosing(InternalFrameEvent e) {
				closeWindow(true);
			}

			public void internalFrameActivated(InternalFrameEvent e) {
				connectionWindowController.showStatusInfo();
			}
		});

		// Get Imageloader
		imgLoader = ApplicationContext.get().imageLoader();

		/******************************************************************
		 *
		 *		JToolbar
		 */

		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		refreshTreeButton = new JButton(imgLoader.getIcon("pc"));
		refreshTreeButton.setToolTipText("Refresh tree");
		createTableButton = new JButton(imgLoader.getIcon("imgCreateTable"));
		createTableButton.setToolTipText("Create table");
		dropTableButton = new JButton(imgLoader.getIcon("imgDropTable"));
		dropTableButton.setToolTipText("Drop table");
		designerButton = new JButton(imgLoader.getIcon("imgDesigner"));
		designerButton.setToolTipText("Open database in designer");
		userManagerButton = new JButton(imgLoader.getIcon("imgUserManager"));
		userManagerButton.setToolTipText("User manager");
		runQueryButton = new JButton(imgLoader.getIcon("imgRunQuery"));
		runQueryButton.setToolTipText("Run SQL query");
		newRowButton = new JButton(imgLoader.getIcon("imgNewRow"));
		newRowButton.setToolTipText("Insert new row");
		updateRowButton = new JButton(imgLoader.getIcon("imgUpdateRow"));
		updateRowButton.setToolTipText("Update changes");
		deleteRowButton = new JButton(imgLoader.getIcon("imgDeleteRow"));
		deleteRowButton.setToolTipText("Delete row");
		addFieldButton = new JButton(imgLoader.getIcon("imgAddField"));
		addFieldButton.setToolTipText("Add field");
		deleteFieldButton = new JButton(imgLoader.getIcon("imgDeleteField"));
		deleteFieldButton.setToolTipText("Delete field");

		// Voeg knoppen toe aan toolbar
		toolbar.add(refreshTreeButton);
		toolbar.add(userManagerButton);
		toolbar.add(runQueryButton);
		toolbar.addSeparator();
		toolbar.add(designerButton);
		toolbar.addSeparator();
		toolbar.add(createTableButton);
		toolbar.add(dropTableButton);
		toolbar.addSeparator();
		toolbar.add(addFieldButton);
		toolbar.add(deleteFieldButton);
		toolbar.addSeparator();
		toolbar.add(newRowButton);
		toolbar.add(deleteRowButton);
		toolbar.add(updateRowButton);
		ToolbarButtons.style(refreshTreeButton, userManagerButton, runQueryButton, designerButton, createTableButton, dropTableButton, addFieldButton,
			deleteFieldButton, newRowButton,
			deleteRowButton, updateRowButton);

		// Disable.
		this.newRowButton.setEnabled(false);
		this.updateRowButton.setEnabled(false);
		this.deleteRowButton.setEnabled(false);
		this.designerButton.setEnabled(false);
		this.dropTableButton.setEnabled(false);
		this.createTableButton.setEnabled(false);
		this.addFieldButton.setEnabled(false);
		this.deleteFieldButton.setEnabled(false);
		this.getContentPane().add(toolbar, BorderLayout.NORTH);

		/******************************************************************
		 *
		 *		Right SplitPane setup
		 */

		// TabbedPane properties.
		tabbedPane = new JTabbedPane();
		tabbedPane.putClientProperty("JTabbedPane.tabClosable", true);
		tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback", (java.util.function.BiConsumer<JTabbedPane, Integer>) (pane, index) -> closeTab(index));

		/******************************************************************
		 *
		 *		Left SplitPane setup
		 */

		// JTree
		treeScroll = new JScrollPane();
		treeScroll.getViewport().setBackground(UIManager.getColor("Tree.background"));

		// SplitPane properties.
		// One bar below the tabs: what this connection did last on the left, the paging of the table data in the centre.
		// The message gets the left share of the width and is cut with an ellipsis, so it never pushes the paging away.
		status = new JLabel(READY, SwingConstants.LEFT);
		status.setPreferredSize(new Dimension(0, status.getPreferredSize().height));
		status.setMinimumSize(new Dimension(0, 0));
		status.setText(""); // The height is taken from the placeholder text above, a tab without a message leaves the bar empty
		status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		status.setForeground(UIManager.getColor("Label.disabledForeground"));
		navigation = new JPanel(new BorderLayout());
		navigation.setVisible(false);
		JPanel statusBar = new JPanel(new GridBagLayout());
		statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));
		GridBagConstraints cell = new GridBagConstraints();
		cell.fill = GridBagConstraints.HORIZONTAL;
		cell.weightx = 1;
		statusBar.add(status, cell);
		cell.gridx = 1;
		cell.weightx = 0;
		statusBar.add(navigation, cell);
		cell.gridx = 2;
		cell.weightx = 1;
		statusBar.add(Box.createHorizontalGlue(), cell);
		tabbedPane.addChangeListener(e -> {
			showNavigation();
			showTabStatus();
		});
		JPanel tabsWithStatus = new JPanel(new BorderLayout());
		tabsWithStatus.add(tabbedPane, BorderLayout.CENTER);
		tabsWithStatus.add(statusBar, BorderLayout.SOUTH);

		treeSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, tabsWithStatus);
		treeSplit.setDividerLocation(200);

		// Add SplitPane.
		getContentPane().add(treeSplit, BorderLayout.CENTER);

		/******************************************************************
		 *
		 *		Action Listeners.
		 */

		// Toolbar actionlisteners.
		refreshTreeButton.addActionListener(this);
		createTableButton.addActionListener(this);
		dropTableButton.addActionListener(this);
		designerButton.addActionListener(this);
		userManagerButton.addActionListener(this);
		runQueryButton.addActionListener(this);
		newRowButton.addActionListener(this);
		updateRowButton.addActionListener(this);
		deleteRowButton.addActionListener(this);
		addFieldButton.addActionListener(this);
		deleteFieldButton.addActionListener(this);

		// Pack & show.
		this.pack();
		this.setVisible(true);
	}
	/*
	 *@description: returns the class controlling this UI.
	 */
	public nl.errorsoft.esql.connection.control.ConnectionWindowController getController() {
		return this.connectionWindowController;
	}

	// Disable buttons if root selected.
	public void rootSelected() {
		designerButton.setEnabled(false);
		createTableButton.setEnabled(false);
		dropTableButton.setEnabled(false);
		addFieldButton.setEnabled(false);
		deleteFieldButton.setEnabled(false);
		newRowButton.setEnabled(false);
		deleteRowButton.setEnabled(false);
		updateRowButton.setEnabled(false);
	}

	// Disable buttons if database selected.
	public void databaseSelected() {
		designerButton.setEnabled(true);
		createTableButton.setEnabled(true);
		dropTableButton.setEnabled(false);
		addFieldButton.setEnabled(false);
		deleteFieldButton.setEnabled(false);
		newRowButton.setEnabled(false);
		deleteRowButton.setEnabled(false);
		updateRowButton.setEnabled(false);
	}

	// Disable buttons if table selected.
	public void tableSelected() {
		designerButton.setEnabled(false);
		createTableButton.setEnabled(true);
		dropTableButton.setEnabled(true);
		addFieldButton.setEnabled(true);
		deleteFieldButton.setEnabled(false);
		newRowButton.setEnabled(true);
		deleteRowButton.setEnabled(true);
		updateRowButton.setEnabled(true);
	}

	// Disable buttons if field selected.
	public void fieldSelected() {
		designerButton.setEnabled(false);
		createTableButton.setEnabled(true);
		dropTableButton.setEnabled(true);
		addFieldButton.setEnabled(true);
		deleteFieldButton.setEnabled(true);
		newRowButton.setEnabled(true);
		deleteRowButton.setEnabled(true);
		updateRowButton.setEnabled(true);
	}

	/*
	 * @descriptions: when running a custom query data cannot be editted. Disable all
	 * buttons which aren`t available.
	 */
	public void disableDataEdit() {
		newRowButton.setEnabled(false);
		deleteRowButton.setEnabled(false);
		updateRowButton.setEnabled(false);
	}

	// Close frame.
	public void closeWindow(boolean confirmation) {
		if (confirmation) {
			if (Dialogs.confirm(this, "Disconnect", "Disconnect from " + getTitle() + "?", "Disconnect") && connectionWindowController.closeDesigners()) {
				connectionWindowController.closeWindow();
			}
		} else {
			connectionWindowController.closeWindow();
		}
	}

	// Shows the database tree.
	public void showDatabaseTree(DatabaseTree tree) {
		databaseTree = tree;
		// A new tree starts without a selection, the old node is not part of it.
		selectedNode = null;
		rootSelected();
		treeScroll.getViewport().add(databaseTree);

		databaseTree.addMouseListener(this);
		databaseTree.addTreeSelectionListener(e -> {
			TreePath treePath = e.getPath();
			selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

			if (selectedNode.getUserObject() instanceof Database) {
				if (e.isAddedPath()) {
					connectionWindowController.databaseSelected((Database) selectedNode.getUserObject());

				}
			} else if (selectedNode.getUserObject() instanceof Schema schema) {
				if (e.isAddedPath()) {
					connectionWindowController.schemaSelected(schema);
				}
			} else if (selectedNode.getUserObject() instanceof Table) {
				if (e.isAddedPath()) {
					if (((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getChildCount() > 0) {
						connectionWindowController.tableSelected((Table) selectedNode.getUserObject(), false);
					} else {
						connectionWindowController.tableSelected((Table) selectedNode.getUserObject(), true);
					}
				}
			} else if (selectedNode.getUserObject() instanceof nl.errorsoft.esql.table.TableColumn) {
				connectionWindowController.fieldSelected();
			}
			// Root?!
			else {
				connectionWindowController.rootSelected();
			}
		});
	}

	public DatabaseTree getDatabaseTree() {
		return (DatabaseTree) treeScroll.getViewport().getView();
	}

	public void showTableDataTab(String tabTitle, TableDataTab tableDataTab) {
		showView(tabTitle, tableDataTab);
		navigation.removeAll();
		navigation.add(tableDataTab.getNavigationBar(), BorderLayout.CENTER);
		this.tabbedPane.setSelectedComponent(tableDataTab);
		showNavigation();
		SwingUtilities.invokeLater(tableDataTab::requestFocusInWindow);
	}

	public void showTableListTab(String tabTitle, TableListTab tableListTab) {
		showView(tabTitle, tableListTab);
		this.tabbedPane.setSelectedComponent(tableListTab);
		SwingUtilities.invokeLater(tableListTab::requestFocusInWindow);
	}

	/** The table data or table list takes the first tab, the help stays available behind it. */
	private void showView(String tabTitle, Component view) {
		if (viewTab == null) {
			this.tabbedPane.insertTab(tabTitle, null, view, "", 0);
		} else {
			this.tabbedPane.setTitleAt(0, tabTitle);
			this.tabbedPane.setComponentAt(0, view);
		}

		viewTab = view;
	}

	/** The paging buttons belong to the table data, they are only shown while that tab is in front. */
	private void showNavigation() {
		navigation.setVisible(viewTab instanceof TableDataTab && tabbedPane.getSelectedComponent() == viewTab);
		navigation.revalidate();
	}

	/** Opens a query in a new tab ("Query", "Query 2", ...), puts it in front and gives its editor the focus. */
	public void showQueryTab(QueryTab query) {
		queryTabs++;
		String title = queryTabs == 1 ? "Query" : "Query " + queryTabs;
		this.tabbedPane.addTab(title, query);
		this.tabbedPane.setSelectedComponent(query);
		SwingUtilities.invokeLater(() -> query.getEditor().requestFocusInWindow());
	}

	/** Puts the editor tab opened under this key in front, false when there is none. */
	public boolean selectEditorTab(String key) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			if (tabbedPane.getComponentAt(i) instanceof JComponent tab && key.equals(tab.getClientProperty(EDITOR_KEY))) {
				tabbedPane.setSelectedIndex(i);
				return true;
			}
		}
		return false;
	}

	/** Opens an editor (table, indexes) in a new tab, found again by its key, and puts it in front. */
	public void showEditorTab(String key, String title, JComponent editor) {
		editor.putClientProperty(EDITOR_KEY, key);
		this.tabbedPane.addTab(title, editor);
		this.tabbedPane.setSelectedComponent(editor);
	}

	/** Closes a tab as its close button does, an editor with unsaved changes asks first. */
	public void closeTab(Component tab) {
		int index = tabbedPane.indexOfComponent(tab);

		if (index >= 0) {
			closeTab(index);
		}
	}

	/** Closes a tab without asking, for an editor that has just been saved. */
	public void removeTab(Component tab) {
		int index = tabbedPane.indexOfComponent(tab);

		if (index >= 0) {
			removeTab(index);
		}
	}

	private void closeTab(int index) {
		if (this.tabbedPane.getComponentAt(index) instanceof EditorTab editor && !editor.confirmClose()) {
			return;
		}

		removeTab(index);
	}

	private void removeTab(int index) {
		Component tab = this.tabbedPane.getComponentAt(index);

		if (tab == viewTab) {
			viewTab = null;
		}
		if (tab instanceof QueryTab query) {
			query.close();
		}

		this.tabbedPane.removeTabAt(index);
		showNavigation();
	}

	/**
	 * A message about the view tab, the first tab with the table data or table list (a table loaded, tables listed). The help and the query and
	 * editor tabs keep their own messages.
	 */
	public void setStatus(String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(text));
			return;
		}

		if (viewTab != null) {
			setStatus(viewTab, text);
		}
	}

	/** Keeps a message with its tab; the status bar shows it while that tab is in front. */
	public void setStatus(Component tab, String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(tab, text));
			return;
		}

		if (tab instanceof JComponent component) {
			component.putClientProperty(STATUS_KEY, text);
		}
		if (tab == tabbedPane.getSelectedComponent()) {
			showStatus(text);
		}
	}

	private void showTabStatus() {
		showStatus(tabbedPane.getSelectedComponent() instanceof JComponent tab ? (String) tab.getClientProperty(STATUS_KEY) : null);
	}

	private void showStatus(String text) {
		// A tab without a message of its own leaves the bar empty.
		boolean empty = text == null || text.isEmpty();
		status.setText(empty ? "" : text);
		status.setToolTipText(empty ? null : text);
	}

	public String toString() {
		return this.getTitle();
	}

	/******************************************************************
	 *
	 *		Action Listeners implementation
	 */

	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();

		// Insert new row
		if (eventSource == newRowButton) {
			connectionWindowController.insertNewRow();
		}
		// Delete row
		else if (eventSource == deleteRowButton) {
			connectionWindowController.deleteSelectedRows();
		}
		// Update or insert row
		else if (eventSource == updateRowButton) {
			connectionWindowController.saveSelectedRow();
		}
		// Refresh database tree.
		else if (eventSource == refreshTreeButton) {
			connectionWindowController.showDatabaseTree();
		}
		// Run SQL query window.
		else if (eventSource == runQueryButton) {
			connectionWindowController.startQueryTab();
		}
		// Read the database into the designer.
		else if (eventSource == designerButton) {
			connectionWindowController.openDatabaseInDesigner();
		}
		// Drop table.
		else if (eventSource == dropTableButton) {
			dropTable();
		}
		// Add field.
		else if (eventSource == addFieldButton) {
			connectionWindowController.showColumnPropertiesDialog(true, false);
		}
		// Drop field.
		else if (eventSource == deleteFieldButton) {
			dropField();
		} else if (eventSource == userManagerButton) {
			connectionWindowController.showUserManagerDialog();
		} else if (eventSource == createTableButton) {
			connectionWindowController.showCreateTableTab();
		}
	}

	/** Runs an item of the context menu of the tree, on the node that was right clicked (it is selected first). */
	private void perform(TreeMenu.Item item) {
		switch (item) {
			case CREATE_DATABASE -> connectionWindowController.startCreateDatabase();
			case NEW_QUERY -> connectionWindowController.startQueryTab();
			case USERS -> connectionWindowController.showUserManagerDialog();
			case PROCESS_LIST -> connectionWindowController.showProcessListDialog();
			case SERVER_STATUS -> connectionWindowController.showServerStatus();
			case SERVER_VARIABLES -> connectionWindowController.showServerVariables();
			case EXPORT -> connectionWindowController.showExportDialog();
			case IMPORT -> connectionWindowController.showImportDialog();
			case RELOAD_DATABASES -> connectionWindowController.showDatabaseTree();
			case OPEN_DATABASE -> connectionWindowController.openDatabase(getDatabase());
			case CREATE_TABLE -> connectionWindowController.showCreateTableTab();
			case OPEN_IN_DESIGNER -> connectionWindowController.openDatabaseInDesigner();
			case DROP_DATABASE -> {
				Database database = getDatabase();
				String term = dialect().databaseTerm();
				String name = database != null ? term + " '" + database.getName() + "'" : "the selected " + term;

				if (Dialogs.confirmDestructive(this, "Drop " + term, "Drop " + name + " and all its tables? This cannot be undone.", "Drop")) {
					connectionWindowController.dropDatabase();
				}
			}
			case RELOAD_TABLES, RELOAD_SCHEMAS -> connectionWindowController.reloadSelectedDatabase();
			case CREATE_SCHEMA -> connectionWindowController.startCreateSchema();
			case RENAME_SCHEMA -> connectionWindowController.renameSchema();
			case DROP_SCHEMA -> {
				Schema schema = getSchema();
				String term = dialect().schemaTerm();

				if (Dialogs.confirmDestructive(this, "Drop " + term, "Drop " + term + " '" + (schema == null ? "" : schema.getName())
					+ "' and everything in it? This cannot be undone.", "Drop")) {
					connectionWindowController.dropSchema();
				}
			}
			case RENAME_TABLE -> connectionWindowController.renameSelectedTable();
			case DUPLICATE_TABLE -> connectionWindowController.duplicateSelectedTable();
			case PROPERTIES -> connectionWindowController.showProperties();
			case OPEN_TABLE -> connectionWindowController.openTable(getTable());
			case EDIT_TABLE -> connectionWindowController.showEditTableTab();
			case INDEXES -> connectionWindowController.showIndexesTab();
			case ADD_FIELD -> connectionWindowController.showColumnPropertiesDialog(true, false);
			case EMPTY_TABLE -> {
				if (Dialogs.confirmDestructive(this, "Empty table", "Delete all rows from " + tableName(getTable()) + "? This cannot be undone.", "Empty")) {
					connectionWindowController.flushSelectedTable();
				}
			}
			case DROP_TABLE -> dropTable();
			case OPTIMIZE -> connectionWindowController.optimizeTable();
			case ANALYZE -> connectionWindowController.analyzeTable();
			case CHECK -> connectionWindowController.checkTable();
			case REPAIR -> connectionWindowController.repairTable();
			case RELOAD_COLUMNS -> connectionWindowController.reloadSelectedTable();
			case EDIT_FIELD -> connectionWindowController.showColumnPropertiesDialog(false, true);
			case DROP_FIELD -> dropField();
			case DISCONNECT -> closeWindow(true);
			// Items of saved profiles, which this window does not show.
			case CONNECT, EDIT_PROFILE, SEPARATOR -> {
			}
		}
	}

	private void dropTable() {
		if (Dialogs.confirmDestructive(this, "Drop table", "Drop table " + tableName(getTable()) + "? All its data will be lost.", "Drop")) {
			connectionWindowController.dropTable();
		}
	}

	private void dropField() {
		TableColumn column = getTableColumn();
		String name = column != null ? "column '" + column.getName() + "' from table " + tableName(column.getTable()) : "the selected column";

		if (Dialogs.confirmDestructive(this, "Drop column", "Drop " + name + "? This cannot be undone.", "Drop")) {
			connectionWindowController.dropTableColumn();
		}
	}

	/** The context menu of a node: a title with its name, then only the items the server supports. */
	JPopupMenu contextMenu(Object node) {
		TreeMenu.Node kind;
		String title;
		String icon;

		if (node instanceof Database database) {
			kind = TreeMenu.Node.DATABASE;
			title = database.getName();
			icon = "db_select_20x20";
		} else if (node instanceof Schema schema) {
			kind = TreeMenu.Node.SCHEMA;
			title = schema.getName();
			icon = "sc_select_20x20";
		} else if (node instanceof Table table) {
			kind = TreeMenu.Node.TABLE;
			title = table.getName();
			icon = "tb_select_20x20";
		} else if (node instanceof TableColumn column) {
			kind = TreeMenu.Node.COLUMN;
			title = column.getName();
			icon = "fd_select_20x20";
		} else {
			kind = TreeMenu.Node.SERVER;
			title = getTitle();
			icon = "rt_select_20x20";
		}

		JPopupMenu menu = new JPopupMenu();
		JLabel label = new JLabel(title, imgLoader.getIcon(icon), SwingConstants.LEFT);
		label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		menu.add(label);
		menu.addSeparator();

		for (TreeMenu.Item item : TreeMenu.itemsFor(kind, dialect())) {
			if (item == TreeMenu.Item.SEPARATOR) {
				menu.addSeparator();
			} else {
				JMenuItem menuItem = new JMenuItem(item.label(dialect()));
				String itemIcon = menuIcon(item);

				if (itemIcon != null) {
					menuItem.setIcon(imgLoader.getIcon(itemIcon));
				}
				menuItem.addActionListener(e -> perform(item));
				menu.add(menuItem);
			}
		}
		return menu;
	}

	/** The registered Lucide icon of an item, null when there is none that fits. */
	private static String menuIcon(TreeMenu.Item item) {
		return switch (item) {
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

	private Dialect dialect() {
		return connectionWindowController.getConnectionProfile().getServerType().getDialect();
	}
	/******************************************************************
	 *
	 *		Mouse Listener Implementation
	 */

	// The popup trigger comes on press on macOS (also ctrl-click) and Linux, on release on Windows.
	public void mouseReleased(MouseEvent e) {
		showContextMenu(e);
	}

	public void mousePressed(MouseEvent e) {
		showContextMenu(e);
	}

	/** Selects the node under the cursor and shows its context menu. */
	private void showContextMenu(MouseEvent e) {
		if (e.getSource() != databaseTree || !e.isPopupTrigger()) {
			return;
		}

		TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());

		if (path == null) {
			return;
		}
		databaseTree.setSelectionPath(path);
		selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		contextMenu(selectedNode.getUserObject()).show(databaseTree, e.getX(), e.getY());
	}

	/** The selected table, or the table of the selected column; null when nothing or something else is selected. */
	public Table getTable() {
		return TreeSelection.table(selectedObject());
	}

	public void removeDataTab() {
		if (viewTab != null) {
			this.tabbedPane.remove(viewTab);
			viewTab = null;
			navigation.removeAll();
			showNavigation();
		}
	}

	/** The selected database, or the database of the selected schema, table or column; null when nothing is selected. */
	public Database getDatabase() {
		return TreeSelection.database(selectedObject());
	}

	/** The selected schema, or the schema of the selected table or column; null when there is none. */
	public Schema getSchema() {
		return TreeSelection.schema(selectedObject());
	}

	/** The selected column, null when no column is selected. */
	public TableColumn getTableColumn() {
		return TreeSelection.column(selectedObject());
	}

	/** What is selected in the tree (a database, schema, table, column or the server), null before anything is selected. */
	public Object selectedObject() {
		return selectedNode == null ? null : selectedNode.getUserObject();
	}

	/** A double click on a database or table opens its tab, a single click only selects. */
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() != databaseTree || e.getClickCount() != 2 || e.isMetaDown()) {
			return;
		}

		TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());

		if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node) {
			if (node.getUserObject() instanceof Database database) {
				connectionWindowController.openDatabase(database);
			} else if (node.getUserObject() instanceof Table table) {
				connectionWindowController.openTable(table);
			}
		}
	}
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() == databaseTree) {
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		}
	}
	public void mouseExited(MouseEvent e) {
		if (e.getSource() == databaseTree) {
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		}
	}
}

package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.error.Dialogs;

import org.apache.logging.log4j.LogManager;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
import nl.errorsoft.esql.query.ui.QueryUI;
import nl.errorsoft.esql.ui.util.HyperLinkListener;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.ui.TableDataView;
import nl.errorsoft.esql.table.ui.TableListView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ConnectionWindowUI extends JInternalFrame implements ActionListener, MouseListener {
	// Components.
	private nl.errorsoft.esql.connection.control.ConnectionWindowCC cwcc;
	private ImageLoader imgLoader;
	private DatabaseTreeView dtv;
	private DefaultMutableTreeNode selectedNode;
	private JScrollPane jsp; // ScrolPane for JTree
	private JSplitPane jsplp; // Tree and jsplData
	private JTabbedPane tabbedPane; // Contains jsp2
	private JScrollPane helpPane;
	private static final String READY = "Ready";
	private JLabel status;
	private JPanel navigation;
	private Component viewTab; // The table data or table list, shown in front of the help
	private JEditorPane html; // The HTML info data.
	private int queryTabs; // Query tabs opened so far, for their numbers

	// Internal toolbar.
	private JToolBar tbTable;
	private JButton btnRefreshTree;
	private JButton btnDropTable;
	private JButton btnCreateTable;
	private JButton btnUserManager;
	private JButton btnDesigner;
	private JButton btnRunQuery;
	private JButton btnNewRow;
	private JButton btnUpdateRow;
	private JButton btnDeleteRow;
	private JButton btnAddField;
	private JButton btnDeleteField;

	public ConnectionWindowUI(nl.errorsoft.esql.connection.control.ConnectionWindowCC cwcc, ESQLManagerUI jmui) {
		// Windowconstructor
		this.cwcc = cwcc;
		this.setTitle(cwcc.getTitle());
		this.setFrameIcon(ApplicationContext.get().imageLoader().getIcon("pc"));
		this.setResizable(true);
		this.setMaximizable(true);
		this.setClosable(true);
		this.setIconifiable(true);

		// Set internal variables
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		this.addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosing(InternalFrameEvent e) {
				closeUI(true);
			}

			public void internalFrameActivated(InternalFrameEvent e) {
				cwcc.showStatusInfo();
			}
		});

		// Get Imageloader
		imgLoader = ApplicationContext.get().imageLoader();

		/******************************************************************
		 *
		 *		JToolbar
		 */

		tbTable = new JToolBar();
		tbTable.setFloatable(false);
		btnRefreshTree = new JButton(imgLoader.getIcon("pc"));
		btnRefreshTree.setToolTipText("Refresh tree");
		btnCreateTable = new JButton(imgLoader.getIcon("imgCreateTable"));
		btnCreateTable.setToolTipText("Create table");
		btnDropTable = new JButton(imgLoader.getIcon("imgDropTable"));
		btnDropTable.setToolTipText("Drop table");
		btnDesigner = new JButton(imgLoader.getIcon("imgDesigner"));
		btnDesigner.setToolTipText("Open database in designer");
		btnUserManager = new JButton(imgLoader.getIcon("imgUserManager"));
		btnUserManager.setToolTipText("User manager");
		btnRunQuery = new JButton(imgLoader.getIcon("imgRunQuery"));
		btnRunQuery.setToolTipText("Run SQL query");
		btnNewRow = new JButton(imgLoader.getIcon("imgNewRow"));
		btnNewRow.setToolTipText("Insert new row");
		btnUpdateRow = new JButton(imgLoader.getIcon("imgUpdateRow"));
		btnUpdateRow.setToolTipText("Update changes");
		btnDeleteRow = new JButton(imgLoader.getIcon("imgDeleteRow"));
		btnDeleteRow.setToolTipText("Delete row");
		btnAddField = new JButton(imgLoader.getIcon("imgAddField"));
		btnAddField.setToolTipText("Add field");
		btnDeleteField = new JButton(imgLoader.getIcon("imgDeleteField"));
		btnDeleteField.setToolTipText("Delete field");

		// Voeg knoppen toe aan toolbar
		tbTable.add(btnRefreshTree);
		tbTable.add(btnUserManager);
		tbTable.add(btnRunQuery);
		tbTable.addSeparator();
		tbTable.add(btnDesigner);
		tbTable.addSeparator();
		tbTable.add(btnCreateTable);
		tbTable.add(btnDropTable);
		tbTable.addSeparator();
		tbTable.add(btnAddField);
		tbTable.add(btnDeleteField);
		tbTable.addSeparator();
		tbTable.add(btnNewRow);
		tbTable.add(btnDeleteRow);
		tbTable.add(btnUpdateRow);

		// Disable.
		this.btnNewRow.setEnabled(false);
		this.btnUpdateRow.setEnabled(false);
		this.btnDeleteRow.setEnabled(false);
		this.btnRunQuery.setEnabled(false);
		this.btnDesigner.setEnabled(false);
		this.btnDropTable.setEnabled(false);
		this.btnCreateTable.setEnabled(false);
		this.btnAddField.setEnabled(false);
		this.btnDeleteField.setEnabled(false);
		this.getContentPane().add(tbTable, BorderLayout.NORTH);

		/******************************************************************
		 *
		 *		Right SplitPane setup
		 */

		// EditorPane
		try {
			html = new JEditorPane(this.getClass().getResource("/html/help.html"));
			html.setEditable(false);
			// The help page is written for a white page, FlatLaf paints a read-only pane grey.
			html.setBackground(Color.white);
			html.setForeground(Color.black);
			html.addHyperlinkListener(new HyperLinkListener(cwcc));
		} catch (Exception e) {
			LogManager.getLogger(ConnectionWindowUI.class).warn("The help page could not be loaded: {}", e.getMessage());
		}

		// TabbedPane properties.
		tabbedPane = new JTabbedPane();
		tabbedPane.putClientProperty("JTabbedPane.tabClosable", true);
		tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback", (java.util.function.BiConsumer<JTabbedPane, Integer>) (pane, index) -> closeTab(index));
		helpPane = new JScrollPane(html);
		tabbedPane.addTab("eSQLManager Help", helpPane);
		tabbedPane.setSelectedIndex(0);

		/******************************************************************
		 *
		 *		Left SplitPane setup
		 */

		// JTree
		jsp = new JScrollPane();
		jsp.getViewport().setBackground(Color.white);

		// SplitPane properties.
		// One bar below the tabs: the paging of the table data on the left, what this connection did last on the right.
		status = new JLabel(READY, SwingConstants.RIGHT);
		status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		status.setForeground(UIManager.getColor("Label.disabledForeground"));
		navigation = new JPanel(new BorderLayout());
		navigation.setVisible(false);
		JPanel statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));
		statusBar.add(navigation, BorderLayout.WEST);
		statusBar.add(status, BorderLayout.CENTER);
		tabbedPane.addChangeListener(e -> showNavigation());
		JPanel tabsWithStatus = new JPanel(new BorderLayout());
		tabsWithStatus.add(tabbedPane, BorderLayout.CENTER);
		tabsWithStatus.add(statusBar, BorderLayout.SOUTH);

		jsplp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp, tabsWithStatus);
		jsplp.setDividerLocation(200);

		// Add SplitPane.
		getContentPane().add(jsplp, BorderLayout.CENTER);

		/******************************************************************
		 *
		 *		Action Listeners.
		 */

		// Toolbar actionlisteners.
		btnRefreshTree.addActionListener(this);
		btnCreateTable.addActionListener(this);
		btnDropTable.addActionListener(this);
		btnDesigner.addActionListener(this);
		btnUserManager.addActionListener(this);
		btnRunQuery.addActionListener(this);
		btnNewRow.addActionListener(this);
		btnUpdateRow.addActionListener(this);
		btnDeleteRow.addActionListener(this);
		btnAddField.addActionListener(this);
		btnDeleteField.addActionListener(this);

		// Pack & show.
		this.pack();
		this.setVisible(true);
	}
	/*
	 *@description: returns the class controlling this UI.
	 */
	public nl.errorsoft.esql.connection.control.ConnectionWindowCC getControlClass() {
		return this.cwcc;
	}

	// Disable buttons if root selected.
	public void rootSelected() {
		btnDesigner.setEnabled(false);
		btnCreateTable.setEnabled(false);
		btnDropTable.setEnabled(false);
		btnAddField.setEnabled(false);
		btnDeleteField.setEnabled(false);
		btnRunQuery.setEnabled(false);
		btnNewRow.setEnabled(false);
		btnDeleteRow.setEnabled(false);
		btnUpdateRow.setEnabled(false);
	}

	// Disable buttons if database selected.
	public void databaseSelected() {
		btnDesigner.setEnabled(true);
		btnCreateTable.setEnabled(true);
		btnDropTable.setEnabled(false);
		btnAddField.setEnabled(false);
		btnDeleteField.setEnabled(false);
		btnRunQuery.setEnabled(true);
		btnNewRow.setEnabled(false);
		btnDeleteRow.setEnabled(false);
		btnUpdateRow.setEnabled(false);
	}

	// Disable buttons if table selected.
	public void tableSelected() {
		btnDesigner.setEnabled(false);
		btnCreateTable.setEnabled(true);
		btnDropTable.setEnabled(true);
		btnAddField.setEnabled(true);
		btnDeleteField.setEnabled(false);
		btnRunQuery.setEnabled(true);
		btnNewRow.setEnabled(true);
		btnDeleteRow.setEnabled(true);
		btnUpdateRow.setEnabled(true);
	}

	// Disable buttons if field selected.
	public void fieldSelected() {
		btnDesigner.setEnabled(false);
		btnCreateTable.setEnabled(true);
		btnDropTable.setEnabled(true);
		btnAddField.setEnabled(true);
		btnDeleteField.setEnabled(true);
		btnRunQuery.setEnabled(true);
		btnNewRow.setEnabled(true);
		btnDeleteRow.setEnabled(true);
		btnUpdateRow.setEnabled(true);
	}

	/*
	 * @descriptions: when running a custom query data cannot be editted. Disable all
	 * buttons which aren`t available.
	 */
	public void disableDataEdit() {
		btnNewRow.setEnabled(false);
		btnDeleteRow.setEnabled(false);
		btnUpdateRow.setEnabled(false);
	}

	// Close frame.
	public void closeUI(boolean confirmation) {
		if (confirmation) {
			if (Dialogs.confirm(this, "Disconnect", "Disconnect from " + getTitle() + "?", "Disconnect")) {
				cwcc.closeUI();
			}
		} else {
			cwcc.closeUI();
		}
	}

	// Shows the database tree.
	public void showDatabaseTreeView(DatabaseTreeView tv) {
		dtv = tv;
		jsp.getViewport().add(dtv);

		dtv.addMouseListener(this);
		dtv.addTreeSelectionListener(e -> {
			TreePath tp = e.getPath();
			selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

			if (selectedNode.getUserObject() instanceof Database) {
				if (e.isAddedPath()) {
					cwcc.databaseSelected((Database) selectedNode.getUserObject());

				}
			} else if (selectedNode.getUserObject() instanceof Table) {
				if (e.isAddedPath()) {
					if (((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getChildCount() > 0) {
						cwcc.tableSelected((Table) selectedNode.getUserObject(), false);
					} else {
						cwcc.tableSelected((Table) selectedNode.getUserObject(), true);
					}
				}
			} else if (selectedNode.getUserObject() instanceof nl.errorsoft.esql.table.TableColumn) {
				cwcc.fieldSelected();
			}
			// Root?!
			else {
				cwcc.rootSelected();
			}
		});
	}

	public DatabaseTreeView getDatabaseTreeView() {
		return (DatabaseTreeView) jsp.getViewport().getView();
	}

	public DefaultMutableTreeNode getSelectedNode() {
		return selectedNode;
	}

	public void showTableDataView(String tabTitle, TableDataView tdv) {
		showView(tabTitle, tdv);
		navigation.removeAll();
		navigation.add(tdv.getNavigationBar(), BorderLayout.CENTER);
		this.tabbedPane.setSelectedComponent(tdv);
		showNavigation();
		SwingUtilities.invokeLater(tdv::requestFocusInWindow);
	}

	public void showTableListView(String tabTitle, TableListView tlv) {
		showView(tabTitle, tlv);
		this.tabbedPane.setSelectedComponent(tlv);
		SwingUtilities.invokeLater(tlv::requestFocusInWindow);
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

	/** Shows the help, also when its tab has been closed. */
	public void showHelp() {
		if (this.tabbedPane.indexOfComponent(helpPane) < 0) {
			this.tabbedPane.addTab("eSQLManager Help", helpPane);
		}

		this.tabbedPane.setSelectedComponent(helpPane);
	}

	/** The paging buttons belong to the table data, they are only shown while that tab is in front. */
	private void showNavigation() {
		navigation.setVisible(viewTab instanceof TableDataView && tabbedPane.getSelectedComponent() == viewTab);
		navigation.revalidate();
	}

	/** Opens a query in a new tab ("Query", "Query 2", ...), puts it in front and gives its editor the focus. */
	public void showQueryTab(QueryUI query) {
		queryTabs++;
		String title = queryTabs == 1 ? "Query" : "Query " + queryTabs;
		this.tabbedPane.addTab(title, query);
		this.tabbedPane.setSelectedComponent(query);
		SwingUtilities.invokeLater(() -> query.getEditor().requestFocusInWindow());
	}

	private void closeTab(int index) {
		Component tab = this.tabbedPane.getComponentAt(index);

		if (tab == viewTab) {
			viewTab = null;
		}
		if (tab instanceof QueryUI query) {
			query.close();
		}

		this.tabbedPane.removeTabAt(index);
		showNavigation();
	}

	/** Shows what the connection did last at the bottom of its window. */
	public void setStatus(String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(text));
			return;
		}

		status.setText(text == null || text.isEmpty() ? READY : text);
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
		if (eventSource == btnNewRow) {
			cwcc.insertNewRow();
		}
		// Delete row
		else if (eventSource == btnDeleteRow) {
			cwcc.deleteSelectedRows();
		}
		// Update or insert row
		else if (eventSource == btnUpdateRow) {
			cwcc.saveSelectedRow();
		}
		// Refresh database tree.
		else if (eventSource == btnRefreshTree) {
			cwcc.showDatabaseTree();
		}
		// Run SQL query window.
		else if (eventSource == btnRunQuery) {
			cwcc.startQueryUI();
		}
		// Read the database into the designer.
		else if (eventSource == btnDesigner) {
			cwcc.openDatabaseInDesigner();
		}
		// Drop table.
		else if (eventSource == btnDropTable) {
			dropTable();
		}
		// Add field.
		else if (eventSource == btnAddField) {
			cwcc.startFieldUI(true, false);
		}
		// Drop field.
		else if (eventSource == btnDeleteField) {
			dropField();
		} else if (eventSource == btnUserManager) {
			cwcc.dispatchUserManagerUI();
		} else if (eventSource == btnCreateTable) {
			cwcc.dispatchCreateTableUI();
		}
	}

	/** Runs an item of the context menu of the tree, on the node that was right clicked (it is selected first). */
	private void perform(TreeMenu.Item item) {
		switch (item) {
			case CREATE_DATABASE -> {
				String input = Dialogs.input(this, "Create database", "Name of the new database:");

				if (input != null) {
					cwcc.createDatabase(input);
				}
			}
			case NEW_QUERY -> cwcc.startQueryUI();
			case USERS -> cwcc.dispatchUserManagerUI();
			case PROCESS_LIST -> cwcc.dispatchProcessUI();
			case SERVER_STATUS -> cwcc.showServerStatus();
			case SERVER_VARIABLES -> cwcc.showServerVariables();
			case EXPORT -> cwcc.dispatchExportUI();
			case IMPORT -> cwcc.dispatchImportUI();
			case RELOAD_DATABASES -> cwcc.showDatabaseTree();
			case OPEN_DATABASE -> cwcc.openDatabase(getDatabase());
			case CREATE_TABLE -> cwcc.dispatchCreateTableUI();
			case OPEN_IN_DESIGNER -> cwcc.openDatabaseInDesigner();
			case DROP_DATABASE -> {
				Database database = getDatabase();
				String name = database != null ? "'" + database.getName() + "'" : "the selected database";

				if (Dialogs.confirmDestructive(this, "Drop database", "Drop database " + name + " and all its tables? This cannot be undone.", "Drop")) {
					cwcc.dropDatabase();
				}
			}
			case RELOAD_TABLES -> cwcc.reloadSelectedDatabase();
			case OPEN_TABLE -> cwcc.openTable(getTable());
			case EDIT_TABLE -> cwcc.dispatchModifyTableUI();
			case INDEXES -> cwcc.dispatchTableIndexesUI();
			case ADD_FIELD -> cwcc.startFieldUI(true, false);
			case EMPTY_TABLE -> {
				if (Dialogs.confirmDestructive(this, "Empty table", "Delete all rows from " + tableName(getTable()) + "? This cannot be undone.", "Empty")) {
					cwcc.flushSelectedTable();
				}
			}
			case DROP_TABLE -> dropTable();
			case OPTIMIZE -> cwcc.optimizeTable();
			case ANALYZE -> cwcc.analyseTable();
			case CHECK -> cwcc.checkTable();
			case REPAIR -> cwcc.repairTable();
			case RELOAD_COLUMNS -> cwcc.reloadSelectedTable();
			case EDIT_FIELD -> cwcc.startFieldUI(false, true);
			case DROP_FIELD -> dropField();
			case SEPARATOR -> {
			}
		}
	}

	private void dropTable() {
		if (Dialogs.confirmDestructive(this, "Drop table", "Drop table " + tableName(getTable()) + "? All its data will be lost.", "Drop")) {
			cwcc.dropTable();
		}
	}

	private void dropField() {
		TableColumn column = getTableColumn();
		String name = column != null ? "column '" + column.getName() + "' from table " + tableName(column.getTable()) : "the selected column";

		if (Dialogs.confirmDestructive(this, "Drop column", "Drop " + name + "? This cannot be undone.", "Drop")) {
			cwcc.dropTableColumn();
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

		for (TreeMenu.Item item : TreeMenu.itemsFor(kind, cwcc.getConnectionProfile().getServerType().getDialect())) {
			if (item == TreeMenu.Item.SEPARATOR) {
				menu.addSeparator();
			} else {
				JMenuItem menuItem = new JMenuItem(item.label());
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
			case RELOAD_DATABASES, RELOAD_TABLES, RELOAD_COLUMNS -> "imgRun";
			default -> null;
		};
	}

	/** 'database.table' for the questions before a table is changed. */
	private static String tableName(Table table) {
		return table != null ? "'" + table.getDatabase().getName() + "." + table.getName() + "'" : "the selected table";
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
		if (e.getSource() != dtv || !e.isPopupTrigger()) {
			return;
		}

		TreePath path = dtv.getPathForLocation(e.getX(), e.getY());

		if (path == null) {
			return;
		}
		dtv.setSelectionPath(path);
		selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		contextMenu(selectedNode.getUserObject()).show(dtv, e.getX(), e.getY());
	}

	public Table getTable() {
		Object selected = selectedNode.getUserObject();

		if (selected instanceof Table table) {
			return table;
		}
		if (selected instanceof TableColumn column) {
			return column.getTable();
		}

		return null;
	}

	public void removeDataTab() {
		if (viewTab != null) {
			this.tabbedPane.remove(viewTab);
			viewTab = null;
			navigation.removeAll();
			showNavigation();
		}
	}

	public Database getDatabase() {
		Object selected = selectedNode.getUserObject();

		if (selected instanceof Database database) {
			return database;
		}
		if (selected instanceof Table table) {
			return table.getDatabase();
		}
		if (selected instanceof TableColumn column) {
			return column.getTable().getDatabase();
		}
		return null;
	}

	public TableColumn getTableColumn() {
		Object selected = selectedNode.getUserObject();

		if (selected instanceof TableColumn column) {
			return column;
		}
		return null;
	}

	/** A double click on a database or table opens its tab, a single click only selects. */
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() != dtv || e.getClickCount() != 2 || e.isMetaDown()) {
			return;
		}

		TreePath path = dtv.getPathForLocation(e.getX(), e.getY());

		if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node) {
			if (node.getUserObject() instanceof Database database) {
				cwcc.openDatabase(database);
			} else if (node.getUserObject() instanceof Table table) {
				cwcc.openTable(table);
			}
		}
	}
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() == dtv) {
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		}
	}
	public void mouseExited(MouseEvent e) {
		if (e.getSource() == dtv) {
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		}
	}
}

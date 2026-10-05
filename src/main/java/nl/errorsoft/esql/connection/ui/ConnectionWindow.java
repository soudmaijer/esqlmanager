package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.ui.util.ToolbarButtons;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import org.apache.logging.log4j.LogManager;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;

import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.ui.DatabaseTree;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.help.ui.HelpPanel;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.util.EditorTab;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.table.ui.TableListTab;

import java.awt.BorderLayout;
import java.awt.Color;
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
	private nl.errorsoft.esql.connection.control.ConnectionWindowCC cwcc;
	private ImageLoader imgLoader;
	private DatabaseTree dtv;
	private DefaultMutableTreeNode selectedNode;
	private JScrollPane jsp; // ScrolPane for JTree
	private JSplitPane jsplp; // Tree and jsplData
	private JTabbedPane tabbedPane; // Contains jsp2
	private JScrollPane helpPane;
	private static final String READY = "Ready"; // Only sizes the status bar
	private JLabel status;
	private JPanel navigation;
	private Component viewTab; // The table data or table list, shown in front of the help
	private HelpPanel html; // The user documentation from docs/.
	private int queryTabs; // Query tabs opened so far, for their numbers
	private static final String EDITOR_KEY = "ConnectionWindow.editorKey"; // Finds an editor tab again
	private static final String STATUS_KEY = "ConnectionWindow.status"; // The status message of a tab

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

	public ConnectionWindow(nl.errorsoft.esql.connection.control.ConnectionWindowCC cwcc, MainWindow jmui) {
		// Windowconstructor
		this.cwcc = cwcc;
		this.setTitle(cwcc.getTitle());
		this.setFrameIcon(ApplicationContext.get().imageLoader().getIcon(cwcc.getConnectionProfile().getServerType().iconName()));
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
		ToolbarButtons.style(btnRefreshTree, btnUserManager, btnRunQuery, btnDesigner, btnCreateTable, btnDropTable, btnAddField, btnDeleteField, btnNewRow,
			btnDeleteRow, btnUpdateRow);

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
		html = new HelpPanel();

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
		jsp.getViewport().setBackground(UIManager.getColor("Tree.background"));

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
			if (Dialogs.confirm(this, "Disconnect", "Disconnect from " + getTitle() + "?", "Disconnect") && cwcc.closeDesigners()) {
				cwcc.closeUI();
			}
		} else {
			cwcc.closeUI();
		}
	}

	// Shows the database tree.
	public void showDatabaseTree(DatabaseTree tv) {
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
			} else if (selectedNode.getUserObject() instanceof Schema schema) {
				if (e.isAddedPath()) {
					cwcc.schemaSelected(schema);
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

	public DatabaseTree getDatabaseTree() {
		return (DatabaseTree) jsp.getViewport().getView();
	}

	public DefaultMutableTreeNode getSelectedNode() {
		return selectedNode;
	}

	public void showTableDataTab(String tabTitle, TableDataTab tdv) {
		showView(tabTitle, tdv);
		navigation.removeAll();
		navigation.add(tdv.getNavigationBar(), BorderLayout.CENTER);
		this.tabbedPane.setSelectedComponent(tdv);
		showNavigation();
		SwingUtilities.invokeLater(tdv::requestFocusInWindow);
	}

	public void showTableListTab(String tabTitle, TableListTab tlv) {
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
			cwcc.startQueryTab();
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
			case CREATE_DATABASE -> cwcc.startCreateDatabase();
			case NEW_QUERY -> cwcc.startQueryTab();
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
			case RELOAD_TABLES, RELOAD_SCHEMAS -> cwcc.reloadSelectedDatabase();
			case CREATE_SCHEMA -> cwcc.startCreateSchema();
			case RENAME_SCHEMA -> cwcc.renameSchema();
			case DROP_SCHEMA -> {
				Schema schema = getSchema();
				String term = dialect().schemaTerm();

				if (Dialogs.confirmDestructive(this, "Drop " + term, "Drop " + term + " '" + (schema == null ? "" : schema.getName())
					+ "' and everything in it? This cannot be undone.", "Drop")) {
					cwcc.dropSchema();
				}
			}
			case RENAME_TABLE -> cwcc.renameSelectedTable();
			case DUPLICATE_TABLE -> cwcc.duplicateSelectedTable();
			case PROPERTIES -> cwcc.showProperties();
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
		return cwcc.getConnectionProfile().getServerType().getDialect();
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
		if (selected instanceof Schema schema) {
			return schema.getDatabase();
		}
		if (selected instanceof Table table) {
			return table.getDatabase();
		}
		if (selected instanceof TableColumn column) {
			return column.getTable().getDatabase();
		}
		return null;
	}

	/** The selected schema, or the schema of the selected table or column; null when there is none. */
	public Schema getSchema() {
		Object selected = selectedNode == null ? null : selectedNode.getUserObject();

		if (selected == null || selected instanceof Schema) {
			return (Schema) selected;
		}
		Table table = getTable();
		return table != null ? table.getSchema() : null;
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

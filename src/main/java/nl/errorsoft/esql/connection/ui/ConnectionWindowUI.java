package nl.errorsoft.esql.connection.ui;

import org.apache.logging.log4j.LogManager;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
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
	private Component viewTab; // The table data or table list, shown in front of the help
	private JEditorPane html; // The HTML info data.

	// Root menu.
	private JLabel rtlabel;
	private JPopupMenu rtmenu;
	private JMenuItem rtCreate;
	private JMenu rtMySQL;
	private JMenuItem rtProcess;
	private JMenuItem rtStatus;
	private JMenuItem rtVariables;
	private JMenu rtPostgres;
	private JMenu rtSQLServer;
	private JMenuItem rtRefresh;

	// Database menu.
	private JLabel dblabel;
	private JPopupMenu dbmenu;
	private JMenuItem dbCreateDatabase;
	private JMenuItem dbDrop;
	private JMenuItem dbCreateTable;
	private JMenuItem dbDesigner;
	private JMenuItem dbRefresh;

	// Table menu.
	private JLabel tblabel;
	private JPopupMenu tbmenu;
	private JMenuItem tbCreateTable;
	private JMenuItem tbEditTable;
	private JMenuItem tbEmptyTable;
	private JMenuItem tbDropTable;
	private JMenuItem tbIndexes;
	private JMenu tbMySQL;
	private JMenuItem tbmAnalyze;
	private JMenuItem tbmCheck;
	private JMenuItem tbmOptimize;
	private JMenuItem tbmRepair;
	private JMenu tbPostgres;
	private JMenu tbSQLServer;
	private JMenuItem tbAddField;
	private JMenuItem tbRefresh;

	// Field menu.
	private JLabel fdlabel;
	private JPopupMenu fdmenu;
	private JMenuItem fdAddField;
	private JMenuItem fdEditField;
	private JMenuItem fdDropField;
	private JMenuItem fdRefresh;

	// Internal toolbar.
	private JToolBar tbTable;
	private JButton btnRefreshTree;
	private JButton btnDropDb;
	private JButton btnCreateDb;
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
		this.setFrameIcon(new ImageIcon(jmui.getIconImage()));
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
		btnCreateDb = new JButton(imgLoader.getIcon("imgCreateDb"));
		btnCreateDb.setToolTipText("Create database");
		btnDropDb = new JButton(imgLoader.getIcon("imgDropDb"));
		btnDropDb.setToolTipText("Drop database");
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
		tbTable.add(btnCreateDb);
		tbTable.add(btnDropDb);
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
		this.btnDropDb.setEnabled(false);
		this.btnDesigner.setEnabled(false);
		this.btnDropTable.setEnabled(false);
		this.btnCreateTable.setEnabled(false);
		this.btnAddField.setEnabled(false);
		this.btnDeleteField.setEnabled(false);
		this.getContentPane().add(tbTable, BorderLayout.NORTH);

		/******************************************************************
		 *
		 *		Popup menu`s
		 */

		// Root menu.
		rtlabel = new JLabel();
		rtlabel.setIcon(imgLoader.getIcon("rt_select_20x20"));
		rtlabel.setIconTextGap(0);
		rtmenu = new JPopupMenu();
		rtCreate = new JMenuItem("Create database");
		rtMySQL = new JMenu("MySQL");
		rtProcess = new JMenuItem("Process monitor");
		rtStatus = new JMenuItem("Show status");
		rtVariables = new JMenuItem("Show variables");
		rtPostgres = new JMenu("Postgres");
		rtSQLServer = new JMenu("SQL Server");
		rtRefresh = new JMenuItem("Reload database(s)");

		// Root menu item placement.
		rtmenu.add(rtlabel);
		rtmenu.addSeparator();
		rtmenu.add(rtCreate);
		rtmenu.addSeparator();
		rtmenu.add(rtMySQL);
		rtMySQL.add(rtProcess);
		rtMySQL.add(rtStatus);
		rtMySQL.add(rtVariables);
		rtmenu.add(rtPostgres);
		rtmenu.add(rtSQLServer);
		rtmenu.addSeparator();
		rtmenu.add(rtRefresh);

		// Database menu.
		dblabel = new JLabel();
		dblabel.setIcon(imgLoader.getIcon("db_select_20x20"));
		dblabel.setIconTextGap(0);
		dbmenu = new JPopupMenu();
		dbCreateDatabase = new JMenuItem("Create database");
		dbDrop = new JMenuItem("Drop database");
		dbCreateTable = new JMenuItem("Create table");
		dbRefresh = new JMenuItem("Reload table(s)");
		dbDesigner = new JMenuItem("Open in designer");

		// Database menu item placement.
		dbmenu.add(dblabel);
		dbmenu.addSeparator();
		dbmenu.add(dbCreateDatabase);
		dbmenu.add(dbDrop);
		dbmenu.addSeparator();
		dbmenu.add(dbCreateTable);
		dbmenu.add(dbDesigner);
		dbmenu.addSeparator();
		dbmenu.add(dbRefresh);

		// Table menu.
		tblabel = new JLabel();
		tblabel.setIcon(imgLoader.getIcon("tb_select_20x20"));
		tblabel.setIconTextGap(0);
		tbmenu = new JPopupMenu();
		tbCreateTable = new JMenuItem("Create table");
		tbEditTable = new JMenuItem("Edit table");
		tbEmptyTable = new JMenuItem("Empty table");
		tbDropTable = new JMenuItem("Drop table");
		tbIndexes = new JMenuItem("Index(es)");
		tbAddField = new JMenuItem("Add field");
		tbMySQL = new JMenu("MySQL");
		tbmAnalyze = new JMenuItem("Analyze table");
		tbmCheck = new JMenuItem("Check table");
		tbmOptimize = new JMenuItem("Optimize table");
		tbmRepair = new JMenuItem("Repair table");
		tbPostgres = new JMenu("Postgres");
		tbSQLServer = new JMenu("SQL Server");
		tbRefresh = new JMenuItem("Reload data");

		//Table menu item placement.
		tbmenu.add(tblabel);
		tbmenu.addSeparator();
		tbmenu.add(tbCreateTable);
		tbmenu.add(tbEditTable);
		tbmenu.add(tbEmptyTable);
		tbmenu.add(tbDropTable);
		tbmenu.add(tbIndexes);
		tbmenu.addSeparator();
		tbmenu.add(tbAddField);
		tbmenu.addSeparator();

		tbMySQL.add(tbmAnalyze);
		tbMySQL.add(tbmCheck);
		tbMySQL.add(tbmOptimize);
		tbMySQL.add(tbmRepair);

		tbmenu.add(tbMySQL);
		tbmenu.add(tbPostgres);
		tbmenu.add(tbSQLServer);
		tbmenu.addSeparator();
		tbmenu.add(tbRefresh);

		// Field menu.
		fdlabel = new JLabel();
		fdlabel.setIcon(imgLoader.getIcon("fd_select_20x20"));
		fdlabel.setIconTextGap(0);
		fdmenu = new JPopupMenu();
		fdAddField = new JMenuItem("Add field");
		fdEditField = new JMenuItem("Edit field");
		fdDropField = new JMenuItem("Drop field");
		fdRefresh = new JMenuItem("Reload field(s)");

		// Field menu item placement.
		fdmenu.add(fdlabel);
		fdmenu.addSeparator();
		fdmenu.add(fdAddField);
		fdmenu.add(fdEditField);
		fdmenu.add(fdDropField);
		fdmenu.addSeparator();
		fdmenu.add(fdRefresh);

		// Disable menu`s which are not required.
		if (cwcc.getConnectionProfile().getServerType().getType() == ServerType.MY_SQL) {
			rtPostgres.setEnabled(false);
			rtSQLServer.setEnabled(false);
			tbPostgres.setEnabled(false);
			tbSQLServer.setEnabled(false);
		} else if (cwcc.getConnectionProfile().getServerType().getType() == ServerType.POSTGRES) {
			rtMySQL.setEnabled(false);
			rtSQLServer.setEnabled(false);
			tbMySQL.setEnabled(false);
			tbSQLServer.setEnabled(false);
		} else if (cwcc.getConnectionProfile().getServerType().getType() == ServerType.MS_SQL_SERVER) {
			rtPostgres.setEnabled(false);
			rtMySQL.setEnabled(false);
			tbPostgres.setEnabled(false);
			tbMySQL.setEnabled(false);
		}

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
		// What this connection did last, below the tabs.
		status = new JLabel(READY);
		status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
			BorderFactory.createEmptyBorder(3, 8, 3, 8)));
		status.setForeground(UIManager.getColor("Label.disabledForeground"));
		status.setPreferredSize(new Dimension(10, 24));
		JPanel tabsWithStatus = new JPanel(new BorderLayout());
		tabsWithStatus.add(tabbedPane, BorderLayout.CENTER);
		tabsWithStatus.add(status, BorderLayout.SOUTH);

		jsplp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp, tabsWithStatus);
		jsplp.setDividerLocation(200);
		jsplp.setOneTouchExpandable(true);

		// Add SplitPane.
		getContentPane().add(jsplp, BorderLayout.CENTER);

		/******************************************************************
		 *
		 *		Action Listeners.
		 */

		// Tablemenu listeners
		tbCreateTable.addActionListener(this);
		tbEditTable.addActionListener(this);
		tbDropTable.addActionListener(this);
		tbAddField.addActionListener(this);
		tbRefresh.addActionListener(this);
		tbEmptyTable.addActionListener(this);
		tbIndexes.addActionListener(this);

		tbmOptimize.addActionListener(this);
		tbmAnalyze.addActionListener(this);
		tbmRepair.addActionListener(this);
		tbmCheck.addActionListener(this);

		// Database menu listeners
		dbCreateDatabase.addActionListener(this);
		dbDrop.addActionListener(this);
		dbCreateTable.addActionListener(this);
		dbRefresh.addActionListener(this);
		dbDesigner.addActionListener(this);

		// Rootmenu listeners
		rtCreate.addActionListener(this);
		rtRefresh.addActionListener(this);
		rtVariables.addActionListener(this);
		rtStatus.addActionListener(this);
		rtProcess.addActionListener(this);

		// Fieldmenu listeners
		fdAddField.addActionListener(this);
		fdEditField.addActionListener(this);
		fdDropField.addActionListener(this);
		fdRefresh.addActionListener(this);

		// Toolbar actionlisteners.
		btnRefreshTree.addActionListener(this);
		btnCreateTable.addActionListener(this);
		btnDropTable.addActionListener(this);
		btnCreateDb.addActionListener(this);
		btnDropDb.addActionListener(this);
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
		btnCreateDb.setEnabled(true);
		btnDropDb.setEnabled(false);
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
		btnCreateDb.setEnabled(true);
		btnDropDb.setEnabled(true);
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
		btnCreateDb.setEnabled(true);
		btnDropDb.setEnabled(true);
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
		btnCreateDb.setEnabled(true);
		btnDropDb.setEnabled(true);
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
			JOptionPane pane = new JOptionPane();

			if ((pane.showConfirmDialog(this, "Are you sure you want to close this window ?", "Close window", JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE)) == JOptionPane.YES_OPTION) {
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
		this.tabbedPane.setSelectedComponent(tdv);
	}

	public void showTableListView(String tabTitle, TableListView tlv) {
		showView(tabTitle, tlv);
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

	private void closeTab(int index) {
		if (this.tabbedPane.getComponentAt(index) == viewTab) {
			viewTab = null;
		}

		this.tabbedPane.removeTabAt(index);
	}

	/** Shows what the connection did last at the bottom of its window. */
	public void setStatus(String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(text));
			return;
		}

		status.setText(text == null || text.isEmpty() ? READY : text);
	}

	public void showMessage(String message) {
		JOptionPane pane = new JOptionPane();
		pane.showMessageDialog(this, message, this.getTitle(), JOptionPane.INFORMATION_MESSAGE);
	}

	public void showMessage(String title, String message) {
		JOptionPane pane = new JOptionPane();
		pane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	public void showErrorMessage(String message) {
		JOptionPane pane = new JOptionPane();
		pane.showMessageDialog(this, message, this.getTitle(), JOptionPane.WARNING_MESSAGE);
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
		else if (eventSource == btnRefreshTree || eventSource == rtRefresh) {
			cwcc.showDatabaseTree();
		}
		// Run SQL query window.
		else if (eventSource == btnRunQuery) {
			cwcc.startQueryUI();
		}
		// Create database
		else if (eventSource == btnCreateDb || eventSource == rtCreate || eventSource == dbCreateDatabase) {
			String input = JOptionPane.showInputDialog(this, "Enter database name", "Create new database", JOptionPane.INFORMATION_MESSAGE);

			if (input != null) {
				cwcc.createDatabase(input);
			}
		}
		// Drop database
		else if (eventSource == btnDropDb || eventSource == dbDrop) {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want drop the selected database?", "Drop database", JOptionPane.YES_NO_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				cwcc.dropDatabase();
			}
		}
		// Read the database into the designer.
		else if (eventSource == btnDesigner || eventSource == dbDesigner) {
			cwcc.openDatabaseInDesigner();
		}
		// Reload databases.
		else if (eventSource == this.dbRefresh) {
			cwcc.reloadSelectedDatabase();
		}
		// Refresh data.
		else if (eventSource == this.tbRefresh) {
			cwcc.reloadSelectedTable();
		}
		// Flush table data.
		else if (eventSource == this.tbEmptyTable) {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want empty the selected table?", "Empty table", JOptionPane.YES_NO_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				cwcc.flushSelectedTable();
			}
		}
		// Drop table.
		else if (eventSource == btnDropTable || eventSource == tbDropTable) {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want drop the selected table?", "Drop table", JOptionPane.YES_NO_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				cwcc.dropTable();
			}
		}
		// Add field.
		else if (eventSource == this.btnAddField || eventSource == this.tbAddField || eventSource == this.fdAddField) {
			cwcc.startFieldUI(true, false);
		}
		// Edit field.
		else if (eventSource == this.fdEditField) {
			cwcc.startFieldUI(false, true);
		}
		// Drop field.
		else if (eventSource == btnDeleteField || eventSource == fdDropField) {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want drop the selected field?", "Drop field", JOptionPane.YES_NO_OPTION);

			if (result == JOptionPane.YES_OPTION) {
				cwcc.dropTableColumn();
			}
		} else if (eventSource == rtStatus) {
			cwcc.showServerStatus();
		} else if (eventSource == rtVariables) {
			cwcc.showServerVariables();
		}
		// MySQL: OPTIMIZE TABLE.
		else if (eventSource == tbmOptimize) {
			cwcc.optimizeTable();
		}
		// MySQL: ANALYSE TABLE.
		else if (eventSource == tbmAnalyze) {
			cwcc.analyseTable();
		}
		// MySQL: REPAIR TABLE.
		else if (eventSource == tbmRepair) {
			cwcc.repairTable();
		}
		// MySQL: CHECK TABLE.
		else if (eventSource == tbmCheck) {
			cwcc.checkTable();
		} else if (eventSource == btnUserManager) {
			cwcc.dispatchUserManagerUI();
		} else if (eventSource == tbIndexes) {
			cwcc.dispatchTableIndexesUI();
		} else if (eventSource == rtProcess) {
			cwcc.dispatchProcessUI();
		} else if (eventSource == dbCreateTable || eventSource == btnCreateTable || eventSource == tbCreateTable) {
			cwcc.dispatchCreateTableUI();
		} else if (eventSource == tbEditTable) {
			cwcc.dispatchModifyTableUI();
		} else {
			JOptionPane.showMessageDialog(this, "Not implemented yet!");
		}
	}
	/******************************************************************
	 *
	 *		Mouse Listener Implementation
	 */

	// Mouse event on JTree or JTable
	public void mouseReleased(MouseEvent e) {
		Object eventSource = e.getSource();

		if (eventSource == dtv && dtv.isSelectionEmpty()) {
			return;
		} else if (eventSource == dtv) {
			if (e.isMetaDown()) {
				int i = dtv.getRowForLocation(e.getX(), e.getY());
				selectedNode = (DefaultMutableTreeNode) dtv.getLastSelectedPathComponent();

				if (i > -1) {
					selectedNode = (DefaultMutableTreeNode) dtv.getPathForRow(i).getLastPathComponent();
				}

				Object selected = selectedNode.getUserObject();

				try {
					if (selected instanceof Table table) {
						tblabel.setText(table.getName());
						tbmenu.show(dtv, e.getX(), e.getY());
					} else if (selected instanceof Database database) {
						dblabel.setText(database.getName());
						dbmenu.show(dtv, e.getX(), e.getY());
					} else if (selected instanceof nl.errorsoft.esql.table.TableColumn column) {
						fdlabel.setText(column.getName());
						fdmenu.show(dtv, e.getX(), e.getY());
					} else {
						rtlabel.setText(this.getTitle());
						rtmenu.show(dtv, e.getX(), e.getY());
					}
				} catch (Exception ex) { // No nodes selected
				}
			}
		}
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

	public void mousePressed(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent e) {
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

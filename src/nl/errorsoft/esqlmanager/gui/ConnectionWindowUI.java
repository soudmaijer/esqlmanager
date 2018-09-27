package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.domain.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class ConnectionWindowUI extends JInternalFrame implements ActionListener, MouseListener
{	
	// Components.
	private nl.errorsoft.esqlmanager.control.ConnectionWindowCC cwcc;
	private ImageLoader imgLoader;
	private DatabaseTreeView dtv;
	private DefaultMutableTreeNode selectedNode;
	private JScrollPane jsp;												// ScrolPane for JTree
	private JSplitPane jsplp;												// Tree and jsplData
	private JTabbedPane tabbedPane;										// Contains jsp2
	private JEditorPane html;												// The HTML info data.

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
	private JMenuItem dbRefresh;
	
	// Table menu.
	private JLabel tblabel;			
	private JPopupMenu tbmenu;					
	private JMenuItem tbCreateTable;
	private JMenuItem tbEditTable;
	private JMenuItem tbEmptyTable;
	private JMenuItem tbDropTable;
	private JMenuItem tbIndexes;
	private JMenu 	  tbMySQL;
	private JMenuItem tbmAnalyze;
	private JMenuItem tbmCheck;
	private JMenuItem tbmOptimize;
	private JMenuItem tbmRepair;
	private JMenu 	  tbPostgres;
	private JMenu 	  tbSQLServer;
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
	private JButton btnRunQuery;
	private JButton btnNewRow;
	private JButton btnUpdateRow;
	private JButton btnDeleteRow;
	private JButton btnAddField;
	private JButton btnDeleteField;

		
	public ConnectionWindowUI( nl.errorsoft.esqlmanager.control.ConnectionWindowCC cwcc, ESQLManagerUI jmui )
	{	
		// Windowconstructor
		this.cwcc = cwcc;
		this.setTitle( cwcc.getTitle() );
		this.setFrameIcon( new ImageIcon( jmui.getIconImage() ) );
		this.setResizable( true );
		this.setMaximizable( true );
		this.setClosable( true );
		this.setIconifiable( true );
		
		// Set internal variables
     	this.setDefaultCloseOperation( JInternalFrame.DO_NOTHING_ON_CLOSE );
     	this.addInternalFrameListener( new InternalFrameAdapter()
     	{
			public void internalFrameClosing(InternalFrameEvent e)
			{
				closeUI(true);
			}     		
     	});
				
		// Get Imageloader
		imgLoader = cwcc.getImageLoader();
						
		/******************************************************************
		 *
		 *		JToolbar
		 */

		tbTable = new JToolBar();  
		tbTable.setFloatable( false );
		btnRefreshTree = new JButton( new ImageIcon( imgLoader.getImage("pc") ) );  
		btnRefreshTree.setToolTipText("Refresh tree");
		btnCreateDb = new JButton( new ImageIcon( imgLoader.getImage("imgCreateDb") ) );  
		btnCreateDb.setToolTipText("Create database");
		btnDropDb = new JButton( new ImageIcon( imgLoader.getImage("imgDropDb") ) );  
		btnDropDb.setToolTipText("Drop database");
		btnCreateTable = new JButton( new ImageIcon( imgLoader.getImage("imgCreateTable") ) );  
		btnCreateTable.setToolTipText("Create table");
		btnDropTable = new JButton( new ImageIcon( imgLoader.getImage("imgDropTable") ) );  
		btnDropTable.setToolTipText("Drop table");
		btnUserManager = new JButton( new ImageIcon( imgLoader.getImage("imgUserManager") ) ); 
		btnUserManager.setToolTipText("User manager");
		btnRunQuery = new JButton( new ImageIcon( imgLoader.getImage("imgRunQuery") ) ); 
		btnRunQuery.setToolTipText("Run SQL query");
		btnNewRow = new JButton( new ImageIcon( imgLoader.getImage("imgNewRow") ) ); 
		btnNewRow.setToolTipText("Insert new row");
		btnUpdateRow = new JButton( new ImageIcon( imgLoader.getImage("imgUpdateRow") ) ); 
		btnUpdateRow.setToolTipText("Update changes");
		btnDeleteRow = new JButton( new ImageIcon( imgLoader.getImage("imgDeleteRow") ) ); 
		btnDeleteRow.setToolTipText("Delete row");
		btnAddField = new JButton( new ImageIcon( imgLoader.getImage("imgAddField") ) ); 
		btnAddField.setToolTipText("Add field");
		btnDeleteField = new JButton( new ImageIcon( imgLoader.getImage("imgDeleteField") ) ); 
		btnDeleteField.setToolTipText("Delete field");

		// Voeg knoppen toe aan toolbar
		tbTable.add( btnRefreshTree );  
		tbTable.add( btnUserManager ); 
		tbTable.add( btnRunQuery ); 
		tbTable.addSeparator(); 
		tbTable.add( btnCreateDb );  
		tbTable.add( btnDropDb );  
		tbTable.addSeparator(); 
		tbTable.add( btnCreateTable );  
		tbTable.add( btnDropTable );  
		tbTable.addSeparator(); 
		tbTable.add( btnAddField );  
		tbTable.add( btnDeleteField );  
		tbTable.addSeparator(); 
		tbTable.add( btnNewRow ); 
		tbTable.add( btnDeleteRow ); 
		tbTable.add( btnUpdateRow ); 

		// Disable.
		this.btnNewRow.setEnabled( false );
		this.btnUpdateRow.setEnabled( false );
		this.btnDeleteRow.setEnabled( false );
		this.btnRunQuery.setEnabled( false );
		this.btnDropDb.setEnabled( false );
		this.btnDropTable.setEnabled( false );
		this.btnCreateTable.setEnabled( false );		
		this.btnAddField.setEnabled( false );
		this.btnDeleteField.setEnabled( false );			
		this.getContentPane().add( tbTable, BorderLayout.NORTH );
		
		/******************************************************************
		 *
		 *		Popup menu`s
		 */		

		// Root menu.
		rtlabel = new JLabel();
		rtlabel.setIcon( new ImageIcon(imgLoader.getImage("rt_select_20x20")) );
		rtlabel.setIconTextGap( 0 );
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
		rtmenu.add( rtCreate );
		rtmenu.addSeparator();
		rtmenu.add( rtMySQL );
		rtMySQL.add( rtProcess );
		rtMySQL.add( rtStatus );
		rtMySQL.add( rtVariables );
		rtmenu.add( rtPostgres );
		rtmenu.add( rtSQLServer );
		rtmenu.addSeparator();
		rtmenu.add( rtRefresh );
		
		// Database menu.
		dblabel = new JLabel();
		dblabel.setIcon(new ImageIcon(imgLoader.getImage("db_select_20x20")));
		dblabel.setIconTextGap( 0 );
		dbmenu = new JPopupMenu();		
		dbCreateDatabase = new JMenuItem("Create database");
		dbDrop = new JMenuItem("Drop database");
		dbCreateTable = new JMenuItem("Create table");
		dbRefresh = new JMenuItem("Reload table(s)");

		// Database menu item placement.
		dbmenu.add( dblabel );		
		dbmenu.addSeparator();
		dbmenu.add( dbCreateDatabase );		
		dbmenu.add( dbDrop );		
		dbmenu.addSeparator();
		dbmenu.add( dbCreateTable );
		dbmenu.addSeparator();
		dbmenu.add( dbRefresh );	

		// Table menu.
		tblabel = new JLabel();
		tblabel.setIcon(new ImageIcon(imgLoader.getImage("tb_select_20x20")));
		tblabel.setIconTextGap( 0 );
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
		tbmenu.add( tblabel );
		tbmenu.addSeparator();
		tbmenu.add( tbCreateTable );
		tbmenu.add( tbEditTable );
		tbmenu.add( tbEmptyTable );
		tbmenu.add( tbDropTable );
		tbmenu.add( tbIndexes );
		tbmenu.addSeparator();
		tbmenu.add( tbAddField );
		tbmenu.addSeparator();
		
		tbMySQL.add( tbmAnalyze );
		tbMySQL.add( tbmCheck );
		tbMySQL.add( tbmOptimize );
		tbMySQL.add( tbmRepair );
		
		tbmenu.add( tbMySQL );
		tbmenu.add( tbPostgres );
		tbmenu.add( tbSQLServer );
		tbmenu.addSeparator();
		tbmenu.add( tbRefresh );
		
		// Field menu.
		fdlabel = new JLabel();
		fdlabel.setIcon(new ImageIcon(imgLoader.getImage("fd_select_20x20")));
		fdlabel.setIconTextGap( 0 );
		fdmenu = new JPopupMenu();				
		fdAddField = new JMenuItem("Add field");
		fdEditField = new JMenuItem("Edit field");
		fdDropField = new JMenuItem("Drop field");
		fdRefresh = new JMenuItem("Reload field(s)");

		// Field menu item placement.
		fdmenu.add( fdlabel );
		fdmenu.addSeparator();
		fdmenu.add( fdAddField );		
		fdmenu.add( fdEditField );		
		fdmenu.add( fdDropField );		
		fdmenu.addSeparator();
		fdmenu.add( fdRefresh );
		
		// Disable menu`s which are not required.
		if( cwcc.getConnectionProfile().getServerType().getType() == ServerType.MY_SQL )
		{
			rtPostgres.setEnabled( false );
			rtSQLServer.setEnabled( false );
			tbPostgres.setEnabled( false );
			tbSQLServer.setEnabled( false );
		}
		else if( cwcc.getConnectionProfile().getServerType().getType() == ServerType.POSTGRES )
		{
			rtMySQL.setEnabled( false );
			rtSQLServer.setEnabled( false );
			tbMySQL.setEnabled( false );
			tbSQLServer.setEnabled( false );			
		}		
		else if( cwcc.getConnectionProfile().getServerType().getType() == ServerType.MS_SQL_SERVER )
		{
			rtPostgres.setEnabled( false );
			rtMySQL.setEnabled( false );
			tbPostgres.setEnabled( false );
			tbMySQL.setEnabled( false );			
		}		
		
		/******************************************************************
		 *
		 *		Right SplitPane setup
		 */

		// EditorPane
	   try
	   {	
	   	html = new JEditorPane( this.getClass().getResource("../../html/help.html") );
	    	html.setEditable(false);
	    	html.addHyperlinkListener( new HyperLinkListener(cwcc) );
	   }
	   catch( Exception e )
	   {
	   }
		
		// TabbedPane properties.
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab( "eSQLManager Help", new JScrollPane(html) );
		tabbedPane.setSelectedIndex( 0 );
		
		/******************************************************************
		 *
		 *		Left SplitPane setup
		 */		

		// JTree
		jsp = new JScrollPane();
		jsp.getViewport().setBackground( Color.white );

		// SplitPane properties.
		jsplp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp, tabbedPane );
		jsplp.setDividerLocation( 200 );
		jsplp.setOneTouchExpandable(true);
				
		// Add SplitPane.
		getContentPane().add(jsplp);

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
		btnUserManager.addActionListener(this);
		btnRunQuery.addActionListener(this);
		btnNewRow.addActionListener(this);
		btnUpdateRow.addActionListener(this);
		btnDeleteRow.addActionListener(this);
		btnAddField.addActionListener(this);
		btnDeleteField.addActionListener(this);

		// Pack & show.
		this.pack();
		this.setVisible( true );
	}
	/*
	 *@description: returns the class controlling this UI.
	 */	
	public nl.errorsoft.esqlmanager.control.ConnectionWindowCC getControlClass()
	{
		return this.cwcc;
	}
	
	// Disable buttons if root selected.
	public void rootSelected()
	{
		btnCreateDb.setEnabled( true );
		btnDropDb.setEnabled( false );
		btnCreateTable.setEnabled( false );
		btnDropTable.setEnabled( false );
		btnAddField.setEnabled( false );
		btnDeleteField.setEnabled( false );		
		btnRunQuery.setEnabled( false );
		btnNewRow.setEnabled( false );
		btnDeleteRow.setEnabled( false );
		btnUpdateRow.setEnabled( false );
	}
	
	// Disable buttons if database selected.
	public void databaseSelected()
	{
		btnCreateDb.setEnabled( true );
		btnDropDb.setEnabled( true );
		btnCreateTable.setEnabled( true );
		btnDropTable.setEnabled( false );
		btnAddField.setEnabled( false );
		btnDeleteField.setEnabled( false );		
		btnRunQuery.setEnabled( true );
		btnNewRow.setEnabled( false );
		btnDeleteRow.setEnabled( false );
		btnUpdateRow.setEnabled( false );		
	}
	
	// Disable buttons if table selected.
	public void tableSelected()
	{
		btnCreateDb.setEnabled( true );
		btnDropDb.setEnabled( true );
		btnCreateTable.setEnabled( true );
		btnDropTable.setEnabled( true );
		btnAddField.setEnabled( true );
		btnDeleteField.setEnabled( false );		
		btnRunQuery.setEnabled( true );
		btnNewRow.setEnabled( true );
		btnDeleteRow.setEnabled( true );
		btnUpdateRow.setEnabled( true );		
	}

	// Disable buttons if field selected.
	public void fieldSelected()
	{
		btnCreateDb.setEnabled( true );
		btnDropDb.setEnabled( true );
		btnCreateTable.setEnabled( true );
		btnDropTable.setEnabled( true );
		btnAddField.setEnabled( true );
		btnDeleteField.setEnabled( true );		
		btnRunQuery.setEnabled( true );
		btnNewRow.setEnabled( true );
		btnDeleteRow.setEnabled( true );
		btnUpdateRow.setEnabled( true );		
	}	
	
	/*
	 * @descriptions: when running a custom query data cannot be editted. Disable all 
	 * buttons which aren`t available.
	 */
	public void disableDataEdit()
	{
		btnNewRow.setEnabled( false );
		btnDeleteRow.setEnabled( false );
		btnUpdateRow.setEnabled( false );				
	}
	
	// Close frame.
	public void closeUI( boolean confirmation )
	{
		if( confirmation )
		{
			JOptionPane pane = new JOptionPane();
			
			if( (pane.showConfirmDialog(this, "Are you sure you want to close this window ?", "Close window", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) == JOptionPane.YES_OPTION)
			{	cwcc.closeUI();
			}
		}
		else
			cwcc.closeUI();
	}
	
	// Shows the database tree.
	public void showDatabaseTreeView( DatabaseTreeView tv )
	{
		dtv = tv;
		jsp.getViewport().add( dtv );

		dtv.addMouseListener( this );
		dtv.addTreeSelectionListener( new TreeSelectionListener()
		{
			/*
			 * Tree selection listener implementation.
			 */
			public void valueChanged( TreeSelectionEvent e )
			{
				TreePath tp = e.getPath();
				selectedNode = (DefaultMutableTreeNode)e.getPath().getLastPathComponent();
				
				if( selectedNode.getUserObject() instanceof Database )
				{					
					if( e.isAddedPath() )
					{
						cwcc.databaseSelected( (Database)selectedNode.getUserObject() );
						
					}
				}
				else if( selectedNode.getUserObject() instanceof Table )
				{
					if( e.isAddedPath() )
					{
						if( ((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getChildCount() > 0 )
							cwcc.tableSelected( (Table)selectedNode.getUserObject(), false );
						else
							cwcc.tableSelected( (Table)selectedNode.getUserObject(), true );
					}
				}
				else if( selectedNode.getUserObject() instanceof nl.errorsoft.esqlmanager.domain.TableColumn )
				{
					cwcc.fieldSelected();
				}
				// Root?!
				else
				{
					cwcc.rootSelected();
				}
			}
		});
	}
	
	public DatabaseTreeView getDatabaseTreeView()
	{
		return (DatabaseTreeView)jsp.getViewport().getView();
	}
	
	public DefaultMutableTreeNode getSelectedNode()
	{
		return selectedNode;
	}
	
	public void showTableDataView( String tabTitle, TableDataView tdv )
	{
		if( this.tabbedPane.getTabCount() == 1 )
			this.tabbedPane.insertTab( tabTitle, null, tdv, "", 0 );
			
		this.tabbedPane.setTitleAt( 0, tabTitle );
		this.tabbedPane.setComponentAt( 0, tdv );
		this.tabbedPane.setSelectedIndex( 0 );
	}	

	public void showTableListView( String tabTitle, TableListView tlv )
	{
		if( this.tabbedPane.getTabCount() == 1 )
			this.tabbedPane.insertTab( tabTitle, null, tlv, "", 0 );
			
		this.tabbedPane.setTitleAt( 0, tabTitle );
		this.tabbedPane.setComponentAt( 0, tlv );
	}	
 	
 	public void showHelp()
 	{
 		if( this.tabbedPane.getTabCount() == 1 )
 			this.tabbedPane.setSelectedIndex( 0 );
 		else
 			this.tabbedPane.setSelectedIndex( 1 );
 	}

	public void showMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.INFORMATION_MESSAGE );  					
	}	
	
	public void showMessage( String title, String message ) 
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );  					
	}		 	
	 	
	public void showErrorMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.WARNING_MESSAGE );  					
	}	
	
	public String toString()
	{
		return this.getTitle();
	}
		
	/******************************************************************
	 *
	 *		Action Listeners implementation
	 */

	public void actionPerformed(ActionEvent e)
	{	
		Object eventSource = e.getSource();

		// Insert new row
		if( eventSource == btnNewRow )
		{
			cwcc.insertNewRow();	
		}
		// Delete row
		else if( eventSource == btnDeleteRow )
		{
			cwcc.deleteSelectedRows();
		}
		// Update or insert row
		else if( eventSource == btnUpdateRow )
		{	
			cwcc.saveSelectedRow();
		}	
		// Refresh database tree.
		else if( eventSource == btnRefreshTree || eventSource == rtRefresh )
		{	
			cwcc.showDatabaseTree();
		}
		// Run SQL query window.
		else if( eventSource == btnRunQuery )
		{
			cwcc.startQueryUI();
		}
		// Create database
		else if( eventSource == btnCreateDb || eventSource == rtCreate || eventSource == dbCreateDatabase )
		{
			String input = JOptionPane.showInputDialog(this, "Enter database name", "Create new database", JOptionPane.INFORMATION_MESSAGE );
			
			if( input != null )
			{
				cwcc.createDatabase( input );
			}			
		}		
		// Drop database
		else if( eventSource == btnDropDb || eventSource == dbDrop )
		{
			int result = JOptionPane.showConfirmDialog( this, "Are you sure you want drop the selected database?", "Drop database", JOptionPane.YES_NO_OPTION );
			
			if( result == JOptionPane.YES_OPTION )
			{
				cwcc.dropDatabase();
			}				
		}
		// Reload databases.
		else if( eventSource == this.dbRefresh )
		{
			cwcc.reloadSelectedDatabase();
		}	
		// Refresh data.
		else if( eventSource == this.tbRefresh )
		{
			cwcc.reloadSelectedTable();
		}	
		// Flush table data.
		else if( eventSource == this.tbEmptyTable )
		{
			int result = JOptionPane.showConfirmDialog( this, "Are you sure you want empty the selected table?", "Empty table", JOptionPane.YES_NO_OPTION );
			
			if( result == JOptionPane.YES_OPTION )
			{
				cwcc.flushSelectedTable();
			}
		}		
		// Drop table.
		else if( eventSource == btnDropTable || eventSource == tbDropTable )
		{
			int result = JOptionPane.showConfirmDialog( this, "Are you sure you want drop the selected table?", "Drop table", JOptionPane.YES_NO_OPTION );
			
			if( result == JOptionPane.YES_OPTION )
			{
				cwcc.dropTable();
			}				
		}				
		// Add field.
		else if( eventSource == this.btnAddField || eventSource == this.tbAddField || eventSource == this.fdAddField )
		{
			cwcc.startFieldUI( true, false );
		}
		// Edit field.
		else if( eventSource == this.fdEditField )
		{
			cwcc.startFieldUI( false, true );
		}						
		// Drop field.
		else if( eventSource == btnDeleteField || eventSource == fdDropField )
		{
			int result = JOptionPane.showConfirmDialog( this, "Are you sure you want drop the selected field?", "Drop field", JOptionPane.YES_NO_OPTION );
			
			if( result == JOptionPane.YES_OPTION )
			{
				cwcc.dropTableColumn();
			}				
		}
		// MySQL: SHOW STATUS
		else if( eventSource == rtStatus )
		{
			cwcc.showMySQLStatus();
		}
		// MySQL: SHOW VARIABLES
		else if( eventSource == rtVariables )
		{
			cwcc.showMySQLVariables();
		}			
		// MySQL: OPTIMIZE TABLE.
		else if( eventSource == tbmOptimize )
		{
			cwcc.optimizeTable();
		}
		// MySQL: ANALYSE TABLE.
		else if( eventSource == tbmAnalyze )
		{
			cwcc.analyseTable();
		}
		// MySQL: REPAIR TABLE.
		else if( eventSource == tbmRepair )
		{
			cwcc.repairTable();
		}
		// MySQL: CHECK TABLE.
		else if( eventSource == tbmCheck )
		{
			cwcc.checkTable();
		}							
		else if (  eventSource == btnUserManager )
		{	
			cwcc.dispatchUserManagerUI();
		}
		else if (  eventSource == tbIndexes )
		{	
			cwcc.dispatchTableIndexesUI();
		}		
		else if( eventSource == rtProcess )
		{
			cwcc.dispatchProcessUI();
		}		
		else if( eventSource == dbCreateTable || eventSource == btnCreateTable || eventSource == tbCreateTable )
		{	
			cwcc.dispatchCreateTableUI();
		}		
		else if( eventSource == tbEditTable )
		{	
			cwcc.dispatchModifyTableUI();
		}		
		else
		{
			JOptionPane.showMessageDialog( this, "Not implemented yet!");
		}		
	}
	/******************************************************************
	 *
	 *		Mouse Listener Implementation
	 */

 	// Mouse event on JTree or JTable
	public void mouseReleased(MouseEvent e)
	{		
		Object eventSource = e.getSource();
		
		if( eventSource == dtv && dtv.isSelectionEmpty() )
		{
			return;
		}
		else if( eventSource == dtv )
		{				
			if( e.isMetaDown() )
			{
				int i = dtv.getRowForLocation( e.getX(), e.getY() );
				selectedNode = (DefaultMutableTreeNode)dtv.getLastSelectedPathComponent();
				
				if( i > -1 )
				{
					selectedNode = (DefaultMutableTreeNode)dtv.getPathForRow( i ).getLastPathComponent();
				}
				
				Object selected = selectedNode.getUserObject();

				try
				{
					if( selected instanceof Table )
					{	
						tblabel.setText( ((Table)selected).getName() );
						tbmenu.show(dtv,e.getX(), e.getY());			
					}
					else if( selected instanceof Database )
					{	
						dblabel.setText( ((Database)selected).getName() );	
						dbmenu.show(dtv,e.getX(), e.getY());
					}
					else if( selected instanceof nl.errorsoft.esqlmanager.domain.TableColumn )
					{	
						fdlabel.setText( ((nl.errorsoft.esqlmanager.domain.TableColumn)selected).getName() );
						fdmenu.show(dtv, e.getX(), e.getY());
					}
					else
					{	
						rtlabel.setText( this.getTitle() );
						rtmenu.show(dtv,e.getX(), e.getY());
					}
				}
				catch(Exception ex)
				{	// No nodes selected 
				}
			}
		}
	}
	
	public Table getTable()
	{
		Object selected = selectedNode.getUserObject();
		
		if( selected instanceof Table )
		{
			return (Table)selected;	
		}
		if( selected instanceof TableColumn )
		{
			return ((TableColumn)selected).getTable();	
		}

		return null;
	}
	
	public void removeDataTab()
	{
		if( this.tabbedPane.getTabCount() == 2 )
			this.tabbedPane.removeTabAt(0);
	}
	
	public Database getDatabase()
	{
		Object selected = selectedNode.getUserObject();
		
		if( selected instanceof Database )
		{
			return (Database)selected;	
		}
		if( selected instanceof Table )
		{
			return ((Table)selected).getDatabase();	
		}		
		if( selected instanceof TableColumn )
		{
			return ((TableColumn)selected).getTable().getDatabase();	
		}		
		return null;
	}

	public TableColumn getTableColumn()
	{
		Object selected = selectedNode.getUserObject();
		
		if( selected instanceof TableColumn )
		{
			return (TableColumn)selected;	
		}
		return null;
	}
	
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) 
	{
		if( e.getSource() == dtv )
			this.setCursor( new java.awt.Cursor( java.awt.Cursor.HAND_CURSOR ) );
	}
	public void mouseExited(MouseEvent e) 
	{
		if( e.getSource() == dtv )
			this.setCursor( new java.awt.Cursor( java.awt.Cursor.DEFAULT_CURSOR ) );
	}
}
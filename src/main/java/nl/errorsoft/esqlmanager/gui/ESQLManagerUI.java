package nl.errorsoft.esqlmanager.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import nl.errorsoft.esqlmanager.domain.*;
import nl.errorsoft.esqlmanager.control.*;

public class ESQLManagerUI extends JFrame implements ActionListener
{
	// Control class for ESQLManager UI, manages all use-cases actions.
	private ESQLManagerCC jmcc;
	private JTabbedPane jtpQueryOutput;

	// Menubar
	private JMenuBar menubar;
	private JMenu mnuGroupOptions;
	private JMenuItem mnuConnect;
	private JMenuItem mnuDisconnect;
	private JMenuItem mnuExit;
	private JMenu mnuGroupSettings;
	private JMenuItem mnuSettings;
	private JMenuItem mnuJDBC;
	private JMenu mnuGroupServer;
	private JMenuItem mnuProcesses;
	private JMenu mnuGroupImportExport;
	private JMenuItem mnuImportFromFile;
	private JMenuItem mnuExportToFile;
	private JMenuItem mnuDesigner;
	
	private JMenu mnuGroupWindow;
	private JMenuItem mnuTileCascade;
	private JMenuItem mnuTileHorizontal;
	private JMenuItem mnuTileVertical;
	private JMenu mnuGroupHelp;
	private JMenuItem mnuAbout;
	private JMenuItem mnuUpdate;
	private JMenuItem mnuRegister;

	// Toolbar
	private JToolBar toolbar;
	private JButton btnConnect;
	private JButton btnDisconnect;
	private JButton btnCascade;
	private JButton btnTileHorizontal;
	private JButton btnTileVertical;
	private JComboBox cmbWindows;

	// Statusbar
	private JPanel statusbar;
	private StatusLight stl;
	private Label statusMsg;

	// Containers etc.
	private JSplitPane jsplit;
	private JScrollPane jsp;
	private JTextPane jta;
	private JDesktopPane jdp;
	private ImageLoader imgLoader;
	private MutableAttributeSet attributeSet;
	
	SyntaxDocument syndoc = new SyntaxDocument();
	private JPanel jp;

	public ESQLManagerUI( ESQLManagerCC jmcc )
	{
		this.jmcc = jmcc;

		// Get Imageloader
		imgLoader = jmcc.getImageLoader();
		
		// Create components.
		initComponents();

		// Set window properties
		this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		this.setTitle( jmcc.getTitle() );
		this.setIconImage( imgLoader.getImage("windowIcon") );
		this.addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent w)
			{
				closeUI();
			}
		});
		this.pack();
		
		// Fix found on Sun forum: http://forum.java.sun.com/thread.jsp?forum=57&thread=158893
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle screenRect = ge.getMaximumWindowBounds();
		
		this.setSize( (int)screenRect.getWidth(), (int)screenRect.getHeight() );
		this.setVisible( true );
		this.setExtendedState( JFrame.MAXIMIZED_BOTH );

		// Show rest
		jsplit.setDividerLocation( 0.85 );
	}

	public void initComponents()
	{
		try
		{
			// Set default platform Look & Feel.
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch( Exception e )	{}

		/*
		 * Menubar
		 */
 		menubar = new JMenuBar();
		mnuGroupOptions = new JMenu("Options");
		mnuConnect = new JMenuItem("Connect");
		mnuDisconnect = new JMenuItem("Disconnect");
		mnuExit = new JMenuItem("Exit");
		mnuGroupOptions.add(mnuConnect);
		mnuGroupOptions.add(mnuDisconnect);
		mnuGroupOptions.addSeparator();
		mnuGroupOptions.add(mnuExit);
		menubar.add(mnuGroupOptions);

		mnuGroupSettings = new JMenu("Settings");
		mnuSettings = new JMenuItem("Preferences");
		mnuJDBC = new JMenuItem("JDBC Driver settings");
		mnuJDBC.addActionListener( this );
		mnuGroupSettings.add( mnuSettings );
		mnuGroupSettings.add( mnuJDBC );
		menubar.add(mnuGroupSettings);

		mnuGroupImportExport = new JMenu("Tools");
		mnuImportFromFile = new JMenuItem("Import data...");
		mnuExportToFile = new JMenuItem("Export data...");
		mnuDesigner = new JMenuItem("Database Designer");
		mnuGroupImportExport.add( mnuImportFromFile );
		mnuGroupImportExport.add( mnuExportToFile );
		mnuGroupImportExport.addSeparator();
		mnuGroupImportExport.add( mnuDesigner );
		menubar.add( mnuGroupImportExport );

		mnuGroupWindow = new JMenu("Window");
		mnuTileCascade = new JMenuItem("Cascade");
		mnuTileHorizontal = new JMenuItem("Tile horizontal");
		mnuTileVertical = new JMenuItem("Tile vertical");
		mnuGroupWindow.add(mnuTileCascade);
		mnuGroupWindow.add(mnuTileHorizontal);
		mnuGroupWindow.add(mnuTileVertical);
		menubar.add( mnuGroupWindow );

		mnuGroupHelp = new JMenu("Help");
		mnuAbout = new JMenuItem("About");
		mnuUpdate = new JMenuItem("Check for updates");
		mnuRegister = new JMenuItem("Enter registration details...");
		mnuGroupHelp.add(mnuUpdate);
		
		if( jmcc.isPro() )
			mnuGroupHelp.add(mnuRegister);
			
		mnuGroupHelp.addSeparator();
		mnuGroupHelp.add(mnuAbout);
		menubar.add( mnuGroupHelp );

		setJMenuBar(menubar);

		/*
		 * Toolbar
		 */
		toolbar = new JToolBar();
		toolbar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
		toolbar.setFloatable( true );
		btnConnect = new JButton( new ImageIcon( imgLoader.getImage("imgConnect") ) );
		btnConnect.setEnabled( true );
		btnConnect.setToolTipText("Connect");
		toolbar.add( btnConnect );

		btnDisconnect = new JButton( new ImageIcon( imgLoader.getImage("imgDisconnect") ) );
		btnDisconnect.setEnabled( false );
		btnDisconnect.setToolTipText("Disconnect");
		toolbar.add( btnDisconnect );

		toolbar.addSeparator();

		btnCascade = new JButton( new ImageIcon( imgLoader.getImage("imgCascade") ) );
		btnCascade.setEnabled( false );
		btnCascade.setToolTipText("Cascade");
		toolbar.add( btnCascade );

		btnTileHorizontal = new JButton( new ImageIcon( imgLoader.getImage("imgTileHorizontal") ) );
		btnTileHorizontal.setEnabled( false );
		btnTileHorizontal.setToolTipText("Tile horizontal");
		toolbar.add( btnTileHorizontal );

		btnTileVertical = new JButton( new ImageIcon( imgLoader.getImage("imgTileVertical") ) );
		btnTileVertical.setEnabled( false );
		btnTileVertical.setToolTipText("Tile vertical");
		toolbar.add( btnTileVertical );

		toolbar.addSeparator();

		cmbWindows = new JComboBox();
		cmbWindows.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if( cmbWindows.getItemCount() <= 0 )
						return;

					ConnectionWindowUI window = (ConnectionWindowUI)cmbWindows.getSelectedItem();

					if( window != null )
						jdp.getDesktopManager().activateFrame( window );
				}
				catch( Exception ae )
				{
					System.out.println( ae.getMessage() );
				}
			}
		});

		cmbWindows.setPreferredSize( new Dimension( 125, 20 ) );
		toolbar.add( cmbWindows );
		this.getContentPane().add( toolbar, BorderLayout.NORTH );

		/*
		 *	Statusbar
		 */
		statusbar = new JPanel();
		statusbar.setLayout( new BorderLayout() );
		stl = new StatusLight(imgLoader);
		stl.setPreferredSize(new Dimension(20,16));
		statusbar.add( stl, BorderLayout.EAST );
		
		jp = new JPanel( new FlowLayout( FlowLayout.LEFT, 3, 2 ) );
		statusMsg = new Label( "© Copyright Errorsoft 2002-2003." );
		statusMsg.setBackground( new JLabel().getBackground() );
		statusMsg.setSize( new Dimension( 200, 20 ) );
		jp.add( statusMsg );
		
		statusbar.add( jp, BorderLayout.WEST);
		this.getContentPane().add( statusbar, BorderLayout.SOUTH );

		/*
		 * Other components
		 */
		jtpQueryOutput = new JTabbedPane( JTabbedPane.BOTTOM );
		jtpQueryOutput.setFont( new Font(jtpQueryOutput.getFont().getName(), Font.BOLD, jtpQueryOutput.getFont().getSize()) ); 
		
		jta = new JTextPane();
		jta.setFont(new Font("arial", Font.PLAIN, 11));
		jta.setEditable(false);

		attributeSet = new javax.swing.text.SimpleAttributeSet();
		StyleConstants.setBold(attributeSet, false);
		StyleConstants.setForeground(attributeSet, java.awt.Color.black);
		jta.setDocument( syndoc );
		
		// DesktopPane.
		jdp = new JDesktopPane();
		jdp.setBackground( Color.gray );

		//ScrollPane for tree.
		jsp = new JScrollPane(jta);
		jsp.getViewport().setBackground(Color.white);
		jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jtpQueryOutput.addTab( "Output", jsp );
		
		jsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jdp, jtpQueryOutput);
		jsplit.setOneTouchExpandable(true);
		getContentPane().add(jsplit);

		/*
		 *	ActionListeners
		 */
		// Menubar
		mnuConnect.addActionListener(this);
		mnuDisconnect.addActionListener(this);
		mnuExit.addActionListener(this);

		mnuSettings.addActionListener(this);
		mnuTileCascade.addActionListener(this);
		mnuTileVertical.addActionListener(this);
		mnuTileHorizontal.addActionListener(this);

		mnuExportToFile.addActionListener(this);
		mnuImportFromFile.addActionListener(this);
		mnuDesigner.addActionListener(this);

		btnCascade.addActionListener(this);
		btnTileVertical.addActionListener(this);
		btnTileHorizontal.addActionListener(this);

		mnuAbout.addActionListener(this);
		mnuUpdate.addActionListener(this);
		mnuRegister.addActionListener(this);

		// Toolbar
		btnConnect.addActionListener(this);
		btnDisconnect.addActionListener(this);

		// Window and component listener to listen to resize events.
		addComponentListener( new ComponentAdapter()
		{
	    	public void componentResized( java.awt.event.ComponentEvent event )
			{	jsplit.setDividerLocation( 0.85 );
			}
		});
	}

	public void closeUI()
	{
		JOptionPane pane = new JOptionPane("Shutdown",JOptionPane.WARNING_MESSAGE );

		if( JOptionPane.showConfirmDialog( this, "Are you sure you want to exit ?", "Shutdown", JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
		{
			jmcc.closeUI();
		}
	}

	public JInternalFrame getSelectedFrame()
	{
		return this.jdp.getSelectedFrame();
	}

	public void updateStatus( final String message, final boolean red )
	{
		stl.switchRedLight(red);
		
		statusMsg.setText( message );
		statusMsg.setBackground( new JLabel().getBackground() );
		statusMsg.setSize( new Dimension( 200, 20 ) );
	}

	// Displays messages in output window.
	public void print( String s )
	{
		try
		{	
			syndoc.append( s );
			jta.setCaretPosition( jta.getDocument().getLength() );
		}
		catch(Exception e)	{	}
	}

	public void addConnectionWindow( ConnectionWindowUI cw )
	{
		btnConnect.setEnabled( true );
		btnDisconnect.setEnabled( true );
		btnCascade.setEnabled( true );
		btnTileHorizontal.setEnabled( true );
		btnTileVertical.setEnabled( true );

		jdp.add( cw );
		jdp.getDesktopManager().maximizeFrame( cw );

		cmbWindows.addItem( cw );
		cmbWindows.setSelectedIndex( cmbWindows.getItemCount()-1 );
	}
	
	public ConnectionWindowUI getConnectionWindow()
	{
		return (ConnectionWindowUI)cmbWindows.getSelectedItem();
	}

	public int getConnectionWindowCount()
	{
		return cmbWindows.getItemCount();
	}	
	
	public void removeConnectionWindow( ConnectionWindowUI cw )
	{
		cmbWindows.removeItemAt( cmbWindows.getSelectedIndex() );
		jdp.getDesktopManager().closeFrame( cw );

		if( jdp.getAllFrames().length == 0 )
		{
			btnConnect.setEnabled( true );
			btnDisconnect.setEnabled( false );
			btnCascade.setEnabled( false );
			btnTileHorizontal.setEnabled( false );
			btnTileVertical.setEnabled( false );
			jmcc.dispatchConnectionProfileUI();
		}
	}

	public void actionPerformed( java.awt.event.ActionEvent event )
	{
		Object object = event.getSource();

		// Check for menu or Toolbar events.
		if (object == mnuExit)
		{
			closeUI();
		}
		else if ( object == mnuRegister )
		{
			jmcc.startRegistrationUI();
		}		
		else if ( object == mnuConnect || object == btnConnect )
		{
			jmcc.dispatchConnectionProfileUI();
		}
		else if ( object == mnuSettings )
		{
			jmcc.dispatchSettingsUI();
		}
		else if ( object == mnuDisconnect || object == btnDisconnect )
		{
			((ConnectionWindowUI)jdp.getSelectedFrame() ).closeUI(true);
		}
		else if( object == mnuTileVertical || object == btnTileVertical )
		{
			DesktopUtils.tileVertical(jdp);
		}
		else if( object == mnuTileHorizontal || object == btnTileHorizontal )
		{
			DesktopUtils.tileHorizontal(jdp);
		}
		else if( object == mnuTileCascade || object == btnCascade )
		{
			DesktopUtils.cascadeAll(jdp);
		}
		else if( object == mnuAbout )
		{
			jmcc.showSplashScreen(0);
		}
		else if( object == mnuUpdate )
		{
			jmcc.checkForUpdates();
		}
		else if( object == mnuJDBC )
		{
			jmcc.dispatchDriverUI();
		}
		// Import sql file.
		else if( object == mnuImportFromFile )
		{
		   if( jdp.getAllFrames().length > 0 )
		   {
		   	jmcc.dispatchImportUI();
		   }
		}		
		// Export sql file.
		else if( object == mnuExportToFile )
		{
		   if( jdp.getAllFrames().length > 0 )
		   {
		   	jmcc.dispatchExportUI();
		   }
		}
		// Start designer
		else if( object == mnuDesigner )
		{
		   if( jdp.getAllFrames().length > 0 )
		   {
		   	jmcc.dispatchDesigner();
		   }
		}		
	}
	
	public void showMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.INFORMATION_MESSAGE );  					
	}	
	
	public void showMessage(String title, String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, title, JOptionPane.INFORMATION_MESSAGE );  					
	}	
	 	
	public void showErrorMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.WARNING_MESSAGE );  					
	}	
}
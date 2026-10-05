package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.ui.util.ToolbarButtons;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.connection.ui.ServerIconRenderer;
import nl.errorsoft.esql.designer.ui.DBCreator;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.DesktopUtils;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.icon.StatusLight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class ESQLManagerUI extends JFrame implements ActionListener {
	private static final Logger log = LogManager.getLogger(ESQLManagerUI.class);

	// Control class for ESQLManager UI, manages all use-cases actions.
	private ESQLManagerCC jmcc;
	private static final int MAX_OUTPUT_CHARS = 200000;
	private JPanel outputPanel;

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

	// Toolbar
	private JToolBar toolbar;
	private JButton btnConnect;
	private JButton btnDisconnect;
	private JButton btnCascade;
	private JButton btnTileHorizontal;
	private JButton btnTileVertical;
	private JComboBox<JInternalFrame> cmbWindows; // Connection windows and designers

	// Statusbar
	private JPanel statusbar;
	private StatusLight stl;
	private static final String NO_CONNECTION = "No connection";
	private JLabel statusMsg;
	private JLabel statusInfo;

	// Containers etc.
	private JSplitPane jsplit;
	private JScrollPane jsp;
	private RSyntaxTextArea jta;
	private JDesktopPane jdp;
	private ImageLoader imgLoader;

	public ESQLManagerUI(ESQLManagerCC jmcc) {
		this.jmcc = jmcc;

		// Get Imageloader
		imgLoader = ApplicationContext.get().imageLoader();

		// Create components.
		initComponents();

		// Set window properties
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle(jmcc.getTitle());
		java.util.List<Image> logo = ImageLoader.logoImages();
		this.setIconImages(logo);
		try {
			if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
				Taskbar.getTaskbar().setIconImage(logo.get(logo.size() - 1));
			}
		} catch (UnsupportedOperationException | SecurityException e) {
			// The Dock icon is cosmetic: without it the platform's default icon stays.
		}
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent w) {
				closeUI();
			}
		});
		this.pack();

		// Fix found on Sun forum: http://forum.java.sun.com/thread.jsp?forum=57&thread=158893
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle screenRect = ge.getMaximumWindowBounds();

		this.setSize((int) screenRect.getWidth(), (int) screenRect.getHeight());
		this.setVisible(true);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);

		// Show rest
		jsplit.setDividerLocation(0.85);
	}

	public void initComponents() {
		ApplicationContext.get().settings().getAppearance().apply();

		/*
		 * Menubar
		 */
		menubar = new JMenuBar();
		mnuGroupOptions = new JMenu("Options");
		mnuConnect = new JMenuItem("Connect...");
		mnuDisconnect = new JMenuItem("Disconnect");
		mnuExit = new JMenuItem("Exit");
		mnuGroupOptions.add(mnuConnect);
		mnuGroupOptions.add(mnuDisconnect);
		mnuGroupOptions.addSeparator();
		mnuGroupOptions.add(mnuExit);
		menubar.add(mnuGroupOptions);

		mnuGroupSettings = new JMenu("Settings");
		mnuSettings = new JMenuItem("Preferences...");
		mnuJDBC = new JMenuItem("JDBC Driver settings...");
		mnuJDBC.addActionListener(this);
		mnuGroupSettings.add(mnuSettings);
		mnuGroupSettings.add(mnuJDBC);
		menubar.add(mnuGroupSettings);

		mnuGroupImportExport = new JMenu("Tools");
		mnuImportFromFile = new JMenuItem("Import data...");
		mnuExportToFile = new JMenuItem("Export data...");
		mnuDesigner = new JMenuItem("Database Designer");
		mnuGroupImportExport.add(mnuImportFromFile);
		mnuGroupImportExport.add(mnuExportToFile);
		mnuGroupImportExport.addSeparator();
		mnuGroupImportExport.add(mnuDesigner);
		menubar.add(mnuGroupImportExport);
		updateMenus();

		mnuGroupWindow = new JMenu("Window");
		mnuTileCascade = new JMenuItem("Cascade");
		mnuTileHorizontal = new JMenuItem("Tile horizontal");
		mnuTileVertical = new JMenuItem("Tile vertical");
		mnuGroupWindow.add(mnuTileCascade);
		mnuGroupWindow.add(mnuTileHorizontal);
		mnuGroupWindow.add(mnuTileVertical);
		menubar.add(mnuGroupWindow);

		mnuGroupHelp = new JMenu("Help");
		mnuAbout = new JMenuItem("About...");
		mnuGroupHelp.add(mnuAbout);
		menubar.add(mnuGroupHelp);

		setJMenuBar(menubar);

		/*
		 * Toolbar
		 */
		toolbar = new JToolBar();
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		toolbar.setFloatable(true);
		btnConnect = new JButton(imgLoader.getIcon("imgConnect"));
		btnConnect.setEnabled(true);
		btnConnect.setToolTipText("Connect");
		toolbar.add(btnConnect);

		btnDisconnect = new JButton(imgLoader.getIcon("imgDisconnect"));
		btnDisconnect.setEnabled(false);
		btnDisconnect.setToolTipText("Disconnect");
		toolbar.add(btnDisconnect);

		toolbar.addSeparator();

		btnCascade = new JButton(imgLoader.getIcon("imgCascade"));
		btnCascade.setEnabled(false);
		btnCascade.setToolTipText("Cascade");
		toolbar.add(btnCascade);

		btnTileHorizontal = new JButton(imgLoader.getIcon("imgTileHorizontal"));
		btnTileHorizontal.setEnabled(false);
		btnTileHorizontal.setToolTipText("Tile horizontal");
		toolbar.add(btnTileHorizontal);

		btnTileVertical = new JButton(imgLoader.getIcon("imgTileVertical"));
		btnTileVertical.setEnabled(false);
		btnTileVertical.setToolTipText("Tile vertical");
		toolbar.add(btnTileVertical);

		toolbar.addSeparator();

		cmbWindows = new JComboBox<>();
		cmbWindows.setRenderer(new ServerIconRenderer());
		cmbWindows.addActionListener(e -> {
			try {
				if (cmbWindows.getItemCount() <= 0) {
					return;
				}

				JInternalFrame window = (JInternalFrame) cmbWindows.getSelectedItem();

				if (window != null) {
					if (window.isIcon()) {
						window.setIcon(false);
					}
					window.setSelected(true);
				}
			} catch (Exception ae) {
				log.error(ae.getMessage(), ae);
			}
		});

		cmbWindows.setToolTipText("Active window");
		cmbWindows.setMinimumSize(new Dimension(160, ToolbarButtons.HEIGHT));
		cmbWindows.setPreferredSize(new Dimension(220, ToolbarButtons.HEIGHT));
		toolbar.add(cmbWindows);
		ToolbarButtons.style(btnConnect, btnDisconnect, btnCascade, btnTileHorizontal, btnTileVertical);
		this.getContentPane().add(toolbar, BorderLayout.NORTH);

		/*
		 *	Statusbar
		 */
		statusbar = new JPanel();
		statusbar.setLayout(new BorderLayout());
		stl = new StatusLight(imgLoader);
		stl.switchRedLight(true);

		statusMsg = new JLabel(NO_CONNECTION);
		statusMsg.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 8));
		statusMsg.setPreferredSize(new Dimension(200, 20));
		JPanel statusState = new JPanel(new BorderLayout());
		statusState.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		statusState.add(stl, BorderLayout.WEST);
		statusState.add(statusMsg, BorderLayout.CENTER);

		statusInfo = new JLabel(" ", SwingConstants.RIGHT);
		statusInfo.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		statusInfo.setForeground(UIManager.getColor("Label.disabledForeground"));

		statusbar.add(statusState, BorderLayout.WEST);
		statusbar.add(statusInfo, BorderLayout.CENTER);
		this.getContentPane().add(statusbar, BorderLayout.SOUTH);

		/*
		 * Other components
		 */
		outputPanel = new JPanel(new BorderLayout());
		JLabel outputTitle = new JLabel("Output");
		outputTitle.setFont(outputTitle.getFont().deriveFont(Font.BOLD));
		outputTitle.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		outputPanel.add(outputTitle, BorderLayout.NORTH);

		// No line wrapping: re-wrapping a long log on every width change made resizing slow.
		// The log shows the statements that were run, so it gets the SQL colours of the query editor.
		jta = new RSyntaxTextArea();
		jta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		jta.setLineWrap(false);
		jta.setEditable(false);
		jta.setHighlightCurrentLine(false);
		EditorTheme.install(jta);

		// DesktopPane.
		jdp = new JDesktopPane();
		jdp.setBackground(UIManager.getColor("Desktop.background"));
		// A designer that is not maximized stays reachable when the main window gets smaller.
		jdp.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				DesktopUtils.keepFramesInside(jdp);
			}
		});

		//ScrollPane for tree.
		jsp = new JScrollPane(jta);
		jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		outputPanel.add(jsp, BorderLayout.CENTER);

		jsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jdp, outputPanel);
		// Layout is cheap with FlatLaf, so the panels follow the divider while dragging.
		jsplit.setContinuousLayout(true);
		// A maximized internal frame must not limit how far the divider can move.
		jdp.setMinimumSize(new Dimension(0, 0));
		// Extra window height goes to the desktop, the output panel keeps its height unless the divider is moved.
		jsplit.setResizeWeight(1.0);
		outputPanel.setMinimumSize(new Dimension(0, 60));
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

		// Toolbar
		btnConnect.addActionListener(this);
		btnDisconnect.addActionListener(this);
	}

	public void closeUI() {
		if (Dialogs.confirmDestructive(this, "Exit eSQLManager", "Exit eSQLManager? Open connections will be closed.", "Exit")) {
			jmcc.closeUI();
		}
	}

	public JInternalFrame getSelectedFrame() {
		return this.jdp.getSelectedFrame();
	}

	/** The resting state: green and connected while a connection window is open, red when there is none. */
	public void showConnectionState() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::showConnectionState);
			return;
		}

		boolean connected = getConnectionWindowCount() > 0;
		updateStatus(connected ? "Connected" : NO_CONNECTION, !connected);
		updateMenus();
	}

	/** The items that work on a connection are enabled while a connection window is open. */
	private void updateMenus() {
		boolean connected = getConnectionWindowCount() > 0;
		for (JMenuItem item : new JMenuItem[]{mnuDisconnect, mnuImportFromFile, mnuExportToFile, mnuDesigner}) {
			item.setEnabled(connected);
		}
	}

	public void updateStatus(final String message, final boolean red) {
		stl.switchRedLight(red);

		statusMsg.setText(message);
	}

	/** Shows what the active connection is looking at, next to the action that is going on. */
	public void setStatusInfo(final String info) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatusInfo(info));
			return;
		}
		statusInfo.setText(info.length() == 0 ? " " : info);
	}

	// Displays messages in output window.
	public void print(String s) {
		// Log messages come from any thread, the document may only be changed on the event thread.
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> print(s));
			return;
		}

		try {
			jta.append(s);
			trimOutput();
			jta.setCaretPosition(jta.getDocument().getLength());
		} catch (javax.swing.text.BadLocationException e) {
			// Cannot happen, trimOutput only reads and removes inside the document.
		}
	}

	// Keeps the output panel from growing without limit.
	private void trimOutput() throws javax.swing.text.BadLocationException {
		javax.swing.text.Document doc = jta.getDocument();
		int length = doc.getLength();

		if (length > MAX_OUTPUT_CHARS) {
			int cut = length - MAX_OUTPUT_CHARS / 2;
			String head = doc.getText(cut, Math.min(200, length - cut));
			int lineEnd = head.indexOf('\n');
			doc.remove(0, lineEnd < 0 ? cut : cut + lineEnd + 1);
		}
	}

	public void addConnectionWindow(ConnectionWindowUI cw) {
		btnConnect.setEnabled(true);
		btnDisconnect.setEnabled(true);
		btnCascade.setEnabled(true);
		btnTileHorizontal.setEnabled(true);
		btnTileVertical.setEnabled(true);

		jdp.add(cw);

		// A maximized frame follows the size of the desktop, so the content scales with the output panel.
		try {
			cw.setMaximum(true);
		} catch (java.beans.PropertyVetoException e) {
			log.warn("Could not maximize the connection window", e);
		}

		cmbWindows.addItem(cw);
		cmbWindows.setSelectedIndex(cmbWindows.getItemCount() - 1);
		updateMenus();
	}

	/** Shows a designer on the desktop, centred and in front, and lists it in the window selector. */
	public void addDesignerWindow(DBCreator designer) {
		Dimension desktop = jdp.getSize();
		designer.setSize(Math.min(designer.getWidth(), desktop.width), Math.min(designer.getHeight(), desktop.height));
		designer.setLocation((desktop.width - designer.getWidth()) / 2, (desktop.height - designer.getHeight()) / 2);
		jdp.add(designer);
		designer.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, e -> cmbWindows.repaint());
		designer.setVisible(true);

		cmbWindows.addItem(designer);
		cmbWindows.setSelectedItem(designer);
	}

	public void removeDesignerWindow(DBCreator designer) {
		cmbWindows.removeItem(designer);
		designer.dispose();
	}

	/**
	 * Closes the designers opened from a connection window, each asking to save its model first.
	 * @return false when the user cancelled one of them
	 */
	public boolean closeDesigners(ConnectionWindowUI cw) {
		for (JInternalFrame frame : jdp.getAllFrames()) {
			if (frame instanceof DBCreator designer && designer.getConnectionWindow() == cw && !designer.close()) {
				return false;
			}
		}
		return true;
	}

	/** The connection window in front, or the one the designer in front belongs to. */
	public ConnectionWindowUI getConnectionWindow() {
		return switch (cmbWindows.getSelectedItem()) {
			case ConnectionWindowUI cw -> cw;
			case DBCreator designer -> designer.getConnectionWindow();
			case null, default -> null;
		};
	}

	public int getConnectionWindowCount() {
		int count = 0;
		for (int i = 0; i < cmbWindows.getItemCount(); i++) {
			if (cmbWindows.getItemAt(i) instanceof ConnectionWindowUI) {
				count++;
			}
		}
		return count;
	}

	public void removeConnectionWindow(ConnectionWindowUI cw) {
		cmbWindows.removeItem(cw);
		jdp.getDesktopManager().closeFrame(cw);

		if (getConnectionWindowCount() == 0) {
			btnConnect.setEnabled(true);
			btnDisconnect.setEnabled(false);
			btnCascade.setEnabled(false);
			btnTileHorizontal.setEnabled(false);
			btnTileVertical.setEnabled(false);
			jmcc.dispatchConnectionProfileUI();
		}

		showConnectionState();
	}

	public void actionPerformed(java.awt.event.ActionEvent event) {
		Object object = event.getSource();

		// Check for menu or Toolbar events.
		if (object == mnuExit) {
			closeUI();
		} else if (object == mnuConnect || object == btnConnect) {
			jmcc.dispatchConnectionProfileUI();
		} else if (object == mnuSettings) {
			jmcc.dispatchSettingsUI();
		} else if (object == mnuDisconnect || object == btnDisconnect) {
			ConnectionWindowUI cw = getConnectionWindow();
			if (cw != null) {
				cw.closeUI(true);
			}
		} else if (object == mnuTileVertical || object == btnTileVertical) {
			DesktopUtils.tileVertical(jdp);
		} else if (object == mnuTileHorizontal || object == btnTileHorizontal) {
			DesktopUtils.tileHorizontal(jdp);
		} else if (object == mnuTileCascade || object == btnCascade) {
			DesktopUtils.cascadeAll(jdp);
		} else if (object == mnuAbout) {
			new AboutDialog(this, jmcc.getAppName(), jmcc.getAppVersion(), jmcc.getAppCommit(), () -> jmcc.showSplashScreen(0)).showDialog();
		} else if (object == mnuJDBC) {
			jmcc.dispatchDriverUI();
		}
		// Import sql file.
		else if (object == mnuImportFromFile) {
			if (getConnectionWindowCount() > 0) {
				jmcc.dispatchImportUI();
			}
		}
		// Export sql file.
		else if (object == mnuExportToFile) {
			if (getConnectionWindowCount() > 0) {
				jmcc.dispatchExportUI();
			}
		}
		// Start designer
		else if (object == mnuDesigner) {
			if (getConnectionWindowCount() > 0) {
				jmcc.dispatchDesigner();
			}
		}
	}

}

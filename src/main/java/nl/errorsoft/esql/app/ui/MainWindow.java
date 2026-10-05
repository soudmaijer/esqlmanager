package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.app.ui.dialog.AboutDialog;

import nl.errorsoft.esql.ui.util.ToolbarButtons;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.connection.ui.ServerIconRenderer;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.DesktopWindows;
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

public class MainWindow extends JFrame implements ActionListener {
	private static final Logger log = LogManager.getLogger(MainWindow.class);

	// Control class for BuildInfo UI, manages all use-cases actions.
	private MainController mainController;
	private static final int MAX_OUTPUT_CHARS = 200000;
	private JPanel outputPanel;

	// Menubar
	private JMenuBar menubar;
	private JMenu optionsMenu;
	private JMenuItem connectItem;
	private JMenuItem disconnectItem;
	private JMenuItem exitItem;
	private JMenu settingsMenu;
	private JMenuItem settingsItem;
	private JMenuItem jdbcItem;
	private JMenu serverMenu;
	private JMenuItem processesItem;
	private JMenu importExportMenu;
	private JMenuItem importFromFileItem;
	private JMenuItem exportToFileItem;
	private JMenuItem designerItem;

	private JMenu windowMenu;
	private JMenuItem tileCascadeItem;
	private JMenuItem tileHorizontalItem;
	private JMenuItem tileVerticalItem;
	private JMenu helpMenu;
	private JMenuItem aboutItem;

	// Toolbar
	private JToolBar toolbar;
	private JButton connectButton;
	private JButton disconnectButton;
	private JButton cascadeButton;
	private JButton tileHorizontalButton;
	private JButton tileVerticalButton;
	private JComboBox<JInternalFrame> windowsCombo; // Connection windows and designers

	// Statusbar
	private JPanel statusbar;
	private StatusLight statusLight;
	private static final int MINIMUM_WIDTH = 640;
	private static final int MINIMUM_HEIGHT = 420;
	private static final String NO_CONNECTION = "No connection";
	private JLabel statusMsg;
	private JLabel statusInfo;

	// Containers etc.
	private JSplitPane split;
	private JScrollPane outputScroll;
	private RSyntaxTextArea outputText;
	private JDesktopPane desktop;
	private ImageLoader imageLoader;

	public MainWindow(MainController mainController) {
		this.mainController = mainController;

		// Get Imageloader
		imageLoader = ApplicationContext.get().imageLoader();

		// Create components.
		initComponents();

		// Set window properties
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle(mainController.getTitle());
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
				closeWindow();
			}
		});
		this.pack();
		// Below this the toolbar wraps and the output panel, the desktop and the status bar no longer fit; the window cannot be made smaller.
		this.setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));

		// Fix found on Sun forum: http://forum.java.sun.com/thread.outputScroll?forum=57&thread=158893
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle screenRect = ge.getMaximumWindowBounds();

		this.setSize((int) screenRect.getWidth(), (int) screenRect.getHeight());
		this.setVisible(true);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);

		// Settings that could not be read were replaced by the defaults; the user hears it once, over the window.
		var settingsProblem = ApplicationContext.get().settings().takeLoadProblem();
		if (settingsProblem != null) {
			SwingUtilities.invokeLater(() -> ApplicationContext.get().errors().report(this, "Read settings", settingsProblem));
		}

		// Show rest
		split.setDividerLocation(0.85);
	}

	public void initComponents() {
		ApplicationContext.get().settings().getAppearance().apply();

		/*
		 * Menubar
		 */
		menubar = new JMenuBar();
		optionsMenu = new JMenu("Options");
		connectItem = new JMenuItem("Connect...");
		disconnectItem = new JMenuItem("Disconnect");
		exitItem = new JMenuItem("Exit");
		optionsMenu.add(connectItem);
		optionsMenu.add(disconnectItem);
		optionsMenu.addSeparator();
		optionsMenu.add(exitItem);
		menubar.add(optionsMenu);

		settingsMenu = new JMenu("Settings");
		settingsItem = new JMenuItem("Preferences...");
		jdbcItem = new JMenuItem("JDBC Driver settings...");
		jdbcItem.addActionListener(this);
		settingsMenu.add(settingsItem);
		settingsMenu.add(jdbcItem);
		menubar.add(settingsMenu);

		importExportMenu = new JMenu("Tools");
		importFromFileItem = new JMenuItem("Import data...");
		exportToFileItem = new JMenuItem("Export data...");
		designerItem = new JMenuItem("Database Designer");
		importExportMenu.add(importFromFileItem);
		importExportMenu.add(exportToFileItem);
		importExportMenu.addSeparator();
		importExportMenu.add(designerItem);
		menubar.add(importExportMenu);

		windowMenu = new JMenu("Window");
		tileCascadeItem = new JMenuItem("Cascade");
		tileHorizontalItem = new JMenuItem("Tile horizontal");
		tileVerticalItem = new JMenuItem("Tile vertical");
		windowMenu.add(tileCascadeItem);
		windowMenu.add(tileHorizontalItem);
		windowMenu.add(tileVerticalItem);
		menubar.add(windowMenu);

		helpMenu = new JMenu("Help");
		aboutItem = new JMenuItem("About...");
		helpMenu.add(aboutItem);
		menubar.add(helpMenu);

		setJMenuBar(menubar);

		/*
		 * Toolbar
		 */
		toolbar = new JToolBar();
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		toolbar.setFloatable(true);
		connectButton = new JButton(imageLoader.getIcon("imgConnect"));
		connectButton.setEnabled(true);
		connectButton.setToolTipText("Connect");
		toolbar.add(connectButton);

		disconnectButton = new JButton(imageLoader.getIcon("imgDisconnect"));
		disconnectButton.setEnabled(false);
		disconnectButton.setToolTipText("Disconnect");
		toolbar.add(disconnectButton);

		toolbar.addSeparator();

		cascadeButton = new JButton(imageLoader.getIcon("imgCascade"));
		cascadeButton.setEnabled(false);
		cascadeButton.setToolTipText("Cascade");
		toolbar.add(cascadeButton);

		tileHorizontalButton = new JButton(imageLoader.getIcon("imgTileHorizontal"));
		tileHorizontalButton.setEnabled(false);
		tileHorizontalButton.setToolTipText("Tile horizontal");
		toolbar.add(tileHorizontalButton);

		tileVerticalButton = new JButton(imageLoader.getIcon("imgTileVertical"));
		tileVerticalButton.setEnabled(false);
		tileVerticalButton.setToolTipText("Tile vertical");
		toolbar.add(tileVerticalButton);

		toolbar.addSeparator();

		windowsCombo = new JComboBox<>();
		windowsCombo.setRenderer(new ServerIconRenderer());
		windowsCombo.addActionListener(e -> {
			try {
				if (windowsCombo.getItemCount() <= 0) {
					return;
				}

				JInternalFrame window = (JInternalFrame) windowsCombo.getSelectedItem();

				if (window != null) {
					if (window.isIcon()) {
						window.setIcon(false);
					}
					window.setSelected(true);
				}
			} catch (Exception ae) {
				ApplicationContext.get().errors().report(this, "Switch window", ae);
			}
		});

		windowsCombo.setToolTipText("Active window");
		windowsCombo.setMinimumSize(new Dimension(160, ToolbarButtons.HEIGHT));
		windowsCombo.setPreferredSize(new Dimension(220, ToolbarButtons.HEIGHT));
		toolbar.add(windowsCombo);
		updateMenus();
		ToolbarButtons.style(connectButton, disconnectButton, cascadeButton, tileHorizontalButton, tileVerticalButton);
		this.getContentPane().add(toolbar, BorderLayout.NORTH);

		/*
		 *	Statusbar
		 */
		statusbar = new JPanel();
		statusbar.setLayout(new BorderLayout());
		statusLight = new StatusLight(imageLoader);
		statusLight.switchRedLight(true);

		statusMsg = new JLabel(NO_CONNECTION);
		statusMsg.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 8));
		statusMsg.setPreferredSize(new Dimension(200, 20));
		JPanel statusState = new JPanel(new BorderLayout());
		statusState.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		statusState.add(statusLight, BorderLayout.WEST);
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
		outputText = new RSyntaxTextArea();
		outputText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		outputText.setLineWrap(false);
		outputText.setEditable(false);
		outputText.setHighlightCurrentLine(false);
		EditorTheme.install(outputText);

		// DesktopPane.
		desktop = new JDesktopPane();
		desktop.setBackground(UIManager.getColor("Desktop.background"));
		// A designer that is not maximized stays reachable when the main window gets smaller.
		desktop.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				DesktopWindows.keepFramesInside(desktop);
			}
		});

		//ScrollPane for tree.
		outputScroll = new JScrollPane(outputText);
		outputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		outputPanel.add(outputScroll, BorderLayout.CENTER);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, desktop, outputPanel);
		// Layout is cheap with FlatLaf, so the panels follow the divider while dragging.
		split.setContinuousLayout(true);
		// A maximized internal frame must not limit how far the divider can move.
		desktop.setMinimumSize(new Dimension(0, 0));
		// Extra window height goes to the desktop, the output panel keeps its height unless the divider is moved.
		split.setResizeWeight(1.0);
		outputPanel.setMinimumSize(new Dimension(0, 60));
		getContentPane().add(split);

		/*
		 *	ActionListeners
		 */
		// Menubar
		connectItem.addActionListener(this);
		disconnectItem.addActionListener(this);
		exitItem.addActionListener(this);

		settingsItem.addActionListener(this);
		tileCascadeItem.addActionListener(this);
		tileVerticalItem.addActionListener(this);
		tileHorizontalItem.addActionListener(this);

		exportToFileItem.addActionListener(this);
		importFromFileItem.addActionListener(this);
		designerItem.addActionListener(this);

		cascadeButton.addActionListener(this);
		tileVerticalButton.addActionListener(this);
		tileHorizontalButton.addActionListener(this);

		aboutItem.addActionListener(this);

		// Toolbar
		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
	}

	public void closeWindow() {
		if (Dialogs.confirmDestructive(this, "Exit eSQLManager", "Exit eSQLManager? Open connections will be closed.", "Exit")) {
			mainController.closeWindow();
		}
	}

	public JInternalFrame getSelectedFrame() {
		return this.desktop.getSelectedFrame();
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
		for (JMenuItem item : new JMenuItem[]{disconnectItem, importFromFileItem, exportToFileItem, designerItem}) {
			item.setEnabled(connected);
		}
	}

	public void updateStatus(final String message, final boolean red) {
		statusLight.switchRedLight(red);

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
			outputText.append(s);
			trimOutput();
			outputText.setCaretPosition(outputText.getDocument().getLength());
		} catch (javax.swing.text.BadLocationException e) {
			// Cannot happen, trimOutput only reads and removes inside the document.
		}
	}

	// Keeps the output panel from growing without limit.
	private void trimOutput() throws javax.swing.text.BadLocationException {
		javax.swing.text.Document doc = outputText.getDocument();
		int length = doc.getLength();

		if (length > MAX_OUTPUT_CHARS) {
			int cut = length - MAX_OUTPUT_CHARS / 2;
			String head = doc.getText(cut, Math.min(200, length - cut));
			int lineEnd = head.indexOf('\n');
			doc.remove(0, lineEnd < 0 ? cut : cut + lineEnd + 1);
		}
	}

	public void addConnectionWindow(ConnectionWindow connectionWindow) {
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(true);
		cascadeButton.setEnabled(true);
		tileHorizontalButton.setEnabled(true);
		tileVerticalButton.setEnabled(true);

		desktop.add(connectionWindow);

		// A maximized frame follows the size of the desktop, so the content scales with the output panel.
		try {
			connectionWindow.setMaximum(true);
		} catch (java.beans.PropertyVetoException e) {
			log.warn("Could not maximize the connection window", e);
		}

		windowsCombo.addItem(connectionWindow);
		windowsCombo.setSelectedIndex(windowsCombo.getItemCount() - 1);
		updateMenus();
	}

	/** Shows a designer on the desktop, centred and in front, and lists it in the window selector. */
	public void addDesignerWindow(DesignerWindow designer) {
		Dimension desktopSize = desktop.getSize();
		designer.setSize(Math.min(designer.getWidth(), desktopSize.width), Math.min(designer.getHeight(), desktopSize.height));
		designer.setLocation((desktopSize.width - designer.getWidth()) / 2, (desktopSize.height - designer.getHeight()) / 2);
		desktop.add(designer);
		designer.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, e -> windowsCombo.repaint());
		designer.setVisible(true);

		windowsCombo.addItem(designer);
		windowsCombo.setSelectedItem(designer);
	}

	public void removeDesignerWindow(DesignerWindow designer) {
		windowsCombo.removeItem(designer);
		designer.dispose();
	}

	/**
	 * Closes the designers opened from a connection window, each asking to save its model first.
	 * @return false when the user cancelled one of them
	 */
	public boolean closeDesigners(ConnectionWindow connectionWindow) {
		for (JInternalFrame frame : desktop.getAllFrames()) {
			if (frame instanceof DesignerWindow designer && designer.getConnectionWindow() == connectionWindow && !designer.close()) {
				return false;
			}
		}
		return true;
	}

	/** The connection window in front, or the one the designer in front belongs to. */
	public ConnectionWindow getConnectionWindow() {
		return switch (windowsCombo.getSelectedItem()) {
			case ConnectionWindow connectionWindow -> connectionWindow;
			case DesignerWindow designer -> designer.getConnectionWindow();
			case null, default -> null;
		};
	}

	public int getConnectionWindowCount() {
		int count = 0;
		for (int i = 0; i < windowsCombo.getItemCount(); i++) {
			if (windowsCombo.getItemAt(i) instanceof ConnectionWindow) {
				count++;
			}
		}
		return count;
	}

	public void removeConnectionWindow(ConnectionWindow connectionWindow) {
		windowsCombo.removeItem(connectionWindow);
		desktop.getDesktopManager().closeFrame(connectionWindow);

		if (getConnectionWindowCount() == 0) {
			connectButton.setEnabled(true);
			disconnectButton.setEnabled(false);
			cascadeButton.setEnabled(false);
			tileHorizontalButton.setEnabled(false);
			tileVerticalButton.setEnabled(false);
			mainController.showConnectionProfileDialog();
		}

		showConnectionState();
	}

	public void actionPerformed(java.awt.event.ActionEvent event) {
		Object object = event.getSource();

		// Check for menu or Toolbar events.
		if (object == exitItem) {
			closeWindow();
		} else if (object == connectItem || object == connectButton) {
			mainController.showConnectionProfileDialog();
		} else if (object == settingsItem) {
			mainController.showSettingsDialog();
		} else if (object == disconnectItem || object == disconnectButton) {
			ConnectionWindow connectionWindow = getConnectionWindow();
			if (connectionWindow != null) {
				connectionWindow.closeWindow(true);
			}
		} else if (object == tileVerticalItem || object == tileVerticalButton) {
			DesktopWindows.tileVertical(desktop);
		} else if (object == tileHorizontalItem || object == tileHorizontalButton) {
			DesktopWindows.tileHorizontal(desktop);
		} else if (object == tileCascadeItem || object == cascadeButton) {
			DesktopWindows.cascadeAll(desktop);
		} else if (object == aboutItem) {
			new AboutDialog(this, mainController.getAppName(), mainController.getAppVersion(), mainController.getAppCommit(),
				() -> mainController.showSplashScreen(0)).showDialog();
		} else if (object == jdbcItem) {
			mainController.showDriverDialog();
		}
		// Import sql file.
		else if (object == importFromFileItem) {
			if (getConnectionWindowCount() > 0) {
				mainController.showImportDialog();
			}
		}
		// Export sql file.
		else if (object == exportToFileItem) {
			if (getConnectionWindowCount() > 0) {
				mainController.showExportDialog();
			}
		}
		// Start designer
		else if (object == designerItem) {
			if (getConnectionWindowCount() > 0) {
				mainController.openDesigner();
			}
		}
	}

}

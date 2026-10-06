package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.app.ui.dialog.AboutDialog;

import nl.errorsoft.esql.help.ui.HelpWindow;
import nl.errorsoft.esql.ui.util.ToolbarButtons;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.icon.StatusLight;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
	private JButton helpButton;
	private JMenuItem helpItem;
	private HelpWindow helpWindow; // The documentation, null while it is closed.
	private JButton preferencesButton;
	private JMenuItem jdbcItem;
	private JMenu serverMenu;
	private JMenuItem processesItem;
	private JMenu importExportMenu;
	private JMenuItem importFromFileItem;
	private JMenuItem exportToFileItem;
	private JMenuItem designerItem;

	private JMenu windowMenu;
	private JMenuItem nextWindowItem;
	private JMenuItem previousWindowItem;
	private JMenu helpMenu;
	private JMenuItem aboutItem;

	// Toolbar
	private JToolBar toolbar;
	private JButton connectButton;
	private JButton disconnectButton;

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
	private WindowTabsPanel windowTabs; // Connection windows and designers
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
		optionsMenu.setMnemonic('O');
		connectItem.setMnemonic('C');
		disconnectItem.setMnemonic('D');
		exitItem.setMnemonic('x');
		menubar.add(optionsMenu);

		settingsMenu = new JMenu("Settings");
		settingsItem = new JMenuItem("Preferences...");
		jdbcItem = new JMenuItem("JDBC driver settings...");
		jdbcItem.addActionListener(this);
		settingsMenu.add(settingsItem);
		settingsMenu.add(jdbcItem);
		settingsMenu.setMnemonic('S');
		settingsItem.setMnemonic('P');
		jdbcItem.setMnemonic('J');
		menubar.add(settingsMenu);

		importExportMenu = new JMenu("Tools");
		importFromFileItem = new JMenuItem("Import data...");
		exportToFileItem = new JMenuItem("Export data...");
		designerItem = new JMenuItem("Database designer");
		importExportMenu.add(importFromFileItem);
		importExportMenu.add(exportToFileItem);
		importExportMenu.addSeparator();
		importExportMenu.add(designerItem);
		importExportMenu.setMnemonic('T');
		importFromFileItem.setMnemonic('I');
		exportToFileItem.setMnemonic('E');
		designerItem.setMnemonic('D');
		menubar.add(importExportMenu);

		// The open windows are listed when the menu opens, below next and previous.
		windowMenu = new JMenu("Window");
		int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		nextWindowItem = new JMenuItem("Next window");
		nextWindowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, menuKey | KeyEvent.SHIFT_DOWN_MASK));
		previousWindowItem = new JMenuItem("Previous window");
		previousWindowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, menuKey | KeyEvent.SHIFT_DOWN_MASK));
		windowMenu.setMnemonic('W');
		nextWindowItem.setMnemonic('N');
		previousWindowItem.setMnemonic('P');
		windowMenu.addMenuListener(new javax.swing.event.MenuListener() {
			@Override
			public void menuSelected(javax.swing.event.MenuEvent e) {
				fillWindowMenu();
			}

			@Override
			public void menuDeselected(javax.swing.event.MenuEvent e) {
				// Nothing to undo, the items are built again when the menu opens.
			}

			@Override
			public void menuCanceled(javax.swing.event.MenuEvent e) {
				// Nothing to undo, the items are built again when the menu opens.
			}
		});
		fillWindowMenu();
		menubar.add(windowMenu);

		helpMenu = new JMenu("Help");
		aboutItem = new JMenuItem("About...");
		helpItem = new JMenuItem("eSQLManager Help");
		helpMenu.add(helpItem);
		helpMenu.add(aboutItem);
		helpMenu.setMnemonic('H');
		helpItem.setMnemonic('H');
		aboutItem.setMnemonic('A');
		menubar.add(helpMenu);

		setJMenuBar(menubar);

		/*
		 * Toolbar
		 */
		toolbar = new JToolBar();
		toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		toolbar.setFloatable(false);
		connectButton = new JButton(imageLoader.getIcon("imgConnect"));
		connectButton.setEnabled(true);
		connectButton.setToolTipText("Connect");
		toolbar.add(connectButton);

		disconnectButton = new JButton(imageLoader.getIcon("imgDisconnect"));
		disconnectButton.setEnabled(false);
		disconnectButton.setToolTipText("Disconnect");
		toolbar.add(disconnectButton);

		helpButton = new JButton(imageLoader.getIcon("imgHelp"));
		helpButton.setToolTipText("Help");
		helpButton.getAccessibleContext().setAccessibleName("Help");
		JSeparator buttonsSeparator = new JSeparator(SwingConstants.VERTICAL);
		buttonsSeparator.setPreferredSize(new Dimension(6, 22));
		toolbar.add(buttonsSeparator);
		toolbar.add(helpButton);

		preferencesButton = new JButton(imageLoader.getIcon("imgPreferences"));
		preferencesButton.setToolTipText("Preferences");
		preferencesButton.getAccessibleContext().setAccessibleName("Preferences");
		toolbar.add(preferencesButton);

		ToolbarButtons.style(connectButton, disconnectButton, helpButton, preferencesButton);

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
		windowTabs = new WindowTabsPanel(desktop);
		// The window tabs share the row with the toolbar buttons: tabs fill the width, the buttons sit at the right end behind a thin line.
		JPanel topRow = new JPanel(new BorderLayout());
		JPanel toolbarEnd = new JPanel(new BorderLayout());
		toolbarEnd.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 0));
		toolbarEnd.add(new JSeparator(SwingConstants.VERTICAL), BorderLayout.WEST);
		toolbarEnd.add(toolbar, BorderLayout.CENTER);
		topRow.add(windowTabs, BorderLayout.CENTER);
		topRow.add(toolbarEnd, BorderLayout.EAST);
		this.getContentPane().add(topRow, BorderLayout.NORTH);
		JPanel workArea = new JPanel(new BorderLayout());
		workArea.add(desktop, BorderLayout.CENTER);

		//ScrollPane for tree.
		outputScroll = new JScrollPane(outputText);
		outputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		outputPanel.add(outputScroll, BorderLayout.CENTER);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, workArea, outputPanel);
		// Layout is cheap with FlatLaf, so the panels follow the divider while dragging.
		split.setContinuousLayout(true);
		// A maximized internal frame must not limit how far the divider can move.
		desktop.setMinimumSize(new Dimension(0, 0));
		workArea.setMinimumSize(new Dimension(0, 0));
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
		preferencesButton.addActionListener(this);
		helpButton.addActionListener(e -> showHelp());
		helpItem.addActionListener(e -> showHelp());
		nextWindowItem.addActionListener(e -> windowTabs.selectNext(1));
		previousWindowItem.addActionListener(e -> windowTabs.selectNext(-1));

		exportToFileItem.addActionListener(this);
		importFromFileItem.addActionListener(this);
		designerItem.addActionListener(this);

		aboutItem.addActionListener(this);

		// Toolbar
		connectButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		updateMenus();
	}

	public void closeWindow() {
		if (Dialogs.confirmDestructive(this, "Exit eSQLManager", "Exit eSQLManager? Open connections will be closed.", "Exit")) {
			mainController.closeWindow();
		}
	}

	public JInternalFrame getSelectedFrame() {
		return windowTabs.selectedFrame();
	}

	/** Next and previous, then a check item per open window that brings it to the front. */
	private void fillWindowMenu() {
		windowMenu.removeAll();
		java.util.List<JInternalFrame> frames = windowTabs == null ? java.util.List.of() : windowTabs.getFrames();
		nextWindowItem.setEnabled(frames.size() > 1);
		previousWindowItem.setEnabled(frames.size() > 1);
		windowMenu.add(nextWindowItem);
		windowMenu.add(previousWindowItem);
		if (!frames.isEmpty()) {
			windowMenu.addSeparator();
		}
		for (JInternalFrame frame : frames) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(frame.getTitle(), frame.getFrameIcon(), frame == windowTabs.selectedFrame());
			item.addActionListener(e -> windowTabs.select(frame));
			windowMenu.add(item);
		}
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
		disconnectButton.setEnabled(true);
		windowTabs.addWindow(connectionWindow);
		updateMenus();
	}

	/** Shows the help in a tab of its own, or brings it to the front when it is open. */
	public void showHelp() {
		if (helpWindow == null) {
			helpWindow = new HelpWindow(() -> {
				windowTabs.removeWindow(helpWindow);
				helpWindow = null;
			});
			windowTabs.addWindow(helpWindow);
		} else {
			windowTabs.select(helpWindow);
		}
	}

	/** Shows a designer on the desktop with a tab of its own, in front. */
	public void addDesignerWindow(DesignerWindow designer) {
		windowTabs.addWindow(designer);
	}

	public void removeDesignerWindow(DesignerWindow designer) {
		windowTabs.removeWindow(designer);
	}

	/**
	 * Closes the designers opened from a connection window, each asking to save its model first.
	 * @return false when the user cancelled one of them
	 */
	public boolean closeDesigners(ConnectionWindow connectionWindow) {
		for (JInternalFrame frame : windowTabs.getFrames()) {
			if (frame instanceof DesignerWindow designer && designer.getConnectionWindow() == connectionWindow && !designer.close()) {
				return false;
			}
		}
		return true;
	}

	/** The connection window in front, or the one the designer in front belongs to. */
	public ConnectionWindow getConnectionWindow() {
		return switch (windowTabs.selectedFrame()) {
			case ConnectionWindow connectionWindow -> connectionWindow;
			case DesignerWindow designer -> designer.getConnectionWindow();
			case null, default -> null;
		};
	}

	public int getConnectionWindowCount() {
		return (int) windowTabs.getFrames().stream().filter(ConnectionWindow.class::isInstance).count();
	}

	public void removeConnectionWindow(ConnectionWindow connectionWindow) {
		windowTabs.removeWindow(connectionWindow);

		if (getConnectionWindowCount() == 0) {
			disconnectButton.setEnabled(false);
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
		} else if (object == settingsItem || object == preferencesButton) {
			mainController.showSettingsDialog();
		} else if (object == disconnectItem || object == disconnectButton) {
			ConnectionWindow connectionWindow = getConnectionWindow();
			if (connectionWindow != null) {
				connectionWindow.closeWindow(true);
			}
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

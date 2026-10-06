package nl.errorsoft.esql.app.control;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.OutputPanelAppender;

import nl.errorsoft.esql.connection.control.ConnectionProfileController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.connection.control.DatabaseDriverController;
import nl.errorsoft.esql.exporter.control.ExportController;
import nl.errorsoft.esql.importer.control.ImportController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JInternalFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.app.BuildInfo;
import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.ui.util.EscapeToClose;
import nl.errorsoft.esql.connection.ui.WorkFrame;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.settings.ui.dialog.SettingsDialog;
import nl.errorsoft.esql.app.ui.SplashWindow;

/**
 *		Controls all users-systems actions for the MainWindow.
 */
public class MainController {
	private static final Logger log = LogManager.getLogger(MainController.class);

	private BuildInfo buildInfo;
	private MainWindow mainWindow;
	private final List<ConnectionWindowController> connections = new ArrayList<>(); // Open connections, in the order they were opened

	public MainController() {
		// Both must happen before the first window or icon exists: macOS reads its desktop properties only once.
		Appearance.prepareDesktop();
		EscapeToClose.install();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> ApplicationContext.get().errors().report("Unexpected error", error));

		// Start domein class.
		buildInfo = new BuildInfo();

		// Show eSQLManager Window.
		mainWindow = new MainWindow(this);
		OutputPanelAppender.install(mainWindow);
		log.info("{} starting on Java {} ({}), {} {}", getTitle(), System.getProperty("java.version"), System.getProperty("java.vendor"),
			System.getProperty("os.name"), System.getProperty("os.arch"));
		log.info("Working directory: {}", System.getProperty("user.dir"));
		// Show splash.
		showSplashScreen(3000);
		refreshProfiles();
		mainWindow.showConnectionState();
	}

	public void splashReady() {
		// Show connection profile window.
		ConnectionProfileController connectionProfileController = new ConnectionProfileController(this);
		connectionProfileController.showDialog(mainWindow, true);
	}

	public void closeWindow() {
		System.exit(0);
	}

	/**
	 *		Use-case: 	show eSQLManager splash screen
	 *		Requires: 	eSQLManager UI use-case
	 */
	public void showSplashScreen(int time) {
		// Show a new Splash screen UI.
		new SplashWindow(this, mainWindow, time);
	}

	/** A connection was opened: it is listed in the explorer instead of its saved profile. */
	public void addConnection(ConnectionWindowController connection) {
		connections.add(connection);
		mainWindow.getOutput().addConnection(connection.getTitle(), connection.getConnectionProfile().getServerType().iconName());
		refreshProfiles();
		showConnectionState();
	}

	public void removeConnection(ConnectionWindowController connection) {
		connections.remove(connection);
		mainWindow.getOutput().removeConnection(connection.getTitle());
		refreshProfiles();
		showConnectionState();
	}

	/** The open connections, in the order they were opened. */
	public List<ConnectionWindowController> getConnections() {
		return List.copyOf(connections);
	}

	public int connectionCount() {
		return connections.size();
	}

	/** The connection of the window in front (a work window or a designer), else of the node selected in the explorer; null when there is none. */
	public ConnectionWindowController activeConnection() {
		JInternalFrame frame = mainWindow.getSelectedFrame();
		if (frame instanceof WorkFrame work) {
			return work.getConnection();
		}
		if (frame instanceof DesignerWindow designer && designer.getConnection() != null) {
			return designer.getConnection();
		}
		ConnectionWindowController selected = mainWindow.getExplorer().selectedConnection();
		if (selected != null) {
			return selected;
		}
		return connections.isEmpty() ? null : connections.getLast();
	}

	/** Lists the saved profiles that are not connected in the explorer, in grey. */
	public void refreshProfiles() {
		ConnectionProfile[] saved;
		try {
			saved = new ConnectionProfile().getProfiles();
		} catch (Exception e) {
			// The profile dialog reports a profiles.xml that cannot be read; the explorer then shows the connections only.
			log.warn("The saved profiles cannot be listed: {}", e.getMessage());
			saved = new ConnectionProfile[0];
		}
		List<String> connected = connections.stream().map(c -> c.getConnectionProfile().getName()).toList();
		mainWindow.getExplorer()
			.showProfiles(Arrays.stream(saved).filter(profile -> connected.stream().noneMatch(name -> name.equalsIgnoreCase(profile.getName()))).toList());
	}

	public void openConnectionWindow(ConnectionProfile profile) {
		new ConnectionWindowController(this, profile);
	}

	/** Connects a saved profile, asking for its password or driver first when needed. */
	public void connect(ConnectionProfile profile) {
		new ConnectionProfileController(this).connect(profile);
	}

	/** Disconnects the active connection, after asking. */
	public void disconnect() {
		ConnectionWindowController connection = activeConnection();
		if (connection != null) {
			connection.disconnect();
		}
	}

	public void showDriverDialog() {
		DatabaseDriverController driverController = new DatabaseDriverController(this);
		driverController.showDialog(mainWindow);
	}

	/** The profile dialog with a profile selected, to edit it. */
	public void showConnectionProfileDialog(String profileName) {
		new ConnectionProfileController(this).showDialog(mainWindow, profileName);
	}

	public void showConnectionProfileDialog() {
		ConnectionProfileController connectionProfileController = new ConnectionProfileController(this);
		connectionProfileController.showDialog(mainWindow, false);
	}

	public void showSettingsDialog() {
		new SettingsDialog(mainWindow);
	}

	public void showImportDialog() {
		ConnectionWindowController connection = activeConnection();
		if (connection != null) {
			new ImportController(this).startImport(connection);
		}
	}

	public void showExportDialog() {
		ConnectionWindowController connection = activeConnection();
		if (connection != null) {
			new ExportController(this).startExport(connection);
		}
	}

	public void openDesigner() {
		ConnectionWindowController connection = activeConnection();
		if (connection != null && connection.requireFeature(Dialect.Feature.DESIGNER, "The designer")) {
			new DesignerWindow(mainWindow, connection);
		}
	}

	public void showConnectionState() {
		mainWindow.showConnectionState();
	}

	public void updateStatus(String message, boolean red) {
		mainWindow.updateStatus(message, red);
	}

	public void setStatusInfo(String info) {
		mainWindow.setStatusInfo(info);
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public String getTitle() {
		return getAppName() + " " + getAppVersion() + " (" + getAppCommit() + ")";
	}

	public String getAppName() {
		return buildInfo.getAppName();
	}

	public String getAppVersion() {
		return buildInfo.getAppVersion();
	}

	public String getAppCommit() {
		return buildInfo.getAppCommit();
	}

}

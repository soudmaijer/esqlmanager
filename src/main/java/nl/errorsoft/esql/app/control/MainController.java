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
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.app.BuildInfo;
import nl.errorsoft.esql.app.ExitPlan;
import nl.errorsoft.esql.app.StatusContext;
import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import java.awt.Desktop;
import nl.errorsoft.esql.connection.ProfileNode;
import nl.errorsoft.esql.connection.TreeSelection;
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
	private final StatusContext statusContext = new StatusContext();
	private boolean quitting; // Set while the exit questions are open
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
		installQuitHandler();
		refreshProfiles();
		showConnectionState();
		// After the main window has been shown and laid out, so the splash comes up over it, not behind or before it.
		SwingUtilities.invokeLater(() -> showSplashScreen(3000));
	}

	public void splashReady() {
		// Show connection profile window.
		ConnectionProfileController connectionProfileController = new ConnectionProfileController(this);
		connectionProfileController.showDialog(mainWindow, true);
	}

	/**
	 * Quits the application. Nothing open: at once. Otherwise one question names what closes; then every connection with unsaved editors and every designer
	 * asks to save, and a Cancel anywhere keeps the application open with everything still open.
	 */
	public void quit() {
		if (quitting) {
			return;
		}
		quitting = true;
		try {
			List<DesignerWindow> designers = mainWindow.designers();
			ExitPlan plan = new ExitPlan(connections.size(), designers.size(), Appearance.isMac());

			if (plan.needsConfirmation() && !Dialogs.confirm(mainWindow, plan.title(getAppName()), plan.message(), plan.verb())) {
				return;
			}
			for (ConnectionWindowController connection : List.copyOf(connections)) {
				if (!connection.confirmCloseEditors()) {
					return;
				}
			}
			for (DesignerWindow designer : designers) {
				if (!designer.offerToSaveBeforeQuit()) {
					return;
				}
			}
			System.exit(0);
		} finally {
			quitting = false;
		}
	}

	/** macOS: Cmd+Q and Quit in the application menu go through the same question. */
	private void installQuitHandler() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
			Desktop.getDesktop().setQuitHandler((event, response) -> {
				// The application ends itself when the user confirms; until then the system must not.
				response.cancelQuit();
				SwingUtilities.invokeLater(this::quit);
			});
		}
	}

	/**
	 *		Use-case: 	show eSQLManager splash screen
	 *		Requires: 	eSQLManager UI use-case
	 */
	public void showSplashScreen(int time) {
		// Show a new Splash screen UI.
		new SplashWindow(mainWindow, new SplashWindow.Info(getAppName(), getAppVersion(), getAppCommit()), time, this::splashReady);
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

	/** The connection of the window or explorer node the user touched last; the last opened one when that has none, null without connections. */
	public ConnectionWindowController activeConnection() {
		ConnectionWindowController touched = statusContext.pick(source -> {
			Current current = current(source);
			return current == null ? null : current.connection();
		});
		if (touched != null) {
			return touched;
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

	/** The user touched a window or the explorer: the status bar describes that from now on. */
	public void contextTouched(StatusContext.Source source) {
		statusContext.touched(source);
		showConnectionState();
	}

	/** What the status bar describes: a connection (or a saved profile) and, for a window or node of a database, where. */
	private record Current(ConnectionWindowController connection, String profileName, String where) {
	}

	private Current current(StatusContext.Source source) {
		if (source == StatusContext.Source.WORK) {
			return switch (mainWindow.getSelectedFrame()) {
				case WorkFrame work -> new Current(work.getConnection(), null, work.where());
				case DesignerWindow designer when designer.getConnection() != null -> new Current(designer.getConnection(), null, "");
				case null, default -> null;
			};
		}
		var explorer = mainWindow.getExplorer();
		Object selected = explorer.selectedObject();

		if (selected instanceof ProfileNode profile) {
			return new Current(null, profile.profile().getName(), "");
		}
		ConnectionWindowController connection = explorer.selectedConnection();
		if (connection == null) {
			return null;
		}
		var schema = TreeSelection.schema(selected);
		var database = TreeSelection.database(selected);
		return new Current(connection, null, StatusContext.where(database == null ? null : database.getName(), schema == null ? null : schema.getName()));
	}

	/**
	 * Shows the resting state of the status bar for the window or explorer node the user touched last: the state of its connection on the left, the server,
	 * account and database on the right. Without any of them "No connection".
	 */
	public void showConnectionState() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::showConnectionState);
			return;
		}
		Current current = statusContext.pick(this::current);

		if (current == null) {
			mainWindow.showConnectionState("No connection", true, "");
		} else if (current.connection() == null) {
			mainWindow.showConnectionState("Not connected", true, "");
		} else if (current.connection().isConnected()) {
			mainWindow.showConnectionState("Connected", false, current.connection().statusInfo(current.where()));
		} else {
			mainWindow.showConnectionState("Connecting...", true, "");
		}
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

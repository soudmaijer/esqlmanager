package nl.errorsoft.esql.app.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionProfileController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.connection.control.DatabaseDriverController;
import nl.errorsoft.esql.export.control.ExportController;
import nl.errorsoft.esql.importer.control.ImportController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.app.ESQLManager;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.ui.util.EscapeToClose;
import nl.errorsoft.esql.settings.Settings;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.settings.ui.dialog.SettingsDialog;
import nl.errorsoft.esql.app.ui.SplashWindow;

/**
 *		Controls all users-systems actions for the MainWindow.
 */
public class MainController {
	private static final Logger log = LogManager.getLogger(MainController.class);

	private ESQLManager application;
	private MainWindow mainWindow;

	public MainController() {
		// Both must happen before the first window or icon exists: macOS reads its desktop properties only once.
		Appearance.prepareDesktop();
		EscapeToClose.install();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> ApplicationContext.get().errors().report("Unexpected error", error));

		// Start domein class.
		application = new ESQLManager();

		// Show ESQLManager Window.
		mainWindow = new MainWindow(this);
		OutputPanelAppender.install(mainWindow);
		log.info("{} starting on Java {} ({}), {} {}", getTitle(), System.getProperty("java.version"), System.getProperty("java.vendor"),
			System.getProperty("os.name"), System.getProperty("os.arch"));
		log.info("Working directory: {}", System.getProperty("user.dir"));
		// Show splash.
		showSplashScreen(3000);
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
	 *		Use-case: 	show ESQLManager splash screen
	 *		Requires: 	ESQLManager UI use-case
	 */
	public void showSplashScreen(int time) {
		// Show a new Splash screen UI.
		new SplashWindow(this, mainWindow, time);
	}

	public void showConnectionWindow(ConnectionWindow connectionWindow) {
		mainWindow.addConnectionWindow(connectionWindow);
	}

	public void removeConnectionWindow(ConnectionWindow connectionWindow) {
		mainWindow.removeConnectionWindow(connectionWindow);
	}

	public void openConnectionWindow(ConnectionProfile cp) {
		// Connect and start window.
		new ConnectionWindowController(this, cp);
	}

	public void showDriverDialog() {
		DatabaseDriverController driverController = new DatabaseDriverController(this);
		driverController.showDialog(mainWindow);
	}

	public void showConnectionProfileDialog() {
		ConnectionProfileController connectionProfileController = new ConnectionProfileController(this);
		connectionProfileController.showDialog(mainWindow, false);
	}

	public void showSettingsDialog() {
		SettingsDialog settingsDialog = new SettingsDialog(this, mainWindow);
	}

	public void showImportDialog() {
		if (mainWindow.getConnectionWindowCount() > 0) {
			ImportController importController = new ImportController(this);
			importController.startImport(mainWindow.getConnectionWindow().getController());
		}
	}

	public void showExportDialog() {
		if (mainWindow.getConnectionWindowCount() > 0) {
			ExportController exportController = new ExportController(this);
			exportController.startExport(mainWindow.getConnectionWindow().getController());
		}
	}

	public void openDesigner() {
		try {
			if (!mainWindow.getConnectionWindow().getController().getDatabaseConnection().getConnectionProfile().getServerType().getDialect()
				.supports(Dialect.Feature.DESIGNER)) {
				Dialogs.info(mainWindow, "Designer", "The designer is only available for MySQL.");
				return;
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Designer", e);
		}

		if (mainWindow.getConnectionWindowCount() > 0) {
			new DesignerWindow(mainWindow, mainWindow.getConnectionWindow());
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
		return application.getAppName();
	}

	public String getAppVersion() {
		return application.getAppVersion();
	}

	public String getAppCommit() {
		return application.getAppCommit();
	}

}

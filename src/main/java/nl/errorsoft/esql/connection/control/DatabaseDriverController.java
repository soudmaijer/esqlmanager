package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.DatabaseDriver;
import nl.errorsoft.esql.connection.ui.dialog.DriverDialog;

public class DatabaseDriverController {
	private MainController connectionWindowController;
	private DatabaseDriver[] drivers;
	private DriverDialog driverDialog;

	public DatabaseDriverController(MainController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	public void showDialog(MainWindow mainWindow) {
		driverDialog = new DriverDialog(this, mainWindow);
		driverDialog.loadDrivers(getDatabaseDrivers());
		driverDialog.setVisible(true);
	}

	public DatabaseDriver[] getDatabaseDrivers() {
		drivers = new DatabaseDriver().getDatabaseDrivers();
		return drivers;
	}

	public void saveProperties(int id, String name, String url, String className, String fieldOpen, String fieldClose, String dataOpen, String dataClose) {
		try {
			new DatabaseDriver().saveProperties(drivers, id, name, url, className, "", fieldOpen, fieldClose, dataOpen, dataClose);
			Dialogs.info(driverDialog, driverDialog.getTitle(), "Driver properties saved.");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(driverDialog, "Save properties", e);
		}
	}
}

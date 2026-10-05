package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.DatabaseDriver;
import nl.errorsoft.esql.connection.DriverProperties;
import nl.errorsoft.esql.connection.ui.dialog.DriverDialog;
import nl.errorsoft.esql.driver.DriverStatus;
import nl.errorsoft.esql.driver.control.DriverDownloadController;

public class DatabaseDriverController {
	private MainController mainController;
	private DatabaseDriver[] drivers;
	private DriverDialog driverDialog;

	public DatabaseDriverController(MainController mainController) {
		this.mainController = mainController;
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

	/** Where the saved driver comes from. */
	public DriverStatus status(DatabaseDriver driver) {
		return ApplicationContext.get().drivers().status(driver.driverSource());
	}

	/** Downloads the driver with a progress dialog; {@code done} runs on the event thread afterwards, also when it failed (which is reported). */
	public void download(DatabaseDriver driver, Runnable done) {
		new DriverDownloadController(driverDialog).download(driver.driverSource().artifact(), done, done);
	}

	/** Saves the properties of the driver; false when that failed, which is reported. */
	public boolean saveProperties(DatabaseDriver driver, DriverProperties properties) {
		try {
			new DatabaseDriver().saveProperties(drivers, driver.getId(), properties);
			return true;
		} catch (Exception e) {
			ApplicationContext.get().errors().report(driverDialog, "Save driver properties", e);
			return false;
		}
	}
}

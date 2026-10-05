package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.DatabaseDriver;
import nl.errorsoft.esql.connection.ui.dialog.DriverDialog;

public class DatabaseDriverCC {
	private ESQLManagerCC cwcc;
	private DatabaseDriver[] drivers;
	private DriverDialog du;

	public DatabaseDriverCC(ESQLManagerCC cwcc) {
		this.cwcc = cwcc;
	}

	public void startUI(MainWindow emui) {
		du = new DriverDialog(this, emui);
		du.loadDrivers(getDatabaseDrivers());
		du.setVisible(true);
	}

	public DatabaseDriver[] getDatabaseDrivers() {
		drivers = new DatabaseDriver().getDatabaseDrivers();
		return drivers;
	}

	public void saveProperties(int id, String name, String url, String className, String fieldOpen, String fieldClose, String dataOpen, String dataClose) {
		try {
			new DatabaseDriver().saveProperties(drivers, id, name, url, className, "", fieldOpen, fieldClose, dataOpen, dataClose);
			Dialogs.info(du, du.getTitle(), "Driver properties saved.");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(du, "Save properties", e);
		}
	}
}

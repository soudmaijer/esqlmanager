package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import nl.errorsoft.esql.data.*;

public class ConnectionWindow {
	private ConnectionWindowCC cwcc;
	private ConnectionProfile cp;
	private DatabaseConnection db;

	public ConnectionWindow(ConnectionWindowCC cwcc, ConnectionProfile cp) {
		this.cwcc = cwcc;
		this.cp = cp;
	}

	public void start() throws Exception {
		db = new DatabaseConnection();

		db.connect(cp, "");
	}

	public void stop() throws Exception {
		db.close();
	}

	public DatabaseConnection getDatabaseConnection() {
		return this.db;
	}

	public ConnectionProfile getConnectionProfile() {
		return cp;
	}
}

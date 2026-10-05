package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;

public class ConnectionSession {
	private ConnectionWindowController connectionWindowController;
	private ConnectionProfile cp;
	private DatabaseConnection db;

	public ConnectionSession(ConnectionWindowController connectionWindowController, ConnectionProfile cp) {
		this.connectionWindowController = connectionWindowController;
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

package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;

public class ConnectionSession {
	private ConnectionWindowController connectionWindowController;
	private ConnectionProfile profile;
	private DatabaseConnection db;

	public ConnectionSession(ConnectionWindowController connectionWindowController, ConnectionProfile profile) {
		this.connectionWindowController = connectionWindowController;
		this.profile = profile;
	}

	public void start() throws Exception {
		db = new DatabaseConnection();

		db.connect(profile, "");
	}

	public void stop() throws Exception {
		db.close();
	}

	public DatabaseConnection getDatabaseConnection() {
		return this.db;
	}

	public ConnectionProfile getConnectionProfile() {
		return profile;
	}
}

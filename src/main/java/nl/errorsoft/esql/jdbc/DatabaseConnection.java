package nl.errorsoft.esql.jdbc;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ConnectionProfile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection implements AutoCloseable {
	private String serverDescription = "";
	private static final Logger log = LogManager.getLogger(DatabaseConnection.class);

	private String driver = "";
	private String url = "";
	private String database = "";
	private String schema;

	private ConnectionProfile profile;
	private Connection connection;

	// Connection types.
	private int connectionType = -1;

	public DatabaseConnection() {
	}

	// Set the database url.
	public void setUrl(String url) {
		this.url = url;
	}

	// Set the database driver.
	public void setDriver(String driver) {
		this.driver = driver;
	}

	public void setConnectionProfile(ConnectionProfile profile) {
		this.profile = profile;
	}

	public ConnectionProfile getConnectionProfile() {
		return profile;
	}

	// Connect to given database, an empty name connects to the profile's default.
	public void connect(ConnectionProfile profile, String database) throws SQLException {
		this.profile = profile;
		this.database = profile.getServerType().getDialect().getConnectionDatabase(profile, database);
		this.schema = null;
		this.url = profile.getServerType().getConnectionURL(profile, this.database);

		if (connection != null) {
			this.close();
		}

		log.info("Connecting to {} as {}", url, profile.getUsername());
		Properties credentials = new Properties();
		if (profile.getUsername() != null) {
			credentials.setProperty("user", profile.getUsername());
		}
		if (profile.getPassword() != null) {
			credentials.setProperty("password", profile.getPassword());
		}
		// The driver of this server type, not whichever driver DriverManager finds first for the URL.
		connection = ApplicationContext.get().drivers().connect(profile.getServerType().driverSource(), url, credentials);

		DatabaseMetaData meta = connection.getMetaData();
		serverDescription = meta.getDatabaseProductName() + " " + meta.getDatabaseMajorVersion() + "." + meta.getDatabaseMinorVersion();
		log.info("Connected to {} {} using driver {} {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getDriverName(),
			meta.getDriverVersion());
	}

	// The name and version of the server, for display.
	public String getServerDescription() {
		return serverDescription;
	}

	// The database the connection was made to.
	public String getDatabase() {
		return database;
	}

	// Makes the given database the active one, the way the server type needs it.
	public void useDatabase(String database) throws SQLException {
		switch (profile.getServerType().getDialect().databaseSwitch()) {
			case CATALOG -> connection.setCatalog(database);
			case RECONNECT -> reconnect(database);
			case NONE -> {
				// The database is part of the connection.
			}
		}
	}

	private void reconnect(String database) throws SQLException {
		if (database.equals(this.database)) {
			return;
		}

		connect(profile, database);
	}

	// The schema tables are looked up in, null when the server type has none.
	public String getSchema() throws SQLException {
		String sql = profile.getServerType().getDialect().currentSchemaSql();

		if (schema == null && sql != null) {
			try (ResultSet rs = executeQuery(sql)) {
				schema = rs.next() ? rs.getString(1) : null;
			}
		}

		return schema;
	}

	// Makes unqualified names resolve to the schema, on servers that have schemas; does nothing when it is the current one already.
	public void useSchema(String name) throws SQLException {
		String sql = profile.getServerType().getDialect().useSchemaSql(name);

		if (sql != null && name != null && !name.equals(getSchema())) {
			executeUpdate(sql);
			schema = name;
		}
	}

	// Execute a query.
	public ResultSet executeQuery(String query) throws SQLException {
		log.debug("Query: {}", query);
		Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		// The statement lives as long as its result: closing the result set closes it. Without a result it is closed here.
		try {
			statement.closeOnCompletion();
			return statement.executeQuery(query);
		} catch (SQLException e) {
			statement.close();
			throw e;
		}
	}

	public int executeUpdate(String query) throws SQLException {
		log.debug("Update: {}", query);
		try (Statement statement = connection.createStatement()) {
			int i = statement.executeUpdate(query);
			log.debug("Update: {} row(s) updated", i);
			return i;
		}
	}

	// Runs a statement whose result, if any, is not needed.
	public void execute(String query) throws SQLException {
		log.debug("Execute: {}", query);
		try (Statement statement = connection.createStatement()) {
			statement.execute(query);
		}
	}

	// Returns the active database connection.
	public Connection getConnection() {
		return connection;
	}

	// Closes the connection.
	@Override
	public void close() {
		if (connection == null) {
			return;
		}
		try {
			connection.close();
			log.info("Connection to {} closed", url);
		} catch (Exception e) {
			// The connection is given up either way and nothing can be done about a failed close, it is only worth a note in the log.
			log.debug("Closing the connection to {} failed: {}", url, e.getMessage());
		}
	}

	/** A value as a literal that can safely be put in a statement, escaped the way this server expects. */
	public String formatFieldValue(String in) {
		if (in == null) {
			return "NULL";
		}

		return this.getConnectionProfile().getServerType().getDialect().literal(in);
	}
}

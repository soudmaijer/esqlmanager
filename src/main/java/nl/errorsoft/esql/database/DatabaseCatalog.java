package nl.errorsoft.esql.database;

import java.sql.SQLException;
import java.util.List;

import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.DatabaseSelection;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.jdbc.DatabaseConnection;

/**
 * Lists every database of a server and the schemas of each, whatever the profile selects, to let the user choose what the profile shows. It owns one
 * scratch connection from the typed settings and moves it to a database to list its schemas (PostgreSQL needs a connection per database). Not thread
 * safe: use it from one thread at a time, and close it when done.
 */
public class DatabaseCatalog implements AutoCloseable {
	private final ConnectionProfile profile;
	private final DatabaseConnection connection = new DatabaseConnection();
	private DatabaseRepository repository;

	public DatabaseCatalog(ConnectionProfile profile) {
		this.profile = profile;
	}

	/**
	 * Connects to the server. The first selected database is tried first; when it cannot be connected to (it was dropped), the default database of the server
	 * type is used.
	 */
	public void connect() throws Exception {
		try {
			connection.connect(profile, "");
		} catch (Exception e) {
			if (profile.getSelection().isEmpty()) {
				throw e;
			}
			ConnectionProfile unselected = profile.copyAs(profile.getName());
			unselected.setSelection(DatabaseSelection.NONE);
			connection.connect(unselected, "");
		}
		repository = new DatabaseRepository(connection);
	}

	public String serverDescription() {
		return connection.getServerDescription();
	}

	public Dialect dialect() {
		return profile.getServerType().getDialect();
	}

	/** Every database of the server. */
	public List<String> databases() throws SQLException {
		return repository.listNames();
	}

	/** Every schema of the database, empty on servers without schemas. */
	public List<String> schemas(String database) throws SQLException {
		return repository.listSchemaNames(new Database(database));
	}

	@Override
	public void close() {
		connection.close();
	}
}

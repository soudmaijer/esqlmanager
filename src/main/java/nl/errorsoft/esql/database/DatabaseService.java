package nl.errorsoft.esql.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Application logic for databases: which ones are shown, creating and dropping them. */
public class DatabaseService {
	private static final Logger log = LogManager.getLogger(DatabaseService.class);

	private final DatabaseRepository repository;
	private final String[] profileFilter;

	public DatabaseService(DatabaseRepository repository, String profileDatabases) {
		this.repository = repository;
		this.profileFilter = splitFilter(profileDatabases);
	}

	/** The databases on the server, limited to the ones named in the profile when it names any. */
	public List<Database> getDatabases() throws Exception {
		List<Database> all = new ArrayList<Database>();

		for (String name : repository.listNames()) {
			all.add(new Database(name));
		}

		log.info("Found {} database(s) on the server", all.size());

		if (profileFilter.length == 0) {
			return all;
		}

		List<Database> shown = new ArrayList<Database>();

		for (Database database : all) {
			for (String wanted : profileFilter) {
				if (database.getName().equalsIgnoreCase(wanted)) {
					shown.add(database);
					break;
				}
			}
		}

		log.info("Showing {} database(s) matching the profile filter: {}", shown.size(), String.join(", ", profileFilter));
		return shown;
	}

	public List<Table> getTables(Database database) throws Exception {
		List<Table> tables = repository.listTables(database);
		log.info("Database {}: {} table(s)", database.getName(), tables.size());
		return tables;
	}

	public boolean exists(Database database) throws Exception {
		return repository.exists(database.getName());
	}

	/** Makes the database the active one of the connection. */
	public void use(Database database) throws Exception {
		repository.use(database.getName());
	}

	public Database createDatabase(String name) throws Exception {
		repository.create(name);
		return new Database(name);
	}

	public void dropDatabase(Database database) throws Exception {
		repository.drop(database);
	}

	private static String[] splitFilter(String databases) {
		List<String> names = new ArrayList<String>();

		for (String name : databases.split(",")) {
			if (!name.trim().isEmpty()) {
				names.add(name.trim());
			}
		}

		return names.toArray(new String[0]);
	}
}

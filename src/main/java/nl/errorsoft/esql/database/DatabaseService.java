package nl.errorsoft.esql.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.connection.DatabaseSelection;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.Table;

/** Application logic for databases and their schemas: which ones are shown, creating and dropping them. */
public class DatabaseService {
	private static final Logger log = LogManager.getLogger(DatabaseService.class);

	private final DatabaseRepository repository;
	private final DatabaseSelection selection;

	public DatabaseService(DatabaseRepository repository, DatabaseSelection selection) {
		this.repository = repository;
		this.selection = selection;
	}

	/** The databases on the server, limited to the ones the profile selects when it selects any. */
	public List<Database> getDatabases() throws Exception {
		List<Database> shown = new ArrayList<>();

		for (String name : repository.listNames()) {
			if (selection.showsDatabase(name)) {
				shown.add(new Database(name));
			}
		}

		log.info("Showing {} database(s){}", shown.size(), selection.isEmpty() ? "" : " of the profile selection: " + selection.toCsv());
		return shown;
	}

	public List<Table> getTables(Database database) throws Exception {
		List<Table> tables = repository.listTables(database);
		log.info("Database {}: {} table(s)", database.getName(), tables.size());
		return tables;
	}

	/** The schemas of the database that the profile shows, empty on servers without schemas. */
	public List<Schema> getSchemas(Database database) throws Exception {
		List<Schema> schemas = new ArrayList<>();

		for (String name : repository.listSchemaNames(database)) {
			if (selection.showsSchema(database.getName(), name)) {
				schemas.add(new Schema(database, name));
			}
		}
		log.info("Database {}: {} schema(s)", database.getName(), schemas.size());
		return schemas;
	}

	public List<Table> getTables(Schema schema) throws Exception {
		List<Table> tables = repository.listTables(schema);
		log.info("Schema {}.{}: {} table(s)", schema.getDatabase().getName(), schema.getName(), tables.size());
		return tables;
	}

	/** The schema unqualified table names resolve to (the first of the search path), null on servers without schemas. */
	public String currentSchema(Database database) throws Exception {
		return repository.currentSchema(database);
	}

	public Schema createSchema(Database database, String name) throws Exception {
		repository.createSchema(database, name);
		return new Schema(database, name);
	}

	public void dropSchema(Schema schema) throws Exception {
		repository.dropSchema(schema);
	}

	public boolean exists(Database database) throws Exception {
		return repository.exists(database.getName());
	}

	/** Makes the database the active one of the connection. */
	public void use(Database database) throws Exception {
		repository.use(database.getName());
	}

	public Database createDatabase(String name) throws Exception {
		return createDatabase(name, Map.of());
	}

	/**
	 * Creates a database with the chosen options (see {@link DatabaseRepository#createDatabaseChoices}).
	 * @throws EsqlException when the name is empty or a database of that name exists
	 */
	public Database createDatabase(String name, Map<String, String> options) throws Exception {
		if (name == null || name.isBlank()) {
			throw new EsqlException("Enter a name for the " + repository.databaseTerm() + ".");
		}
		if (exists(new Database(name))) {
			throw new EsqlException("A " + repository.databaseTerm() + " named '" + name + "' exists already.");
		}

		repository.create(name, options);
		log.info("Created {} {}", repository.databaseTerm(), name);
		return new Database(name);
	}

	/** The values to choose from when creating a database, by option key. */
	public Map<String, List<String>> createDatabaseChoices() throws Exception {
		return repository.createDatabaseChoices();
	}

	/** The properties of the database; the table count covers the schemas the profile shows. */
	public DatabaseProperties properties(Database database) throws Exception {
		int tables = 0;
		List<Schema> schemas = getSchemas(database);

		if (schemas.isEmpty()) {
			tables = repository.listTables(database).size();
		}
		for (Schema schema : schemas) {
			tables += repository.listTables(schema).size();
		}
		return new DatabaseProperties(database.getName(), repository.describe(database), tables);
	}

	/** Renames the schema, the returned schema has the new name. */
	public Schema renameSchema(Schema schema, String newName) throws Exception {
		repository.renameSchema(schema, newName);
		return new Schema(schema.getDatabase(), newName);
	}

	public void dropDatabase(Database database) throws Exception {
		repository.drop(database);
	}
}

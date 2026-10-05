package nl.errorsoft.esql.query;

/** Application logic for the statements typed in the query window. */
public class QueryService {
	private final QueryRepository repository;

	public QueryService(QueryRepository repository) {
		this.repository = repository;
	}

	/**
	 * Runs one statement. A switch of database written the way of the server ({@code USE shop} on MySQL, {@code \connect shop} elsewhere) goes through
	 * the connection, so the application knows the database in use; every other statement goes to the server as it is.
	 */
	public ExecutionResult execute(String sql) throws Exception {
		String database = repository.databaseSwitchTarget(sql);

		if (database != null) {
			repository.switchDatabase(database);
			return new ExecutionResult.DatabaseChanged(database);
		}
		return repository.execute(sql);
	}

	/** Makes unqualified names of the statements resolve to the schema of the database, on servers with schemas. */
	public void useSchema(String database, String schema) throws Exception {
		repository.switchDatabase(database);
		repository.switchSchema(schema);
	}

	/** Runs a statement that changes data and returns the number of affected rows. */
	public int update(String sql) throws Exception {
		return repository.update(sql);
	}
}

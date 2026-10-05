package nl.errorsoft.esql.query;

import java.util.StringTokenizer;

import nl.errorsoft.esql.jdbc.DatabaseConnection;

/** Application logic for the statements typed in the query window. */
public class QueryService {
	private final QueryRepository repository;

	public QueryService(QueryRepository repository) {
		this.repository = repository;
	}

	/** True for a statement that returns rows to show. */
	public boolean returnsRows(String sql) {
		String lower = sql.toLowerCase();
		return lower.startsWith("select") || lower.startsWith("show");
	}

	/** True for a USE statement, which changes the active database instead of running as SQL. */
	public boolean isUse(String sql) {
		return sql.toLowerCase().startsWith("use");
	}

	public void use(String sql) throws Exception {
		StringTokenizer names = new StringTokenizer(sql.substring(3), "; `", false);

		if (names.hasMoreTokens()) {
			repository.switchDatabase(names.nextToken());
		}
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

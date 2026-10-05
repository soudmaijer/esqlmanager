package nl.errorsoft.esql.query;

import nl.errorsoft.esql.table.QueryResult;

/** What one statement typed in the query tab did: it returned rows, changed rows, or switched the database. */
public sealed interface ExecutionResult {
	/** The statement returned rows (a SELECT, WITH, EXPLAIN, SHOW, VALUES, ...). */
	record Rows(QueryResult result) implements ExecutionResult {
	}

	/** The statement changed something; the number of affected rows, or -1 when the server does not tell. */
	record Updated(int count) implements ExecutionResult {
	}

	/** The statement made another database the one in use. */
	record DatabaseChanged(String database) implements ExecutionResult {
	}
}

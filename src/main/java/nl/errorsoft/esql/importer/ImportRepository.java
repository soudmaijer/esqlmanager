package nl.errorsoft.esql.importer;

import java.sql.SQLException;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;

/** Runs the statements of an import script. */
public class ImportRepository extends AbstractRepository {
	public ImportRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	public void switchDatabase(String name) throws SQLException {
		useDatabase(name);
	}

	/** A statement that returns a result, such as moving a sequence, cannot go through executeUpdate. */
	public void run(String statement) throws SQLException {
		if (statement.trim().toUpperCase().startsWith("SELECT")) {
			dbc.execute(statement);
		} else {
			executeUpdate(statement);
		}
	}
}

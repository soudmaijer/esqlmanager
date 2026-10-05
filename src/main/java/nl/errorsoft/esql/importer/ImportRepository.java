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

	/** The schema unqualified names resolve to, null on servers without schemas. */
	public String currentSchema() throws SQLException {
		return dbc.getSchema();
	}

	/** Makes unqualified names resolve to the schema; nothing on servers without schemas. */
	public void switchSchema(String name) throws SQLException {
		useSchema(name);
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

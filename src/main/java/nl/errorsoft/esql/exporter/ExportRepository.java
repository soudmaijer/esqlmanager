package nl.errorsoft.esql.exporter;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableName;
import nl.errorsoft.esql.table.TableColumn;

/** Reads what an export script is made of: table names, table definitions and rows as statements. */
public class ExportRepository extends AbstractRepository {
	public ExportRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	/** The tables of a database that have structure and data of their own, so no views; on servers with schemas those of every schema. */
	public List<TableName> tableNames(Database database) throws SQLException {
		return names(database, false);
	}

	/** The tables of one schema, no views. */
	public List<TableName> tableNames(Schema schema) throws SQLException {
		return names(listTables(schema), false);
	}

	/** The views of a database, on servers with schemas those of every schema. */
	public List<TableName> viewNames(Database database) throws SQLException {
		return names(database, true);
	}

	/** The views of one schema. */
	public List<TableName> viewNames(Schema schema) throws SQLException {
		return names(listTables(schema), true);
	}

	private List<TableName> names(Database database, boolean views) throws SQLException {
		String schemas = dialect().listSchemasSql();

		if (schemas == null) {
			return names(listTables(database), views);
		}

		useDatabase(database.getName());
		List<TableName> names = new ArrayList<>();

		for (String schema : queryStrings(schemas)) {
			names.addAll(names(listTables(new Schema(database, schema)), views));
		}
		return names;
	}

	private static List<TableName> names(List<Table> tables, boolean views) {
		List<TableName> names = new ArrayList<>();

		for (Table table : tables) {
			if ("VIEW".equalsIgnoreCase(table.getType()) == views) {
				names.add(table.qualifiedName());
			}
		}

		return names;
	}

	public String createDatabaseSql(String database) {
		return dialect().createDatabaseSql(database);
	}

	public String useDatabaseSql(String database) {
		return dialect().useDatabaseSql(database);
	}

	public String createSchemaSql(String schema) {
		return dialect().createSchemaIfMissingSql(schema);
	}

	public String dropTableSql(TableName table, boolean ifExists) {
		return dialect().dropTableSql(table, ifExists);
	}

	public String dropViewSql(TableName view, boolean ifExists) {
		return dialect().dropViewSql(view, ifExists);
	}

	/** The statement that creates the table, written to leave an existing table alone when asked. */
	public String structureSql(TableName table, boolean ifNotExists) throws SQLException {
		String create = structureSql(table);
		return ifNotExists ? dialect().createTableIfNotExists(create) : create;
	}

	/** The CREATE VIEW statement of a view, null when the server cannot give it. */
	public String viewSql(TableName view) throws SQLException {
		String show = dialect().showCreateViewSql(view);

		if (show == null) {
			return null;
		}
		try (ResultSet rs = dbc.executeQuery(show)) {
			rs.first();
			return rs.getString(2).replaceAll(";\\s*$", "");
		}
	}

	/** The statement that starts a transaction in a script, null when the server has none. */
	public String beginSql() {
		return dialect().beginTransactionSql();
	}

	public String commitSql() {
		return dialect().commitSql();
	}

	public String disableForeignKeyChecksSql() {
		return dialect().disableForeignKeyChecksSql();
	}

	public String enableForeignKeyChecksSql() {
		return dialect().enableForeignKeyChecksSql();
	}

	public String structureSql(TableName table) throws SQLException {
		String show = dialect().showCreateTableSql(table);

		if (show != null) {
			try (ResultSet rs = dbc.executeQuery(show)) {
				rs.first();
				return rs.getString(2);
			}
		}

		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		String schema = table.schema() == null ? dbc.getSchema() : table.schema();
		String catalog = dbc.getConnection().getCatalog();
		List<String> columns = new ArrayList<>();

		try (ResultSet rs = dmd.getColumns(catalog, schema, table.name(), "%")) {
			while (rs.next()) {
				columns.add(dialect().columnDdl(rs));
			}
		}

		Map<Integer, String> primary = new TreeMap<>();
		try (ResultSet keys = dmd.getPrimaryKeys(catalog, schema, table.name())) {
			while (keys.next()) {
				primary.put(keys.getInt("KEY_SEQ"), keys.getString("COLUMN_NAME"));
			}
		}

		return dialect().createTableDdl(table, columns, new ArrayList<>(primary.values()));
	}

	/**
	 * Passes the rows of the table to the sink as INSERT statements, binary columns are left empty.
	 * @param rowsPerInsert how many rows share one statement, 1 gives an INSERT per row.
	 */
	public void insertStatements(TableName table, int rowsPerInsert, Consumer<String> sink) throws SQLException {
		try (ResultSet rs = dbc.executeQuery("SELECT * FROM " + quote(table))) {
			ResultSetMetaData rsm = rs.getMetaData();
			List<String> batch = new ArrayList<>();

			while (rs.next()) {
				List<String> values = new ArrayList<>();

				for (int d = 1; d <= rsm.getColumnCount(); d++) {
					TableColumn column = new TableColumn(null);
					column.setType(rsm.getColumnType(d));

					if (column.isBinary()) {
						values.add("''");
					} else if (rs.getString(d) != null) {
						values.add(dialect().literal(rs.getString(d)));
					} else {
						values.add("NULL");
					}
				}

				batch.add("(" + String.join(",", values) + ")");

				if (batch.size() == rowsPerInsert) {
					sink.accept(insert(table, batch));
					batch.clear();
				}
			}

			if (!batch.isEmpty()) {
				sink.accept(insert(table, batch));
			}
		}
	}

	private String insert(TableName table, List<String> tuples) {
		return "INSERT INTO " + quote(table) + " VALUES" + String.join(",\n", tuples) + ";";
	}

	/** Statements that must follow the data, such as moving a sequence past the imported rows. */
	public List<String> afterDataStatements(TableName table) throws SQLException {
		String sql = dialect().autoNumberedColumnsSql();
		List<String> columns = sql == null ? List.of() : queryStrings(sql, table.schema(), table.name());

		return dialect().afterDataLoadSql(table, columns);
	}
}

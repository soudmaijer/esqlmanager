package nl.errorsoft.esql.export;

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
		String schemas = dialect().listSchemasSql();

		if (schemas == null) {
			return withoutViews(listTables(database));
		}

		useDatabase(database.getName());
		List<TableName> names = new ArrayList<>();

		for (String schema : queryStrings(schemas)) {
			names.addAll(tableNames(new Schema(database, schema)));
		}
		return names;
	}

	/** The tables of one schema, no views. */
	public List<TableName> tableNames(Schema schema) throws SQLException {
		return withoutViews(listTables(schema));
	}

	private static List<TableName> withoutViews(List<Table> tables) {
		List<TableName> names = new ArrayList<>();

		for (Table table : tables) {
			if (!"VIEW".equalsIgnoreCase(table.getType())) {
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

	public String dropTableSql(TableName table) {
		return "DROP TABLE IF EXISTS " + quote(table);
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

	/** Passes every row of the table to the sink as an INSERT statement, binary columns are left empty. */
	public void insertStatements(TableName table, Consumer<String> sink) throws SQLException {
		try (ResultSet rs = dbc.executeQuery("SELECT * FROM " + quote(table))) {
			ResultSetMetaData rsm = rs.getMetaData();

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

				sink.accept("INSERT INTO " + quote(table) + " VALUES(" + String.join(",", values) + ");");
			}
		}
	}

	/** Statements that must follow the data, such as moving a sequence past the imported rows. */
	public List<String> afterDataStatements(TableName table) throws SQLException {
		String sql = dialect().autoNumberedColumnsSql();
		List<String> columns = sql == null ? List.of() : queryStrings(sql, table.schema(), table.name());

		return dialect().afterDataLoadSql(table, columns);
	}
}

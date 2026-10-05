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
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/** Reads what an export script is made of: table names, table definitions and rows as statements. */
public class ExportRepository extends AbstractRepository {
	public ExportRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	/** The tables of a database that have structure and data of their own, so no views. */
	public List<String> tableNames(Database database) throws SQLException {
		List<String> names = new ArrayList<>();

		for (Table table : listTables(database)) {
			if (!"VIEW".equalsIgnoreCase(table.getType())) {
				names.add(table.getName());
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

	public String dropTableSql(String table) {
		return "DROP TABLE IF EXISTS " + quote(table);
	}

	public String structureSql(String table) throws SQLException {
		String show = dialect().showCreateTableSql(table);

		if (show != null) {
			try (ResultSet rs = dbc.executeQuery(show)) {
				rs.first();
				return rs.getString(2);
			}
		}

		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		String catalog = dbc.getConnection().getCatalog();
		List<String> columns = new ArrayList<>();

		try (ResultSet rs = dmd.getColumns(catalog, dbc.getSchema(), table, "%")) {
			while (rs.next()) {
				columns.add(dialect().columnDdl(rs));
			}
		}

		Map<Integer, String> primary = new TreeMap<>();
		try (ResultSet keys = dmd.getPrimaryKeys(catalog, dbc.getSchema(), table)) {
			while (keys.next()) {
				primary.put(keys.getInt("KEY_SEQ"), keys.getString("COLUMN_NAME"));
			}
		}

		return dialect().createTableDdl(table, columns, new ArrayList<>(primary.values()));
	}

	/** Passes every row of the table to the sink as an INSERT statement, binary columns are left empty. */
	public void insertStatements(String table, Consumer<String> sink) throws SQLException {
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
	public List<String> afterDataStatements(String table) throws SQLException {
		String sql = dialect().autoNumberedColumnsSql();
		List<String> columns = sql == null ? List.of() : queryStrings(sql, table);

		return dialect().afterDataLoadSql(table, columns);
	}
}

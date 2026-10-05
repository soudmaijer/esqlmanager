package nl.errorsoft.esql.designer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.CreateColumn;

/** Reads the structure of an existing database for the designer from the JDBC metadata; what differs per server is asked from the dialect. */
public class DesignerRepository extends AbstractRepository {
	public DesignerRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	/** The names of the tables of the database, views are left out. */
	public List<String> loadTableNames(Database database) throws SQLException {
		useDatabase(database.getName());
		List<String> names = new ArrayList<>();

		try (ResultSet rs = dbc.getConnection().getMetaData().getTables(dbc.getConnection().getCatalog(), dbc.getSchema(), "%", new String[]{"TABLE"})) {
			while (rs.next()) {
				names.add(rs.getString("TABLE_NAME"));
			}
		}
		return names;
	}

	/** The storage engines a table can have, empty when the server has none. */
	public List<String> tableTypes() {
		return Arrays.asList(dialect().getTableTypes());
	}

	/** The columns of a table of the active database in their order, with the primary key columns marked. */
	public List<CreateColumn> loadColumns(String table) throws SQLException {
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		List<CreateColumn> columns = new ArrayList<>();

		try (ResultSet rs = dmd.getColumns(dbc.getConnection().getCatalog(), dbc.getSchema(), table, "%")) {
			while (rs.next()) {
				columns.add(dialect().readColumn(rs));
			}
		}

		try (ResultSet rs = dmd.getPrimaryKeys(dbc.getConnection().getCatalog(), dbc.getSchema(), table)) {
			while (rs.next()) {
				for (CreateColumn column : columns) {
					column.primary |= column.name.equals(rs.getString("COLUMN_NAME"));
				}
			}
		}
		return columns;
	}

	/** The foreign keys a table of the active database has on other tables, the columns of a composite key in key order. */
	public List<DesignedForeignKey> loadForeignKeys(String table) throws SQLException {
		Map<String, TreeMap<Integer, String[]>> pairs = new LinkedHashMap<>();
		Map<String, String[]> details = new LinkedHashMap<>();

		try (ResultSet rs = dbc.getConnection().getMetaData().getImportedKeys(dbc.getConnection().getCatalog(), dbc.getSchema(), table)) {
			while (rs.next()) {
				String name = rs.getString("FK_NAME");
				pairs.computeIfAbsent(name, key -> new TreeMap<>()).put(rs.getInt("KEY_SEQ"),
					new String[]{rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME")});
				details.put(name, new String[]{rs.getString("PKTABLE_NAME"), action(rs.getInt("DELETE_RULE")), action(rs.getInt("UPDATE_RULE"))});
			}
		}

		List<DesignedForeignKey> keys = new ArrayList<>();
		for (Map.Entry<String, TreeMap<Integer, String[]>> key : pairs.entrySet()) {
			List<String> columns = new ArrayList<>();
			List<String> referenced = new ArrayList<>();
			for (String[] pair : key.getValue().values()) {
				columns.add(pair[0]);
				referenced.add(pair[1]);
			}
			String[] detail = details.get(key.getKey());
			keys.add(new DesignedForeignKey(key.getKey(), columns, detail[0], referenced, detail[1], detail[2]));
		}
		return keys;
	}

	/** NO ACTION is what a key does when nothing is said, so it is left empty like a key drawn in the designer. */
	private static String action(int rule) {
		return switch (rule) {
			case DatabaseMetaData.importedKeyCascade -> "CASCADE";
			case DatabaseMetaData.importedKeySetNull -> "SET NULL";
			case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
			case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
			default -> "";
		};
	}
}

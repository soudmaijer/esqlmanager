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
import nl.errorsoft.esql.table.ColumnDefinition;

/** Reads the structure of an existing database for the designer from the JDBC metadata; what differs per server is asked from the dialect. */
public class DesignerRepository extends AbstractRepository {
	public DesignerRepository(DatabaseConnection connection) {
		super(connection);
	}

	/**
	 * The names of the tables of the database, views are left out.
	 * @param schema the schema to read, null for the current one.
	 */
	public List<String> loadTableNames(Database database, String schema) throws SQLException {
		useDatabase(database.getName());
		List<String> names = new ArrayList<>();

		try (ResultSet rs = connection.getConnection().getMetaData().getTables(connection.getConnection().getCatalog(), schemaOrCurrent(schema), "%",
			new String[]{"TABLE"})) {
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

	/**
	 * The columns of a table of the database in their order, with the primary key columns marked.
	 * @param schema the schema of the table, null for the current one.
	 */
	public List<ColumnDefinition> loadColumns(Database database, String schema, String table) throws SQLException {
		// The connection is shared with the query tabs, which may have moved it to another database meanwhile.
		useDatabase(database.getName());
		DatabaseMetaData metaData = connection.getConnection().getMetaData();
		List<ColumnDefinition> columns = new ArrayList<>();

		try (ResultSet rs = metaData.getColumns(connection.getConnection().getCatalog(), schemaOrCurrent(schema), table, "%")) {
			while (rs.next()) {
				columns.add(dialect().readColumn(rs));
			}
		}

		try (ResultSet rs = metaData.getPrimaryKeys(connection.getConnection().getCatalog(), schemaOrCurrent(schema), table)) {
			while (rs.next()) {
				for (ColumnDefinition column : columns) {
					column.primary |= column.name.equals(rs.getString("COLUMN_NAME"));
				}
			}
		}
		return columns;
	}

	/**
	 * The foreign keys a table of the database has on other tables of the same schema (and database), the columns of a composite key in key
	 * order. A key on a table elsewhere is left out, even when a table of this schema has the same name.
	 */
	public List<DesignedForeignKey> loadForeignKeys(Database database, String schema, String table) throws SQLException {
		useDatabase(database.getName());
		Map<String, TreeMap<Integer, ColumnPair>> pairs = new LinkedHashMap<>();
		Map<String, KeyDetail> details = new LinkedHashMap<>();
		String catalog = connection.getConnection().getCatalog();
		String readSchema = schemaOrCurrent(schema);

		try (ResultSet rs = connection.getConnection().getMetaData().getImportedKeys(catalog, readSchema, table)) {
			while (rs.next()) {
				if (!sameName(rs.getString("PKTABLE_CAT"), catalog) || !sameName(rs.getString("PKTABLE_SCHEM"), readSchema)) {
					continue;
				}
				String name = rs.getString("FK_NAME");
				pairs.computeIfAbsent(name, key -> new TreeMap<>()).put(rs.getInt("KEY_SEQ"),
					new ColumnPair(rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME")));
				details.put(name, new KeyDetail(rs.getString("PKTABLE_NAME"), action(rs.getInt("DELETE_RULE")), action(rs.getInt("UPDATE_RULE"))));
			}
		}

		List<DesignedForeignKey> keys = new ArrayList<>();
		for (Map.Entry<String, TreeMap<Integer, ColumnPair>> key : pairs.entrySet()) {
			List<String> columns = new ArrayList<>();
			List<String> referenced = new ArrayList<>();
			for (ColumnPair pair : key.getValue().values()) {
				columns.add(pair.column());
				referenced.add(pair.referenced());
			}
			KeyDetail detail = details.get(key.getKey());
			keys.add(new DesignedForeignKey(key.getKey(), columns, detail.referencedTable(), referenced, detail.onDelete(), detail.onUpdate()));
		}
		return keys;
	}

	/** A column of a foreign key and the column of the referenced table it refers to. */
	private record ColumnPair(String column, String referenced) {
	}

	/** What a foreign key says once, whatever the number of columns: the referenced table and the actions. */
	private record KeyDetail(String referencedTable, String onDelete, String onUpdate) {
	}

	/** Whether the referenced catalog or schema is the one read; a server without catalogs or schemas reports null. */
	private static boolean sameName(String referenced, String read) {
		return referenced == null || read == null || referenced.equals(read);
	}

	private String schemaOrCurrent(String schema) throws SQLException {
		return schema == null ? connection.getSchema() : schema;
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

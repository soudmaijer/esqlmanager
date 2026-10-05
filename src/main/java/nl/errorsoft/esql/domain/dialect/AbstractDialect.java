package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.table.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.domain.CreateColumn;
import nl.errorsoft.esql.domain.EsqlException;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.connection.ServerProcess;
import nl.errorsoft.esql.table.Table;

/**
 * Plain JDBC and ANSI SQL behaviour that works on any database. Dialects override what is different.
 */
public abstract class AbstractDialect implements Dialect {
	public boolean supports(Feature feature) {
		return false;
	}

	public String getConnectionDatabase(ConnectionProfile cp, String requested) {
		return requested;
	}

	public void useDatabase(DatabaseConnection dbc, String database) throws SQLException {
		dbc.getConnection().setCatalog(database);
	}

	public String getSchema(DatabaseConnection dbc) throws SQLException {
		return null;
	}

	public Vector<Table> listTables(DatabaseConnection dbc, Database db) throws SQLException {
		return listTablesFromMetaData(dbc, db, db.getName(), getSchema(dbc), new String[]{"TABLE", "VIEW"});
	}

	public String selectPage(String quotedTable, String orderBy, int skip, int show) {
		return null;
	}

	public String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}

	public String[] getTableTypes() {
		return new String[0];
	}

	public List<String> createTableSql(String table, List<CreateColumn> columns, String tableType, String comment) {
		List<String> definitions = new ArrayList<>();
		List<String> primary = new ArrayList<>();

		for (CreateColumn column : columns) {
			definitions.add(quote(column.name) + " " + columnDefinition(column));

			if (column.primary) {
				primary.add(quote(column.name));
			}
			if (column.unique) {
				definitions.add("UNIQUE (" + quote(column.name) + ")");
			}
		}

		if (!primary.isEmpty()) {
			definitions.add("PRIMARY KEY (" + String.join(", ", primary) + ")");
		}

		List<String> statements = new ArrayList<>();
		statements.add("CREATE TABLE " + quote(table) + " (" + String.join(", ", definitions) + ")");

		for (CreateColumn column : columns) {
			if (column.index) {
				statements.addAll(addIndexSql(table, table + "_" + column.name + "_idx", "INDEX", Arrays.asList(column.name)));
			}
		}

		if (comment.trim().length() > 0) {
			statements.addAll(setTableCommentSql(table, comment));
		}

		return statements;
	}

	public List<String> renameTableSql(String table, String newName) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " RENAME TO " + quote(newName));
	}

	public List<String> setTableTypeSql(String table, String tableType) {
		return new ArrayList<>();
	}

	public List<String> setTableCommentSql(String table, String comment) {
		return Arrays.asList("COMMENT ON TABLE " + quote(table) + " IS " + literal(comment));
	}

	public List<String> addColumnSql(String table, CreateColumn column) {
		String statement = "ALTER TABLE " + quote(table) + " ADD COLUMN " + quote(column.name) + " " + columnDefinition(column);

		if (column.primary) {
			statement += " PRIMARY KEY";
		}

		return Arrays.asList(statement);
	}

	public List<String> modifyColumnSql(String table, String oldName, CreateColumn column) {
		String alter = "ALTER TABLE " + quote(table);
		String name = quote(column.name);
		List<String> statements = new ArrayList<>();

		if (!oldName.equals(column.name)) {
			statements.add(alter + " RENAME COLUMN " + quote(oldName) + " TO " + name);
		}

		statements.add(alter + " ALTER COLUMN " + name + " TYPE " + columnType(column));
		statements.add(alter + " ALTER COLUMN " + name + (column.notnull ? " SET NOT NULL" : " DROP NOT NULL"));

		if (column.defaultval.trim().length() > 0) {
			statements.add(alter + " ALTER COLUMN " + name + " SET DEFAULT " + literal(column.defaultval));
		} else {
			statements.add(alter + " ALTER COLUMN " + name + " DROP DEFAULT");
		}

		return statements;
	}

	public List<String> addIndexSql(String table, String name, String type, List<String> columns) {
		List<String> quoted = new ArrayList<>();

		for (String column : columns) {
			quoted.add(quote(column));
		}

		if (name.equals("PRIMARY")) {
			return Arrays.asList("ALTER TABLE " + quote(table) + " ADD PRIMARY KEY (" + String.join(", ", quoted) + ")");
		}

		if ("FULLTEXT".equalsIgnoreCase(type)) {
			return Arrays.asList("CREATE INDEX " + quote(name) + " ON " + quote(table) + " USING GIN (to_tsvector('simple', "
				+ String.join(" || ' ' || ", quoted) + "))");
		}

		String unique = "UNIQUE".equalsIgnoreCase(type) ? "UNIQUE " : "";
		return Arrays.asList("CREATE " + unique + "INDEX " + quote(name) + " ON " + quote(table) + " (" + String.join(", ", quoted) + ")");
	}

	public List<String> dropIndexSql(DatabaseConnection dbc, String table, String name) throws SQLException {
		if (name.equals("PRIMARY")) {
			return Arrays.asList("ALTER TABLE " + quote(table) + " DROP CONSTRAINT " + quote(primaryKeyName(dbc, table)));
		}

		return Arrays.asList("DROP INDEX " + quote(name));
	}

	public List<String> modifyIndexSql(DatabaseConnection dbc, String table, String name, String type, List<String> columns) throws SQLException {
		List<String> statements = new ArrayList<>(dropIndexSql(dbc, table, name));
		statements.addAll(addIndexSql(table, name, type, columns));
		return statements;
	}

	public List<String> addForeignKeySql(String table, String name, List<String> columns, String refTable, List<String> refColumns, String onDelete,
		String onUpdate) {
		String statement = "ALTER TABLE " + quote(table) + " ADD CONSTRAINT " + quote(name) + " FOREIGN KEY (" + quoteAll(columns) + ") REFERENCES "
			+ quote(refTable) + " (" + quoteAll(refColumns) + ")" + referentialAction("ON DELETE", onDelete) + referentialAction("ON UPDATE", onUpdate);

		return Arrays.asList(statement);
	}

	public List<String> dropForeignKeySql(String table, String name) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " DROP CONSTRAINT " + quote(name));
	}

	public void checkForeignKeyTable(DatabaseConnection dbc, String table) throws SQLException {
	}

	private String referentialAction(String clause, String action) {
		if (action == null || action.isBlank()) {
			return "";
		}

		String normalised = action.trim().toUpperCase().replaceAll("\\s+", " ");
		if (!REFERENTIAL_ACTIONS.contains(normalised)) {
			throw new EsqlException("'" + action + "' is not a foreign key action, use one of NO ACTION, CASCADE, SET NULL, RESTRICT or SET DEFAULT.");
		}
		return " " + clause + " " + normalised;
	}

	private String quoteAll(List<String> names) {
		List<String> quoted = new ArrayList<>();

		for (String name : names) {
			quoted.add(quote(name));
		}
		return String.join(", ", quoted);
	}

	public List<String> afterDataLoadSql(DatabaseConnection dbc, String table) throws SQLException {
		return new ArrayList<>();
	}

	public String createDatabaseSql(String database) {
		return "CREATE DATABASE " + quote(database);
	}

	public void dropDatabase(DatabaseConnection dbc, String database) throws SQLException {
		dbc.executeUpdate("DROP DATABASE " + quote(database));
	}

	/** Not every server can switch database with a statement, so scripts use the psql meta command that Import understands. */
	public String useDatabaseSql(String database) {
		return "\\connect " + quote(database);
	}

	/** Builds the statement from JDBC metadata, for servers that cannot show it themselves. */
	public String createTableDdl(DatabaseConnection dbc, String table) throws SQLException {
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		String catalog = dbc.getConnection().getCatalog();
		List<String> definitions = new ArrayList<>();
		try (ResultSet rs = dmd.getColumns(catalog, getSchema(dbc), table, "%")) {

			while (rs.next()) {
				String definition = quote(rs.getString("COLUMN_NAME")) + " ";
				String defaultValue = rs.getString("COLUMN_DEF");
				boolean identity = "YES".equals(rs.getString("IS_AUTOINCREMENT"));

				definition += identity ? identityType(rs.getString("TYPE_NAME")) : typeWithSize(rs);

				if (identity) {
					definition += " GENERATED BY DEFAULT AS IDENTITY";
				} else if (defaultValue != null) {
					definition += " DEFAULT " + defaultValue;
				}

				if (rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls) {
					definition += " NOT NULL";
				}

				definitions.add(definition);
			}
		}

		Map<Integer, String> primary = new TreeMap<>();
		try (ResultSet keys = dmd.getPrimaryKeys(catalog, getSchema(dbc), table)) {
			while (keys.next()) {
				primary.put(keys.getInt("KEY_SEQ"), quote(keys.getString("COLUMN_NAME")));
			}
		}

		if (!primary.isEmpty()) {
			definitions.add("PRIMARY KEY (" + String.join(", ", primary.values()) + ")");
		}

		return "CREATE TABLE " + quote(table) + " (" + String.join(", ", definitions) + ")";
	}

	private String identityType(String typeName) {
		return switch (typeName) {
			case "serial" -> "integer";
			case "bigserial" -> "bigint";
			case "smallserial" -> "smallint";
			default -> typeName;
		};
	}

	private String typeWithSize(ResultSet rs) throws SQLException {
		String type = rs.getString("TYPE_NAME");
		int size = rs.getInt("COLUMN_SIZE");

		if (type.equals("bpchar")) {
			type = "char";
		}

		if ((type.equals("varchar") || type.equals("char")) && size > 0 && size < Integer.MAX_VALUE) {
			return type + "(" + size + ")";
		}
		if (type.equals("numeric") && size > 0 && size < 1000) {
			return type + "(" + size + "," + rs.getInt("DECIMAL_DIGITS") + ")";
		}

		return type;
	}

	public String getStatusQuery() {
		return null;
	}

	public String getVariablesQuery() {
		return null;
	}

	public List<ServerProcess> listProcesses(DatabaseConnection dbc) throws SQLException {
		throw new UnsupportedOperationException("The process list is not available on this server");
	}

	public void killProcess(DatabaseConnection dbc, String processId) throws SQLException {
		throw new UnsupportedOperationException("Processes cannot be ended on this server");
	}

	public UserAdmin getUserAdmin() {
		throw new UnsupportedOperationException("User management is not available on this server");
	}

	/** Without a command of its own the server does nothing, PostgreSQL overrides this with VACUUM and ANALYZE. */
	public String maintain(DatabaseConnection dbc, Maintenance command, String table) throws SQLException {
		throw new UnsupportedOperationException(command + " is not available on " + dbc.getConnectionProfile().getServerType().getDescription());
	}

	/** The type, size, default and nullability of a column as used in CREATE TABLE and ALTER TABLE. */
	protected String columnDefinition(CreateColumn column) {
		String definition = columnType(column);

		if (column.defaultval.trim().length() > 0) {
			definition += " DEFAULT " + literal(column.defaultval);
		}
		if (column.notnull) {
			definition += " NOT NULL";
		}
		if (column.autoincrement) {
			definition += " GENERATED BY DEFAULT AS IDENTITY";
		}

		return definition;
	}

	protected String columnType(CreateColumn column) {
		String type = column.type.getName();

		if (column.length.trim().length() > 0) {
			type += " (" + column.length + ")";
		}

		return type;
	}

	/** A text value as an SQL literal. */
	public String literal(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	private String primaryKeyName(DatabaseConnection dbc, String table) throws SQLException {
		try (ResultSet rs = dbc.getConnection().getMetaData().getPrimaryKeys(dbc.getConnection().getCatalog(), getSchema(dbc), table)) {
			if (rs.next()) {
				return rs.getString("PK_NAME");
			}
		}
		throw new SQLException("Table " + table + " has no primary key");
	}

	/** Lists tables through DatabaseMetaData and counts the rows of each of them. */
	protected Vector<Table> listTablesFromMetaData(DatabaseConnection dbc, Database db, String catalog, String schema, String[] types) throws SQLException {
		Vector<Table> tables = new Vector<>();
		DatabaseMetaData dmd = dbc.getConnection().getMetaData();
		try (ResultSet rs = dmd.getTables(catalog, schema, "%", types)) {

			while (rs.next()) {
				Table table = new Table(db);
				table.setName(rs.getString("TABLE_NAME"));
				table.setType(rs.getString("TABLE_TYPE"));
				table.setComment(rs.getString("REMARKS"));
				tables.add(table);
			}
		}

		for (Table table : tables) {
			try (ResultSet counted = dbc.executeQuery("SELECT count(*) AS cnt FROM " + quote(table.getName()))) {
				if (counted.first()) {
					table.setRowCount(counted.getInt("cnt"));
				}
			} catch (SQLException e) {
				// A table we cannot count (no rights, broken view) is still listed, without a row count.
			}
		}
		return tables;
	}
}

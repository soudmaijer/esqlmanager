package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.table.TableName;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.table.Table;

/**
 * Plain JDBC and ANSI SQL behaviour that works on any database. Dialects override what is different.
 */
public abstract class AbstractDialect implements Dialect {
	/** A text default with an optional cast, as PostgreSQL reports it: 'it''s'::character varying. */
	private static final Pattern QUOTED_DEFAULT = Pattern.compile("^'(.*)'(::[\\w\\s\\[\\]]+)?$", Pattern.DOTALL);

	public boolean supports(Feature feature) {
		return false;
	}

	public String getConnectionDatabase(ConnectionProfile cp, String requested) {
		return requested;
	}

	public DatabaseSwitch databaseSwitch() {
		return DatabaseSwitch.CATALOG;
	}

	public String databaseTerm() {
		return "database";
	}

	public String schemaTerm() {
		return "schema";
	}

	public String listSchemasSql() {
		return null;
	}

	public String createSchemaSql(String schema) {
		return "CREATE SCHEMA " + quote(schema);
	}

	public String createSchemaIfMissingSql(String schema) {
		return "CREATE SCHEMA IF NOT EXISTS " + quote(schema);
	}

	public String useSchemaSql(String schema) {
		return null;
	}

	public String dropSchemaSql(String schema) {
		return "DROP SCHEMA " + quote(schema) + " CASCADE";
	}

	public String currentSchemaSql() {
		return null;
	}

	public String listTablesSql() {
		return null;
	}

	public Table readTable(ResultSet row, Database db) throws SQLException {
		throw new UnsupportedOperationException("Tables are listed from the metadata on this server");
	}

	public String[] metadataTableTypes() {
		return new String[]{"TABLE", "VIEW"};
	}

	public String selectPage(String quotedTable, String orderBy, int skip, int show) {
		return null;
	}

	public String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}

	public String quote(TableName table) {
		return table.schema() == null ? quote(table.name()) : quote(table.schema()) + "." + quote(table.name());
	}

	public String[] getTableTypes() {
		return new String[0];
	}

	public List<String> createTableSql(TableName table, List<CreateColumn> columns, String tableType, String comment) {
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
				statements.addAll(addIndexSql(table, table.name() + "_" + column.name + "_idx", "INDEX", Arrays.asList(column.name)));
			}
		}

		if (comment.trim().length() > 0) {
			statements.addAll(setTableCommentSql(table, comment));
		}

		for (CreateColumn column : columns) {
			statements.addAll(columnCommentSql(table, column, false));
		}

		return statements;
	}

	public boolean supportsColumnComments() {
		return false;
	}

	/** The statements that set the comment of a column, none where the comment is part of the column definition. @param always write a statement for an empty comment too, to clear it */
	protected List<String> columnCommentSql(TableName table, CreateColumn column, boolean always) {
		if (!supportsColumnComments() || (!always && column.comment.isBlank())) {
			return List.of();
		}
		String comment = column.comment.isBlank() ? "NULL" : literal(column.comment);
		return List.of("COMMENT ON COLUMN " + quote(table) + "." + quote(column.name) + " IS " + comment);
	}

	public List<String> renameTableSql(TableName table, String newName) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " RENAME TO " + quote(newName));
	}

	public List<String> setTableTypeSql(TableName table, String tableType) {
		return new ArrayList<>();
	}

	public List<String> setTableCommentSql(TableName table, String comment) {
		return Arrays.asList("COMMENT ON TABLE " + quote(table) + " IS " + literal(comment));
	}

	public List<String> addColumnSql(TableName table, CreateColumn column) {
		String statement = "ALTER TABLE " + quote(table) + " ADD COLUMN " + quote(column.name) + " " + columnDefinition(column);

		if (column.primary) {
			statement += " PRIMARY KEY";
		}

		List<String> statements = new ArrayList<>(List.of(statement));
		statements.addAll(columnCommentSql(table, column, false));
		return statements;
	}

	public List<String> modifyColumnSql(TableName table, String oldName, CreateColumn column) {
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

		statements.addAll(columnCommentSql(table, column, true));
		return statements;
	}

	public List<String> addIndexSql(TableName table, String name, String type, List<String> columns) {
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

	public List<String> dropIndexSql(TableName table, String name, String primaryKeyName) {
		if (name.equals("PRIMARY")) {
			if (primaryKeyName == null) {
				throw new EsqlException("Table " + table + " has no primary key");
			}
			return Arrays.asList("ALTER TABLE " + quote(table) + " DROP CONSTRAINT " + quote(primaryKeyName));
		}

		return Arrays.asList("DROP INDEX " + quote(table.sibling(name)));
	}

	public List<String> modifyIndexSql(TableName table, String name, String primaryKeyName, String type, List<String> columns) {
		List<String> statements = new ArrayList<>(dropIndexSql(table, name, primaryKeyName));
		statements.addAll(addIndexSql(table, name, type, columns));
		return statements;
	}

	public List<String> addForeignKeySql(TableName table, String name, List<String> columns, String refTable, List<String> refColumns, String onDelete,
		String onUpdate) {
		String statement = "ALTER TABLE " + quote(table) + " ADD CONSTRAINT " + quote(name) + " FOREIGN KEY (" + quoteAll(columns) + ") REFERENCES "
			+ quote(table.sibling(refTable)) + " (" + quoteAll(refColumns) + ")" + referentialAction("ON DELETE", onDelete)
			+ referentialAction("ON UPDATE", onUpdate);

		return Arrays.asList(statement);
	}

	public List<String> dropForeignKeySql(TableName table, String name) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " DROP CONSTRAINT " + quote(name));
	}

	public String tableTypeSql() {
		return null;
	}

	public void checkForeignKeyTable(TableName table, String tableType) {
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

	public String autoNumberedColumnsSql() {
		return null;
	}

	public List<String> afterDataLoadSql(TableName table, List<String> autoNumberedColumns) {
		return new ArrayList<>();
	}

	public String createDatabaseSql(String database) {
		return "CREATE DATABASE " + quote(database);
	}

	public List<DatabaseOption> createDatabaseOptions() {
		return List.of();
	}

	public String createDatabaseSql(String database, java.util.Map<String, String> options) {
		StringBuilder sql = new StringBuilder("CREATE DATABASE ").append(quote(database));

		for (DatabaseOption option : createDatabaseOptions()) {
			String value = options.get(option.key());

			if (value != null && !value.isBlank()) {
				sql.append(" ").append(databaseOptionClause(option.key(), value.trim()));
			}
		}
		return sql.toString();
	}

	/** The part of CREATE DATABASE that sets one option, the value is a literal or identifier to be written safely. */
	protected String databaseOptionClause(String key, String value) {
		throw new UnsupportedOperationException("Unknown database option " + key);
	}

	public String databasePropertiesSql() {
		return null;
	}

	public String renameSchemaSql(String schema, String newName) {
		return "ALTER SCHEMA " + quote(schema) + " RENAME TO " + quote(newName);
	}

	public List<String> copyTableSql(TableName source, TableName target, boolean withData) {
		List<String> statements = new ArrayList<>();
		statements.add("CREATE TABLE " + quote(target) + " (LIKE " + quote(source) + " INCLUDING ALL)");

		if (withData) {
			statements.add("INSERT INTO " + quote(target) + " SELECT * FROM " + quote(source));
		}
		return statements;
	}

	public String tableSizeSql() {
		return null;
	}

	public String dropDatabaseSql(String database) {
		return "DROP DATABASE " + quote(database);
	}

	public String databaseToLeaveFor(String database) {
		return null;
	}

	/** Not every server can switch database with a statement, so scripts use the psql meta command that Import understands. */
	public String useDatabaseSql(String database) {
		return "\\connect " + quote(database);
	}

	/** Servers that cannot show the statement themselves get one built from the JDBC metadata. */
	public String showCreateTableSql(TableName table) {
		return null;
	}

	public String showCreateViewSql(TableName view) {
		return null;
	}

	public String dropTableSql(TableName table, boolean ifExists) {
		return "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + quote(table);
	}

	public String dropViewSql(TableName view, boolean ifExists) {
		return "DROP VIEW " + (ifExists ? "IF EXISTS " : "") + quote(view);
	}

	public String createTableIfNotExists(String createTableSql) {
		return createTableSql.replaceFirst("^CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
	}

	public String beginTransactionSql() {
		return null;
	}

	public String commitSql() {
		return null;
	}

	public String disableForeignKeyChecksSql() {
		return null;
	}

	public String enableForeignKeyChecksSql() {
		return null;
	}

	public String columnDdl(ResultSet rs) throws SQLException {
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
		return definition;
	}

	public String createTableDdl(TableName table, List<String> columnDefinitions, List<String> primaryKey) {
		List<String> definitions = new ArrayList<>(columnDefinitions);

		if (!primaryKey.isEmpty()) {
			definitions.add("PRIMARY KEY (" + quoteAll(primaryKey) + ")");
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

	/** Reads the PostgreSQL type names of the driver (int4, bpchar, serial) as the names the designer uses (integer, char). */
	public CreateColumn readColumn(ResultSet rs) throws SQLException {
		CreateColumn column = new CreateColumn(rs.getString("COLUMN_NAME"));
		String type = rs.getString("TYPE_NAME").toLowerCase();
		String defaultValue = rs.getString("COLUMN_DEF");
		int size = rs.getInt("COLUMN_SIZE");

		column.autoincrement = "YES".equals(rs.getString("IS_AUTOINCREMENT")) || (defaultValue != null && defaultValue.startsWith("nextval("));
		column.notnull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
		column.type = new DataType(designerTypeName(identityType(type)), false, false, false, false, false, false, false, false);
		column.defaultval = column.autoincrement || defaultValue == null ? "" : plainDefault(defaultValue);

		if ((type.equals("varchar") || type.equals("bpchar")) && size > 0 && size < Integer.MAX_VALUE) {
			column.length = String.valueOf(size);
		} else if (type.equals("numeric") && size > 0 && size < 1000) {
			column.length = size + "," + rs.getInt("DECIMAL_DIGITS");
		}
		return column;
	}

	private static String designerTypeName(String type) {
		return switch (type) {
			case "int2" -> "smallint";
			case "int4" -> "integer";
			case "int8" -> "bigint";
			case "float4" -> "real";
			case "float8" -> "double precision";
			case "bool" -> "boolean";
			case "bpchar" -> "char";
			default -> type;
		};
	}

	/** A default such as {@code 'it''s'::character varying} as the text it stands for; an expression such as now() stays as it is. */
	private static String plainDefault(String defaultValue) {
		Matcher literal = QUOTED_DEFAULT.matcher(defaultValue);
		return literal.matches() ? literal.group(1).replace("''", "'") : defaultValue;
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

	public String listProcessesSql() {
		throw new UnsupportedOperationException("The process list is not available on this server");
	}

	public ServerProcess readProcess(ResultSet row) throws SQLException {
		throw new UnsupportedOperationException("The process list is not available on this server");
	}

	public String killProcessSql(String processId) {
		throw new UnsupportedOperationException("Processes cannot be ended on this server");
	}

	public UserAdmin getUserAdmin() {
		throw new UnsupportedOperationException("User management is not available on this server");
	}

	public java.util.Set<Maintenance> maintenanceCommands() {
		return java.util.EnumSet.noneOf(Maintenance.class);
	}

	/** Without a command of its own the server does nothing, PostgreSQL overrides this with VACUUM and ANALYZE. */
	public MaintenanceStatement maintenanceSql(Maintenance command, TableName table) {
		throw new UnsupportedOperationException(command + " is not available on this server");
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
}

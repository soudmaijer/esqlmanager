package nl.errorsoft.esql.dialect.mysql;

import nl.errorsoft.esql.table.TableName;

import nl.errorsoft.esql.dialect.AbstractDialect;
import nl.errorsoft.esql.dialect.MaintenanceStatement;
import nl.errorsoft.esql.dialect.UserAdmin;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.table.Table;

public class MySqlDialect extends AbstractDialect {
	public int getType() {
		return ServerType.MY_SQL;
	}

	public String getDefaultPort() {
		return "3306";
	}

	public String getDefaultUsername() {
		return "root";
	}

	/** The MySQL specific tools were written for MySQL, so it is the only server that supports all of them. */
	public String getStatusQuery() {
		return "SHOW STATUS";
	}

	public String getVariablesQuery() {
		return "SHOW VARIABLES";
	}

	public String listProcessesSql() {
		return "SHOW PROCESSLIST";
	}

	public ServerProcess readProcess(ResultSet rs) throws SQLException {
		return new ServerProcess(rs.getString("Id"), rs.getString("User"), rs.getString("Host"), rs.getString("db"), rs.getString("Command"),
			rs.getString("Time"), rs.getString("Info"));
	}

	public String killProcessSql(String processId) {
		// The id is a number, parsing it keeps anything else out of the statement.
		return "KILL " + Long.parseLong(processId);
	}

	public UserAdmin getUserAdmin() {
		return new MySqlUserAdmin(this);
	}

	/** Everything but schemas: a MySQL schema is a database. */
	public boolean supports(Feature feature) {
		return feature != Feature.SCHEMAS;
	}

	public java.util.Set<Maintenance> maintenanceCommands() {
		return java.util.EnumSet.allOf(Maintenance.class);
	}

	public MaintenanceStatement maintenanceSql(Maintenance command, TableName table) {
		return new MaintenanceStatement(command + " TABLE " + quote(table), "Msg_Text", "");
	}

	public String createDatabaseSql(String database) {
		return "CREATE DATABASE IF NOT EXISTS " + quote(database);
	}

	public String useDatabaseSql(String database) {
		return "USE " + quote(database);
	}

	public String showCreateTableSql(TableName table) {
		return "SHOW CREATE TABLE " + quote(table);
	}

	public String showCreateViewSql(TableName view) {
		return "SHOW CREATE VIEW " + quote(view);
	}

	public String beginTransactionSql() {
		return "START TRANSACTION";
	}

	public String commitSql() {
		return "COMMIT";
	}

	public String disableForeignKeyChecksSql() {
		return "SET FOREIGN_KEY_CHECKS=0";
	}

	public String enableForeignKeyChecksSql() {
		return "SET FOREIGN_KEY_CHECKS=1";
	}

	public String quote(String identifier) {
		return "`" + identifier.replace("`", "``") + "`";
	}

	public String[] getTableTypes() {
		return new String[]{"InnoDB", "MyISAM", "MEMORY", "ARCHIVE", "CSV"};
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
			if (column.index) {
				definitions.add("INDEX (" + quote(column.name) + ")");
			}
		}

		if (!primary.isEmpty()) {
			definitions.add("PRIMARY KEY (" + String.join(", ", primary) + ")");
		}

		String statement = "CREATE TABLE " + quote(table) + " (" + String.join(", ", definitions) + ")";

		if (tableType != null && tableType.length() > 0) {
			statement += " ENGINE=" + tableType;
		}
		if (comment.trim().length() > 0) {
			statement += " COMMENT=" + literal(comment);
		}

		return Arrays.asList(statement);
	}

	public List<String> setTableTypeSql(TableName table, String tableType) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " ENGINE=" + tableType);
	}

	public List<String> setTableCommentSql(TableName table, String comment) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " COMMENT=" + literal(comment));
	}

	public List<String> modifyColumnSql(TableName table, String oldName, CreateColumn column) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " CHANGE " + quote(oldName) + " " + quote(column.name) + " " + columnDefinition(column));
	}

	public List<String> addIndexSql(TableName table, String name, String type, List<String> columns) {
		List<String> quoted = new ArrayList<>();

		for (String column : columns) {
			quoted.add(quote(column));
		}

		String cols = "(" + String.join(", ", quoted) + ")";

		if (name.equals("PRIMARY")) {
			return Arrays.asList("ALTER TABLE " + quote(table) + " ADD PRIMARY KEY " + cols);
		}

		return Arrays.asList("ALTER TABLE " + quote(table) + " ADD " + type + " " + quote(name) + " " + cols);
	}

	public List<String> dropIndexSql(TableName table, String name, String primaryKeyName) {
		if (name.equals("PRIMARY")) {
			return Arrays.asList("ALTER TABLE " + quote(table) + " DROP PRIMARY KEY");
		}

		return Arrays.asList("ALTER TABLE " + quote(table) + " DROP INDEX " + quote(name));
	}

	/** Dropping and adding in one statement keeps an AUTO_INCREMENT primary key valid in between. */
	public List<String> modifyIndexSql(TableName table, String name, String primaryKeyName, String type, List<String> columns) {
		String drop = name.equals("PRIMARY") ? "DROP PRIMARY KEY" : "DROP INDEX " + quote(name);
		String add = addIndexSql(table, name, type, columns).get(0).substring(("ALTER TABLE " + quote(table) + " ").length());

		return Arrays.asList("ALTER TABLE " + quote(table) + " " + drop + ", " + add);
	}

	public List<String> dropForeignKeySql(TableName table, String name) {
		return Arrays.asList("ALTER TABLE " + quote(table) + " DROP FOREIGN KEY " + quote(name));
	}

	/** Only InnoDB enforces foreign keys, other engines accept the statement and silently ignore the key. */
	public void checkForeignKeyTable(TableName table, String tableType) {
		if (tableType != null && !"InnoDB".equalsIgnoreCase(tableType)) {
			throw new EsqlException("Table " + table + " uses the " + tableType + " engine, foreign keys need InnoDB.");
		}
	}

	public String tableTypeSql() {
		return "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = coalesce(?, DATABASE()) AND TABLE_NAME = ?";
	}

	/** The driver reports types in upper case with the sign attached (INT UNSIGNED) and defaults as plain text. */
	public CreateColumn readColumn(ResultSet rs) throws SQLException {
		CreateColumn column = new CreateColumn(rs.getString("COLUMN_NAME"));
		String type = rs.getString("TYPE_NAME").toUpperCase();
		String defaultValue = rs.getString("COLUMN_DEF");
		int size = rs.getInt("COLUMN_SIZE");

		column.unsigned = type.endsWith(" UNSIGNED");
		type = type.replace(" UNSIGNED", "");
		column.autoincrement = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
		column.notnull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
		column.type = new DataType(type, false, false, false, false, false, false, false, false);
		column.defaultval = defaultValue == null ? "" : defaultValue;

		if ((type.equals("VARCHAR") || type.equals("CHAR")) && size > 0) {
			column.length = String.valueOf(size);
		} else if (type.equals("DECIMAL") && size > 0) {
			column.length = size + "," + rs.getInt("DECIMAL_DIGITS");
		}
		return column;
	}

	protected String columnDefinition(CreateColumn column) {
		String definition = column.type.getName();

		if (column.length.trim().length() > 0) {
			definition += " (" + column.length + ")";
		}
		if (column.unsigned) {
			definition += " UNSIGNED";
		}
		if (column.zerofill) {
			definition += " ZEROFILL";
		}
		if (column.binary) {
			definition += " BINARY";
		}
		if (column.defaultval.trim().length() > 0) {
			definition += " DEFAULT " + literal(column.defaultval);
		}
		if (column.notnull) {
			definition += " NOT NULL";
		}
		if (column.autoincrement) {
			definition += " AUTO_INCREMENT";
		}

		return definition;
	}

	public String literal(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
	}

	public String listDatabasesSql() {
		return "SHOW DATABASES";
	}

	public String listTablesSql() {
		return "SHOW TABLE STATUS";
	}

	public Table readTable(ResultSet rs, Database db) throws SQLException {
		Table table = new Table(db);
		table.setName(rs.getString("Name"));
		// A view has no engine, SHOW TABLE STATUS lists it with the comment VIEW.
		table.setType(rs.getString("Engine") == null && "VIEW".equals(rs.getString("Comment")) ? "VIEW" : rs.getString("Engine"));
		table.setRowCount(rs.getInt("Rows"));
		table.setComment(rs.getString("Comment"));
		return table;
	}

	public String selectPage(String quotedTable, String orderBy, int skip, int show) {
		return "SELECT * FROM " + quotedTable + (orderBy.length() > 0 ? " ORDER BY " + orderBy : "") + " LIMIT " + skip + "," + show;
	}
}

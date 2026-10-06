package nl.errorsoft.esql.table;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.MaintenanceStatement;

/**
 * The only place that runs SQL for tables, their columns, indexes and rows.
 * What differs per database is asked from the {@link Dialect}.
 */
public class TableRepository extends AbstractRepository {
	public TableRepository(DatabaseConnection connection) {
		super(connection);
	}

	// Structure

	public TableColumn[] loadColumns(Table table) throws SQLException {
		useDatabaseOf(table);
		List<TableColumn> columns = new ArrayList<>();
		DatabaseMetaData metaData = connection.getConnection().getMetaData();

		try (ResultSet rs = metaData.getColumns(connection.getConnection().getCatalog(), schemaOf(table), table.getName(), "%")) {
			while (rs.next()) {
				TableColumn column = new TableColumn(table);
				column.setNativeTypeName(rs.getString("TYPE_NAME"));
				column.setName(rs.getString("COLUMN_NAME"));
				column.setSize(rs.getInt("COLUMN_SIZE"));
				column.setNullable(rs.getBoolean("NULLABLE"));
				column.setDefault(rs.getString("COLUMN_DEF"));
				column.setComment(rs.getString("REMARKS"));
				column.setGenerated("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
				if (column.getNativeTypeName() != null) {
					column.setDisplayTypeName(dialect().datatypeName(column.getNativeTypeName()));
				}
				columns.add(column);
			}
		}

		try (ResultSet rs = metaData.getPrimaryKeys(connection.getConnection().getCatalog(), schemaOf(table), table.getName())) {
			while (rs.next()) {
				for (TableColumn column : columns) {
					if (column.getName().equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
						column.setPrimary(true);
						break;
					}
				}
			}
		}

		try (ResultSet rs = metaData.getImportedKeys(connection.getConnection().getCatalog(), schemaOf(table), table.getName())) {
			while (rs.next()) {
				for (TableColumn column : columns) {
					if (column.getName().equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
						column.setReferences(rs.getString("PKTABLE_NAME") + "." + rs.getString("PKCOLUMN_NAME"));
						break;
					}
				}
			}
		}

		return columns.toArray(new TableColumn[columns.size()]);
	}

	/** Loads the indexes of the table, its columns must be loaded first. */
	public TableIndex[] loadIndexes(Table table) throws SQLException {
		List<TableIndex> indexes = new ArrayList<>();
		useDatabaseOf(table);
		DatabaseMetaData metaData = connection.getConnection().getMetaData();
		String primaryKeyName = primaryKeyName(table);

		try (ResultSet rs = metaData.getIndexInfo(connection.getConnection().getCatalog(), schemaOf(table), table.getName(), false, false)) {
			while (rs.next()) {
				// Some drivers add a statistics row without an index name.
				String indexName = rs.getString("INDEX_NAME");
				if (indexName == null) {
					continue;
				}

				// The primary key is always shown as PRIMARY, whatever name the server gave it.
				String name = indexName.equals(primaryKeyName) ? "PRIMARY" : indexName;
				TableColumn column = table.getTableColumn(rs.getString("COLUMN_NAME"));

				// Some drivers report columns that are not in the column list.
				if (column == null) {
					continue;
				}

				TableIndex index = find(indexes, name);

				if (index == null) {
					index = new TableIndex(table);
					index.setName(name);
					index.setUnique(!rs.getBoolean("NON_UNIQUE"));
					index.setFulltext("FULLTEXT".equalsIgnoreCase(rs.getString("TYPE")));
					indexes.add(index);
				}

				column.setHasUniqueIndex(index.isUnique());
				column.setIndexed(true);
				column.setIndexPosition(rs.getInt("ORDINAL_POSITION"));
				index.addTableColumn(column);
			}
		}

		return indexes.toArray(new TableIndex[indexes.size()]);
	}

	private TableIndex find(List<TableIndex> indexes, String name) {
		for (TableIndex index : indexes) {
			if (index.getName().equals(name)) {
				return index;
			}
		}
		return null;
	}

	/** The name the server gave the primary key of the table, null when it has none. */
	private String primaryKeyName(Table table) throws SQLException {
		try (
			ResultSet rs = connection.getConnection().getMetaData().getPrimaryKeys(connection.getConnection().getCatalog(), schemaOf(table), table.getName())) {
			return rs.next() ? rs.getString("PK_NAME") : null;
		}
	}

	/** @param schema the schema to look in, null for the current one. */
	public boolean exists(Database database, Schema schema, String name) throws SQLException {
		useDatabase(database.getName());
		String schemaName = schema == null ? connection.getSchema() : schema.getName();

		try (ResultSet rs = connection.getConnection().getMetaData().getTables(connection.getConnection().getCatalog(), schemaName, name,
			new String[]{"TABLE"})) {
			return rs.next();
		}
	}

	public void create(TableDefinition table) throws Exception {
		useDatabase(table.database().getName());
		executeAll(createStatements(table));
	}

	/** The statements {@link #create} runs, without running them. */
	public List<String> createStatements(TableDefinition table) {
		return dialect().createTableSql(new TableName(table.schema() == null ? null : table.schema().getName(), table.name()), table.columns(), table.type(),
			table.comment());
	}

	/** The statements that give the table the name, type and comment, only for the parts that differ; a null type means the server has none. */
	public List<String> modifyStatements(Table table, String name, String type, String comment) {
		List<String> statements = new ArrayList<>();
		TableName current = table.qualifiedName();

		if (!table.getName().equalsIgnoreCase(name)) {
			statements.addAll(dialect().renameTableSql(current, name));
			current = current.sibling(name);
		}
		if (type != null && !type.equalsIgnoreCase(table.getType())) {
			statements.addAll(dialect().setTableTypeSql(current, type));
		}
		if (!comment.equals(table.getComment() == null ? "" : table.getComment())) {
			statements.addAll(dialect().setTableCommentSql(current, comment));
		}
		return statements;
	}

	public void run(Table table, List<String> statements) throws Exception {
		useDatabaseOf(table);
		executeAll(statements);
	}

	public void dropTable(Table table) throws Exception {
		useDatabaseOf(table);
		executeUpdate("DROP TABLE " + quote(table));
	}

	public void flushTable(Table table) throws Exception {
		useDatabaseOf(table);
		executeUpdate("DELETE FROM " + quote(table));
	}

	public void renameTable(Table table, String newName) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().renameTableSql(table.qualifiedName(), newName));
	}

	/** Creates a table with the structure of the source in the same schema, with its rows when asked. */
	public void copyTable(Table source, String newName, boolean withData) throws Exception {
		useDatabaseOf(source);
		TableName target = source.qualifiedName().sibling(newName);
		executeAll(dialect().copyTableSql(source.qualifiedName(), target, withData));

		if (withData) {
			String autoNumbered = dialect().autoNumberedColumnsSql();
			List<String> columns = autoNumbered == null ? List.of() : queryStrings(autoNumbered, target.schema(), target.name());

			for (String statement : dialect().afterDataLoadSql(target, columns)) {
				connection.execute(statement);
			}
		}
	}

	/** The exact number of rows. */
	public long countRows(Table table) throws SQLException {
		useDatabaseOf(table);

		try (ResultSet rs = connection.executeQuery("SELECT count(*) FROM " + quote(table))) {
			return rs.next() ? rs.getLong(1) : 0;
		}
	}

	/** The size of the table with its indexes in bytes, null when the server cannot tell. */
	public Long sizeInBytes(Table table) throws SQLException {
		String sql = dialect().tableSizeSql();

		if (sql == null) {
			return null;
		}

		useDatabaseOf(table);
		List<String> size = queryStrings(sql, table.getSchema() == null ? null : table.getSchema().getName(), table.getName());
		return size.isEmpty() || size.getFirst() == null ? null : Long.valueOf(size.getFirst());
	}

	public void setTableType(Table table, String type) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().setTableTypeSql(table.qualifiedName(), type));
	}

	public void setTableComment(Table table, String comment) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().setTableCommentSql(table.qualifiedName(), comment));
	}

	public String maintain(Table table, Dialect.Maintenance maintenance) throws Exception {
		useDatabaseOf(table);
		MaintenanceStatement statement = dialect().maintenanceSql(maintenance, table.qualifiedName());

		if (statement.resultColumn() == null) {
			executeUpdate(statement.sql());
			return statement.message();
		}

		try (ResultSet rs = connection.executeQuery(statement.sql())) {
			return rs.first() ? rs.getString(statement.resultColumn()) : statement.message();
		}
	}

	// Columns

	public void addColumn(Table table, ColumnDefinition column) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().addColumnSql(table.qualifiedName(), column));
	}

	public void modifyColumn(TableColumn previousColumn, ColumnDefinition column) throws Exception {
		useDatabaseOf(previousColumn.getTable());
		executeAll(dialect().modifyColumnSql(previousColumn.getTable().qualifiedName(), previousColumn.getName(), column));
	}

	public void dropColumn(TableColumn column) throws Exception {
		useDatabaseOf(column.getTable());
		executeUpdate("ALTER TABLE " + quote(column.getTable()) + " DROP " + quote(column.getName()));
	}

	// Indexes

	public void addIndex(Table table, String name, String type, List<String> columns) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().addIndexSql(table.qualifiedName(), name, type, columns));
	}

	public void modifyIndex(Table table, String name, String type, List<String> columns) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().modifyIndexSql(table.qualifiedName(), name, primaryKeyName(table), type, columns));
	}

	public void dropIndex(Table table, String name) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().dropIndexSql(table.qualifiedName(), name, primaryKeyName(table)));
	}

	// Foreign keys

	public void addForeignKey(Table table, TableForeignKey key) throws Exception {
		useDatabaseOf(table);
		String typeSql = dialect().tableTypeSql();
		String tableType = typeSql == null
			? null
			: queryStrings(typeSql, table.getSchema() == null ? null : table.getSchema().getName(), table.getName()).stream().findFirst().orElse(null);
		dialect().checkForeignKeyTable(table.qualifiedName(), tableType);
		executeAll(dialect().addForeignKeySql(table.qualifiedName(), key));
	}

	public void dropForeignKey(Table table, String name) throws Exception {
		useDatabaseOf(table);
		executeAll(dialect().dropForeignKeySql(table.qualifiedName(), name));
	}

	/** The names of the foreign keys the table has on other tables. */
	public List<String> loadForeignKeyNames(Table table) throws SQLException {
		useDatabaseOf(table);
		List<String> names = new ArrayList<>();

		try (ResultSet rs = connection.getConnection().getMetaData().getImportedKeys(connection.getConnection().getCatalog(), schemaOf(table),
			table.getName())) {
			while (rs.next()) {
				String name = rs.getString("FK_NAME");
				if (name != null && !names.contains(name)) {
					names.add(name);
				}
			}
		}
		return names;
	}

	// Rows

	/** Reads one page of rows and sets the row count and the column metadata on the table. The columns must be loaded first. */
	public TableCell[][] readPage(Table table, int skip, int show) throws Exception {
		useDatabaseOf(table);
		TableColumn[] columns = table.getColumns();
		String quotedTable = quote(table);

		try (ResultSet rs = connection.executeQuery("SELECT count(*) FROM " + quotedTable)) {
			if (rs.first()) {
				table.setRowCount(rs.getInt(1));
			}
		}

		// Let the server do the paging when the dialect can, otherwise skip rows in the result set.
		List<String> keyColumns = new ArrayList<>();
		for (TableColumn column : columns) {
			if (column.isPrimary()) {
				keyColumns.add(quote(column.getName()));
			}
		}
		String pageQuery = dialect().selectPage(quotedTable, String.join(", ", keyColumns), skip, show);

		try (ResultSet rs = pageQuery != null ? connection.executeQuery(pageQuery) : connection.executeQuery("SELECT * FROM " + quotedTable)) {
			if (pageQuery == null) {
				if (skip > 0) {
					rs.absolute(skip);
				} else {
					rs.beforeFirst();
				}
			}

			ResultSetMetaData rsmd = rs.getMetaData();

			for (int i = 0; i < columns.length; i++) {
				columns[i].setTypeName(rsmd.getColumnTypeName(i + 1));
				columns[i].setClassName(rsmd.getColumnClassName(i + 1));
				columns[i].setWritable(rsmd.isWritable(i + 1));
				columns[i].setAutoIncrement(rsmd.isAutoIncrement(i + 1));
				columns[i].setSigned(rsmd.isSigned(i + 1));
				columns[i].setType(rsmd.getColumnType(i + 1));
			}

			List<TableCell[]> rows = new ArrayList<>();

			while (rs.next()) {
				TableCell[] row = new TableCell[columns.length];

				for (int i = 0; i < row.length; i++) {
					row[i] = new TableCell();
					row[i].setTableColumn(columns[i]);

					if (!columns[i].isBinary()) {
						row[i].setData(rs.getObject(i + 1));
					}
				}

				rows.add(row);
			}

			return rows.toArray(new TableCell[rows.size()][]);
		}
	}

	public void insertRow(Table table, TableCell[] row) throws Exception {
		List<String> names = new ArrayList<>();
		List<String> values = new ArrayList<>();

		for (TableCell cell : row) {
			names.add(quote(cell.getTableColumn().getName()));
			values.add(sqlValue(cell));
		}

		useDatabaseOf(table);
		executeUpdate("INSERT INTO " + quote(table)
			+ " (" + String.join(",", names) + ") VALUES (" + String.join(",", values) + ")");
	}

	/** Sets one cell of the row, {@code newValue} null writes SQL NULL. */
	public int updateCell(Table table, TableCell[] row, TableCell cell, String newValue) throws Exception {
		String where = rowFilter(row);

		useDatabaseOf(table);
		return executeUpdate("UPDATE " + quote(table)
			+ " SET " + quote(cell.getTableColumn().getName()) + "=" + (newValue == null ? "NULL" : literal(newValue))
			+ " WHERE " + where);
	}

	public void deleteRow(Table table, TableCell[] row) throws Exception {
		String where = rowFilter(row);

		useDatabaseOf(table);
		executeUpdate("DELETE FROM " + quote(table) + " WHERE " + where);
	}

	/**
	 * The condition that selects the given row: its key columns when it has any,
	 * otherwise all of its columns that are not binary.
	 */
	public String rowFilter(TableCell[] row) throws Exception {
		List<String> keys = new ArrayList<>();
		List<String> columns = new ArrayList<>();

		for (TableCell cell : row) {
			TableColumn column = cell.getTableColumn();
			String name = quote(column.getName());

			if (column.isPrimary() || column.hasUniqueIndex()) {
				keys.add(name + "=" + sqlValue(cell));
			} else if (!column.isBinary()) {
				columns.add(cell.isNull() ? name + " IS NULL" : name + "=" + sqlValue(cell));
			}
		}

		List<String> conditions = keys.isEmpty() ? columns : keys;

		if (conditions.isEmpty()) {
			throw new EsqlException("The row can't be identified, it has no key and no column to compare.");
		}

		return String.join(" AND ", conditions);
	}

	// Free queries

	public int run(String sql) throws Exception {
		return executeUpdate(sql);
	}

	/** Runs a query and returns its result with one column object per result column. */
	public QueryResult query(String sql, boolean readOnly) throws Exception {
		try (ResultSet rs = connection.executeQuery(sql)) {
			return readResult(rs, readOnly);
		}
	}

	// Plumbing

	/** Every statement on a table starts here: the connection is shared with the query tabs, which may have switched to another database. */
	private void useDatabaseOf(Table table) throws SQLException {
		useDatabase(table.getDatabase().getName());
	}

	/** The value of a cell as it goes into a statement, an empty cell is NULL and not the text "null". */
	private String sqlValue(TableCell cell) {
		return cell.isNull() ? "NULL" : literal(cell.getData());
	}
}

package nl.errorsoft.esql.table;

import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.dialect.Dialect;

/** Application logic for tables, columns, indexes and rows. Controllers call this, it calls the repository. */
public class TableService {
	private final TableRepository repository;

	public TableService(TableRepository repository) {
		this.repository = repository;
	}

	// Structure

	public TableColumn[] loadColumns(Table table) throws Exception {
		table.setColumns(repository.loadColumns(table));
		return table.getColumns();
	}

	public TableIndex[] loadIndexes(Table table) throws Exception {
		if (table.getColumns() == null) {
			loadColumns(table);
		}

		table.setIndexes(repository.loadIndexes(table));
		return table.getIndexes();
	}

	/** Whether the table is in the current schema of the database. */
	public boolean exists(Database database, String name) throws Exception {
		return repository.exists(database, null, name);
	}

	public boolean exists(Schema schema, String name) throws Exception {
		return repository.exists(schema.getDatabase(), schema, name);
	}

	/** Creates the table, in the current schema of the database when the definition names no schema. */
	public void createTable(TableDefinition table) throws Exception {
		repository.create(table);
	}

	public void dropTable(Table table) throws Exception {
		repository.dropTable(table);
	}

	public void flushTable(Table table) throws Exception {
		repository.flushTable(table);
	}

	/** Gives the table another name, the table object is updated. @throws EsqlException when the name is empty or a table of that name exists */
	public void renameTable(Table table, String newName) throws Exception {
		checkNewName(table, newName);
		repository.renameTable(table, newName);
		table.setName(newName);
	}

	/** Copies the structure of the table to a new table in the same schema, with the rows when asked; returns the new table. */
	public Table duplicateTable(Table source, String newName, boolean withData) throws Exception {
		checkNewName(source, newName);
		repository.copyTable(source, newName, withData);

		Table copy = source.getSchema() != null ? new Table(source.getSchema()) : new Table(source.getDatabase());
		copy.setName(newName);
		copy.setType(source.getType());
		copy.setComment(source.getComment());
		copy.setRowCount(withData ? source.getRowCount() : 0);
		return copy;
	}

	/** The message for a name that cannot be used for a table next to the given one, null when it is fine. */
	public String newNameProblem(Table sibling, String newName) throws Exception {
		if (newName == null || newName.isBlank()) {
			return "Enter a name for the table.";
		}
		if (repository.exists(sibling.getDatabase(), sibling.getSchema(), newName.trim())) {
			return "A table named '" + newName.trim() + "' exists already.";
		}
		return null;
	}

	private void checkNewName(Table sibling, String newName) throws Exception {
		String problem = newNameProblem(sibling, newName);

		if (problem != null) {
			throw new EsqlException(problem);
		}
	}

	/** The facts of the Properties window. */
	public TableInfo describe(Table table) throws Exception {
		int columns = loadColumns(table).length;
		return new TableInfo(table.getName(), table.getDatabase().getName(), table.getSchema() == null ? null : table.getSchema().getName(), table.getType(),
			table.getComment(), repository.countRows(table), columns, repository.sizeInBytes(table));
	}

	/** The statements {@link #modifyTable} would run, for a preview. */
	public List<String> modifyStatements(Table table, String name, String type, String comment) {
		return repository.modifyStatements(table, name, type, comment);
	}

	/** The statements {@link #createTable} would run, for a preview. */
	public List<String> createStatements(TableDefinition table) {
		return repository.createStatements(table);
	}

	/** Applies the parts that changed; a null type means the database has no table types. */
	public void modifyTable(Table table, String name, String type, String comment) throws Exception {
		repository.run(table, repository.modifyStatements(table, name, type, comment));

		table.setName(name);
		if (type != null) {
			table.setType(type);
		}
		table.setComment(comment);
	}

	public String optimizeTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.OPTIMIZE);
	}

	public String analyzeTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.ANALYZE);
	}

	public String checkTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.CHECK);
	}

	public String repairTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.REPAIR);
	}

	// Columns

	public void addColumn(Table table, ColumnDefinition column) throws Exception {
		repository.addColumn(table, column);
	}

	/** Changes the column to the given definition; {@code column.primary} adds or drops the primary key. */
	public void editColumn(TableColumn previousColumn, ColumnDefinition column) throws Exception {
		repository.modifyColumn(previousColumn, column);

		if (previousColumn.isPrimary() && !column.primary) {
			repository.dropIndex(previousColumn.getTable(), "PRIMARY");
		}
		if (!previousColumn.isPrimary() && column.primary) {
			repository.addIndex(previousColumn.getTable(), "PRIMARY", "INDEX", List.of(column.name));
		}
	}

	public void dropColumn(TableColumn column) throws Exception {
		repository.dropColumn(column);
	}

	/** A column definition from the form of a dialog: options the type does not have are switched off. */
	public static ColumnDefinition newColumn(String name, String length, String defaultValue, DataType type, boolean auto, boolean unsigned, boolean nullable) {
		ColumnDefinition column = new ColumnDefinition(name);
		column.type = type;
		column.length = length;
		column.defaultValue = defaultValue;
		column.unsigned = type.allows(DataType.Option.UNSIGNED) && unsigned;
		column.notNull = type.allows(DataType.Option.NOT_NULL) && !nullable;
		column.autoIncrement = auto;
		return column;
	}

	// Indexes

	public void addIndex(Table table, TableIndex index, TableColumn[] columns, String type) throws Exception {
		if (columns == null || columns.length <= 0) {
			return;
		}

		repository.addIndex(table, index.getName(), indexType(type), columnNames(columns));
		loadIndexes(table);
	}

	public void modifyIndex(Table table, TableIndex index, TableColumn[] columns, String type) throws Exception {
		if (columns == null || columns.length <= 0) {
			return;
		}

		repository.modifyIndex(table, index.getName(), indexType(type), columnNames(columns));
		loadIndexes(table);
	}

	public void dropIndex(Table table, TableIndex index) throws Exception {
		repository.dropIndex(table, index.getName());
		loadIndexes(table);
	}

	private String indexType(String type) {
		return type == null || type.length() <= 0 ? "INDEX" : type;
	}

	private List<String> columnNames(TableColumn[] columns) {
		List<String> names = new ArrayList<>();

		for (TableColumn column : columns) {
			names.add(column.getName());
		}

		return names;
	}

	// Foreign keys

	public void addForeignKey(Table table, TableForeignKey key) throws Exception {
		if (key.columns().isEmpty() || key.columns().size() != key.referencedColumns().size()) {
			throw new EsqlException("Foreign key " + key.name() + " needs as many referenced columns as columns, and at least one.");
		}

		repository.addForeignKey(table, key);
	}

	public void dropForeignKey(Table table, String name) throws Exception {
		repository.dropForeignKey(table, name);
	}

	public List<String> foreignKeyNames(Table table) throws Exception {
		return repository.loadForeignKeyNames(table);
	}

	// Rows

	/** Loads the columns, indexes and row count of the table and returns one page of its rows. */
	public TableCell[][] loadPage(Table table, int skip, int show) throws Exception {
		loadColumns(table);
		loadIndexes(table);
		return repository.readPage(table, skip, show);
	}

	public void insertRow(Table table, TableCell[] row) throws Exception {
		repository.insertRow(table, row);
		table.setRowCount(table.getRowCount() + 1);
	}

	/**
	 * Writes the text typed into a cell; an empty text is SQL NULL.
	 * Returns the number of changed rows, zero when the value is the same.
	 */
	public int changeCell(Table table, TableCell[] row, TableCell cell, Object newValue) throws Exception {
		String text = newValue == null ? "" : newValue.toString();
		boolean unchanged = cell.isNull() ? text.isEmpty() : cell.getData().equals(text);
		if (unchanged) {
			return 0;
		} else if (cell.getTableColumn().isBinary()) {
			throw new EsqlException("Binary data cannot be edited in the grid, use Upload instead.");
		}

		return repository.updateCell(table, row, cell, text.isEmpty() ? null : text);
	}

	public void deleteRow(Table table, TableCell[] row) throws Exception {
		repository.deleteRow(table, row);
		table.setRowCount(table.getRowCount() - 1);
	}

	/** The condition that selects the given row, for statements that handle a single row. */
	public String rowFilter(TableCell[] row) throws Exception {
		return repository.rowFilter(row);
	}

	// Free queries

	public void executeUpdate(String sql) throws Exception {
		repository.run(sql);
	}

	public QueryResult executeQuery(String sql) throws Exception {
		return repository.query(sql, false);
	}

	/** A query whose result is shown but cannot be edited, such as server status. */
	public QueryResult runCommand(String sql) throws Exception {
		return repository.query(sql, true);
	}
}

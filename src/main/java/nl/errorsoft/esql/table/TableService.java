package nl.errorsoft.esql.table;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;

import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.domain.CreateColumn;
import nl.errorsoft.esql.domain.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.domain.dialect.Dialect;

/** Application logic for tables, columns, indexes and rows. Controllers call this, it calls the repository. */
public class TableService {
	private final TableRepository repository;

	public TableService(DatabaseConnection dbc) {
		this.repository = new TableRepository(dbc);
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

	public boolean exists(Database database, String name) throws Exception {
		return repository.exists(database, name);
	}

	public void createTable(Database database, String name, List<CreateColumn> columns, String type, String comment) throws Exception {
		repository.create(database, name, columns, type, comment);
	}

	public void dropTable(Table table) throws Exception {
		repository.dropTable(table);
	}

	public void flushTable(Table table) throws Exception {
		repository.flushTable(table);
	}

	/** Applies the parts that changed; a null type means the database has no table types. */
	public void modifyTable(Table table, String name, String type, String comment) throws Exception {
		if (!table.getName().equalsIgnoreCase(name)) {
			repository.renameTable(table, name);
			table.setName(name);
		}
		if (type != null && !type.equalsIgnoreCase(table.getType())) {
			repository.setTableType(table, type);
			table.setType(type);
		}
		if (!comment.equals(table.getComment() == null ? "" : table.getComment())) {
			repository.setTableComment(table, comment);
			table.setComment(comment);
		}
	}

	public String optimizeTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.OPTIMIZE);
	}

	public String analyseTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.ANALYZE);
	}

	public String checkTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.CHECK);
	}

	public String repairTable(Table table) throws Exception {
		return repository.maintain(table, Dialect.Maintenance.REPAIR);
	}

	// Columns

	public void addColumn(Table table, String name, String length, String defaultValue, DataType type, boolean primary, boolean auto, boolean unsigned,
		boolean nullable) throws Exception {
		CreateColumn column = createColumn(name, length, defaultValue, type, auto, unsigned, nullable);
		column.primary = primary;

		repository.addColumn(table, column);
	}

	public void editColumn(TableColumn old, String name, String length, String defaultValue, DataType type, boolean primary, boolean auto, boolean unsigned,
		boolean nullable) throws Exception {
		repository.modifyColumn(old, createColumn(name, length, defaultValue, type, auto, unsigned, nullable));

		if (old.isPrimary() && !primary) {
			repository.dropIndex(old.getTable(), "PRIMARY");
		}
		if (!old.isPrimary() && primary) {
			repository.addIndex(old.getTable(), "PRIMARY", "INDEX", List.of(name));
		}
	}

	public void dropColumn(TableColumn column) throws Exception {
		repository.dropColumn(column);
	}

	private CreateColumn createColumn(String name, String length, String defaultValue, DataType type, boolean auto, boolean unsigned, boolean nullable) {
		CreateColumn column = new CreateColumn(name);
		column.type = type;
		column.length = length;
		column.defaultval = defaultValue;
		column.unsigned = type.unsigned && unsigned;
		column.notnull = type.notnull && !nullable;
		column.autoincrement = auto;
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
		List<String> names = new ArrayList<String>();

		for (TableColumn column : columns) {
			names.add(column.getName());
		}

		return names;
	}

	// Rows

	/** Loads the columns, indexes and row count of the table and returns one page of its rows. */
	public TableData[][] loadPage(Table table, int skip, int show) throws Exception {
		loadColumns(table);
		loadIndexes(table);
		return repository.readPage(table, skip, show);
	}

	public void insertRow(Table table, TableData[] row) throws Exception {
		repository.insertRow(table, row);
		table.setRowCount(table.getRowCount() + 1);
	}

	/** Returns the number of changed rows, zero when the value is the same. */
	public int changeCell(Table table, TableData[] row, TableData cell, Object newValue) throws Exception {
		if (cell.getData().equals(newValue.toString())) {
			return 0;
		} else if (cell.getTableColumn().isBinary()) {
			throw new Exception("Editing of binary data is not supported yet!");
		}

		return repository.updateCell(table, row, cell, newValue.toString());
	}

	public void deleteRow(Table table, TableData[] row) throws Exception {
		repository.deleteRow(table, row);
		table.setRowCount(table.getRowCount() - 1);
	}

	/** The condition that selects the given row, for statements that handle a single row. */
	public String rowFilter(TableData[] row) throws Exception {
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

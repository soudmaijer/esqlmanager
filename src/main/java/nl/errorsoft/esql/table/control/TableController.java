package nl.errorsoft.esql.table.control;

import java.util.List;
import java.util.function.IntConsumer;

import nl.errorsoft.esql.table.ColumnDefinition;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableCell;
import nl.errorsoft.esql.table.ui.TableDataTab;

import nl.errorsoft.esql.blob.control.BlobTransferController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;

import nl.errorsoft.esql.table.QueryResult;
import nl.errorsoft.esql.table.TableService;

public class TableController {
	private TableDataTab tableDataTab;
	private ConnectionWindowController connectionWindowController;

	public TableController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	public void showDownloadFileDialog(Table table, TableCell[] rowData, TableCell cellData) {
		BlobTransferController blobTransferController = new BlobTransferController(connectionWindowController);
		blobTransferController.showDownloadDialog(connectionWindowController.getMainWindow(), table, rowData, cellData);
	}

	public void showUploadFileDialog(Table table, TableCell[] rowData, TableCell cellData) {
		BlobTransferController blobTransferController = new BlobTransferController(connectionWindowController);
		blobTransferController.showUploadDialog(connectionWindowController.getMainWindow(), table, rowData, cellData);
	}

	/** Reads a page of rows; database work, not for the event thread. */
	public TableCell[][] loadPage(Table table, int skip, int show) throws Exception {
		return service().loadPage(table, skip, show);
	}

	/** The tab with the rows of a page read by {@link #loadPage}, on the event thread. */
	public TableDataTab newTableDataTab(Table table, TableCell[][] rows) {
		tableDataTab = new TableDataTab(this);
		tableDataTab.loadData(table, table.getColumns(), rows);
		tableDataTab.setRowEditing(true);
		return tableDataTab;
	}

	public TableColumn[] getColumns(Table table) throws Exception {
		return service().loadColumns(table);
	}

	/** Reads another page in the background and shows it in the tab. */
	public void showTableData(Table table, int skip, int show) {
		TableDataTab tab = tableDataTab;
		connectionWindowController.inBackground("Show data", "Loading table data...", () -> loadPage(table, skip, show),
			rows -> tab.loadData(table, table.getColumns(), rows));
	}

	public TableDataTab executeQuery(String query) throws Exception {
		return show(service().executeQuery(query));
	}

	public void dropTableColumn(TableColumn tableColumn) throws Exception {
		service().dropColumn(tableColumn);
	}

	public void dropTable(Table table) throws Exception {
		service().dropTable(table);
	}

	public void flushTable(Table table) throws Exception {
		service().flushTable(table);
	}

	public void executeUpdate(String query) throws Exception {
		service().executeUpdate(query);
	}

	public void addTableColumn(Table table, ColumnDefinition column) throws Exception {
		service().addColumn(table, column);
	}

	public void editTableColumn(TableColumn tableColumn, ColumnDefinition column) throws Exception {
		service().editColumn(tableColumn, column);
	}

	/** Inserts the row off the event thread, then {@code saved} on it; {@code always} runs on the event thread in any case. */
	public void insertRow(Table table, TableCell[] rowData, Runnable saved, Runnable always) {
		connectionWindowController.inBackground("Save row", "Saving row...", () -> {
			service().insertRow(table, rowData);
			return rowData;
		}, row -> saved.run(), always);
	}

	/** Writes the new value of a cell off the event thread, then {@code saved} on it; {@code always} runs on the event thread in any case. */
	public void changeCell(Table table, TableCell[] rowData, TableCell cellData, Object newValue, Runnable saved, Runnable always) {
		connectionWindowController.inBackground("Change cell", "Saving cell...", () -> {
			service().changeCell(table, rowData, cellData, newValue);
			return cellData;
		}, cell -> saved.run(), always);
	}

	/** How many rows were deleted, in order, and what stopped the rest (null when all were). */
	private record RowsDeleted(int count, Exception failure) {
	}

	/**
	 * Deletes the rows in order off the event thread and stops at the first that fails. {@code deleted} gets on the event thread how many were deleted,
	 * then a failure is reported; {@code always} runs on the event thread in any case.
	 */
	public void deleteRows(Table table, List<TableCell[]> rows, IntConsumer deleted, Runnable always) {
		connectionWindowController.inBackground("Delete rows", "Deleting rows...", () -> {
			int count = 0;

			for (TableCell[] row : rows) {
				try {
					service().deleteRow(table, row);
				} catch (Exception e) {
					return new RowsDeleted(count, e);
				}
				count++;
			}
			return new RowsDeleted(count, null);
		}, result -> {
			deleted.accept(result.count());

			if (result.failure() != null) {
				throw result.failure();
			}
		}, always);
	}

	/*
	 * Server options, the query behind them depends on the database.
	 */
	public TableDataTab showServerStatus() throws Exception {
		return show(connectionWindowController.getContext().servers().getStatus());
	}

	public TableDataTab showServerVariables() throws Exception {
		return show(connectionWindowController.getContext().servers().getVariables());
	}

	public String optimizeTable(Table table) throws Exception {
		return service().optimizeTable(table);
	}

	public String analyzeTable(Table table) throws Exception {
		return service().analyzeTable(table);
	}

	public String checkTable(Table table) throws Exception {
		return service().checkTable(table);
	}

	public String repairTable(Table table) throws Exception {
		return service().repairTable(table);
	}

	private TableDataTab show(QueryResult result) throws Exception {
		TableDataTab view = new TableDataTab(this);
		view.loadData(result.table(), result.table().getColumns(), result.rows());
		view.setRowEditing(false);
		return view;
	}

	private TableService service() throws Exception {
		return connectionWindowController.getContext().tables();
	}
}

package nl.errorsoft.esql.table.control;

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

	public void dropTableColumn(TableColumn tb) throws Exception {
		service().dropColumn(tb);
	}

	public void dropTable(Table tb) throws Exception {
		service().dropTable(tb);
	}

	public void flushTable(Table tb) throws Exception {
		service().flushTable(tb);
	}

	public void executeUpdate(String query) throws Exception {
		service().executeUpdate(query);
	}

	public void addTableColumn(Table tb, ColumnDefinition column) throws Exception {
		service().addColumn(tb, column);
	}

	public void editTableColumn(TableColumn tbc, ColumnDefinition column) throws Exception {
		service().editColumn(tbc, column);
	}

	public void insertNewRow() {
		tableDataTab.insertNewRow();
	}

	public void deleteSelectedRows() {
		tableDataTab.deleteSelectedRows();
	}

	public void saveSelectedRow() {
		tableDataTab.saveSelectedRow();
	}

	public void insertRow(Table table, TableCell[] rowData) throws Exception {
		service().insertRow(table, rowData);
	}

	public void dataChanged(Table table, TableCell[] rowData, TableCell cellData, Object newValue) throws Exception {
		service().changeCell(table, rowData, cellData, newValue);
	}

	public void deleteRow(Table table, TableCell[] rowData) throws Exception {
		service().deleteRow(table, rowData);
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
		return view;
	}

	private TableService service() throws Exception {
		return connectionWindowController.getContext().tables();
	}
}

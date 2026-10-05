package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.ui.TableDataView;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.server.ServerService;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.blob.control.UDDataCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.QueryResult;
import nl.errorsoft.esql.table.TableService;
import java.util.Vector;

public class TableCC {
	private TableDataView tdv;
	private ConnectionWindowCC cwcc;

	public TableCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public void dispatchDownloadFileUI(Table table, TableData[] rowData, TableData cellData) {
		UDDataCC udcc = new UDDataCC(cwcc);
		udcc.startDownloadUI(cwcc.getUI(), table, rowData, cellData);
	}

	public void dispatchUploadFileUI(Table table, TableData[] rowData, TableData cellData) {
		UDDataCC udcc = new UDDataCC(cwcc);
		udcc.startUploadUI(cwcc.getUI(), table, rowData, cellData);
	}

	public TableDataView getTableDataView(Table table, int skip, int show) throws Exception {
		TableData[][] tdata = service().loadPage(table, skip, show);
		tdv = new TableDataView(this);
		tdv.loadData(table, table.getColumns(), tdata);
		return tdv;
	}

	public TableColumn[] getColumns(Table table) throws Exception {
		return service().loadColumns(table);
	}

	public void showTableData(Table table, int skip, int show) throws Exception {
		TableData[][] tdata = service().loadPage(table, skip, show);
		tdv.loadData(table, table.getColumns(), tdata);
	}

	public TableDataView executeQuery(String query) throws Exception {
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

	public void addTableColumn(Table tb, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned,
		boolean nullable) throws Exception {
		service().addColumn(tb, name, length, defaultValue, dt, primary, auto, unsigned, nullable);
	}

	public void editTableColumn(TableColumn tbc, String name, String length, String defaultValue, DataType dt, boolean primary, boolean auto, boolean unsigned,
		boolean nullable) throws Exception {
		service().editColumn(tbc, name, length, defaultValue, dt, primary, auto, unsigned, nullable);
	}

	public void insertNewRow() {
		tdv.insertNewRow();
	}

	public void deleteSelectedRows() {
		tdv.deleteSelectedRows();
	}

	public void saveSelectedRow() {
		tdv.saveSelectedRow();
	}

	public void insertRow(Table table, TableData[] rowData) throws Exception {
		service().insertRow(table, rowData);
	}

	public void dataChanged(Table table, TableData[] rowData, TableData cellData, Object newValue) throws Exception {
		service().changeCell(table, rowData, cellData, newValue);
	}

	public void deleteRow(Table table, TableData[] rowData) throws Exception {
		service().deleteRow(table, rowData);
	}

	/*
	 * Server options, the query behind them depends on the database.
	 */
	public TableDataView showServerStatus() throws Exception {
		return show(cwcc.getContext().servers().getStatus());
	}

	public TableDataView showServerVariables() throws Exception {
		return show(cwcc.getContext().servers().getVariables());
	}

	public String optimizeTable(Table table) throws Exception {
		return service().optimizeTable(table);
	}

	public String analyseTable(Table table) throws Exception {
		return service().analyseTable(table);
	}

	public String checkTable(Table table) throws Exception {
		return service().checkTable(table);
	}

	public String repairTable(Table table) throws Exception {
		return service().repairTable(table);
	}

	private TableDataView show(QueryResult result) throws Exception {
		TableDataView view = new TableDataView(this);
		view.loadData(result.table(), result.table().getColumns(), result.rows());
		return view;
	}

	private TableService service() throws Exception {
		return cwcc.getContext().tables();
	}
}

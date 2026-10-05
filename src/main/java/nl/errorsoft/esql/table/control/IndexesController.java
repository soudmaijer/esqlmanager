package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.ui.IndexesTab;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.TableService;

public class IndexesController {

	private ConnectionWindowController connectionWindowController;
	private Table table;
	private IndexesTab indexesTab;

	public IndexesController(ConnectionWindowController connectionWindowController, Table table) {
		this.connectionWindowController = connectionWindowController;
		this.table = table;
	}

	/** Opens the indexes of the table in a tab ("Indexes orders"), or puts its open tab in front. The columns and indexes load in the background. */
	public void showTab() {
		String key = "indexes:" + table.getDatabase().getName() + "." + table.getName();

		if (!connectionWindowController.requireFeature(Dialect.Feature.INDEXES, "The index manager")
			|| connectionWindowController.getWindow().selectEditorTab(key)) {
			return;
		}

		String title = "Indexes " + table.getName();
		connectionWindowController.inBackground("Load indexes", "Loading indexes...", () -> {
			service().loadColumns(table);
			return service().loadIndexes(table);
		}, indexes -> {
			// Opened twice while loading: the first tab stays.
			if (connectionWindowController.getWindow().selectEditorTab(key)) {
				return;
			}
			indexesTab = new IndexesTab(this, title, connectionWindowController.dialect().indexTypes());
			indexesTab.loadIndexes(table.getIndexes());
			connectionWindowController.getWindow().showEditorTab(key, title, indexesTab);
		});
	}

	/** Close: closes the tab, asking first when something has not been saved. */
	public void close() {
		connectionWindowController.getWindow().closeTab(indexesTab);
	}

	public void addIndex(TableIndex index, TableColumn[] columns, String type) {
		try {
			if (columns.length <= 0) {
				Dialogs.error(connectionWindowController.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				service().addIndex(table, index, columns, type);
				indexesTab.loadIndexes(table.getIndexes());
				connectionWindowController.tableSelected(table, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Add index", e);
		}
	}

	public void modifyIndex(TableIndex index, TableColumn[] columns, String type) {
		try {
			if (columns.length <= 0) {
				Dialogs.error(connectionWindowController.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				if (index.isNew()) {
					service().addIndex(table, index, columns, type);
				} else {
					service().modifyIndex(table, index, columns, type);
				}

				indexesTab.loadIndexes(table.getIndexes());
				connectionWindowController.tableSelected(table, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Modify index", e);
		}
	}

	public void dropIndex(TableIndex index) {
		try {
			service().dropIndex(table, index);
			indexesTab.loadIndexes(table.getIndexes());
			connectionWindowController.tableSelected(table, true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Drop index", e);
		}
	}

	private TableService service() throws Exception {
		return connectionWindowController.getContext().tables();
	}

	public void addNew(String name) {
		TableIndex index = new TableIndex(table);
		index.setName(name);
		index.setNew(true);
		indexesTab.addNewIndex(index, table.getColumns());
	}

	public void addPrimary() {
		TableIndex index = new TableIndex(table);
		index.setName("PRIMARY");
		index.setNew(true);
		indexesTab.addNewIndex(index, table.getColumns());
	}
}

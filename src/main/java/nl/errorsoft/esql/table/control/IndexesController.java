package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.ui.IndexesTab;

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
		change("Add index", index, columns, () -> service().addIndex(table, index, columns, type));
	}

	public void modifyIndex(TableIndex index, TableColumn[] columns, String type) {
		change("Modify index", index, columns, () -> {
			if (index.isNew()) {
				service().addIndex(table, index, columns, type);
			} else {
				service().modifyIndex(table, index, columns, type);
			}
		});
	}

	public void dropIndex(TableIndex index) {
		run("Drop index", "Dropping index...", () -> service().dropIndex(table, index));
	}

	/** A change of an index (the service refuses one without columns). */
	private void change(String action, TableIndex index, TableColumn[] columns, Change change) {
		run(action, "Saving index...", change);
	}

	/** Runs the change in the background with the buttons of the tab disabled, then shows the indexes as they are now. */
	private void run(String action, String status, Change change) {
		indexesTab.setBusy(true);
		connectionWindowController.inBackground(action, status, () -> {
			change.run();
			return new Changed(table.getIndexes(), service().loadColumns(table));
		}, changed -> {
			indexesTab.loadIndexes(changed.indexes());
			connectionWindowController.showTableColumns(table, changed.columns());
		}, () -> indexesTab.setBusy(false));
	}

	/** The indexes after a change, and the columns for the tree. */
	private record Changed(TableIndex[] indexes, TableColumn[] columns) {
	}

	/** Database work that changes the indexes of the table. */
	private interface Change {
		void run() throws Exception;
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

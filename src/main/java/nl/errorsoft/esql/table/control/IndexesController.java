//Source file: d:\\roseoutput\\esql\\esql\\table\\TableController.java

package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.ui.IndexesTab;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.TableService;

public class IndexesController {
	private static final Logger log = LogManager.getLogger(IndexesController.class);

	private ConnectionWindowController connectionWindowController;
	private Table t;
	private IndexesTab indexesTab;

	public IndexesController(ConnectionWindowController connectionWindowController, Table t) {
		this.connectionWindowController = connectionWindowController;
		this.t = t;
	}

	/** Opens the indexes of the table in a tab ("Indexes orders"), or puts its open tab in front. The columns and indexes load in the background. */
	public void showTab() {
		String key = "indexes:" + t.getDatabase().getName() + "." + t.getName();

		if (!connectionWindowController.requireFeature(Dialect.Feature.INDEXES, "The index manager")
			|| connectionWindowController.getWindow().selectEditorTab(key)) {
			return;
		}

		String title = "Indexes " + t.getName();
		connectionWindowController.inBackground("Load indexes", "Loading indexes...", () -> {
			service().loadColumns(t);
			return service().loadIndexes(t);
		}, indexes -> {
			// Opened twice while loading: the first tab stays.
			if (connectionWindowController.getWindow().selectEditorTab(key)) {
				return;
			}
			indexesTab = new IndexesTab(this, title, connectionWindowController.getConnectionProfile().getServerType().getDialect().indexTypes());
			indexesTab.loadIndexes(t.getIndexes());
			connectionWindowController.getWindow().showEditorTab(key, title, indexesTab);
		});
	}

	/** Close: closes the tab, asking first when something has not been saved. */
	public void close() {
		connectionWindowController.getWindow().closeTab(indexesTab);
	}

	public void addIndex(TableIndex ti, TableColumn[] tc, String type) {
		try {
			if (tc.length <= 0) {
				Dialogs.error(connectionWindowController.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				service().addIndex(t, ti, tc, type);
				indexesTab.loadIndexes(t.getIndexes());
				connectionWindowController.tableSelected(t, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Add index", e);
		}
	}

	public void modifyIndex(TableIndex ti, TableColumn[] tc, String type) {
		try {
			if (tc.length <= 0) {
				Dialogs.error(connectionWindowController.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				if (ti.isNew()) {
					service().addIndex(t, ti, tc, type);
				} else {
					service().modifyIndex(t, ti, tc, type);
				}

				indexesTab.loadIndexes(t.getIndexes());
				connectionWindowController.tableSelected(t, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Modify index", e);
		}
	}

	public void dropIndex(TableIndex ti) {
		try {
			service().dropIndex(t, ti);
			indexesTab.loadIndexes(t.getIndexes());
			connectionWindowController.tableSelected(t, true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(connectionWindowController.getWindow(), "Drop index", e);
		}
	}

	private TableService service() throws Exception {
		return connectionWindowController.getContext().tables();
	}

	public void addNew(String name) {
		TableIndex ti = new TableIndex(t);
		ti.setName(name);
		ti.setNew(true);
		indexesTab.addNewIndex(ti, t.getColumns());
	}

	public void addPrimary() {
		TableIndex ti = new TableIndex(t);
		ti.setName("PRIMARY");
		ti.setNew(true);
		indexesTab.addNewIndex(ti, t.getColumns());
	}
}

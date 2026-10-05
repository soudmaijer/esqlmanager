//Source file: d:\\roseoutput\\esql\\esql\\table\\TableCC.java

package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.ui.IndexesUI;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.TableService;

public class IndexesCC {
	private static final Logger log = LogManager.getLogger(IndexesCC.class);

	private ConnectionWindowCC cwcc;
	private Table t;
	private IndexesUI iu;

	public IndexesCC(ConnectionWindowCC cwcc, Table t) {
		this.cwcc = cwcc;
		this.t = t;
	}

	/** Opens the indexes of the table in a tab ("Indexes orders"), or puts its open tab in front. */
	public void startUI() throws Exception {
		String key = "indexes:" + t.getDatabase().getName() + "." + t.getName();

		if (!cwcc.requireFeature(Dialect.Feature.INDEXES, "The index manager") || cwcc.getWindow().selectEditorTab(key)) {
			return;
		}

		String title = "Indexes " + t.getName();
		iu = new IndexesUI(this, title, cwcc.getConnectionProfile().getServerType().getDialect().indexTypes());
		service().loadColumns(t);
		service().loadIndexes(t);
		iu.loadIndexes(t.getIndexes());
		cwcc.getWindow().showEditorTab(key, title, iu);
	}

	/** Close: closes the tab, asking first when something has not been saved. */
	public void close() {
		cwcc.getWindow().closeTab(iu);
	}

	public void addIndex(TableIndex ti, TableColumn[] tc, String type) {
		try {
			if (tc.length <= 0) {
				Dialogs.error(cwcc.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				service().addIndex(t, ti, tc, type);
				iu.loadIndexes(t.getIndexes());
				cwcc.tableSelected(t, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwcc.getWindow(), "Add index", e);
		}
	}

	public void modifyIndex(TableIndex ti, TableColumn[] tc, String type) {
		try {
			if (tc.length <= 0) {
				Dialogs.error(cwcc.getWindow(), "Indexes", "Select at least one column for the index.");
			} else {
				if (ti.isNew()) {
					service().addIndex(t, ti, tc, type);
				} else {
					service().modifyIndex(t, ti, tc, type);
				}

				iu.loadIndexes(t.getIndexes());
				cwcc.tableSelected(t, true);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwcc.getWindow(), "Modify index", e);
		}
	}

	public void dropIndex(TableIndex ti) {
		try {
			service().dropIndex(t, ti);
			iu.loadIndexes(t.getIndexes());
			cwcc.tableSelected(t, true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cwcc.getWindow(), "Drop index", e);
		}
	}

	private TableService service() throws Exception {
		return cwcc.getContext().tables();
	}

	public void addNew(String name) {
		TableIndex ti = new TableIndex(t);
		ti.setName(name);
		ti.setNew(true);
		iu.addNewIndex(ti, t.getColumns());
	}

	public void addPrimary() {
		TableIndex ti = new TableIndex(t);
		ti.setName("PRIMARY");
		ti.setNew(true);
		iu.addNewIndex(ti, t.getColumns());
	}
}

package nl.errorsoft.esql.importexport.control;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.importexport.ExportOptions;
import nl.errorsoft.esql.importexport.ExportService;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.importexport.ui.ExportAsSQLUI;
import nl.errorsoft.esql.importexport.ui.ExportSelectionUI;
import nl.errorsoft.esql.importexport.ui.ImportExportProgressUI;
import nl.errorsoft.esql.ui.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.domain.*;
import javax.swing.tree.*;

public class ExportCC implements ProgressListener {
	private static final Logger log = LogManager.getLogger(ExportCC.class);

	private ESQLManagerCC ecc;
	private ConnectionWindowCC cwcc;
	private ImportExportProgressUI ies;

	public ExportCC(ESQLManagerCC ecc) {
		this.ecc = ecc;
	}

	/*
	 * @description: starts the export selection ui
	 */
	public void startExportSelectionUI(ConnectionWindowCC cwcc) {
		if (!cwcc.requireFeature(Dialect.Feature.EXPORT, "Export")) {
			return;
		}

		this.cwcc = cwcc;
		new ExportSelectionUI(this, ecc.getUI()).setVisible(true);
	}

	/*
	 * @description: starts the export ui for the option: Export data as SQL statements
	 */
	public void startExportSQLUI() {
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			ExportAsSQLUI iasu = new ExportAsSQLUI(ecc.getUI(), this);
			iasu.showDatabaseTreeView(dbcc.getDatabaseTreeView());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Export sql", e);
		}
	}

	public void showTables(ExportAsSQLUI iasu, Database db) {
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			iasu.getDatabaseTreeView().loadTables(db, dbcc.getTables(db));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Tables", e);
		}
	}

	public void exportNodesAsSQL(ExportAsSQLUI iasu, TreePath[] tpa, String file, boolean dumpStructure, boolean dumpData, boolean createDatabase,
		boolean dropTable, boolean useDatabase) {
		// open progress window...
		ies = new ImportExportProgressUI(iasu);

		// start export...
		try {
			Object[] export = new Object[tpa.length];

			for (int i = 0; i < tpa.length; i++) {
				export[i] = ((DefaultMutableTreeNode) tpa[i].getLastPathComponent()).getUserObject();
			}

			ExportService exp = cwcc.getContext().newExport(export, file, new ExportOptions(dumpStructure, dumpData, createDatabase, dropTable, useDatabase));
			exp.setListener(this);
			exp.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(iasu, "Export nodes as sql", e);
		}
	}

	@Override
	public void progressed(int percent) {
		ies.setProgressValue(percent);
	}

	@Override
	public void failed(Exception error) {
		ApplicationContext.get().errors().report(ies, "Export", error);
		ies.dispose();
	}

	/*
	 * @description: starts the export ui for the option: Export data as CSV comma-seperated
	 */
	public void startExportCSVUI() {
	}
}

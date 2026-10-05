package nl.errorsoft.esql.importer.control;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.importer.ImportService;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.importer.ui.ImportAsSQLUI;
import nl.errorsoft.esql.job.ui.ImportExportProgressUI;
import nl.errorsoft.esql.importer.ui.ImportSelectionUI;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ApplicationContext;
import javax.swing.tree.*;

public class ImportCC implements ProgressListener {
	private static final Logger log = LogManager.getLogger(ImportCC.class);

	private ESQLManagerCC ecc;
	private ConnectionWindowCC cwcc;
	private ImportExportProgressUI ies;
	private ImportAsSQLUI iasu;

	public ImportCC(ESQLManagerCC ecc) {
		this.ecc = ecc;
	}

	/*
	 * @description: starts the Import selection ui
	 */
	public void startImportSelectionUI(ConnectionWindowCC cwcc) {
		if (!cwcc.requireFeature(Dialect.Feature.IMPORT, "Import")) {
			return;
		}

		this.cwcc = cwcc;
		new ImportSelectionUI(this, ecc.getUI()).setVisible(true);
	}

	/*
	 * @description: starts the Import ui for the option: Import data as SQL statements
	 */
	public void startImportSQLUI() {
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			iasu = new ImportAsSQLUI(ecc.getUI(), this);
			iasu.showDatabaseTreeView(dbcc.getDatabaseTreeView());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Import as SQL", e);
		}
	}

	public void showTables(ImportAsSQLUI iasu, Database db) {
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			iasu.getDatabaseTreeView().loadTables(db, dbcc.getTables(db));
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Load tables", e);
		}
	}

	public void importNodesAsSQL(ImportAsSQLUI iasu, TreePath tpa, String file) {
		// check if file exist...
		if (!(new java.io.File(file).exists())) {
			Dialogs.error(iasu, iasu.getTitle(), "File does not exist.");
			return;
		}
		// open progress window...
		ies = new ImportExportProgressUI(iasu);

		// start export...
		try {
			Object node = null;

			if (tpa != null) {
				node = ((DefaultMutableTreeNode) tpa.getLastPathComponent()).getUserObject();
			}

			ImportService ie = cwcc.getContext().newImport(node, file);
			ie.setListener(this);
			ie.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(iasu, "Import as SQL", e);
		}
	}

	@Override
	public void progressed(int percent) {
		ies.setProgressValue(percent);
	}

	@Override
	public void failed(Exception error) {
		ApplicationContext.get().errors().report(ies, "Import as SQL", error);
		ies.dispose();
	}

	/*
	 * @description: starts the Import ui for the option: Import data as CSV comma-seperated
	 */
	public void startImportCSVUI() {
	}

}

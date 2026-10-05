package nl.errorsoft.esql.importer.control;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.importer.ImportService;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.importer.ui.ImportAsSQLUI;
import nl.errorsoft.esql.job.ui.ImportExportProgressUI;
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

	/** Opens the import window (SQL statements are the only format). */
	public void startImport(ConnectionWindowCC cwcc) {
		if (!cwcc.requireFeature(Dialect.Feature.IMPORT, "Import")) {
			return;
		}

		this.cwcc = cwcc;
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			iasu = new ImportAsSQLUI(ecc.getUI(), this);
			iasu.showDatabaseTreeView(dbcc.getDatabaseTreeView());
			dbcc.selectInTree(iasu.getDatabaseTreeView(), cwcc.selectedObject());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Import as SQL", e);
		}
	}

	/** Loads the schemas or tables of a database, or the tables of a schema, the first time it is selected. */
	public void showChildren(ImportAsSQLUI iasu, Object node) {
		try {
			new DatabaseCC(cwcc).loadChildren(iasu.getDatabaseTreeView(), node);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(iasu, "Load tables", e);
		}
	}

	public void importNodesAsSQL(ImportAsSQLUI iasu, TreePath tpa, String file) {
		if (file.isBlank()) {
			Dialogs.warn(iasu, iasu.getTitle(), "Select a file first.");
			return;
		}
		if (!(new java.io.File(file).isFile())) {
			Dialogs.error(iasu, iasu.getTitle(), "The file '" + file + "' does not exist.");
			return;
		}
		String target = tpa == null ? "the current database" : String.valueOf(((DefaultMutableTreeNode) tpa.getLastPathComponent()).getUserObject());
		if (!Dialogs.confirm(iasu, iasu.getTitle(), "Run the statements of '" + file + "' in " + target + "?", "Import")) {
			return;
		}

		ies = new ImportExportProgressUI(iasu);

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

}

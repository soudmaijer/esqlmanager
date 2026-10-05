package nl.errorsoft.esql.export.control;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.export.ExportOptions;
import nl.errorsoft.esql.export.ExportService;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.export.ui.ExportAsSQLUI;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.job.ui.ImportExportProgressUI;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ApplicationContext;
import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.tree.*;

public class ExportCC implements ProgressListener {
	private static final Logger log = LogManager.getLogger(ExportCC.class);

	private ESQLManagerCC ecc;
	private ConnectionWindowCC cwcc;
	private ImportExportProgressUI ies;

	public ExportCC(ESQLManagerCC ecc) {
		this.ecc = ecc;
	}

	/** Opens the export window (SQL statements are the only format). */
	public void startExport(ConnectionWindowCC cwcc) {
		if (!cwcc.requireFeature(Dialect.Feature.EXPORT, "Export")) {
			return;
		}

		this.cwcc = cwcc;
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			ExportAsSQLUI iasu = new ExportAsSQLUI(ecc.getUI(), this);
			iasu.showDatabaseTreeView(dbcc.getDatabaseTreeView());
			dbcc.selectInTree(iasu.getDatabaseTreeView(), cwcc.selectedObject());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Export data", e);
		}
	}

	/** Loads the schemas or tables of a database, or the tables of a schema, the first time it is selected. */
	public void showChildren(ExportAsSQLUI iasu, Object node) {
		try {
			new DatabaseCC(cwcc).loadChildren(iasu.getDatabaseTreeView(), node);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(iasu, "Load tables", e);
		}
	}

	public void exportNodesAsSQL(ExportAsSQLUI iasu, TreePath[] tpa, String file, boolean dumpStructure, boolean dumpData, boolean createDatabase,
		boolean dropTable, boolean useDatabase) {
		String title = iasu.getTitle();
		if (tpa == null || tpa.length == 0) {
			Dialogs.warn(iasu, title, "Select the database(s), schema(s) or table(s) to export in the tree.");
			return;
		}
		if (!dumpStructure && !dumpData) {
			Dialogs.warn(iasu, title, "Select at least one of 'Structure' and 'Data'.");
			return;
		}
		if (new File(file).exists() && !Dialogs.confirmDestructive(iasu, title, "Overwrite the existing file '" + file + "'?", "Overwrite")) {
			return;
		}

		try {
			Object[] export = new Object[tpa.length];

			for (int i = 0; i < tpa.length; i++) {
				export[i] = ((DefaultMutableTreeNode) tpa[i].getLastPathComponent()).getUserObject();
			}

			ies = new ImportExportProgressUI(iasu, "Export data", describe(export) + " to " + file);
			ExportService exp = cwcc.getContext().newExport(export, file, new ExportOptions(dumpStructure, dumpData, createDatabase, dropTable, useDatabase));
			exp.setListener(this);
			exp.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(iasu, "Export data", e);
		}
	}

	/** The objects that are exported, for the progress window: the name of one, or the count. */
	private static String describe(Object[] export) {
		return export.length == 1 ? String.valueOf(export[0]) : export.length + " objects";
	}

	@Override
	public void progressed(int percent) {
		ies.setProgressValue(percent);
	}

	@Override
	public void failed(Exception error) {
		SwingUtilities.invokeLater(() -> {
			ApplicationContext.get().errors().report(ies, "Export data", error);
			ies.dispose();
		});
	}
}

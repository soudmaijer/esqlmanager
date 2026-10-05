package nl.errorsoft.esql.export.control;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.export.ExportOptions;
import nl.errorsoft.esql.export.ExportService;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.export.ui.dialog.ExportSqlDialog;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.job.ui.dialog.ImportExportProgressDialog;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ApplicationContext;
import java.io.File;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.tree.*;

public class ExportController implements ProgressListener {
	private static final Logger log = LogManager.getLogger(ExportController.class);

	private MainController mainController;
	private ConnectionWindowController connectionWindowController;
	private ImportExportProgressDialog progressDialog;

	public ExportController(MainController mainController) {
		this.mainController = mainController;
	}

	/** Opens the export window (SQL statements are the only format). */
	public void startExport(ConnectionWindowController connectionWindowController) {
		if (!connectionWindowController.requireFeature(Dialect.Feature.EXPORT, "Export")) {
			return;
		}

		this.connectionWindowController = connectionWindowController;
		try {
			DatabaseController databaseController = new DatabaseController(connectionWindowController);
			ExportSqlDialog exportDialog = new ExportSqlDialog(mainController.getMainWindow(), this);
			exportDialog.showDatabaseTree(databaseController.getDatabaseTree());
			if (connectionWindowController.getConnectionProfile().getSelection().hasSchemaFilter()) {
				exportDialog.showSchemaFilterNote();
			}
			databaseController.selectInTree(exportDialog.getDatabaseTree(), connectionWindowController.selectedObject());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Export data", e);
		}
	}

	/** Loads the schemas or tables of a database, or the tables of a schema, the first time it is selected. */
	public void showChildren(ExportSqlDialog exportDialog, Object node) {
		try {
			new DatabaseController(connectionWindowController).loadChildren(exportDialog.getDatabaseTree(), node);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(exportDialog, "Load tables", e);
		}
	}

	public void exportNodesAsSQL(ExportSqlDialog exportDialog, TreePath[] tpa, String file, ExportOptions options) {
		String title = exportDialog.getTitle();
		if (tpa == null || tpa.length == 0) {
			Dialogs.warn(exportDialog, title, "Select the database(s), schema(s) or table(s) to export in the tree.");
			return;
		}
		if (!options.dumpStructure() && !options.dumpData()) {
			Dialogs.warn(exportDialog, title, "Select at least one of 'Structure' and 'Data'.");
			return;
		}
		if (new File(file).exists() && !Dialogs.confirmDestructive(exportDialog, title, "Overwrite the existing file '" + file + "'?", "Overwrite")) {
			return;
		}

		try {
			Object[] export = new Object[tpa.length];

			for (int i = 0; i < tpa.length; i++) {
				export[i] = ((DefaultMutableTreeNode) tpa[i].getLastPathComponent()).getUserObject();
			}

			ExportService exp = connectionWindowController.getContext().newExport(export, file, options);
			progressDialog = new ImportExportProgressDialog(exportDialog, "Export data", describe(export) + " to " + file, exp::cancel);
			exp.setListener(this);
			exp.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(exportDialog, "Export data", e);
		}
	}

	/** The dialect of the connection, for the options the server supports. */
	public Dialect dialect() {
		return connectionWindowController.dialect();
	}

	/** The objects that are exported, for the progress window: the name of one, or the count. */
	private static String describe(Object[] export) {
		return export.length == 1 ? String.valueOf(export[0]) : export.length + " objects";
	}

	@Override
	public void progressed(int percent) {
		progressDialog.setProgressValue(percent);
	}

	@Override
	public void status(String text) {
		progressDialog.setStatus(text);
	}

	@Override
	public void finished(String summary, List<String> details) {
		log.info(summary);
		progressDialog.finish(summary, details);
	}

	@Override
	public void cancelled(String summary) {
		log.info(summary);
		progressDialog.cancelled(summary);
	}

	@Override
	public void failed(Exception error) {
		SwingUtilities.invokeLater(() -> {
			ApplicationContext.get().errors().report(progressDialog, "Export data", error);
			progressDialog.dispose();
		});
	}
}

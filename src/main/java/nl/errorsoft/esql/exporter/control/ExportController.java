package nl.errorsoft.esql.exporter.control;

import java.util.ArrayList;
import java.util.List;
import nl.errorsoft.esql.job.ScriptTarget;
import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.exporter.ExportOptions;
import nl.errorsoft.esql.exporter.ExportService;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.exporter.ui.dialog.ExportSqlDialog;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.job.ui.dialog.ImportExportProgressDialog;

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
		DatabaseController databaseController = new DatabaseController(connectionWindowController);
		Object selected = connectionWindowController.selectedObject();
		// The tree is read before the window opens, so the window never shows without one.
		connectionWindowController.inBackground("Export data", "Listing databases...", () -> databaseController.treeStart(selected), start -> {
			ExportSqlDialog exportDialog = new ExportSqlDialog(mainController.getMainWindow(), this);
			exportDialog.showDatabaseTree(databaseController.databaseTree(start.databases()));
			if (connectionWindowController.getConnectionProfile().getSelection().hasSchemaFilter()) {
				exportDialog.showSchemaFilterNote();
			}
			databaseController.selectInTree(exportDialog.getDatabaseTree(), start);
		});
	}

	/** Loads the schemas or tables of a database, or the tables of a schema, in the background the first time it is selected. */
	public void showChildren(ExportSqlDialog exportDialog, Object node) {
		DatabaseController databaseController = new DatabaseController(connectionWindowController);
		connectionWindowController.inBackground("Load tables", "Loading tables...", () -> databaseController.children(node),
			children -> databaseController.showChildren(exportDialog.getDatabaseTree(), children));
	}

	public void exportNodesAsSQL(ExportSqlDialog exportDialog, TreePath[] selectedPaths, String file, ExportOptions options) {
		String title = exportDialog.getTitle();
		if (selectedPaths == null || selectedPaths.length == 0) {
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
			List<ScriptTarget> export = new ArrayList<>();

			for (TreePath path : selectedPaths) {
				export.add(ScriptTarget.of(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()));
			}

			ExportService exportService = connectionWindowController.getContext().newExport(export, file, options);
			progressDialog = new ImportExportProgressDialog(exportDialog, "Export data", describe(export) + " to " + file, exportService::cancel);
			exportService.setListener(this);
			exportService.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(exportDialog, "Export data", e);
		}
	}

	/** The dialect of the connection, for the options the server supports. */
	public Dialect dialect() {
		return connectionWindowController.dialect();
	}

	/** The objects that are exported, for the progress window: the name of one, or the count. */
	private static String describe(List<ScriptTarget> export) {
		return export.size() == 1 ? String.valueOf(export.getFirst()) : export.size() + " objects";
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

package nl.errorsoft.esql.importer.control;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.importer.ImportOptions;
import nl.errorsoft.esql.importer.ImportService;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.importer.ui.dialog.ImportSqlDialog;
import nl.errorsoft.esql.job.ui.dialog.ImportExportProgressDialog;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ApplicationContext;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.tree.*;

public class ImportController implements ProgressListener {
	private static final Logger log = LogManager.getLogger(ImportController.class);

	private MainController mainController;
	private ConnectionWindowController connectionWindowController;
	private ImportExportProgressDialog progressDialog;
	private ImportSqlDialog importDialog;

	public ImportController(MainController mainController) {
		this.mainController = mainController;
	}

	/** Opens the import window (SQL statements are the only format). */
	public void startImport(ConnectionWindowController connectionWindowController) {
		if (!connectionWindowController.requireFeature(Dialect.Feature.IMPORT, "Import")) {
			return;
		}

		this.connectionWindowController = connectionWindowController;
		try {
			DatabaseController databaseController = new DatabaseController(connectionWindowController);
			importDialog = new ImportSqlDialog(mainController.getMainWindow(), this);
			importDialog.showDatabaseTree(databaseController.getDatabaseTree());
			databaseController.selectInTree(importDialog.getDatabaseTree(), connectionWindowController.selectedObject());
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Import data", e);
		}
	}

	/** Loads the schemas or tables of a database, or the tables of a schema, the first time it is selected. */
	public void showChildren(ImportSqlDialog importDialog, Object node) {
		try {
			new DatabaseController(connectionWindowController).loadChildren(importDialog.getDatabaseTree(), node);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(importDialog, "Load tables", e);
		}
	}

	public void importNodesAsSQL(ImportSqlDialog importDialog, TreePath tpa, String file, ImportOptions options) {
		if (file.isBlank()) {
			Dialogs.warn(importDialog, importDialog.getTitle(), "Select a file first.");
			return;
		}
		if (!(new java.io.File(file).isFile())) {
			Dialogs.error(importDialog, importDialog.getTitle(), "The file '" + file + "' does not exist.");
			return;
		}
		String target = tpa == null ? "the current database" : String.valueOf(((DefaultMutableTreeNode) tpa.getLastPathComponent()).getUserObject());
		if (!Dialogs.confirm(importDialog, importDialog.getTitle(), "Run the statements of '" + file + "' in " + target + "?", "Import")) {
			return;
		}

		try {
			Object node = null;

			if (tpa != null) {
				node = ((DefaultMutableTreeNode) tpa.getLastPathComponent()).getUserObject();
			}

			ImportService ie = connectionWindowController.getContext().newImport(node, file, options);
			progressDialog = new ImportExportProgressDialog(importDialog, "Import data", file + " into " + target, ie::cancel);
			ie.setListener(this);
			ie.start();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(importDialog, "Import data", e);
		}
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
			ApplicationContext.get().errors().report(progressDialog, "Import data", error);
			progressDialog.dispose();
		});
	}

}

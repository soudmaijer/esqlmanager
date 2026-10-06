package nl.errorsoft.esql.importer.control;

import nl.errorsoft.esql.job.ScriptTarget;
import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.job.ProgressListener;

import nl.errorsoft.esql.importer.ImportOptions;
import nl.errorsoft.esql.importer.ImportService;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.importer.ui.dialog.ImportSqlDialog;
import nl.errorsoft.esql.job.ui.dialog.ImportExportProgressDialog;

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
		DatabaseController databaseController = new DatabaseController(connectionWindowController);
		Object selected = connectionWindowController.selectedObject();
		// The tree is read before the window opens, so the window never shows without one.
		connectionWindowController.inBackground("Import data", "Listing databases...", () -> databaseController.treeStart(selected), start -> {
			importDialog = new ImportSqlDialog(mainController.getMainWindow(), this);
			importDialog.showDatabaseTree(databaseController.databaseTree(start.databases()));
			databaseController.selectInTree(importDialog.getDatabaseTree(), start);
		});
	}

	/** Loads the schemas of a database in the background the first time it is selected. */
	public void showChildren(ImportSqlDialog importDialog, Object node) {
		DatabaseController databaseController = new DatabaseController(connectionWindowController);
		connectionWindowController.inBackground("Load tables", "Loading schemas...", () -> databaseController.children(node),
			children -> databaseController.showChildren(importDialog.getDatabaseTree(), children));
	}

	public void importNodesAsSQL(ImportSqlDialog importDialog, TreePath selectedPath, String file, ImportOptions options) {
		if (file.isBlank()) {
			Dialogs.warn(importDialog, importDialog.getTitle(), "Select a file first.");
			return;
		}
		if (!(new java.io.File(file).isFile())) {
			Dialogs.error(importDialog, importDialog.getTitle(), "The file '" + file + "' does not exist.");
			return;
		}
		String target = selectedPath == null
			? "the current database"
			: String.valueOf(((DefaultMutableTreeNode) selectedPath.getLastPathComponent()).getUserObject());
		if (!Dialogs.confirm(importDialog, importDialog.getTitle(), "Run the statements of '" + file + "' in " + target + "?", "Import")) {
			return;
		}

		try {
			// Nothing or the server selected: the script runs in the database the connection uses.
			ScriptTarget node = selectedPath == null || selectedPath.getPathCount() == 1
				? null
				: ScriptTarget.of(((DefaultMutableTreeNode) selectedPath.getLastPathComponent()).getUserObject());

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

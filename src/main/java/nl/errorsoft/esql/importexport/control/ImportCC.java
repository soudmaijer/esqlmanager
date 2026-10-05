package nl.errorsoft.esql.importexport.control;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.importexport.ImportService;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.importexport.ui.ImportAsSQLUI;
import nl.errorsoft.esql.importexport.ui.ImportExportProgressUI;
import nl.errorsoft.esql.importexport.ui.ImportSelectionUI;
import nl.errorsoft.esql.ui.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import javax.swing.tree.*;
import java.util.Observer;
import java.util.Observable;

public class ImportCC implements Observer {
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
		try {
			if (!cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getDialect().supports(Dialect.Feature.IMPORT)) {
				cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
				return;
			}
		} catch (Exception e) {
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
			log.error(e.getMessage(), e);
		}
	}

	public void showTables(ImportAsSQLUI iasu, Database db) {
		try {
			DatabaseCC dbcc = new DatabaseCC(cwcc);
			iasu.getDatabaseTreeView().loadTables(db, dbcc.getTables(db));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void importNodesAsSQL(ImportAsSQLUI iasu, TreePath tpa, String file) {
		// check if file exist...
		if (!(new java.io.File(file).exists())) {
			iasu.showErrorMessage("File doesn`t exist!");
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
			ie.addObserver(this);
			ie.start();
		} catch (Exception e) {
			iasu.showErrorMessage("An error occured while importing the data! " + e.getMessage());
			log.error(e.getMessage(), e);
		}
	}

	public void update(Observable o, Object arg) {
		if (arg instanceof Integer) {
			ies.setProgressValue(((Integer) arg).intValue());
		} else if (arg instanceof Exception) {
			log.error(((Exception) arg).getMessage(), (Exception) arg);
			ies.showErrorMessage(((Exception) arg).getMessage());
			ies.dispose();
		}
	}

	public ImageLoader getImageLoader() {
		return cwcc.getImageLoader();
	}

	/*
	 * @description: starts the Import ui for the option: Import data as CSV comma-seperated
	 */
	public void startImportCSVUI() {
	}

}

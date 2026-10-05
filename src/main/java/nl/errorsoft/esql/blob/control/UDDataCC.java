package nl.errorsoft.esql.blob.control;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableData;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.blob.ui.DownloadFileUI;
import nl.errorsoft.esql.blob.ui.UDDataIF;
import nl.errorsoft.esql.blob.ui.UploadFileUI;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.blob.BlobService;
import javax.swing.SwingUtilities;

public class UDDataCC {
	private static final Logger log = LogManager.getLogger(UDDataCC.class);

	private ConnectionWindowCC cwcc;
	private UDDataIF udif;
	private Table table;
	private TableData[] row;
	private TableData cell;

	public UDDataCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public void startDownloadUI(ESQLManagerUI parent, Table table, TableData[] row, TableData cell) {
		this.table = table;
		this.row = row;
		this.cell = cell;
		udif = new DownloadFileUI(this, parent);
		udif.open();
	}

	public void startUploadUI(ESQLManagerUI parent, Table table, TableData[] row, TableData cell) {
		this.table = table;
		this.row = row;
		this.cell = cell;
		udif = new UploadFileUI(this, parent);
		udif.open();
	}

	public void downloadFile(String fileLocation) {
		String column = cell.getTableColumn().getName();
		transfer("Save file", service -> service.download(table, row, cell, fileLocation), "Saved " + table + "." + column + " to " + fileLocation);
	}

	public void uploadFile(String fileLocation) {
		String column = cell.getTableColumn().getName();
		transfer("Upload file", service -> service.upload(table, row, cell, fileLocation), "Uploaded " + fileLocation + " into " + table + "." + column);
	}

	private interface Transfer {
		void run(BlobService service) throws Exception;
	}

	/** Runs the transfer on a virtual thread, the window and the status bar are updated on the event thread. */
	private void transfer(String action, Transfer transfer, String outcome) {
		BlobService service;
		try {
			service = cwcc.getContext().newBlobTransfer();
		} catch (Exception e) {
			udif.transferEnded();
			ApplicationContext.get().errors().report((java.awt.Component) udif, action, e);
			return;
		}
		service.setProgress(percent -> SwingUtilities.invokeLater(() -> udif.setProgressValue(percent)));
		Thread.ofVirtual().name("blob-transfer").start(() -> {
			try {
				transfer.run(service);
				log.info(outcome);
				SwingUtilities.invokeLater(() -> cwcc.setViewStatus(outcome));
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					udif.transferEnded();
					ApplicationContext.get().errors().report((java.awt.Component) udif, action, e);
				});
			}
		});
	}
}

package nl.errorsoft.esql.blob.control;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableCell;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.blob.ui.dialog.DownloadFileDialog;
import nl.errorsoft.esql.blob.ui.TransferProgress;
import nl.errorsoft.esql.blob.ui.dialog.UploadFileDialog;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.blob.BlobService;
import javax.swing.SwingUtilities;

public class BlobTransferController {
	private static final Logger log = LogManager.getLogger(BlobTransferController.class);

	private ConnectionWindowController connectionWindowController;
	private TransferProgress progress;
	private Table table;
	private TableCell[] row;
	private TableCell cell;
	private volatile BlobService running;

	public BlobTransferController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	public void showDownloadDialog(MainWindow parent, Table table, TableCell[] row, TableCell cell) {
		this.table = table;
		this.row = row;
		this.cell = cell;
		progress = new DownloadFileDialog(this, parent);
		progress.open();
	}

	public void showUploadDialog(MainWindow parent, Table table, TableCell[] row, TableCell cell) {
		this.table = table;
		this.row = row;
		this.cell = cell;
		progress = new UploadFileDialog(this, parent);
		progress.open();
	}

	public void downloadFile(String fileLocation) {
		String column = cell.getTableColumn().getName();
		transfer("Save file", service -> service.download(table, row, cell, fileLocation), "Saved " + table + "." + column + " to " + fileLocation,
			() -> deleteQuietly(fileLocation));
	}

	public void uploadFile(String fileLocation) {
		String column = cell.getTableColumn().getName();
		transfer("Upload file", service -> service.upload(table, row, cell, fileLocation), "Uploaded " + fileLocation + " into " + table + "." + column,
			() -> {
			});
	}

	private static void deleteQuietly(String file) {
		try {
			java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(file));
		} catch (java.io.IOException e) {
			log.warn("The partial file {} could not be deleted: {}", file, e.getMessage());
		}
	}

	/** Cancels the transfer that runs, if any. */
	public void cancelTransfer() {
		BlobService service = running;
		if (service != null) {
			service.cancel();
		}
	}

	private interface Transfer {
		void run(BlobService service) throws Exception;
	}

	/** Runs the transfer on a virtual thread, the window and the status bar are updated on the event thread. */
	/** {@code discardPartialFile} runs when the transfer is cancelled or fails. */
	private void transfer(String action, Transfer transfer, String outcome, Runnable discardPartialFile) {
		BlobService service;
		try {
			service = connectionWindowController.getContext().newBlobTransfer();
		} catch (Exception e) {
			progress.transferEnded();
			ApplicationContext.get().errors().report((java.awt.Component) progress, action, e);
			return;
		}
		running = service;
		service.setProgress(percent -> SwingUtilities.invokeLater(() -> progress.setProgressValue(percent)));
		Thread.ofVirtual().name("blob-transfer").start(() -> {
			try {
				transfer.run(service);
				log.info(outcome);
				SwingUtilities.invokeLater(() -> connectionWindowController.setViewStatus(outcome));
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					if (service.isCancelled()) {
						// Whatever the driver made of the interrupted transfer, the user asked for it.
						log.info("{} cancelled", action);
						discardPartialFile.run();
						progress.transferCancelled();
					} else {
						discardPartialFile.run();
						progress.transferEnded();
						ApplicationContext.get().errors().report((java.awt.Component) progress, action, e);
					}
				});
			} finally {
				running = null;
			}
		});
	}
}

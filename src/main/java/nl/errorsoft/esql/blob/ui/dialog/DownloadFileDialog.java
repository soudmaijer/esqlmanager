package nl.errorsoft.esql.blob.ui.dialog;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import nl.errorsoft.esql.blob.control.BlobTransferController;
import nl.errorsoft.esql.ui.dialog.Dialogs;

/** Saves the binary data of a cell to a file. */
public class DownloadFileDialog extends TransferFileDialog {
	public DownloadFileDialog(BlobTransferController blobTransferController, JFrame parent) {
		super(blobTransferController, parent, new Labels("Download data", "&Download", "Save &as:", "&Save as...", "Save data..."));
	}

	@Override
	protected int showChooser(JFileChooser chooser) {
		return chooser.showSaveDialog(this);
	}

	@Override
	protected boolean transfer(String path) {
		if (path.isEmpty()) {
			showError("Enter the file to save to.");
			return false;
		}
		if (Files.exists(Path.of(path)) && !Dialogs.confirmDestructive(this, getTitle(), "Overwrite the existing file '" + path + "'?", "Overwrite")) {
			return false;
		}
		blobTransferController.downloadFile(path);
		return true;
	}
}

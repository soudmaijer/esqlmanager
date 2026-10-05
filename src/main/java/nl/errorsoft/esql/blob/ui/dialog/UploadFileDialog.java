package nl.errorsoft.esql.blob.ui.dialog;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import nl.errorsoft.esql.blob.control.BlobTransferController;

/** Puts the contents of a file into a binary cell. */
public class UploadFileDialog extends TransferFileDialog {
	public UploadFileDialog(BlobTransferController blobTransferController, JFrame parent) {
		super(blobTransferController, parent, new Labels("Upload data", "&Upload", "&File:", "&Browse...", "Pick a file..."));
	}

	@Override
	protected int showChooser(JFileChooser chooser) {
		return chooser.showOpenDialog(this);
	}

	@Override
	protected boolean transfer(String path) {
		if (path.isEmpty()) {
			showError("Enter the file to upload.");
			return false;
		}
		if (!Files.isRegularFile(Path.of(path))) {
			showError("The file '" + path + "' does not exist.");
			return false;
		}
		blobTransferController.uploadFile(path);
		return true;
	}
}

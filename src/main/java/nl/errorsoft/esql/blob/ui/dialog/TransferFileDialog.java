package nl.errorsoft.esql.blob.ui.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.blob.control.BlobTransferController;
import nl.errorsoft.esql.blob.ui.TransferProgress;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.FileChoosers;
import nl.errorsoft.esql.ui.util.Forms;

/**
 * What the upload and download of a binary cell share: a file field with a button to pick the file, a progress bar, the action button and Cancel. While
 * a transfer runs the window cannot be closed; Cancel, the close button and Esc cancel the transfer instead.
 */
abstract class TransferFileDialog extends FormDialog implements TransferProgress {
	/** The words that differ between upload and download. */
	record Labels(String title, String action, String fileLabel, String browse, String chooserTitle) {
	}

	protected final BlobTransferController blobTransferController;
	private final JTextField file = new JTextField(24);
	private final JProgressBar progress = new JProgressBar();
	private final JButton start;
	private final JButton browse;
	/** True while a transfer runs, the window cannot be closed then. */
	private boolean busy;

	TransferFileDialog(BlobTransferController blobTransferController, JFrame parent, Labels labels) {
		super(parent, labels.title(), true);
		this.blobTransferController = blobTransferController;
		start = Forms.button(labels.action());
		browse = Forms.button(labels.browse());
		JButton cancel = Forms.button("&Cancel");

		start.addActionListener(e -> start());
		browse.addActionListener(e -> browse(labels.chooserTitle()));
		cancel.addActionListener(e -> close());

		// A transfer in progress cannot be closed, close() decides (also for Esc).
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		JPanel fileRow = new JPanel(new BorderLayout(Forms.GAP, 0));
		fileRow.add(file, BorderLayout.CENTER);
		fileRow.add(browse, BorderLayout.EAST);
		String hint = "Click " + labels.action().replace("&", "") + " to start the transfer.";
		JPanel content = new Forms.Grid().row(Forms.label(labels.fileLabel(), file), fileRow).full(new JLabel(hint)).full(progress).done();
		layoutDialog(content, start, cancel);
		setInitialFocus(file);
	}

	/** Shows the file chooser for this direction, returns its JFileChooser option. */
	protected abstract int showChooser(JFileChooser chooser);

	/** Checks the file and starts the transfer, or shows what is wrong. Returns false when the transfer did not start. */
	protected abstract boolean transfer(String path);

	@Override
	public void open() {
		showDialog();
	}

	private void browse(String title) {
		JFileChooser chooser = FileChoosers.create();
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setDialogTitle(title);
		try {
			if (showChooser(chooser) == JFileChooser.APPROVE_OPTION) {
				file.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Choose file", e);
		}
	}

	/** Busy before the transfer starts, because a transfer that cannot start reports {@link #transferEnded} at once. */
	private void start() {
		setBusy(true);
		if (!transfer(file.getText().trim())) {
			setBusy(false);
		}
	}

	private void setBusy(boolean busy) {
		this.busy = busy;
		if (busy) {
			showError(null);
		}
		start.setEnabled(!busy);
		browse.setEnabled(!busy);
	}

	/** Closes the dialog; while a transfer runs, closing (also with Esc) cancels the transfer. */
	private void close() {
		if (busy) {
			blobTransferController.cancelTransfer();
		} else {
			dispose();
		}
	}

	@Override
	public void transferCancelled() {
		setBusy(false);
		showError("Cancelled.");
		progress.setValue(0);
	}

	@Override
	public void transferEnded() {
		setBusy(false);
	}

	@Override
	public void setProgressValue(int percentage) {
		progress.setValue(percentage);
		if (percentage == 100) {
			dispose();
		}
	}
}

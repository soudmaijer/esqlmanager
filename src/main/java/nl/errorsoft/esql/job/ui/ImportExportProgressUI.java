package nl.errorsoft.esql.job.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/**
 * Shows the progress of an export or import: which operation, the target, a progress bar, what it is doing and a Cancel button that becomes Close when
 * the job has ended. Closing the window (also with Esc) while the job runs cancels it. The methods are called from the job's thread.
 */
public class ImportExportProgressUI extends FormDialog {
	private final JProgressBar bar = new JProgressBar(0, 100);
	private final JLabel status = new JLabel("Working...");
	private final JButton action = Forms.button("&Cancel");
	private final JTextArea details = new JTextArea(6, 50);
	private final JScrollPane detailsScroll = new JScrollPane(details);
	private final Runnable onCancel;
	private boolean done;
	private boolean cancelling;

	/**
	 * @param operation what runs, also the title of the window ("Export data")
	 * @param target what it runs on or into ("shop to /tmp/shop.sql")
	 * @param onCancel asks the job to stop
	 */
	public ImportExportProgressUI(Window owner, String operation, String target, Runnable onCancel) {
		super(owner, operation, false);
		this.onCancel = onCancel;
		// A running job must not be hidden by accident: closing it (also with Esc) cancels the job first.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				pressAction();
			}
		});

		JLabel heading = new JLabel(operation);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		bar.setStringPainted(true);
		bar.setString("0%");
		action.addActionListener(e -> pressAction());
		details.setEditable(false);
		details.setLineWrap(true);
		details.setWrapStyleWord(true);
		detailsScroll.setVisible(false);

		JPanel header = new JPanel(new BorderLayout(0, Forms.GAP));
		header.add(heading, BorderLayout.NORTH);
		header.add(new JLabel(target), BorderLayout.CENTER);
		JPanel progress = new JPanel(new BorderLayout(0, Forms.GAP));
		progress.add(bar, BorderLayout.NORTH);
		progress.add(status, BorderLayout.CENTER);
		JPanel content = new JPanel(new BorderLayout(0, Forms.GAP));
		content.add(header, BorderLayout.NORTH);
		content.add(progress, BorderLayout.CENTER);
		content.add(detailsScroll, BorderLayout.SOUTH);
		layoutDialog(content, action);
		bar.setPreferredSize(new Dimension(360, bar.getPreferredSize().height));

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		toFront();
	}

	private void pressAction() {
		if (done) {
			dispose();
		} else if (!cancelling) {
			cancelling = true;
			action.setEnabled(false);
			status.setText("Cancelling...");
			onCancel.run();
		}
	}

	/** The percentage of the bar. */
	public void setProgressValue(int percentage) {
		SwingUtilities.invokeLater(() -> {
			bar.setValue(percentage);
			bar.setString(percentage + "%");
		});
	}

	/** What the job is doing. */
	public void setStatus(String text) {
		SwingUtilities.invokeLater(() -> {
			if (!done && !cancelling) {
				status.setText(text);
			}
		});
	}

	/** The job ran to the end: the summary and, when there are any, lines such as the statements that failed. */
	public void finish(String summary, List<String> lines) {
		SwingUtilities.invokeLater(() -> {
			bar.setValue(100);
			bar.setString("100%");
			end(summary);
			if (!lines.isEmpty()) {
				details.setText(String.join("\n", lines));
				details.setCaretPosition(0);
				detailsScroll.setVisible(true);
				pack();
			}
		});
	}

	/** The job stopped because the user cancelled it. */
	public void cancelled(String summary) {
		SwingUtilities.invokeLater(() -> end(summary));
	}

	private void end(String summary) {
		done = true;
		status.setText(summary);
		Forms.mnemonic(action, "&Close");
		action.setEnabled(true);
		getRootPane().setDefaultButton(action);
		action.requestFocusInWindow();
	}
}

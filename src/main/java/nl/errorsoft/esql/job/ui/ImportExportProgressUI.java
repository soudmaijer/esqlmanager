package nl.errorsoft.esql.job.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** Shows the progress of an export or import: which operation, the target, a progress bar, and a Close button that is enabled when the job is done. */
public class ImportExportProgressUI extends FormDialog {
	private final JProgressBar bar = new JProgressBar(0, 100);
	private final JLabel status = new JLabel("Working...");
	private final JButton close = Forms.button("Close");
	private boolean done;

	/**
	 * @param operation what runs, also the title of the window ("Export data")
	 * @param target what it runs on or into ("shop to /tmp/shop.sql")
	 */
	public ImportExportProgressUI(Window owner, String operation, String target) {
		super(owner, operation, false);
		// A running job must not be hidden by accident, closing (also with Esc) works when it is done.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (done) {
					dispose();
				}
			}
		});

		JLabel heading = new JLabel(operation);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		bar.setStringPainted(true);
		bar.setString("0%");
		close.setEnabled(false);
		close.addActionListener(e -> dispose());

		JPanel header = new JPanel(new BorderLayout(0, Forms.GAP));
		header.add(heading, BorderLayout.NORTH);
		header.add(new JLabel(target), BorderLayout.CENTER);
		JPanel content = new JPanel(new BorderLayout(0, Forms.GAP));
		content.add(header, BorderLayout.NORTH);
		content.add(bar, BorderLayout.CENTER);
		content.add(status, BorderLayout.SOUTH);
		layoutDialog(content, close);
		bar.setPreferredSize(new java.awt.Dimension(360, bar.getPreferredSize().height));

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		toFront();
	}

	/** Called from the job's thread. */
	public void setProgressValue(int percentage) {
		SwingUtilities.invokeLater(() -> {
			bar.setValue(percentage);
			bar.setString(percentage + "%");

			if (percentage >= 100) {
				done = true;
				status.setText("Completed.");
				close.setEnabled(true);
				getRootPane().setDefaultButton(close);
				close.requestFocusInWindow();
			}
		});
	}
}

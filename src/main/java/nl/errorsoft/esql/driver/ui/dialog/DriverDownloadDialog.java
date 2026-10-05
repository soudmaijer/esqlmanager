package nl.errorsoft.esql.driver.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import nl.errorsoft.esql.driver.DriverArtifact;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** Shows how far the download of a JDBC driver is. It has no buttons: the controller closes it when the download ends. */
public class DriverDownloadDialog extends FormDialog {
	private final JProgressBar bar = new JProgressBar(0, 100);

	public DriverDownloadDialog(Window owner, DriverArtifact artifact) {
		super(owner, "Download " + artifact.name() + " driver", false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		bar.setStringPainted(true);
		JPanel content = new JPanel(new BorderLayout(0, Forms.GAP));
		content.add(new JLabel("Downloading " + artifact.fileName() + " (" + artifact.sizeText() + ") from Maven Central..."), BorderLayout.NORTH);
		content.add(bar, BorderLayout.CENTER);
		layoutDialog(content);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Event thread only. */
	public void progressed(int percent) {
		bar.setValue(percent);
	}
}

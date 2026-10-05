package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

import java.awt.Color;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

/** The About window: name, version, commit, Java version and where the licence notices are. Credits shows the credits over the splash image. */
public class AboutDialog extends FormDialog {
	/**
	 * @param showCredits what the Credits button does, for example showing the splash screen.
	 */
	public AboutDialog(Window owner, String appName, String version, String commit, Runnable showCredits) {
		super(owner, "About " + appName, false);

		JLabel name = new JLabel(appName);
		name.setFont(name.getFont().deriveFont(java.awt.Font.BOLD, name.getFont().getSize2D() + 6f));

		Forms.Grid grid = new Forms.Grid();
		grid.full(name);
		grid.row(new JLabel("Version:"), new JLabel(version));
		grid.row(new JLabel("Commit:"), new JLabel(commit));
		grid.row(new JLabel("Java:"), new JLabel(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"));
		grid.row(new JLabel("System:"),
			new JLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + ", " + System.getProperty("os.arch")));
		grid.full(notice(
			"Icons and logos are used under their own licences (Lucide ISC, Devicon MIT, Simple Icons CC0). See NOTICE.txt in the icons folder of the source."));

		JButton credits = Forms.button("C&redits");
		JButton close = Forms.button("&Close");
		credits.addActionListener(e -> showCredits.run());
		close.addActionListener(e -> dispose());
		setLeadingButton(credits);
		layoutDialog(grid.panel(), close);
		setInitialFocus(close);
	}

	private static JLabel notice(String text) {
		JLabel label = new JLabel("<html><body style='width: 300px'>" + text + "</body></html>");
		Color disabled = UIManager.getColor("Label.disabledForeground");
		if (disabled != null) {
			label.setForeground(disabled);
		}
		return label;
	}
}

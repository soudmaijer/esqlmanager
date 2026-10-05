package nl.errorsoft.esql.ui.util;

import java.awt.Component;
import java.awt.Window;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** A read-only list of properties (a label and its value per row) with a Close button. */
public final class PropertiesDialog {
	private PropertiesDialog() {
	}

	/** Shows the properties in order. A null value is shown as "-". */
	public static void show(Component parent, String title, Map<String, String> properties) {
		Window owner = parent instanceof Window window ? window : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
		FormDialog dialog = new FormDialog(owner, title, true);
		Forms.Grid grid = new Forms.Grid();

		properties.forEach((name, value) -> {
			JLabel shown = new JLabel(value == null || value.isBlank() ? "-" : value);
			shown.setToolTipText(value);
			grid.row(new JLabel(name + ":"), shown);
		});

		JPanel content = grid.panel();
		JButton close = Forms.button("&Close");
		close.addActionListener(e -> dialog.dispose());
		dialog.layoutDialog(content, close);
		dialog.setInitialFocus(close);
		dialog.showDialog();
	}
}

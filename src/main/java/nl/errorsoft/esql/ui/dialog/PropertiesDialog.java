package nl.errorsoft.esql.ui.dialog;

import nl.errorsoft.esql.ui.util.Forms;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A read-only list of properties (a label and its value per row) with a Close button. */
public final class PropertiesDialog extends FormDialog {
	private static final int MIN_WIDTH = 320;

	private PropertiesDialog(Window owner, String title, Map<String, String> properties) {
		super(owner, title, true);
		Forms.Grid grid = new Forms.Grid();

		properties.forEach((name, value) -> {
			JLabel shown = new JLabel(value == null || value.isBlank() ? "-" : value);
			shown.setToolTipText(value);
			grid.row(new JLabel(name + ":"), shown);
		});

		JPanel content = grid.panel();
		Dimension size = content.getPreferredSize();
		content.setPreferredSize(new Dimension(Math.max(MIN_WIDTH, size.width), size.height));
		JButton close = Forms.button("&Close");
		close.addActionListener(e -> dispose());
		layoutDialog(content, close);
		setInitialFocus(close);
	}

	/** Shows the properties in order. A null value is shown as "-". */
	public static void show(Component parent, String title, Map<String, String> properties) {
		new PropertiesDialog(Dialogs.windowOf(parent), title, properties).showDialog();
	}
}

package nl.errorsoft.esql.table.ui.dialog;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** The Duplicate table dialog: the name of the copy and whether the rows are copied too. */
public final class DuplicateTableDialog extends FormDialog {
	/** @param name the name of the new table */
	public record Request(String name, boolean withData) {
	}

	private final JTextField name;
	private final JCheckBox withData = Forms.mnemonic(new JCheckBox(), "Copy the &data too");

	private DuplicateTableDialog(Component parent, String suggestion, Function<String, String> nameProblem) {
		super(Dialogs.windowOf(parent), "Duplicate table", true);
		name = new JTextField(suggestion, 24);
		setOkCancel(new Forms.Grid().row("&Name:", name).full(withData).panel(), "Duplicate", "Cancel");
		setValidator(() -> nameProblem.apply(name.getText().trim()));
		setInitialFocus(name);
	}

	/**
	 * @param suggestion the name filled in
	 * @param nameProblem returns the message for a name that cannot be used, null for a good one
	 * @return null when the user cancels
	 */
	public static Request ask(Component parent, String suggestion, Function<String, String> nameProblem) {
		DuplicateTableDialog dialog = new DuplicateTableDialog(parent, suggestion, nameProblem);
		return dialog.showDialog() ? new Request(dialog.name.getText().trim(), dialog.withData.isSelected()) : null;
	}
}

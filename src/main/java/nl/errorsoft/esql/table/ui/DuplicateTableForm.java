package nl.errorsoft.esql.table.ui;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.ui.util.Forms;

/** The Duplicate table dialog: the name of the copy and whether the rows are copied too. */
public final class DuplicateTableForm {
	/** @param name the name of the new table */
	public record Request(String name, boolean withData) {
	}

	private DuplicateTableForm() {
	}

	/**
	 * @param suggestion the name filled in
	 * @param nameProblem returns the message for a name that cannot be used, null for a good one
	 * @return null when the user cancels
	 */
	public static Request ask(Component parent, String suggestion, Function<String, String> nameProblem) {
		JTextField name = new JTextField(suggestion, 24);
		JCheckBox withData = Forms.mnemonic(new JCheckBox(), "Copy the &data too");
		Forms.Grid grid = new Forms.Grid().row("&Name:", name).full(withData);

		boolean accepted = Dialogs.form(parent, "Duplicate table", grid.panel(), "Duplicate", name, () -> nameProblem.apply(name.getText().trim()));
		return accepted ? new Request(name.getText().trim(), withData.isSelected()) : null;
	}
}

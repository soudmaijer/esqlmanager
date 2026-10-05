package nl.errorsoft.esql.error;

import java.awt.Component;
import java.awt.Window;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

/**
 * The one place that shows message dialogs. ERROR for failures, WARNING for confirmations of destructive actions, QUESTION for plain questions, INFORMATION for
 * information. The title names the action ("Drop table"), the message says what happens, a confirm button is labelled with the action ("Drop").
 */
public final class Dialogs {
	private static final String CANCEL = "Cancel";

	private Dialogs() {
	}

	public static void info(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void warn(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
	}

	public static void error(Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/** A plain question, true when the user clicks the confirm button. */
	public static boolean confirm(Component parent, String title, String message, String confirmLabel) {
		return ask(parent, title, message, confirmLabel, JOptionPane.QUESTION_MESSAGE);
	}

	/** A question before something that cannot be undone (dropping, deleting), with the WARNING icon. */
	public static boolean confirmDestructive(Component parent, String title, String message, String confirmLabel) {
		return ask(parent, title, message, confirmLabel, JOptionPane.WARNING_MESSAGE);
	}

	/** Asks for a line of text that must not be empty, null when the user cancels. The label is written like "&Name:". */
	public static String input(Component parent, String title, String label, String okLabel) {
		return input(parent, title, label, okLabel, "", value -> Validation.required("a name", value));
	}

	/**
	 * Asks for a line of text next to a label, the focus in the field. The dialog stays open with the message of the validator in red until the
	 * input is valid; null when the user cancels.
	 * @param validator returns the message to show for a value that is not acceptable, null for a good one.
	 */
	public static String input(Component parent, String title, String label, String okLabel, String initialValue, Function<String, String> validator) {
		JTextField field = new JTextField(initialValue, 24);
		JPanel content = new Forms.Grid().row(label, field).panel();
		return form(parent, title, content, okLabel, field, () -> validator.apply(field.getText().trim())) ? field.getText().trim() : null;
	}

	/**
	 * A form (a panel with fields) with the action button and Cancel, true when the input was accepted. Enter accepts, Esc cancels. The validator
	 * returns the message to show (in red, the dialog stays open and the typed data is kept), or null.
	 */
	public static boolean form(Component parent, String title, JComponent form, String okLabel, Component focus, Supplier<String> validator) {
		FormDialog dialog = new FormDialog(windowOf(parent), title, true);
		dialog.setOkCancel(form, okLabel, "Cancel");
		dialog.setValidator(validator);
		dialog.setInitialFocus(focus);
		return dialog.showDialog();
	}

	private static Window windowOf(Component parent) {
		return parent instanceof Window window ? window : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
	}

	/** Save, Don't save or Cancel. */
	public enum SaveChoice {
		SAVE, DISCARD, CANCEL
	}

	public static SaveChoice askSave(Component parent, String title, String message) {
		String[] options = {"Save", "Don't save", CANCEL};
		int answer = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		return switch (answer) {
			case 0 -> SaveChoice.SAVE;
			case 1 -> SaveChoice.DISCARD;
			default -> SaveChoice.CANCEL;
		};
	}

	private static boolean ask(Component parent, String title, String message, String confirmLabel, int icon) {
		String[] options = {confirmLabel, CANCEL};
		return JOptionPane.showOptionDialog(parent, message, title, JOptionPane.DEFAULT_OPTION, icon, null, options, options[0]) == 0;
	}
}

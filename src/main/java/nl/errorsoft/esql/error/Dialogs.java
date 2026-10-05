package nl.errorsoft.esql.error;

import java.awt.Component;
import javax.swing.JOptionPane;

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

	/** Asks for a line of text, null when the user cancels. */
	public static String input(Component parent, String title, String message) {
		return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
	}

	/** A form (a panel with fields) with OK and Cancel, true on OK. */
	public static boolean form(Component parent, String title, Object form) {
		return JOptionPane.showConfirmDialog(parent, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
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

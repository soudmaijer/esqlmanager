package nl.errorsoft.esql.ui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * The base of the application's dialogs: the content with the padding of {@link Forms}, a line for an error message, a row of buttons at the bottom
 * right, the first button as the default button (Enter) and Esc to close ({@link EscapeToClose}). The simple case is {@link #setOkCancel}: the action
 * button is labelled with the action ("&amp;Create"), a validator returns the message to show while the input is wrong (the dialog stays open and the
 * input is kept), and {@link #showDialog()} returns true when the input was accepted.
 */
public class FormDialog extends JDialog {
	private final JLabel error = new JLabel(" ");
	private Supplier<String> validator = () -> null;
	private Runnable onAccept = () -> {
	};
	private Component initialFocus;
	private Component leadingButton;
	private boolean accepted;

	public FormDialog(Window owner, String title, boolean modal) {
		super(owner, title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);
		Color red = UIManager.getColor("Actions.Red");
		error.setForeground(red != null ? red : Color.RED);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				focusInitial();
			}
		});
	}

	/** Content, an OK button labelled with the action and a Cancel (or Close) button. OK runs the validator, then the accept action, then closes. */
	public void setOkCancel(JComponent content, String okLabel, String cancelLabel) {
		JButton ok = Forms.button(okLabel);
		JButton cancel = Forms.button(cancelLabel);
		ok.addActionListener(e -> accept());
		cancel.addActionListener(e -> dispose());
		layoutDialog(content, ok, cancel);
	}

	/** Content above the error line and the buttons; the first button is the default button. */
	public void layoutDialog(JComponent content, JButton... buttons) {
		JPanel south = new JPanel(new BorderLayout());
		error.setBorder(BorderFactory.createEmptyBorder(Forms.GAP, 0, 0, 0));
		south.add(error, BorderLayout.NORTH);
		south.add(leadingButton == null ? Forms.buttonRow(buttons) : Forms.buttonRowWithLeading(leadingButton, buttons), BorderLayout.CENTER);

		JPanel root = Forms.padded(new JPanel(new BorderLayout()));
		root.add(content, BorderLayout.CENTER);
		root.add(south, BorderLayout.SOUTH);
		setContentPane(root);
		if (buttons.length > 0) {
			getRootPane().setDefaultButton(buttons[0]);
		}
	}

	/** A button apart from the others at the bottom left, for example Delete. Set it before {@link #layoutDialog}. */
	public void setLeadingButton(Component button) {
		this.leadingButton = button;
	}

	/** The check that runs before the input is accepted: the message to show, null when the input is fine. */
	public void setValidator(Supplier<String> validator) {
		this.validator = validator;
	}

	/** What happens when the input is valid, before the dialog closes. Without one the dialog only records that it was accepted. */
	public void setOnAccept(Runnable onAccept) {
		this.onAccept = onAccept;
	}

	public void setInitialFocus(Component component) {
		this.initialFocus = component;
	}

	/** Shows a message in red inside the dialog, null or empty clears it. */
	public void showError(String message) {
		error.setText(message == null || message.isEmpty() ? " " : message);
	}

	public boolean isAccepted() {
		return accepted;
	}

	/** Validates and, when the input is fine, runs the accept action and closes. */
	public void accept() {
		String problem = validator.get();
		showError(problem);
		if (problem != null) {
			return;
		}
		accepted = true;
		onAccept.run();
		dispose();
	}

	/** Packs, centres on the owner and shows the dialog. For a modal dialog it returns when the dialog is closed: true when the input was accepted. */
	public boolean showDialog() {
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
		return accepted;
	}

	private void focusInitial() {
		if (initialFocus != null) {
			initialFocus.requestFocusInWindow();
			if (initialFocus instanceof JTextComponent text) {
				text.selectAll();
			}
		}
	}
}

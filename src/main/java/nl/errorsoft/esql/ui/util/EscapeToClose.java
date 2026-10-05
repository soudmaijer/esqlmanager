package nl.errorsoft.esql.ui.util;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * Lets Esc close every dialog, as if its close button was pressed. The dialog handles the close request itself, so a dialog that asks for confirmation
 * still does. A dialog opts out with {@code getRootPane().putClientProperty(EscapeToClose.DISABLED, true)}, for example a window with unsaved work or a
 * running job.
 */
public final class EscapeToClose {
	public static final String DISABLED = "EscapeToClose.disabled";

	private static final String ACTION = "EscapeToClose.close";
	private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private EscapeToClose() {
	}

	/** Registers the key for every dialog from the moment it opens. */
	public static void install() {
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof JDialog dialog) {
				bind(dialog);
			}
		}, AWTEvent.WINDOW_EVENT_MASK);
	}

	private static void bind(JDialog dialog) {
		JRootPane root = dialog.getRootPane();

		if (Boolean.TRUE.equals(root.getClientProperty(DISABLED)) || root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESCAPE) != null) {
			return;
		}

		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESCAPE, ACTION);
		root.getActionMap().put(ACTION, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
			}
		});
	}
}

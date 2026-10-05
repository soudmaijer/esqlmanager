package nl.errorsoft.esql.app;

import java.awt.Component;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.EsqlException;

/**
 * The one way errors reach the user. Services and repositories throw, controllers and windows catch and call {@link #report}.
 * It logs the error once and shows a message. An {@link EsqlException} is expected and logged without a stack trace.
 */
public class ErrorHandler {
	private static final Logger log = LogManager.getLogger(ErrorHandler.class);
	private static final String TITLE = "eSQLManager";

	private Component mainWindow;
	private Consumer<String> status = message -> {
	};

	/** The window messages are shown over when the caller has no window of its own. */
	public void setMainWindow(Component mainWindow, Consumer<String> status) {
		this.mainWindow = mainWindow;
		this.status = status;
	}

	public void report(String action, Throwable error) {
		report(mainWindow, action, error);
	}

	/**
	 * @param parent the window the message belongs to, so a dialog is not hidden behind its own modal child
	 * @param action what was being done, in words a user knows ("Drop table")
	 */
	public void report(Component parent, String action, Throwable error) {
		if (error instanceof EsqlException) {
			log.warn("{}: {}", action, error.getMessage());
		} else {
			log.error("{} failed", action, error);
		}

		String message = action + " failed: " + describe(error);
		status.accept("Error...");
		Runnable show = () -> JOptionPane.showMessageDialog(parent != null ? parent : mainWindow, message, TITLE, JOptionPane.WARNING_MESSAGE);

		// On the event thread the message is shown at once, so what follows (a question about continuing) comes after it.
		if (SwingUtilities.isEventDispatchThread()) {
			show.run();
		} else {
			SwingUtilities.invokeLater(show);
		}
	}

	/** The most specific message in the chain of causes, a driver message is clearer than the wrapper around it. */
	static String describe(Throwable error) {
		String message = null;

		for (Throwable current = error; current != null; current = current.getCause()) {
			if (current.getMessage() != null && !current.getMessage().isBlank()) {
				message = current.getMessage();
			}
		}
		return message != null ? message : error.getClass().getSimpleName();
	}
}

package nl.errorsoft.esql.error;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import java.awt.Component;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

		String message = message(action, error);
		status.accept("Error...");
		Runnable show = () -> Dialogs.error(parent != null ? parent : mainWindow, TITLE, message);

		// On the event thread the message is shown at once, so what follows (a question about continuing) comes after it.
		if (SwingUtilities.isEventDispatchThread()) {
			show.run();
		} else {
			SwingUtilities.invokeLater(show);
		}
	}

	/** "Drop table failed: cause", an action that already names the failure ("Unexpected error") is not followed by "failed". */
	static String message(String action, Throwable error) {
		boolean failure = action.toLowerCase().endsWith("error") || action.toLowerCase().endsWith("failed");
		return action + (failure ? ": " : " failed: ") + describe(error);
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

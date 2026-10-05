package nl.errorsoft.esql.server.control;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.server.ServerService;
import nl.errorsoft.esql.server.ui.dialog.ProcessListDialog;
import nl.errorsoft.esql.ui.dialog.Dialogs;

/**
 * Shows the process list of a server and refreshes it every few seconds. The list runs on a connection of its own, so a long query in the connection window
 * does not hold it up.
 */
public class ProcessListController {
	private static final Logger log = LogManager.getLogger(ProcessListController.class);
	/** The intervals offered in the window, in seconds. */
	public static final int[] INTERVALS = {1, 2, 5, 10};
	public static final int DEFAULT_INTERVAL = 5;
	private static final int TICK_MILLIS = 250;

	private final ConnectionProfile profile;
	private final ProcessListDialog dialog;
	private volatile boolean running = true;
	private volatile boolean paused;
	private volatile int intervalSeconds = DEFAULT_INTERVAL;
	/** Set when the list must be read at the next tick: after a resume or a change of the interval. */
	private volatile boolean refreshNow;
	private volatile ServerService servers;

	public ProcessListController(ConnectionProfile profile, JFrame parent) {
		this.profile = profile;
		this.dialog = new ProcessListDialog(this, parent, profile.getUsername() + "@" + profile.getHost() + " - active processes");
	}

	public void start() {
		dialog.setVisible(true);
		Thread.ofVirtual().name("process-list").start(this::refreshLoop);
	}

	/** Called by the window when it closes. */
	public void stop() {
		running = false;
	}

	/** Stops the refreshing, the list stays as it is; resuming reads it again at once. */
	public void setPaused(boolean paused) {
		this.paused = paused;
		if (!paused) {
			refreshNow = true;
		}
	}

	public void setInterval(int seconds) {
		intervalSeconds = seconds;
		refreshNow = true;
	}

	/** Kills a process on a virtual thread, the next refresh shows the result. */
	public void killProcess(String id) {
		ServerService service = servers;
		if (service == null) {
			Dialogs.warn(dialog, "Kill process", "The process list is not connected to the server. Close it and open it again to kill a process.");
			return;
		}
		Thread.ofVirtual().name("kill-process").start(() -> {
			try {
				service.killProcess(id);
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> ApplicationContext.get().errors().report(dialog, "Kill process", e));
			}
		});
	}

	private void refreshLoop() {
		DatabaseConnection connection = new DatabaseConnection();
		try (connection) {
			connection.connect(profile, "");
			servers = ApplicationContext.get().connection(connection).servers();
			long remainingMillis = 0;

			while (running) {
				if (connection.getConnection().isClosed()) {
					throw new EsqlException("The connection of the process list was closed.");
				}
				if (paused) {
					SwingUtilities.invokeLater(dialog::showPaused);
				} else {
					if (remainingMillis <= 0 || refreshNow) {
						refreshNow = false;
						List<ServerProcess> processes = servers.getProcesses();
						SwingUtilities.invokeLater(() -> dialog.showProcesses(processes));
						remainingMillis = intervalSeconds * 1000L;
					}
					int seconds = (int) Math.ceil(remainingMillis / 1000.0);
					SwingUtilities.invokeLater(() -> dialog.showCountdown(seconds));
					remainingMillis -= TICK_MILLIS;
				}
				Thread.sleep(TICK_MILLIS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			servers = null;
			if (running) {
				SwingUtilities.invokeLater(() -> {
					dialog.showDisconnected();
					ApplicationContext.get().errors().report(dialog, "Load processes", e);
				});
			}
		} finally {
			servers = null;
			ApplicationContext.get().release(connection);
		}
	}
}

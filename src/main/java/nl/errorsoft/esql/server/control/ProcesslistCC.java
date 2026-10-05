package nl.errorsoft.esql.server.control;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.server.ServerService;
import nl.errorsoft.esql.server.ui.Processlist;

/**
 * Shows the process list of a server and refreshes it every few seconds. The list runs on a connection of its own, so a long query in the connection window
 * does not hold it up.
 */
public class ProcesslistCC {
	private static final Logger log = LogManager.getLogger(ProcesslistCC.class);
	private static final int REFRESH_SECONDS = 5;

	private final ConnectionProfile profile;
	private final Processlist ui;
	private volatile boolean running = true;
	private volatile ServerService servers;

	public ProcesslistCC(ConnectionProfile profile, JFrame parent) {
		this.profile = profile;
		this.ui = new Processlist(this, parent, profile.getUsername() + "@" + profile.getHost() + " - active processes");
	}

	public void start() {
		ui.setVisible(true);
		Thread.ofVirtual().name("process-list").start(this::refreshLoop);
	}

	/** Called by the window when it closes. */
	public void stop() {
		running = false;
	}

	public void killProcess(String id) {
		try {
			ServerService service = servers;
			if (service != null) {
				service.killProcess(id);
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(ui, "Kill process", e);
		}
	}

	private void refreshLoop() {
		DatabaseConnection connection = new DatabaseConnection();
		try (connection) {
			connection.connect(profile, "");
			servers = ApplicationContext.get().connection(connection).servers();
			while (running && !connection.getConnection().isClosed()) {
				List<ServerProcess> processes = servers.getProcesses();
				SwingUtilities.invokeLater(() -> ui.showProcesses(processes));
				for (int i = REFRESH_SECONDS; i > 0 && running; i--) {
					int seconds = i;
					SwingUtilities.invokeLater(() -> ui.showCountdown(seconds));
					Thread.sleep(1000);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			servers = null;
			ApplicationContext.get().release(connection);
		}
	}
}

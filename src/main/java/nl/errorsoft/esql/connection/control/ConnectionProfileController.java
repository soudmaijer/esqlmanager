package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.database.DatabaseLister;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.connection.ui.dialog.ConnectionProfileDialog;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionProfileController {
	private static final Logger log = LogManager.getLogger(ConnectionProfileController.class);

	private MainController mainController;
	private ConnectionProfileDialog profileDialog;
	private ConnectionProfile cp;

	public ConnectionProfileController(MainController mainController) {
		this.mainController = mainController;

		try {
			cp = new ConnectionProfile();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Load profiles", e);
		}
	}

	public void showDialog(MainWindow mainWindow, boolean autoConnect) {
		// Create Frame.
		mainWindow.updateStatus("Starting profile manager...", true);
		ConnectionProfile[] cpa = cp.getProfiles();
		log.info("Loaded {} connection profile(s) from conf/profiles.xml", cpa.length);
		boolean conLastUsed = false;

		if (autoConnect) {
			for (int i = 0; i < cpa.length; i++) {
				if (cpa[i].isAutoConnect()) {
					connect(cpa[i]);
					conLastUsed = true;
				}
			}
		}
		if (!conLastUsed) {
			profileDialog = new ConnectionProfileDialog(mainWindow, this);
			profileDialog.loadProfiles(cp.getProfiles());
			mainController.showConnectionState();
			profileDialog.setVisible(true);
		}
	}

	public void connect(ConnectionProfile selcp) {
		try {
			if (!selcp.isSavePassword() && selcp.getPassword().isEmpty()) {
				String password = askPassword(selcp);
				if (password == null) {
					return;
				}
				selcp.setPassword(password);
			}
			cp.setLastUsed(selcp);
			mainController.openConnectionWindow(selcp);

			// An auto-connect at startup happens before the profile dialog exists.
			if (profileDialog != null) {
				profileDialog.dispose();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(profileDialog, "Connect", e);
		}
	}

	/** Asks for the password of a profile that does not keep it, null when the user cancels. */
	private String askPassword(ConnectionProfile profile) {
		JPasswordField field = new JPasswordField(20);
		JPanel form = new Forms.Grid().row("&Password:", field).panel();
		Component parent = profileDialog != null ? profileDialog : mainController.getMainWindow();
		boolean accepted = Dialogs.form(parent, "Password for " + profile.getName(), form, "Connect", field, () -> null);
		return accepted ? new String(field.getPassword()) : null;
	}

	public void addProfile(ConnectionProfile typed) {
		try {
			mainController.updateStatus("Adding profile...", true);

			if (!cp.profileExists(typed.getName())) {
				cp.addProfile(typed);
				profileDialog.loadProfiles(cp.getProfiles());
				profileDialog.setSelectedProfile(typed);
				mainController.showConnectionState();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(profileDialog, "Add profile", e);
		}
	}

	/** Saves what is typed in the form over a saved profile. */
	public void editProfile(ConnectionProfile saved, ConnectionProfile typed) {
		try {
			mainController.updateStatus("Saving profile...", true);
			saved.setName(typed.getName());
			saved.setHost(typed.getHost());
			saved.setPort(typed.getPort());
			saved.setUsername(typed.getUsername());
			saved.setPassword(typed.getPassword());
			saved.setSavePassword(typed.isSavePassword());
			saved.setSelection(typed.getSelection());
			saved.setServerType(typed.getServerType());
			saved.setAutoConnect(typed.isAutoConnect());
			cp.editProfile(saved);
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(profileDialog, "Edit profile", e);
		}
	}

	/** Saves a copy of a saved profile as "name copy" and selects it. */
	public void duplicateProfile(ConnectionProfile saved) {
		try {
			ConnectionProfile copy = saved.copyAs(cp.uniqueCopyName(saved.getName()));
			cp.addProfile(copy);
			profileDialog.loadProfiles(cp.getProfiles());
			profileDialog.setSelectedProfile(copy);
			log.info("Profile '{}' duplicated as '{}'", saved.getName(), copy.getName());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(profileDialog, "Duplicate profile", e);
		}
	}

	/** The outcome of a connection test: the server that answered and its databases, or the reason it did not answer (no databases). */
	public record TestResult(boolean success, String message, List<String> databases) {
	}

	/** The scratch connection of the databases tab with the one thread that uses it. */
	private record Session(DatabaseLister catalog, ExecutorService worker) {
		/** Closes the connection after the work that is queued. */
		void close() {
			worker.execute(catalog::close);
			worker.shutdown();
		}
	}

	private Session session;
	/** Counts the changes of the connection settings (event thread only); an answer that belongs to an older number is ignored. */
	private int generation;

	/**
	 * Connects with the given settings on a virtual thread, lists the databases of the server and hands the outcome to the callback on the event thread. The
	 * connection stays open for loading schemas, until {@link #invalidate()} or the next test. The saved profiles are not touched.
	 */
	public void testConnection(ConnectionProfile profile, Consumer<TestResult> callback) {
		invalidate();
		int started = generation;
		Session opened = new Session(new DatabaseLister(profile), Executors.newSingleThreadExecutor(Thread.ofVirtual().name("catalog").factory()));
		session = opened;
		opened.worker().execute(() -> {
			TestResult result;
			try {
				opened.catalog().connect();
				result = new TestResult(true, "Connected to " + opened.catalog().serverDescription(), opened.catalog().databases());
				log.info("Connection test to {} succeeded", profile.getHost());
			} catch (Exception e) {
				log.warn("Connection test to {} failed: {}", profile.getHost(), e.getMessage());
				result = new TestResult(false, e.getMessage() == null ? e.toString() : e.getMessage(), List.of());
			}
			TestResult outcome = result;
			SwingUtilities.invokeLater(() -> {
				if (started != generation) {
					return;
				}
				if (!outcome.success()) {
					closeSession();
				}
				callback.accept(outcome);
			});
		});
	}

	/** Lists the databases of the server again over the open connection. */
	public void reloadDatabases(Consumer<List<String>> callback) {
		Session current = session;
		int started = generation;
		if (current == null) {
			return;
		}
		current.worker().execute(() -> {
			try {
				List<String> databases = current.catalog().databases();
				SwingUtilities.invokeLater(() -> {
					if (started == generation) {
						callback.accept(databases);
					}
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					if (started == generation) {
						ApplicationContext.get().errors().report(profileDialog, "Reload databases", e);
					}
				});
			}
		});
	}

	/** Loads the schemas of one database over the open connection; the callbacks run on the event thread, and not at all when the settings changed. */
	public void loadSchemas(String database, Consumer<List<String>> loaded, Runnable failed) {
		Session current = session;
		int started = generation;
		if (current == null) {
			return;
		}
		current.worker().execute(() -> {
			try {
				List<String> schemas = current.catalog().schemas(database);
				SwingUtilities.invokeLater(() -> {
					if (started == generation) {
						loaded.accept(schemas);
					}
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> {
					if (started == generation) {
						ApplicationContext.get().errors().report(profileDialog, "Load schemas", e);
						failed.run();
					}
				});
			}
		});
	}

	/** The connection settings changed or the dialog closed: answers still on their way are ignored and the scratch connection is closed. */
	public void invalidate() {
		generation++;
		closeSession();
	}

	private void closeSession() {
		if (session != null) {
			session.close();
			session = null;
		}
	}

	public void deleteProfile(ConnectionProfile cp) {
		try {
			mainController.updateStatus("Deleting profile...", true);
			this.cp.deleteProfile(cp);
			profileDialog.loadProfiles(this.cp.getProfiles());
			mainController.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(profileDialog, "Delete profile", e);
		}
	}
}

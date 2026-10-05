package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.connection.ui.ConnectionProfileUI;

import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionProfileCC {
	private static final Logger log = LogManager.getLogger(ConnectionProfileCC.class);

	private ESQLManagerCC jmcc;
	private ConnectionProfileUI cpui;
	private ConnectionProfile cp;

	public ConnectionProfileCC(ESQLManagerCC jmcc) {
		this.jmcc = jmcc;

		try {
			cp = new ConnectionProfile();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Load profiles", e);
		}
	}

	public void startUI(ESQLManagerUI jmui, boolean autoConnect) {
		// Create Frame.
		jmui.updateStatus("Starting profile manager...", true);
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
			cpui = new ConnectionProfileUI(jmui, this);
			cpui.loadProfiles(cp.getProfiles());
			jmcc.showConnectionState();
			cpui.setVisible(true);
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
			jmcc.dispatchConnectionWindowUI(selcp);

			// An auto-connect at startup happens before the profile dialog exists.
			if (cpui != null) {
				cpui.dispose();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Connect", e);
		}
	}

	/** Asks for the password of a profile that does not keep it, null when the user cancels. */
	private String askPassword(ConnectionProfile profile) {
		JPasswordField field = new JPasswordField(20);
		JPanel form = new Forms.Grid().row("&Password:", field).panel();
		Component parent = cpui != null ? cpui : jmcc.getUI();
		boolean accepted = Dialogs.form(parent, "Password for " + profile.getName(), form, "Connect", field, () -> null);
		return accepted ? new String(field.getPassword()) : null;
	}

	public void addProfile(ConnectionProfile typed) {
		try {
			jmcc.updateStatus("Adding profile...", true);

			if (!cp.profileExists(typed.getName())) {
				cp.addProfile(typed);
				cpui.loadProfiles(cp.getProfiles());
				cpui.setSelectedProfile(typed);
				jmcc.showConnectionState();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Add profile", e);
		}
	}

	/** Saves what is typed in the form over a saved profile. */
	public void editProfile(ConnectionProfile saved, ConnectionProfile typed) {
		try {
			jmcc.updateStatus("Saving profile...", true);
			saved.setName(typed.getName());
			saved.setHost(typed.getHost());
			saved.setPort(typed.getPort());
			saved.setUsername(typed.getUsername());
			saved.setPassword(typed.getPassword());
			saved.setSavePassword(typed.isSavePassword());
			saved.setDatabases(typed.getDatabases());
			saved.setServerType(typed.getServerType());
			saved.setAutoConnect(typed.isAutoConnect());
			cp.editProfile(saved);
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Edit profile", e);
		}
	}

	/** Saves a copy of a saved profile as "name copy" and selects it. */
	public void duplicateProfile(ConnectionProfile saved) {
		try {
			ConnectionProfile copy = saved.copyAs(cp.uniqueCopyName(saved.getName()));
			cp.addProfile(copy);
			cpui.loadProfiles(cp.getProfiles());
			cpui.setSelectedProfile(copy);
			log.info("Profile '{}' duplicated as '{}'", saved.getName(), copy.getName());
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Duplicate profile", e);
		}
	}

	/** The outcome of a connection test: the server that answered, or the reason it did not. */
	public record TestResult(boolean success, String message) {
	}

	/**
	 * Connects with the given settings on a virtual thread, closes the connection again and hands the outcome to the callback on the event thread.
	 * The saved profiles are not touched.
	 */
	public void testConnection(ConnectionProfile profile, Consumer<TestResult> callback) {
		Thread.ofVirtual().name("test-connection").start(() -> {
			TestResult result;
			try (DatabaseConnection connection = new DatabaseConnection()) {
				connection.connect(profile, "");
				result = new TestResult(true, "Connected to " + connection.getServerDescription());
				log.info("Connection test to {} succeeded", profile.getHost());
			} catch (Exception e) {
				log.warn("Connection test to {} failed: {}", profile.getHost(), e.getMessage());
				result = new TestResult(false, e.getMessage() == null ? e.toString() : e.getMessage());
			}
			TestResult outcome = result;
			SwingUtilities.invokeLater(() -> callback.accept(outcome));
		});
	}

	public void deleteProfile(ConnectionProfile cp) {
		try {
			jmcc.updateStatus("Deleting profile...", true);
			this.cp.deleteProfile(cp);
			cpui.loadProfiles(this.cp.getProfiles());
			jmcc.showConnectionState();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Delete profile", e);
		}
	}
}

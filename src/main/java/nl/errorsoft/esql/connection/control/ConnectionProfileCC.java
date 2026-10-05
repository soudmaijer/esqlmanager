package nl.errorsoft.esql.connection.control;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.ui.ConnectionProfileUI;

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
			ApplicationContext.get().errors().report("Connection profile cc", e);
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
			cp.setLastUsed(selcp);
			jmcc.dispatchConnectionWindowUI(selcp);
			cpui.dispose();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Connect", e);
		}
	}

	public void addProfile(String name, ServerType type, String host, String port, String username, String password, String databases, boolean autoConnect) {
		try {
			jmcc.updateStatus("Adding profile...", true);
			ConnectionProfile cpt = new ConnectionProfile();

			if (!cp.profileExists(name)) {
				cpt.setName(name);
				cpt.setHost(host);
				cpt.setPort(port);
				cpt.setUsername(username);
				cpt.setPassword(password);
				cpt.setDatabases(databases);
				cpt.setAutoConnect(autoConnect);
				cpt.setServerType(type);
				cp.addProfile(cpt);
				cpui.loadProfiles(cp.getProfiles());
				cpui.setSelectedProfile(cpt);
				jmcc.showConnectionState();
				cpui.showMessage("Profile added succesfully!");
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Add profile", e);
		}
	}

	public void editProfile(ConnectionProfile cpt, String name, ServerType type, String host, String port, String username, String password, String databases,
		boolean autoConnect) {
		try {
			jmcc.updateStatus("Saving profile...", true);
			cpt.setName(name);
			cpt.setHost(host);
			cpt.setPort(port);
			cpt.setUsername(username);
			cpt.setPassword(password);
			cpt.setDatabases(databases);
			cpt.setServerType(type);
			cpt.setAutoConnect(autoConnect);
			cp.editProfile(cpt);
			jmcc.showConnectionState();
			cpui.showMessage("Saved changes!");
		} catch (Exception e) {
			ApplicationContext.get().errors().report(cpui, "Edit profile", e);
		}
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

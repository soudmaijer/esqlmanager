package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.app.DataDirectory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class ConnectionProfile {
	private static final Logger log = LogManager.getLogger(ConnectionProfile.class);

	private String name = "";
	private DatabaseSelection selection = DatabaseSelection.NONE;
	private String host = "";
	private String port = "";
	private String username = "";
	private String password = "";
	private ServerType st;
	private boolean lastUsed = false;
	private boolean autoConnect = false;
	private boolean savePassword = true;
	private org.jdom.Document profileData;

	public ConnectionProfile() {
		try {
			SAXBuilder builder = new SAXBuilder();
			profileData = builder.build(DataDirectory.file("conf/profiles.xml"));
		} catch (Exception e) {
			log.warn("Warning: profiles.xml could not be loaded, no profiles will be available!");
		}
	}

	/** A profile that is not tied to the profile file, for the settings of one connection. */
	public static ConnectionProfile plain() {
		return new ConnectionProfile(null);
	}

	private ConnectionProfile(org.jdom.Document profileData) {
		this.profileData = profileData;
	}

	public ConnectionProfile[] getProfiles() {
		try {
			if (profileData != null && profileData.hasRootElement()) {
				java.util.List<?> profiles = profileData.getRootElement().getChildren("profile");
				ConnectionProfile[] p = new ConnectionProfile[profiles.size()];

				for (int i = 0; i < profiles.size(); i++) {
					p[i] = ProfileXml.read((org.jdom.Element) profiles.get(i));
				}
				return p;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return new ConnectionProfile[0];
	}

	public ServerType getServerType() {
		return st;
	}

	public void setServerType(ServerType st) {
		this.st = st;
	}

	public boolean profileExists(String name) throws Exception {
		if (profileData.hasRootElement()) {
			java.util.List<?> l = profileData.getRootElement().getChildren("profile");

			for (int i = 0; i < l.size(); i++) {
				if (((org.jdom.Element) l.get(i)).getChild("name").getText().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setLastUsed(ConnectionProfile cp) throws Exception {
		java.util.List<?> profiles = null;

		if (profileData.hasRootElement()) {
			profiles = profileData.getRootElement().getChildren("profile");
		}

		for (int i = 0; i < profiles.size(); i++) {
			org.jdom.Element temp = (org.jdom.Element) profiles.get(i);

			if (temp.getChild("lastUsed") != null) {
				if (temp.getChild("name").getText().equalsIgnoreCase(cp.getName())) {
					((org.jdom.Element) profiles.get(i)).getChild("lastUsed").setText("true");
				} else {
					((org.jdom.Element) profiles.get(i)).getChild("lastUsed").setText("false");
				}
			}
		}
		save(profileData);
	}

	public void addProfile(ConnectionProfile cp) throws Exception {
		if (profileData.hasRootElement()) {
			org.jdom.Element newElement = new org.jdom.Element("profile");
			ProfileXml.write(newElement, cp);
			profileData.getRootElement().addContent(newElement);
		}
		save(profileData);
	}

	public void editProfile(ConnectionProfile profile) throws Exception {
		if (profileData.hasRootElement()) {
			java.util.List<?> l = profileData.getRootElement().getChildren("profile");

			for (int i = 0; i < l.size(); i++) {
				org.jdom.Element element = (org.jdom.Element) l.get(i);

				if (profile.getName().equalsIgnoreCase(element.getChildText("name"))) {
					ProfileXml.write(element, profile);
				} else if (profile.isAutoConnect() && element.getChild("autoConnect") != null) {
					element.getChild("autoConnect").setText("false");
				}
			}
			save(profileData);
		}
	}

	/** A copy of the connection settings (not of the profile file) under another name, not auto-connecting and not last used. */
	public ConnectionProfile copyAs(String newName) {
		ConnectionProfile copy = plain();
		copy.setName(newName);
		copy.setHost(host);
		copy.setPort(port);
		copy.setUsername(username);
		copy.setPassword(password);
		copy.setSavePassword(savePassword);
		copy.setSelection(selection);
		copy.setServerType(st);
		return copy;
	}

	/** The first of "name copy", "name copy 2", ... that no profile has. */
	public String uniqueCopyName(String baseName) throws Exception {
		String candidate = baseName + " copy";
		for (int n = 2; profileExists(candidate); n++) {
			candidate = baseName + " copy " + n;
		}
		return candidate;
	}

	public void deleteProfile(ConnectionProfile profile) throws Exception {
		if (profileData.hasRootElement()) {
			java.util.List<?> l = profileData.getRootElement().getChildren("profile");

			for (int i = 0; i < l.size(); i++) {
				if (((org.jdom.Element) l.get(i)).getChild("name").getText().equalsIgnoreCase(profile.getName())) {
					profileData.getRootElement().removeContent((org.jdom.Element) l.get(i));
					save(profileData);
					break;
				}
			}
		}
	}

	public void save(org.jdom.Document doc) throws Exception {
		org.jdom.output.XMLOutputter xmlout = new org.jdom.output.XMLOutputter();
		try (PrintWriter out = new PrintWriter(DataDirectory.file("conf/profiles.xml"), java.nio.charset.StandardCharsets.UTF_8)) {
			xmlout.output(doc, out);
		}
	}

	public String getName() {
		if (name == null) {
			return "";
		}
		return name;
	}

	/** The selected databases as a comma separated list, the first is the one connected to. Schemas are in {@link #getSelection()}. */
	public String getDatabases() {
		return selection.toCsv();
	}

	/** Selects the databases of a comma separated list; schemas chosen for a database that stays selected are kept. */
	public void setDatabases(String databases) {
		java.util.Map<String, java.util.Set<String>> kept = new java.util.LinkedHashMap<>();
		for (String name : DatabaseSelection.parse(databases).databases()) {
			kept.put(name, selection.schemasOf(name));
		}
		selection = new DatabaseSelection(kept);
	}

	public DatabaseSelection getSelection() {
		return selection;
	}

	public void setSelection(DatabaseSelection selection) {
		this.selection = selection;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		if (name == null) {
			return "";
		}
		return username;
	}

	public boolean isLastUsed() {
		return lastUsed;
	}

	public void setLastUsed(boolean lastUsed) {
		this.lastUsed = lastUsed;
	}

	public boolean isAutoConnect() {
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}

	/** Whether the password is written to profiles.xml; when off, it is asked for when connecting. */
	public boolean isSavePassword() {
		return savePassword;
	}

	public void setSavePassword(boolean savePassword) {
		this.savePassword = savePassword;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		if (password == null) {
			return "";
		}

		return password;
	}

	public String toString() {
		return name;
	}
}

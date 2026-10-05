package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.app.DataDirectory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import nl.errorsoft.esql.error.EsqlException;
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
	private ServerType serverType;
	private boolean lastUsed = false;
	private boolean autoConnect = false;
	private boolean savePassword = true;
	private org.jdom.Document profileData;
	/** Why profiles.xml could not be read; then nothing is read from or written to it, so a damaged file is not overwritten. */
	private EsqlException loadProblem;

	/** The profiles of conf/profiles.xml. A missing file means no profiles yet; a file that cannot be read makes every use throw. */
	public ConnectionProfile() {
		File file = DataDirectory.file("conf/profiles.xml");

		if (!file.isFile()) {
			// A first start without the file: no profiles, the first save creates it.
			profileData = new Document(new Element("profiles"));
			return;
		}
		try {
			profileData = new SAXBuilder().build(file);
		} catch (Exception e) {
			loadProblem = new EsqlException("profiles.xml cannot be read: " + e.getMessage(), e);
		}
	}

	/** The document of profiles.xml; throws when the file could not be read. */
	private Document document() {
		if (loadProblem != null) {
			throw loadProblem;
		}
		return profileData;
	}

	/** A profile that is not tied to the profile file, for the settings of one connection. */
	public static ConnectionProfile plain() {
		return new ConnectionProfile(null);
	}

	private ConnectionProfile(org.jdom.Document profileData) {
		this.profileData = profileData;
	}

	/** The saved profiles; one that cannot be used (an unknown server type) is left out with a warning in the log. */
	public ConnectionProfile[] getProfiles() {
		java.util.List<ConnectionProfile> profiles = new java.util.ArrayList<>();

		if (document() != null && document().hasRootElement()) {
			for (Object element : document().getRootElement().getChildren("profile")) {
				try {
					profiles.add(ProfileXml.read((Element) element));
				} catch (EsqlException e) {
					log.warn("Profile skipped: {}", e.getMessage());
				}
			}
		}
		return profiles.toArray(new ConnectionProfile[0]);
	}

	public ServerType getServerType() {
		return serverType;
	}

	public void setServerType(ServerType serverType) {
		this.serverType = serverType;
	}

	public boolean profileExists(String name) throws Exception {
		if (document().hasRootElement()) {
			java.util.List<?> profileElements = document().getRootElement().getChildren("profile");

			for (int i = 0; i < profileElements.size(); i++) {
				if (((org.jdom.Element) profileElements.get(i)).getChild("name").getText().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setLastUsed(ConnectionProfile profile) throws Exception {
		java.util.List<?> profiles = null;

		if (document().hasRootElement()) {
			profiles = document().getRootElement().getChildren("profile");
		}

		for (int i = 0; i < profiles.size(); i++) {
			org.jdom.Element profileElement = (org.jdom.Element) profiles.get(i);

			if (profileElement.getChild("lastUsed") != null) {
				if (profileElement.getChild("name").getText().equalsIgnoreCase(profile.getName())) {
					((org.jdom.Element) profiles.get(i)).getChild("lastUsed").setText("true");
				} else {
					((org.jdom.Element) profiles.get(i)).getChild("lastUsed").setText("false");
				}
			}
		}
		save(document());
	}

	public void addProfile(ConnectionProfile profile) throws Exception {
		if (document().hasRootElement()) {
			org.jdom.Element newElement = new org.jdom.Element("profile");
			ProfileXml.write(newElement, profile);
			document().getRootElement().addContent(newElement);
		}
		save(document());
	}

	public void editProfile(ConnectionProfile profile) throws Exception {
		editProfile(profile.getName(), profile);
	}

	/** Writes the settings over the saved profile that is called {@code previousName}, which renames it when the name of {@code profile} is another. */
	public void editProfile(String previousName, ConnectionProfile profile) throws Exception {
		if (document().hasRootElement()) {
			java.util.List<?> profileElements = document().getRootElement().getChildren("profile");

			for (int i = 0; i < profileElements.size(); i++) {
				org.jdom.Element element = (org.jdom.Element) profileElements.get(i);

				if (previousName.equalsIgnoreCase(element.getChildText("name"))) {
					ProfileXml.write(element, profile);
				} else if (profile.isAutoConnect() && element.getChild("autoConnect") != null) {
					element.getChild("autoConnect").setText("false");
				}
			}
			save(document());
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
		copy.setServerType(serverType);
		return copy;
	}

	/** Whether both hold the same name and connection settings (the last used flag is not a setting). */
	public boolean sameSettings(ConnectionProfile other) {
		return getName().equals(other.getName()) && getHost().equals(other.getHost()) && getPort().equals(other.getPort())
			&& getUsername().equals(other.getUsername()) && getPassword().equals(other.getPassword())
			&& (serverType == null ? other.serverType == null : other.serverType != null && serverType.getType() == other.serverType.getType())
			&& selection.equals(other.selection)
			&& autoConnect == other.autoConnect && savePassword == other.savePassword;
	}

	/** {@code baseName} when no profile has it, else the first of "baseName 2", "baseName 3", ... that no profile has. */
	public String uniqueName(String baseName) throws Exception {
		String candidate = baseName;
		for (int n = 2; profileExists(candidate); n++) {
			candidate = baseName + " " + n;
		}
		return candidate;
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
		if (document().hasRootElement()) {
			java.util.List<?> profileElements = document().getRootElement().getChildren("profile");

			for (int i = 0; i < profileElements.size(); i++) {
				if (((org.jdom.Element) profileElements.get(i)).getChild("name").getText().equalsIgnoreCase(profile.getName())) {
					document().getRootElement().removeContent((org.jdom.Element) profileElements.get(i));
					save(document());
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

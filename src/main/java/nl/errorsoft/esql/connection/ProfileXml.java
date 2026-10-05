package nl.errorsoft.esql.connection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jdom.Element;

/**
 * Reads and writes one {@code <profile>} element of conf/profiles.xml. Elements that are missing in older files get a default. The selection is stored as
 * {@code <databases>db1,db2</databases>} (what older versions understand) and, only for databases with chosen schemas,
 * {@code <schemas><database name="db1"><schema>public</schema></database></schemas>}.
 */
public final class ProfileXml {
	private ProfileXml() {
	}

	/** A profile (not auto-connecting, not last used unless the file says so) from the element. */
	public static ConnectionProfile read(Element element) {
		ConnectionProfile profile = ConnectionProfile.plain();
		profile.setName(text(element, "name"));
		profile.setHost(text(element, "host"));
		profile.setPort(text(element, "port"));
		profile.setUsername(text(element, "username"));
		profile.setPassword(text(element, "password"));
		profile.setServerType(new ServerType(parseInt(text(element, "serverType"))));
		profile.setLastUsed(Boolean.parseBoolean(text(element, "lastUsed")));
		profile.setAutoConnect(Boolean.parseBoolean(text(element, "autoConnect")));
		// Profiles written before the option existed keep their password.
		profile.setSavePassword(!"false".equals(element.getChildText("savePassword")));
		profile.setSelection(readSelection(element));
		return profile;
	}

	/** Puts the settings of the profile in the element, creating the children that are missing. The last used flag is only set on a new element. */
	public static void write(Element element, ConnectionProfile profile) {
		set(element, "name", profile.getName());
		set(element, "host", profile.getHost());
		set(element, "port", profile.getPort());
		set(element, "username", profile.getUsername());
		set(element, "password", profile.isSavePassword() ? profile.getPassword() : "");
		set(element, "savePassword", Boolean.toString(profile.isSavePassword()));
		set(element, "serverType", Integer.toString(profile.getServerType().getType()));
		set(element, "databases", profile.getDatabases());
		set(element, "autoConnect", Boolean.toString(profile.isAutoConnect()));
		if (element.getChild("lastUsed") == null) {
			set(element, "lastUsed", "false");
		}
		writeSchemas(element, profile.getSelection());
	}

	private static DatabaseSelection readSelection(Element element) {
		Map<String, Set<String>> selected = new LinkedHashMap<>();
		for (String database : DatabaseSelection.parse(text(element, "databases")).databases()) {
			selected.put(database, new LinkedHashSet<>());
		}

		Element schemas = element.getChild("schemas");
		if (schemas != null) {
			for (Object child : schemas.getChildren("database")) {
				Element database = (Element) child;
				// Schemas of a database that is not selected are dropped.
				Set<String> names = selected.get(database.getAttributeValue("name"));
				if (names != null) {
					for (Object schema : database.getChildren("schema")) {
						names.add(((Element) schema).getText());
					}
				}
			}
		}
		return new DatabaseSelection(selected);
	}

	private static void writeSchemas(Element element, DatabaseSelection selection) {
		element.removeChildren("schemas");
		if (!selection.hasSchemaFilter()) {
			return;
		}
		Element schemas = new Element("schemas");
		for (String database : selection.databases()) {
			Set<String> names = selection.schemasOf(database);
			if (!names.isEmpty()) {
				Element databaseElement = new Element("database").setAttribute("name", database);
				for (String name : names) {
					databaseElement.addContent(new Element("schema").setText(name));
				}
				schemas.addContent(databaseElement);
			}
		}
		element.addContent(schemas);
	}

	private static String text(Element element, String child) {
		String text = element.getChildText(child);
		return text == null ? "" : text;
	}

	private static int parseInt(String text) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return ServerType.MY_SQL;
		}
	}

	private static void set(Element parent, String name, String value) {
		Element child = parent.getChild(name);
		if (child == null) {
			child = new Element(name);
			parent.addContent(child);
		}
		child.setText(value);
	}
}

package nl.errorsoft.esql.connection;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.xml.XmlFiles;

/**
 * Reads and writes one {@code <profile>} element of conf/profiles.xml. Elements that are missing in older files get a default. The selection is stored as
 * {@code <databases>db1,db2</databases>} (what older versions understand) and, only for databases with chosen schemas,
 * {@code <schemas><database name="db1"><schema>public</schema></database></schemas>}.
 */
public final class ProfileXml {
	private ProfileXml() {
	}

	/** A profile (not auto-connecting, not last used unless the file says so) from the element; an unknown server type is an {@link EsqlException}. */
	public static ConnectionProfile read(Element element) {
		ConnectionProfile profile = ConnectionProfile.plain();
		profile.setName(text(element, "name"));
		profile.setHost(text(element, "host"));
		profile.setPort(text(element, "port"));
		profile.setUsername(text(element, "username"));
		profile.setPassword(text(element, "password"));
		profile.setServerType(serverType(element));
		profile.setLastUsed(Boolean.parseBoolean(text(element, "lastUsed")));
		profile.setAutoConnect(Boolean.parseBoolean(text(element, "autoConnect")));
		// Profiles written before the option existed keep their password.
		profile.setSavePassword(!"false".equals(XmlFiles.childText(element, "savePassword")));
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
		if (XmlFiles.child(element, "lastUsed") == null) {
			set(element, "lastUsed", "false");
		}
		writeSchemas(element, profile.getSelection());
	}

	private static DatabaseSelection readSelection(Element element) {
		Map<String, Set<String>> selected = new LinkedHashMap<>();
		for (String database : DatabaseSelection.parse(text(element, "databases")).databases()) {
			selected.put(database, new LinkedHashSet<>());
		}

		Element schemas = XmlFiles.child(element, "schemas");
		if (schemas != null) {
			for (Element database : XmlFiles.children(schemas, "database")) {
				// Schemas of a database that is not selected are dropped.
				Set<String> names = selected.get(XmlFiles.attribute(database, "name", null));
				if (names != null) {
					for (Element schema : XmlFiles.children(database, "schema")) {
						names.add(schema.getTextContent());
					}
				}
			}
		}
		return new DatabaseSelection(selected);
	}

	private static void writeSchemas(Element element, DatabaseSelection selection) {
		XmlFiles.removeChildren(element, "schemas");
		if (!selection.hasSchemaFilter()) {
			return;
		}
		Element schemas = XmlFiles.addChild(element, "schemas");
		for (String database : selection.databases()) {
			Set<String> names = selection.schemasOf(database);
			if (!names.isEmpty()) {
				Element databaseElement = XmlFiles.addChild(schemas, "database");
				XmlFiles.setAttribute(databaseElement, "name", database);
				for (String name : names) {
					XmlFiles.addChild(databaseElement, "schema", name);
				}
			}
		}
	}

	private static String text(Element element, String child) {
		return XmlFiles.childText(element, child, "");
	}

	/**
	 * The server type of the profile. A missing element gets the default of the oldest files, MySQL; a value that is not a known type is refused, so a
	 * profile is never opened with the dialect of another server.
	 */
	private static ServerType serverType(Element element) {
		String text = text(element, "serverType").trim();
		if (text.isEmpty()) {
			return new ServerType(ServerType.MY_SQL);
		}
		try {
			int type = Integer.parseInt(text);
			if (ServerType.isKnown(type)) {
				return new ServerType(type);
			}
		} catch (NumberFormatException e) {
			// Not a number: refused below like an unknown number.
		}
		throw new EsqlException("profile '" + text(element, "name") + "' has an unknown server type '" + text + "'");
	}

	private static void set(Element parent, String name, String value) {
		XmlFiles.setChildText(parent, name, value);
	}
}

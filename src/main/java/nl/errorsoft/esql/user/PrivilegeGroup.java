package nl.errorsoft.esql.user;

import java.util.Locale;
import java.util.Set;

/** The kind of a privilege, to show the privileges of an account in groups. The grouping goes by the name of the privilege and is the same for every server. */
public enum PrivilegeGroup {
	DATA("Data"), STRUCTURE("Structure"), ADMINISTRATION("Administration");

	private static final Set<String> DATA_PRIVILEGES = Set.of("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE");
	private static final Set<String> STRUCTURE_PRIVILEGES = Set.of("CREATE", "DROP", "ALTER", "INDEX", "REFERENCES", "TRIGGER", "TEMPORARY");

	private final String title;

	PrivilegeGroup(String title) {
		this.title = title;
	}

	public String title() {
		return title;
	}

	/** The group of a privilege; everything that is not about rows or tables is administration (RELOAD, SUPERUSER, CONNECT, ...). */
	public static PrivilegeGroup of(String privilege) {
		String name = privilege.toUpperCase(Locale.ROOT);
		if (DATA_PRIVILEGES.contains(name)) {
			return DATA;
		}
		return STRUCTURE_PRIVILEGES.contains(name) ? STRUCTURE : ADMINISTRATION;
	}
}

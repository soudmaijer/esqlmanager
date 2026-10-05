package nl.errorsoft.esql.dialect.postgres;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.GrantQuery;
import nl.errorsoft.esql.dialect.UserAdmin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantTarget;

/**
 * Accounts are roles. What MySQL calls global privileges are role attributes here,
 * database and table privileges are the ones recorded in the access control lists of those objects.
 */
public class PostgresUserAdmin implements UserAdmin {
	private static final List<String> ROLE_ATTRIBUTES = Arrays.asList("LOGIN", "SUPERUSER", "CREATEDB", "CREATEROLE", "REPLICATION");
	private static final List<String> DATABASE_PRIVILEGES = Arrays.asList("CONNECT", "CREATE", "TEMPORARY");
	private static final List<String> TABLE_PRIVILEGES = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER");

	private final Dialect dialect;

	public PostgresUserAdmin(Dialect dialect) {
		this.dialect = dialect;
	}

	public boolean usesHost() {
		return false;
	}

	public String listUsersSql() {
		return "SELECT rolname FROM pg_roles WHERE rolname !~ '^pg_' ORDER BY rolname";
	}

	public DatabaseUser readUser(ResultSet rs) throws SQLException {
		return new DatabaseUser(rs.getString(1), null);
	}

	public String createUserSql(DatabaseUser user, String password) {
		return "CREATE ROLE " + dialect.quote(user.name()) + " LOGIN" + passwordClause(password);
	}

	public String changePasswordSql(DatabaseUser user, String password) {
		return "ALTER ROLE " + dialect.quote(user.name()) + (password.length() == 0 ? " PASSWORD NULL" : passwordClause(password));
	}

	public String dropUserSql(DatabaseUser user) {
		return "DROP ROLE " + dialect.quote(user.name());
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return switch (scope) {
			case GLOBAL -> ROLE_ATTRIBUTES;
			case DATABASE -> DATABASE_PRIVILEGES;
			default -> TABLE_PRIVILEGES;
		};
	}

	/** Table privileges are read in the table's database, the connection only sees the catalog of the database it is using. */
	public GrantQuery grantsQuery(DatabaseUser user, GrantTarget target) {
		return switch (target.scope()) {
			case GLOBAL -> new GrantQuery("SELECT rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolreplication FROM pg_roles WHERE rolname = ?",
				List.of(user.name()), null);
			case DATABASE -> new GrantQuery(
				"SELECT a.privilege_type FROM pg_database d, aclexplode(d.datacl) a JOIN pg_roles r ON r.oid = a.grantee WHERE d.datname = ? AND r.rolname = ?",
				List.of(target.database(), user.name()), null);
			default -> new GrantQuery(
				"""
					SELECT a.privilege_type FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, aclexplode(c.relacl) a JOIN pg_roles r ON r.oid = a.grantee \
					WHERE n.nspname = current_schema() AND c.relname = ? AND r.rolname = ?""",
				List.of(target.table(), user.name()), target.database());
		};
	}

	/** Role attributes are one row of flags, the other scopes a row per privilege. */
	public Set<String> readGrants(GrantTarget.Scope scope, ResultSet rs) throws SQLException {
		Set<String> granted = new LinkedHashSet<>();

		if (scope == GrantTarget.Scope.GLOBAL) {
			if (rs.next()) {
				for (int i = 0; i < ROLE_ATTRIBUTES.size(); i++) {
					if (rs.getBoolean(i + 1)) {
						granted.add(ROLE_ATTRIBUTES.get(i));
					}
				}
			}
			return granted;
		}

		while (rs.next()) {
			granted.add(rs.getString(1));
		}
		return granted;
	}

	public List<String> setGrantsSql(DatabaseUser user, GrantTarget target, Set<String> current, Set<String> wanted) {
		String role = dialect.quote(user.name());

		if (target.scope() == GrantTarget.Scope.GLOBAL) {
			StringBuilder attributes = new StringBuilder();

			for (String attribute : ROLE_ATTRIBUTES) {
				attributes.append(wanted.contains(attribute) ? " " : " NO").append(attribute);
			}

			return List.of("ALTER ROLE " + role + " WITH" + attributes);
		}

		String object = target.scope() == GrantTarget.Scope.DATABASE
			? "DATABASE " + dialect.quote(target.database())
			: "TABLE " + dialect.quote(target.table());
		List<String> statements = new ArrayList<>();

		for (String privilege : getPrivileges(target.scope())) {
			if (wanted.contains(privilege) && !current.contains(privilege)) {
				statements.add("GRANT " + privilege + " ON " + object + " TO " + role);
			} else if (!wanted.contains(privilege) && current.contains(privilege)) {
				statements.add("REVOKE " + privilege + " ON " + object + " FROM " + role);
			}
		}
		return statements;
	}

	private String passwordClause(String password) {
		return password.length() == 0 ? "" : " PASSWORD " + dialect.literal(password);
	}
}

package nl.errorsoft.esql.domain.dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.data.DatabaseConnection;
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

	public List<DatabaseUser> listUsers(DatabaseConnection dbc) throws SQLException {
		List<DatabaseUser> users = new ArrayList<>();
		try (ResultSet rs = dbc.executeQuery("SELECT rolname FROM pg_roles WHERE rolname !~ '^pg_' ORDER BY rolname")) {

			while (rs.next()) {
				users.add(new DatabaseUser(rs.getString(1), null));
			}

		}
		return users;
	}

	public void createUser(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException {
		dbc.executeUpdate("CREATE ROLE " + dialect.quote(user.name()) + " LOGIN" + passwordClause(password));
	}

	public void changePassword(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException {
		dbc.executeUpdate("ALTER ROLE " + dialect.quote(user.name()) + (password.length() == 0 ? " PASSWORD NULL" : passwordClause(password)));
	}

	public void dropUser(DatabaseConnection dbc, DatabaseUser user) throws SQLException {
		dbc.executeUpdate("DROP ROLE " + dialect.quote(user.name()));
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return switch (scope) {
			case GLOBAL -> ROLE_ATTRIBUTES;
			case DATABASE -> DATABASE_PRIVILEGES;
			default -> TABLE_PRIVILEGES;
		};
	}

	public Set<String> getGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target) throws SQLException {
		Set<String> granted = new LinkedHashSet<>();

		switch (target.scope()) {
			case GLOBAL -> {
				try (PreparedStatement ps = dbc.getConnection()
					.prepareStatement("SELECT rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolreplication FROM pg_roles WHERE rolname = ?")) {
					ps.setString(1, user.name());

					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							for (int i = 0; i < ROLE_ATTRIBUTES.size(); i++) {
								if (rs.getBoolean(i + 1)) {
									granted.add(ROLE_ATTRIBUTES.get(i));
								}
							}
						}
					}
				}
			}
			case DATABASE -> collect(dbc, granted,
				"SELECT a.privilege_type FROM pg_database d, aclexplode(d.datacl) a JOIN pg_roles r ON r.oid = a.grantee WHERE d.datname = ? AND r.rolname = ?",
				target.database(), user.name());
			default -> {
				dbc.useDatabase(target.database());
				collect(dbc, granted,
					"""
						SELECT a.privilege_type FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, aclexplode(c.relacl) a JOIN pg_roles r ON r.oid = a.grantee \
						WHERE n.nspname = current_schema() AND c.relname = ? AND r.rolname = ?""",
					target.table(), user.name());
			}
		}
		return granted;
	}

	public void setGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target, Set<String> privileges) throws SQLException {
		String role = dialect.quote(user.name());

		if (target.scope() == GrantTarget.Scope.GLOBAL) {
			StringBuilder attributes = new StringBuilder();

			for (String attribute : ROLE_ATTRIBUTES) {
				attributes.append(privileges.contains(attribute) ? " " : " NO").append(attribute);
			}

			dbc.executeUpdate("ALTER ROLE " + role + " WITH" + attributes);
			return;
		}

		if (target.scope() == GrantTarget.Scope.TABLE) {
			dbc.useDatabase(target.database());
		}

		String object = target.scope() == GrantTarget.Scope.DATABASE
			? "DATABASE " + dialect.quote(target.database())
			: "TABLE " + dialect.quote(target.table());
		Set<String> current = getGrants(dbc, user, target);

		for (String privilege : getPrivileges(target.scope())) {
			if (privileges.contains(privilege) && !current.contains(privilege)) {
				dbc.executeUpdate("GRANT " + privilege + " ON " + object + " TO " + role);
			} else if (!privileges.contains(privilege) && current.contains(privilege)) {
				dbc.executeUpdate("REVOKE " + privilege + " ON " + object + " FROM " + role);
			}
		}
	}

	private String passwordClause(String password) {
		return password.length() == 0 ? "" : " PASSWORD " + dialect.literal(password);
	}

	private void collect(DatabaseConnection dbc, Set<String> into, String sql, String first, String second) throws SQLException {
		try (PreparedStatement ps = dbc.getConnection().prepareStatement(sql)) {
			ps.setString(1, first);
			ps.setString(2, second);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					into.add(rs.getString(1));
				}
			}
		}
	}
}

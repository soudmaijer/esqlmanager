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

/** Accounts are user@host and privileges are granted with GRANT and REVOKE (MySQL 5.7 and later). */
public class MySqlUserAdmin implements UserAdmin {
	private static final List<String> OBJECT_PRIVILEGES = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "REFERENCES", "INDEX",
		"ALTER");
	private static final List<String> GLOBAL_PRIVILEGES = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "RELOAD", "SHUTDOWN",
		"PROCESS", "FILE", "REFERENCES", "INDEX", "ALTER");

	private final Dialect dialect;

	public MySqlUserAdmin(Dialect dialect) {
		this.dialect = dialect;
	}

	public boolean usesHost() {
		return true;
	}

	public List<DatabaseUser> listUsers(DatabaseConnection dbc) throws SQLException {
		List<DatabaseUser> users = new ArrayList<>();
		try (ResultSet rs = dbc.executeQuery("SELECT User, Host FROM mysql.user ORDER BY User, Host")) {

			while (rs.next()) {
				users.add(new DatabaseUser(rs.getString(1), rs.getString(2)));
			}

		}
		return users;
	}

	public void createUser(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException {
		dbc.executeUpdate("CREATE USER " + account(user) + identifiedBy(password));
	}

	public void changePassword(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException {
		dbc.executeUpdate("ALTER USER " + account(user) + identifiedBy(password));
	}

	public void dropUser(DatabaseConnection dbc, DatabaseUser user) throws SQLException {
		dbc.executeUpdate("DROP USER " + account(user));
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return scope == GrantTarget.Scope.GLOBAL ? GLOBAL_PRIVILEGES : OBJECT_PRIVILEGES;
	}

	public Set<String> getGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target) throws SQLException {
		String sql = switch (target.scope()) {
			case GLOBAL -> "SELECT PRIVILEGE_TYPE FROM information_schema.USER_PRIVILEGES WHERE GRANTEE = ?";
			case DATABASE -> "SELECT PRIVILEGE_TYPE FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ?";
			default -> "SELECT PRIVILEGE_TYPE FROM information_schema.TABLE_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ? AND TABLE_NAME = ?";
		};

		Set<String> granted = new LinkedHashSet<>();

		try (PreparedStatement ps = dbc.getConnection().prepareStatement(sql)) {
			ps.setString(1, "'" + user.name() + "'@'" + user.host() + "'");

			if (target.scope() != GrantTarget.Scope.GLOBAL) {
				ps.setString(2, target.database());
			}
			if (target.scope() == GrantTarget.Scope.TABLE) {
				ps.setString(3, target.table());
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					granted.add(rs.getString(1));
				}
			}
		}
		return granted;
	}

	public void setGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target, Set<String> privileges) throws SQLException {
		Set<String> current = getGrants(dbc, user, target);

		for (String privilege : getPrivileges(target.scope())) {
			if (privileges.contains(privilege) && !current.contains(privilege)) {
				dbc.executeUpdate("GRANT " + privilege + " ON " + objectName(target) + " TO " + account(user));
			} else if (!privileges.contains(privilege) && current.contains(privilege)) {
				dbc.executeUpdate("REVOKE " + privilege + " ON " + objectName(target) + " FROM " + account(user));
			}
		}
	}

	private String account(DatabaseUser user) {
		return dialect.literal(user.name()) + "@" + dialect.literal(user.host());
	}

	private String identifiedBy(String password) {
		return password.length() == 0 ? "" : " IDENTIFIED BY " + dialect.literal(password);
	}

	private String objectName(GrantTarget target) {
		return switch (target.scope()) {
			case GLOBAL -> "*.*";
			case DATABASE -> dialect.quote(target.database()) + ".*";
			default -> dialect.quote(target.database()) + "." + dialect.quote(target.table());
		};
	}
}

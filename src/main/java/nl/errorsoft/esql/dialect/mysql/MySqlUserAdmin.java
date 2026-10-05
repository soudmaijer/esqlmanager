package nl.errorsoft.esql.dialect.mysql;

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

	public String listUsersSql() {
		return "SELECT User, Host FROM mysql.user ORDER BY User, Host";
	}

	public DatabaseUser readUser(ResultSet rs) throws SQLException {
		return new DatabaseUser(rs.getString(1), rs.getString(2));
	}

	public String createUserSql(DatabaseUser user, String password) {
		return "CREATE USER " + account(user) + identifiedBy(password);
	}

	public String changePasswordSql(DatabaseUser user, String password) {
		return "ALTER USER " + account(user) + identifiedBy(password);
	}

	public String dropUserSql(DatabaseUser user) {
		return "DROP USER " + account(user);
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return scope == GrantTarget.Scope.GLOBAL ? GLOBAL_PRIVILEGES : OBJECT_PRIVILEGES;
	}

	public GrantQuery grantsQuery(DatabaseUser user, GrantTarget target) {
		String grantee = "'" + user.name() + "'@'" + user.host() + "'";

		return switch (target.scope()) {
			case GLOBAL -> new GrantQuery("SELECT PRIVILEGE_TYPE FROM information_schema.USER_PRIVILEGES WHERE GRANTEE = ?", List.of(grantee), null);
			case DATABASE -> new GrantQuery("SELECT PRIVILEGE_TYPE FROM information_schema.SCHEMA_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ?",
				List.of(grantee, target.database()), null);
			default -> new GrantQuery(
				"SELECT PRIVILEGE_TYPE FROM information_schema.TABLE_PRIVILEGES WHERE GRANTEE = ? AND TABLE_SCHEMA = ? AND TABLE_NAME = ?",
				List.of(grantee, target.database(), target.table()), null);
		};
	}

	public Set<String> readGrants(GrantTarget.Scope scope, ResultSet rs) throws SQLException {
		Set<String> granted = new LinkedHashSet<>();

		while (rs.next()) {
			granted.add(rs.getString(1));
		}
		return granted;
	}

	public List<String> setGrantsSql(DatabaseUser user, GrantTarget target, Set<String> current, Set<String> wanted) {
		List<String> statements = new ArrayList<>();

		for (String privilege : getPrivileges(target.scope())) {
			if (wanted.contains(privilege) && !current.contains(privilege)) {
				statements.add("GRANT " + privilege + " ON " + objectName(target) + " TO " + account(user));
			} else if (!wanted.contains(privilege) && current.contains(privilege)) {
				statements.add("REVOKE " + privilege + " ON " + objectName(target) + " FROM " + account(user));
			}
		}
		return statements;
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

package nl.errorsoft.esql.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantTarget;

/** Managing database accounts and their privileges, which every server does in its own way. It writes the SQL, the repository runs it. */
public interface UserAdmin {
	/** True when an account is a user at a host, false when it is just a name. */
	boolean usesHost();

	/** A query listing the accounts, read with {@link #readUser}. */
	String listUsersSql();

	DatabaseUser readUser(ResultSet row) throws SQLException;

	/** @param password the password, empty for an account without one. */
	String createUserSql(DatabaseUser user, String password);

	String changePasswordSql(DatabaseUser user, String password);

	String dropUserSql(DatabaseUser user);

	/** The privileges that can be given on the objects in the given scope. */
	List<String> getPrivileges(GrantTarget.Scope scope);

	/** The query that reads the privileges the user has been given directly on the target, read with {@link #readGrants}. */
	GrantQuery grantsQuery(DatabaseUser user, GrantTarget target);

	/** The privileges in the result of {@link #grantsQuery}. */
	Set<String> readGrants(GrantTarget.Scope scope, ResultSet rs) throws SQLException;

	/**
	 * The statements that grant and revoke privileges so that the target ends up with exactly the wanted ones.
	 * @param current the privileges the user has now, from {@link #grantsQuery}.
	 */
	List<String> setGrantsSql(DatabaseUser user, GrantTarget target, Set<String> current, Set<String> wanted);
}

package nl.errorsoft.esql.dialect;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantTarget;

/** Managing database accounts and their privileges, which every server does in its own way. */
public interface UserAdmin {
	/** True when an account is a user at a host, false when it is just a name. */
	boolean usesHost();

	List<DatabaseUser> listUsers(DatabaseConnection dbc) throws SQLException;

	/** @param password the password, empty for an account without one. */
	void createUser(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException;

	void changePassword(DatabaseConnection dbc, DatabaseUser user, String password) throws SQLException;

	void dropUser(DatabaseConnection dbc, DatabaseUser user) throws SQLException;

	/** The privileges that can be given on the objects in the given scope. */
	List<String> getPrivileges(GrantTarget.Scope scope);

	/** The privileges the user has been given directly on the target. */
	Set<String> getGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target) throws SQLException;

	/** Grants and revokes privileges so that the target ends up with exactly the given ones. */
	void setGrants(DatabaseConnection dbc, DatabaseUser user, GrantTarget target, Set<String> privileges) throws SQLException;
}

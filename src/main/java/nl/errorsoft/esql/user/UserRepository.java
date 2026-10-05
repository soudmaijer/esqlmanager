package nl.errorsoft.esql.user;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.dialect.UserAdmin;

/** Accounts and privileges. The SQL differs so much per server that it lives in the dialect's {@link UserAdmin}. */
public class UserRepository extends AbstractRepository {
	public UserRepository(DatabaseConnection dbc) {
		super(dbc);
	}

	public boolean usesHost() {
		return admin().usesHost();
	}

	public List<String> privileges(GrantTarget.Scope scope) {
		return admin().getPrivileges(scope);
	}

	public List<DatabaseUser> listUsers() throws SQLException {
		return admin().listUsers(dbc);
	}

	public void createUser(DatabaseUser user, String password) throws SQLException {
		admin().createUser(dbc, user, password);
	}

	public void changePassword(DatabaseUser user, String password) throws SQLException {
		admin().changePassword(dbc, user, password);
	}

	public void dropUser(DatabaseUser user) throws SQLException {
		admin().dropUser(dbc, user);
	}

	public Set<String> grants(DatabaseUser user, GrantTarget target) throws SQLException {
		return admin().getGrants(dbc, user, target);
	}

	public void setGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws SQLException {
		admin().setGrants(dbc, user, target, privileges);
	}

	private UserAdmin admin() {
		return dialect().getUserAdmin();
	}
}

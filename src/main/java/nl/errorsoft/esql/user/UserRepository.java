package nl.errorsoft.esql.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.errorsoft.esql.jdbc.AbstractRepository;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.dialect.GrantQuery;
import nl.errorsoft.esql.dialect.UserAdmin;

/** Accounts and privileges. The SQL differs so much per server that the dialect's {@link UserAdmin} writes it, this repository runs it. */
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
		List<DatabaseUser> users = new ArrayList<>();

		try (ResultSet rs = dbc.executeQuery(admin().listUsersSql())) {
			while (rs.next()) {
				users.add(admin().readUser(rs));
			}
		}
		return users;
	}

	public void createUser(DatabaseUser user, String password) throws SQLException {
		executeUpdate(admin().createUserSql(user, password));
	}

	public void changePassword(DatabaseUser user, String password) throws SQLException {
		executeUpdate(admin().changePasswordSql(user, password));
	}

	public void dropUser(DatabaseUser user) throws SQLException {
		executeUpdate(admin().dropUserSql(user));
	}

	public Set<String> grants(DatabaseUser user, GrantTarget target) throws SQLException {
		GrantQuery query = admin().grantsQuery(user, target);

		if (query.database() != null) {
			useDatabase(query.database());
		}

		try (PreparedStatement ps = dbc.getConnection().prepareStatement(query.sql())) {
			for (int i = 0; i < query.parameters().size(); i++) {
				ps.setString(i + 1, query.parameters().get(i));
			}

			try (ResultSet rs = ps.executeQuery()) {
				return admin().readGrants(target.scope(), rs);
			}
		}
	}

	/** Reads what the user has now (in the target's database where the server needs that), then grants and revokes the difference. */
	public void setGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws SQLException {
		Set<String> current = grants(user, target);
		executeAll(admin().setGrantsSql(user, target, current, privileges));
	}

	private UserAdmin admin() {
		return dialect().getUserAdmin();
	}
}

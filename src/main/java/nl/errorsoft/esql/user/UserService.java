package nl.errorsoft.esql.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.Table;

/** Application logic of the user manager: accounts, privileges and the databases and tables they apply to. */
public class UserService {
	private final UserRepository repository;
	private final DatabaseService databases;

	public UserService(DatabaseConnection dbc) {
		this.repository = new UserRepository(dbc);
		this.databases = new DatabaseService(dbc);
	}

	public boolean usesHost() {
		return repository.usesHost();
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return repository.privileges(scope);
	}

	public List<DatabaseUser> listUsers() throws Exception {
		return repository.listUsers();
	}

	public void createUser(DatabaseUser user, String password) throws Exception {
		repository.createUser(user, password);
	}

	public void changePassword(DatabaseUser user, String password) throws Exception {
		repository.changePassword(user, password);
	}

	public void dropUser(DatabaseUser user) throws Exception {
		repository.dropUser(user);
	}

	public Set<String> getGrants(DatabaseUser user, GrantTarget target) throws Exception {
		return repository.grants(user, target);
	}

	public void setGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws Exception {
		repository.setGrants(user, target, privileges);
	}

	public List<String> getDatabaseNames() throws Exception {
		List<String> names = new ArrayList<String>();

		for (Database database : databases.getDatabases()) {
			names.add(database.getName());
		}

		return names;
	}

	public List<String> getTableNames(String databaseName) throws Exception {
		List<String> names = new ArrayList<String>();

		for (Database database : databases.getDatabases()) {
			if (database.getName().equals(databaseName)) {
				for (Table table : databases.getTables(database)) {
					names.add(table.getName());
				}
			}
		}
		return names;
	}
}

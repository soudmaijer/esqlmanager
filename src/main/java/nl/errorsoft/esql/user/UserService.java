package nl.errorsoft.esql.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.Table;

/** Application logic of the user manager: accounts, privileges and the databases and tables they apply to. */
public class UserService {
	private final UserRepository repository;
	private final DatabaseService databases;

	public UserService(UserRepository repository, DatabaseService databases) {
		this.repository = repository;
		this.databases = databases;
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

	/** What changes when the user gets exactly these privileges on the target, read from the server now. */
	public GrantChange planGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws Exception {
		return repository.planGrants(user, target, privileges);
	}

	/** Runs a plan made by {@link #planGrants}, so that what the user confirmed is what happens. */
	public void applyGrants(GrantChange change) throws Exception {
		repository.applyGrants(change);
	}

	/** Gives the user exactly these privileges on the target. */
	public void setGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws Exception {
		applyGrants(planGrants(user, target, privileges));
	}

	public List<String> getDatabaseNames() throws Exception {
		List<String> names = new ArrayList<>();

		for (Database database : databases.getDatabases()) {
			names.add(database.getName());
		}

		return names;
	}

	public List<String> getTableNames(String databaseName) throws Exception {
		List<String> names = new ArrayList<>();

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

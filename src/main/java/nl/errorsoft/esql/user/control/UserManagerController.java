package nl.errorsoft.esql.user.control;

import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantChange;
import nl.errorsoft.esql.user.GrantTarget;
import nl.errorsoft.esql.user.UserService;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;

import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.user.ui.dialog.UserManagerDialog;

public class UserManagerController {
	private ConnectionWindowController connectionWindowController;

	public UserManagerController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	public void showDialog(MainWindow mainWindow) {
		try {
			if (!connectionWindowController.requireFeature(Dialect.Feature.USER_MANAGER, "The user manager")) {
				return;
			}

			mainWindow.updateStatus("Starting usermanager...", true);
			UserManagerDialog dialog = new UserManagerDialog(mainWindow, this);
			mainWindow.showConnectionState();
			dialog.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(mainWindow, "Open user manager", e);
		}
	}

	/** What the server calls a database, for the messages of the dialog. */
	public String databaseTerm() {
		return connectionWindowController.dialect().databaseTerm();
	}

	public boolean usesHost() {
		return service().usesHost();
	}

	public List<String> getPrivileges(GrantTarget.Scope scope) {
		return service().getPrivileges(scope);
	}

	public List<DatabaseUser> listUsers() throws Exception {
		return service().listUsers();
	}

	public void createUser(DatabaseUser user, String password) throws Exception {
		service().createUser(user, password);
	}

	public void changePassword(DatabaseUser user, String password) throws Exception {
		service().changePassword(user, password);
	}

	public void dropUser(DatabaseUser user) throws Exception {
		service().dropUser(user);
	}

	public Set<String> getGrants(DatabaseUser user, GrantTarget target) throws Exception {
		return service().getGrants(user, target);
	}

	public GrantChange planGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws Exception {
		return service().planGrants(user, target, privileges);
	}

	public void applyGrants(GrantChange change) throws Exception {
		service().applyGrants(change);
	}

	public List<String> getDatabaseNames() throws Exception {
		return service().getDatabaseNames();
	}

	public List<String> getTableNames(String databaseName) throws Exception {
		return service().getTableNames(databaseName);
	}

	private UserService service() {
		try {
			return connectionWindowController.getContext().users();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}

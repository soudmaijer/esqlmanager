package nl.errorsoft.esql.user.control;

import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantTarget;
import nl.errorsoft.esql.user.UserService;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.user.ui.UserManagerUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagerCC {
	private static final Logger log = LogManager.getLogger(UserManagerCC.class);

	private ConnectionWindowCC cwcc;

	public UserManagerCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public void startUI(ESQLManagerUI emui) {
		try {
			if (!cwcc.requireFeature(Dialect.Feature.USER_MANAGER, "The user manager")) {
				return;
			}

			emui.updateStatus("Starting usermanager...", true);
			UserManagerUI ui = new UserManagerUI(emui, this);
			emui.showConnectionState();
			ui.setVisible(true);
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Start user manager", e);
		}
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

	public void setGrants(DatabaseUser user, GrantTarget target, Set<String> privileges) throws Exception {
		service().setGrants(user, target, privileges);
	}

	public List<String> getDatabaseNames() throws Exception {
		return service().getDatabaseNames();
	}

	public List<String> getTableNames(String databaseName) throws Exception {
		return service().getTableNames(databaseName);
	}

	private UserService service() {
		try {
			return cwcc.getContext().users();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private Dialect getDialect() {
		return cwcc.getConnectionProfile().getServerType().getDialect();
	}
}

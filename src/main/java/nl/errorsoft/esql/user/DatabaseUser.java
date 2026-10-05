package nl.errorsoft.esql.user;

/**
 * A database account: a user at a host on MySQL, a role on PostgreSQL (which has no host).
 *
 * @param host the host the account may connect from, null when the server has no such concept
 */
public record DatabaseUser(String name, String host) {
	@Override
	public String toString() {
		return host == null ? name : name + "@" + host;
	}
}

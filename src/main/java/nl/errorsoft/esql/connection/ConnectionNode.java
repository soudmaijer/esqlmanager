package nl.errorsoft.esql.connection;

/**
 * The root of the subtree of one open connection in a tree: its databases, schemas, tables and columns are below it. The domain objects carry no
 * connection, so the connection of a node is found by walking up to this node ({@link TreeSelection#connection(java.util.List)}).
 * <p>
 * Equal only to itself: two connections with the same profile are two nodes.
 */
public final class ConnectionNode {
	private final ConnectionProfile profile;
	private final String title;

	/** @param title what the tree shows, such as {@code postgres@localhost} */
	public ConnectionNode(ConnectionProfile profile, String title) {
		this.profile = profile;
		this.title = title;
	}

	public ConnectionProfile getProfile() {
		return profile;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return title;
	}
}

package nl.errorsoft.esql.connection;

/** A saved profile that is not connected, shown in a tree so that it can be connected from there. */
public record ProfileNode(ConnectionProfile profile) {
	@Override
	public String toString() {
		return profile.getName();
	}
}

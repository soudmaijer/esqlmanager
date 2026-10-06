package nl.errorsoft.esql.connection;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * The tree nodes whose children are being loaded, so that expanding a node again while its load runs does not start a second one. Nodes are compared by
 * identity: two databases with the same name in different connections are different nodes. Without Swing, so it can be tested.
 */
public final class NodeLoads {
	private final Set<Object> loading = Collections.newSetFromMap(new IdentityHashMap<>());

	/** Marks the node as loading; false when a load of it is already running and no new one may start. */
	public boolean start(Object node) {
		return loading.add(node);
	}

	/** The load of the node finished, successfully or not. */
	public void finish(Object node) {
		loading.remove(node);
	}

	public boolean isLoading(Object node) {
		return loading.contains(node);
	}
}

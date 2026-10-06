package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.database.Database;

class NodeLoadsTest {
	private final NodeLoads loads = new NodeLoads();

	@Test
	void aNodeIsNotLoadedTwiceAtTheSameTime() {
		Database shop = new Database("shop");
		assertTrue(loads.start(shop));
		assertFalse(loads.start(shop));
		loads.finish(shop);
		assertTrue(loads.start(shop));
	}

	@Test
	void nodesWithTheSameNameAreDifferentNodes() {
		assertTrue(loads.start(new Database("shop")));
		assertTrue(loads.start(new Database("shop")));
	}
}

package nl.errorsoft.esql.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.app.OutputRouting.Routed;

class OutputRoutingTest {
	private static final String LINE = "13:37:08 INFO  Schema shop.public: 3 table(s)\n";

	@Test
	void aLineWithoutAConnectionGoesToTheApplicationTab() {
		assertEquals(new Routed(null, LINE), OutputRouting.route(null, LINE, Set.of("local")));
		assertEquals(new Routed(null, LINE), OutputRouting.route("", LINE, Set.of("local")));
	}

	@Test
	void aLineOfAConnectionGoesToItsTabWithoutPrefix() {
		assertEquals(new Routed("local", LINE), OutputRouting.route("local", LINE, Set.of("local", "other")));
	}

	@Test
	void aLineOfAConnectionWithoutTabGoesToTheApplicationTabWithItsName() {
		assertEquals(new Routed(null, "13:37:08 INFO  [gone] Schema shop.public: 3 table(s)\n"), OutputRouting.route("gone", LINE, Set.of("local")));
	}
}

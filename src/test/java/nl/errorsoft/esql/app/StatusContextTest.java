package nl.errorsoft.esql.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.app.StatusContext.Source;

class StatusContextTest {

	private static String lookup(Source source) {
		return source == Source.WORK ? "window" : "explorer";
	}

	@Test
	void theSourceTouchedLastWins() {
		StatusContext context = new StatusContext();
		assertEquals("window", context.pick(StatusContextTest::lookup));
		context.touched(Source.EXPLORER);
		assertEquals("explorer", context.pick(StatusContextTest::lookup));
		context.touched(Source.WORK);
		assertEquals("window", context.pick(StatusContextTest::lookup));
	}

	@Test
	void anEmptySourceFallsBackToTheOther() {
		StatusContext context = new StatusContext();
		context.touched(Source.EXPLORER);
		assertEquals("window", context.pick(source -> source == Source.WORK ? "window" : null));
		context.touched(Source.WORK);
		assertEquals("explorer", context.pick(source -> source == Source.EXPLORER ? "explorer" : null));
		assertNull(context.pick(source -> null));
	}

	@Test
	void textOfTheRightHandSide() {
		assertEquals("shop.public", StatusContext.where("shop", "public"));
		assertEquals("shop", StatusContext.where("shop", null));
		assertEquals("", StatusContext.where(null, "public"));
		assertEquals("PostgreSQL 17  |  me@host:5432  |  shop.public", StatusContext.info("PostgreSQL 17", "me@host:5432", "shop.public"));
		assertEquals("MySQL 8  |  me@host:3306", StatusContext.info("MySQL 8", "me@host:3306", ""));
	}
}

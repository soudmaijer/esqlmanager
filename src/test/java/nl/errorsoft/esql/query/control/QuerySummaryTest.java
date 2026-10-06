package nl.errorsoft.esql.query.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The bar below a query tab does not repeat what the line of a result tab already says. */
class QuerySummaryTest {
	@Test
	void statementsWithRowsLeaveTheBarEmpty() {
		assertEquals("", QueryController.summary(1, 1, 5));
		assertEquals("", QueryController.summary(3, 3, 12));
	}

	@Test
	void statementsWithoutRowsAreCounted() {
		assertEquals("1 statement(s) executed in 4 ms", QueryController.summary(1, 0, 4));
		assertEquals("3 statement(s) executed in 9 ms", QueryController.summary(3, 1, 9));
	}
}

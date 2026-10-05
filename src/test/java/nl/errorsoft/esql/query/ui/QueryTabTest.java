package nl.errorsoft.esql.query.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QueryTabTest {

	@Test
	void resultTabIsTheStatementOnOneShortenedLine() {
		assertEquals("SELECT * FROM orders", QueryTab.tabTitle("SELECT *\n  FROM orders"));
		assertEquals("SELECT c.name, count(o.id) AS or...", QueryTab.tabTitle("SELECT c.name, count(o.id) AS orders FROM customers c"));
	}
}

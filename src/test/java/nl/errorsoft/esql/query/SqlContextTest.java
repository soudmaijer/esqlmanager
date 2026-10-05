package nl.errorsoft.esql.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.query.SqlContext.Kind;

class SqlContextTest {
	/** The caret is where the | is. */
	private static SqlContext at(String text) {
		int caret = text.indexOf('|');
		return SqlContext.at(text.replace("|", ""), caret);
	}

	@Test
	void tablesAfterFromJoinUpdateAndInto() {
		assertEquals(Kind.TABLES, at("select * from |").kind());
		assertEquals(Kind.TABLES, at("select * from a join |").kind());
		assertEquals(Kind.TABLES, at("update |").kind());
		assertEquals(Kind.TABLES, at("insert into |").kind());
		assertEquals(Kind.TABLES, at("select * from a, |").kind());
	}

	@Test
	void prefixIsTheWordBeingTyped() {
		SqlContext context = at("select * from cus|");
		assertEquals(Kind.TABLES, context.kind());
		assertEquals("cus", context.prefix());
	}

	@Test
	void aliasResolvesToItsTable() {
		SqlContext context = at("select o.| from orders o join customers as c on c.id = o.customer_id");
		assertEquals(Kind.COLUMNS, context.kind());
		assertEquals("orders", context.table());
		assertEquals("customers", at("select c.na| from orders o join customers as c on c.id = o.customer_id").table());
		assertEquals("na", at("select c.na| from orders o join customers as c").prefix());
	}

	@Test
	void tableNameAsQualifier() {
		assertEquals("orders", at("select ORDERS.| from orders").table());
		assertEquals("products", at("select products.|").table());
	}

	@Test
	void quotedAndSchemaQualifiedTables() {
		SqlContext context = at("select x.| from public.\"Order Lines\" x");
		assertEquals("Order Lines", context.table());
		assertEquals(List.of("Order Lines"), context.tables());
	}

	@Test
	void elsewhereAnythingWithTheTablesOfTheStatement() {
		SqlContext context = at("select na| from customers c, orders where c.id = 1");
		assertEquals(Kind.ANY, context.kind());
		assertEquals("na", context.prefix());
		assertEquals(List.of("customers", "orders"), context.tables());
	}

	@Test
	void stringsAndCommentsAreIgnored() {
		assertEquals(Kind.ANY, at("select 'from ' |").kind());
		assertEquals(Kind.ANY, at("select 1 -- from\n|").kind());
		assertEquals(Kind.ANY, at("select * from a where |").kind());
	}
}

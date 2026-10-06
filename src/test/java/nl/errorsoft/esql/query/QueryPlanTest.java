package nl.errorsoft.esql.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class QueryPlanTest {
	@Test
	void aSelectOrAWithOfSelectsOnlyReads() {
		assertTrue(QueryPlan.isReadOnly("select * from orders"));
		assertTrue(QueryPlan.isReadOnly("-- recent\n  SELECT 'insert into x' FROM orders"));
		assertTrue(QueryPlan.isReadOnly("WITH recent AS (SELECT * FROM orders) SELECT * FROM recent"));
		assertTrue(QueryPlan.isReadOnly("select \"update\" from t"));
	}

	@Test
	void everythingElseChangesSomething() {
		assertFalse(QueryPlan.isReadOnly("UPDATE orders SET paid = true"));
		assertFalse(QueryPlan.isReadOnly("delete from orders"));
		assertFalse(QueryPlan.isReadOnly("WITH gone AS (DELETE FROM orders RETURNING *) SELECT count(*) FROM gone"));
		assertFalse(QueryPlan.isReadOnly("SELECT * INTO copy FROM orders"));
		assertFalse(QueryPlan.isReadOnly("SELECT * FROM orders FOR UPDATE"));
		assertFalse(QueryPlan.isReadOnly("/* select */ insert into t values (1)"));
		assertFalse(QueryPlan.isReadOnly(""));
	}

	@Test
	void oneColumnIsALinePerRow() {
		QueryPlan plan = new QueryPlan(List.of("QUERY PLAN"), List.of(List.of("Seq Scan on orders"), List.of("Planning Time: 0.050 ms")));

		assertEquals("Seq Scan on orders\nPlanning Time: 0.050 ms", plan.text());
	}

	@Test
	void severalColumnsAreLinedUpUnderAHeader() {
		QueryPlan plan = new QueryPlan(List.of("id", "table", "key"), List.of(Arrays.asList("1", "orders", null)));

		assertEquals("id  table   key\n1   orders  NULL", plan.text());
	}
}

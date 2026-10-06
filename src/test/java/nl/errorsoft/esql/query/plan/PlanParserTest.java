package nl.errorsoft.esql.query.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.error.EsqlException;

/** Plans captured from PostgreSQL 17 and MySQL 8 for a join with a filter, a grouping and a sort (src/test/resources/query/plan). */
class PlanParserTest {
	private static String sample(String name) throws IOException {
		try (InputStream in = PlanParserTest.class.getResourceAsStream("/query/plan/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static List<String> operations(Plan plan) {
		return plan.root().flatten().map(PlanNode::operation).toList();
	}

	@Test
	void readsAPostgresPlan() throws IOException {
		Plan plan = PlanParser.parse(sample("postgres-explain.txt"));

		assertFalse(plan.analyzed());
		assertEquals(List.of("Sort", "Aggregate", "Sort", "Nested Loop", "Seq Scan", "Bitmap Heap Scan", "Bitmap Index Scan"), operations(plan));
		assertEquals(101.81, plan.root().estimate().totalCost());
		PlanNode index = plan.root().flatten().filter(node -> node.index() != null).findFirst().orElseThrow();
		assertEquals("cap_b_a", index.index());
		assertTrue(index.conditions().getFirst().startsWith("Index Cond: "), index.conditions().toString());
		assertEquals("Total cost 101.81", plan.summary());
		assertFalse(plan.hottest().isEmpty());
	}

	@Test
	void readsAnAnalyzedPostgresPlan() throws IOException {
		Plan plan = PlanParser.parse(sample("postgres-analyze.txt"));

		assertTrue(plan.analyzed());
		assertEquals(0.071, plan.planningTime());
		assertEquals(0.958, plan.executionTime());
		PlanNode heap = plan.root().flatten().filter(node -> node.operation().equals("Bitmap Heap Scan")).findFirst().orElseThrow();
		assertEquals("cap_b b", heap.relation());
		assertEquals(286, heap.actual().loops());
		assertTrue(heap.buffers().hit() > 0);
		PlanNode scan = plan.root().flatten().filter(node -> node.operation().equals("Seq Scan")).findFirst().orElseThrow();
		assertTrue(scan.conditions().getFirst().startsWith("Filter: "));
		assertTrue(scan.details().containsKey("Rows Removed by Filter"));
		assertTrue(PlanHints.of(scan).contains("Estimated 3 rows, got 286"), PlanHints.of(scan).toString());
		double shares = plan.root().flatten().mapToDouble(plan::share).sum();
		assertEquals(1, shares, 0.0001);
		assertTrue(plan.summary().startsWith("Planning 0.07 ms, execution 0.96 ms"), plan.summary());
	}

	@Test
	void readsAMySqlJsonPlan() throws IOException {
		Plan plan = PlanParser.parse(sample("mysql-explain.txt"));

		assertFalse(plan.analyzed());
		assertEquals(270.25, plan.totalCost());
		PlanNode a = plan.root().flatten().filter(node -> "a".equals(node.relation())).findFirst().orElseThrow();
		assertEquals("Full table scan (ALL)", a.operation());
		assertEquals(2000, a.estimate().rows());
		assertEquals(List.of("Full table scan of a, about 2,000 rows"), PlanHints.of(a));
		PlanNode b = plan.root().flatten().filter(node -> "b".equals(node.relation())).findFirst().orElseThrow();
		assertEquals("cap_b_a", b.index());
		// Prefix costs 200.25 and 270.25: the lookup adds 70.
		assertEquals(70, b.exclusiveCost(), 0.001);
		assertTrue(operations(plan).contains("Nested loop"), operations(plan).toString());
	}

	@Test
	void readsAMySqlAnalyzeTree() throws IOException {
		Plan plan = PlanParser.parse(sample("mysql-analyze.txt"));

		assertTrue(plan.analyzed());
		assertEquals("Sort: `sum(b.amount)` DESC", plan.root().operation());
		assertEquals(0.877, plan.executionTime());
		PlanNode lookup = plan.root().flatten().filter(node -> node.operation().startsWith("Index lookup")).findFirst().orElseThrow();
		assertEquals("b", lookup.relation());
		assertEquals("cap_b_a", lookup.index());
		assertEquals(286, lookup.actual().loops());
		assertEquals(0.00132 * 286, lookup.actual().totalTime(), 1e-9);
		assertEquals(1, lookup.estimate().rows());
		PlanNode filter = plan.root().flatten().filter(node -> node.operation().equals("Filter")).findFirst().orElseThrow();
		assertEquals(List.of("Filter: (a.kind = 3)"), filter.conditions());
		PlanNode scan = filter.children().getFirst();
		assertEquals("a", scan.relation());
		assertEquals(List.of("Full table scan of a, about 2,000 rows"), PlanHints.of(scan));
		assertNull(scan.buffers());
	}

	@Test
	void pointsOutASortOnDiskAndANestedLoopThatRepeats() {
		Plan plan = PlanParser.parse("""
			[{"Plan": {"Node Type": "Sort", "Total Cost": 10, "Plan Rows": 100000, "Actual Rows": 100000, "Actual Loops": 1, "Actual Total Time": 50,
			  "Sort Method": "external merge", "Sort Space Used": 12000, "Sort Space Type": "Disk",
			  "Plans": [{"Node Type": "Nested Loop", "Join Type": "Inner", "Total Cost": 8, "Plan Rows": 100000, "Actual Rows": 100000, "Actual Loops": 1,
			    "Actual Total Time": 30, "Plans": [
			      {"Node Type": "Seq Scan", "Relation Name": "orders", "Alias": "orders", "Total Cost": 2, "Plan Rows": 5000, "Actual Rows": 5000,
			       "Actual Loops": 1, "Actual Total Time": 2, "Filter": "(paid)", "Rows Removed by Filter": 60000},
			      {"Node Type": "Index Scan", "Relation Name": "lines", "Index Name": "lines_order", "Total Cost": 0.5, "Plan Rows": 20, "Actual Rows": 20,
			       "Actual Loops": 5000, "Actual Total Time": 0.005}]}]},
			  "Planning Time": 0.1, "Execution Time": 51}]
			""");
		PlanNode sort = plan.root();
		PlanNode loop = sort.children().getFirst();
		PlanNode scan = loop.children().getFirst();

		assertEquals(List.of("Sort used the disk (external merge, 12,000 kB)"), PlanHints.of(sort));
		assertEquals(List.of("Nested loop runs Index Scan 5,000 times"), PlanHints.of(loop));
		assertEquals(List.of("Sequential scan of orders reading about 65,000 rows, with a filter"), PlanHints.of(scan));
		assertEquals(20, sort.exclusiveTime());
		assertTrue(plan.hottest().contains(sort));
	}

	@Test
	void aMySqlPlanWithoutStepsSaysWhy() {
		Plan plan = PlanParser.parse("{\"query_block\": {\"select_id\": 1, \"message\": \"no matching row in const table\"}}");

		assertEquals("no matching row in const table", plan.root().operation());
	}

	@Test
	void refusesAnUnknownShape() {
		assertThrows(EsqlException.class, () -> PlanParser.parse("Seq Scan on orders"));
		assertThrows(EsqlException.class, () -> PlanParser.parse("[{\"Plan\": "));
	}
}

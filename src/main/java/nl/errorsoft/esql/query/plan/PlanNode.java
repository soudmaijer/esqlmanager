package nl.errorsoft.esql.query.plan;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * One step of a query plan with the steps it reads from. What the server did not give is null: a plain explain has no {@link Actual}, MySQL's tree has
 * no buffers.
 *
 * @param operation what the step does ("Seq Scan", "Nested loop inner join", "Full table scan")
 * @param relation the table it reads, null for a step that reads other steps
 * @param index the index it uses, or null
 * @param conditions filter, index and join conditions, as the server wrote them
 * @param details everything else the server said about the step (sort method, rows removed by the filter, ...), in its order
 */
public record PlanNode(String operation, String relation, String index, Estimate estimate, Actual actual, Buffers buffers, List<String> conditions,
	Map<String, String> details, List<PlanNode> children) {

	/** What the planner expected: rows per loop and the cost to the first and to the last row (in the server's units). */
	public record Estimate(Double rows, Double startupCost, Double totalCost) {
	}

	/** What happened when the statement ran: rows per loop, the number of loops, and the time of all loops together, children included, in ms. */
	public record Actual(double rows, double loops, double totalTime) {
	}

	/** Blocks found in the server's cache and read from disk. */
	public record Buffers(long hit, long read) {
	}

	public PlanNode {
		conditions = List.copyOf(conditions);
		details = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(details));
		children = List.copyOf(children);
	}

	/** The time spent in this step itself: its time without the time of its children, never below zero; null when not analyzed. */
	public Double exclusiveTime() {
		if (actual == null) {
			return null;
		}
		double children = this.children.stream().filter(child -> child.actual != null).mapToDouble(child -> child.actual.totalTime()).sum();
		return Math.max(0, actual.totalTime() - children);
	}

	/** The cost of this step itself, without its children; null when the server gave no cost. */
	public Double exclusiveCost() {
		if (estimate == null || estimate.totalCost() == null) {
			return null;
		}
		double children = this.children.stream()
			.filter(child -> child.estimate != null && child.estimate.totalCost() != null)
			.mapToDouble(child -> child.estimate.totalCost())
			.sum();
		return Math.max(0, estimate.totalCost() - children);
	}

	/** This step and every step below it, depth first in plan order. */
	public Stream<PlanNode> flatten() {
		return Stream.concat(Stream.of(this), children.stream().flatMap(PlanNode::flatten));
	}

	/** How far the estimate was off, as a factor of at least 1 (estimated 10, got 1000: 100); null when not both are known. */
	public Double estimateError() {
		if (actual == null || estimate == null || estimate.rows() == null) {
			return null;
		}
		double expected = Math.max(1, estimate.rows());
		double got = Math.max(1, actual.rows());
		return Math.max(expected, got) / Math.min(expected, got);
	}
}

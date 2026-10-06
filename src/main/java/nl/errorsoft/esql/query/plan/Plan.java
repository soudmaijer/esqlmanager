package nl.errorsoft.esql.query.plan;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A parsed query plan: its root step, the planning and execution times when the statement ran (null otherwise) and the total cost when the server gave
 * one. The weight of a step is its own time when analyzed, else its own cost.
 */
public record Plan(PlanNode root, Double planningTime, Double executionTime, Double totalCost) {
	/** Only a step with at least this share of the total is marked as hot. */
	private static final double HOT_SHARE = 0.2;
	private static final int MAX_HOT = 3;

	/** Whether the statement ran, so the steps have times and actual rows. */
	public boolean analyzed() {
		return root.actual() != null;
	}

	/** The weight of a step on its own: its exclusive time when analyzed, else its exclusive cost (0 when unknown). */
	public double weight(PlanNode node) {
		Double weight = analyzed() ? node.exclusiveTime() : node.exclusiveCost();
		return weight == null ? 0 : weight;
	}

	/** The share of a step in the whole plan, 0 to 1. */
	public double share(PlanNode node) {
		double total = root.flatten().mapToDouble(this::weight).sum();
		return total <= 0 ? 0 : weight(node) / total;
	}

	/** The steps that take the most, at most three, each with at least a fifth of the total; compared by identity. */
	public Set<PlanNode> hottest() {
		return root.flatten()
			.filter(node -> share(node) >= HOT_SHARE)
			.sorted(Comparator.comparingDouble(this::weight).reversed())
			.limit(MAX_HOT)
			.collect(Collectors.toCollection(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>())));
	}

	/** The summary below the plan: planning time, execution time and total cost, as far as known. */
	public String summary() {
		List<String> parts = new java.util.ArrayList<>();
		if (planningTime != null) {
			parts.add("Planning " + PlanFormat.millis(planningTime));
		}
		if (executionTime != null) {
			parts.add("execution " + PlanFormat.millis(executionTime));
		}
		Double cost = totalCost != null ? totalCost : root.estimate() == null ? null : root.estimate().totalCost();
		if (cost != null) {
			parts.add("total cost " + PlanFormat.number(cost));
		}
		String text = String.join(", ", parts);
		return text.isEmpty() ? (analyzed() ? "Analyzed" : "Estimated") : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}

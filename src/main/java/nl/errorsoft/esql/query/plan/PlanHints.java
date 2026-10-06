package nl.errorsoft.esql.query.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Facts about a step that often explain a slow statement, derived from the plan only: a large sequential scan with a filter, a full table scan, an
 * estimate far from the actual rows, a sort or hash that used the disk, a nested loop that repeats its inner side many times. Neutral wording, no advice.
 */
public final class PlanHints {
	static final double LARGE_SCAN = 10000;
	static final double ESTIMATE_FACTOR = 10;
	static final double MANY_LOOPS = 1000;

	private PlanHints() {
	}

	public static List<String> of(PlanNode node) {
		List<String> hints = new ArrayList<>();
		String operation = node.operation().toLowerCase(Locale.ROOT);
		Double estimated = node.estimate() == null ? null : node.estimate().rows();
		double scanned = node.actual() != null ? node.actual().rows() : estimated == null ? 0 : estimated;

		if (operation.equals("seq scan") && !node.conditions().isEmpty() && Math.max(scanned, rowsRemoved(node) + scanned) > LARGE_SCAN) {
			hints.add("Sequential scan of " + node.relation() + " reading about " + PlanFormat.number(rowsRemoved(node) + scanned)
				+ " rows, with a filter");
		}
		if (operation.startsWith("full table scan") || (operation.startsWith("table scan on") && !operation.contains("<"))) {
			hints.add("Full table scan of " + node.relation() + (estimated == null ? "" : ", about " + PlanFormat.number(estimated) + " rows"));
		}
		Double error = node.estimateError();
		if (error != null && error > ESTIMATE_FACTOR) {
			hints.add("Estimated " + PlanFormat.number(estimated) + " rows, got " + PlanFormat.number(node.actual().rows()));
		}
		String sortMethod = node.details().getOrDefault("Sort Method", "");
		if ("Disk".equals(node.details().get("Sort Space Type")) || sortMethod.contains("external")) {
			String space = node.details().get("Sort Space Used");
			hints.add("Sort used the disk (" + sortMethod + (space == null ? "" : ", " + space + " kB") + ")");
		}
		String batches = node.details().get("Hash Batches");
		if (batches != null && !batches.equals("1")) {
			hints.add("Hash used the disk in " + batches + " batches");
		}
		if (operation.startsWith("nested loop")) {
			node.children()
				.stream()
				.filter(child -> child.actual() != null && child.actual().loops() > MANY_LOOPS)
				.findFirst()
				.ifPresent(inner -> hints.add("Nested loop runs " + inner.operation() + " " + PlanFormat.number(inner.actual().loops()) + " times"));
		}
		return hints;
	}

	private static double rowsRemoved(PlanNode node) {
		try {
			return Double.parseDouble(node.details().getOrDefault("Rows Removed by Filter", "0").replace(",", ""));
		} catch (NumberFormatException e) {
			// Not a number: the hint then uses the rows the step returned.
			return 0;
		}
	}
}

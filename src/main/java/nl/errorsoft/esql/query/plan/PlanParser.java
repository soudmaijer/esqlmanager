package nl.errorsoft.esql.query.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.errorsoft.esql.error.EsqlException;

/**
 * Reads the plan text of a server into a {@link Plan}, recognised by its shape: PostgreSQL's {@code FORMAT JSON} (an array with a "Plan"), MySQL's
 * {@code FORMAT=JSON} (a "query_block") and MySQL's EXPLAIN ANALYZE tree (lines starting with {@code ->}). Pure, no Swing.
 */
public final class PlanParser {
	private static final List<String> POSTGRES_CONDITIONS = List.of("Index Cond", "Recheck Cond", "Hash Cond", "Merge Cond", "Join Filter", "Filter");
	/** PostgreSQL keys that are columns or not worth showing as a detail. */
	private static final Set<String> POSTGRES_SHOWN = Set.of("Node Type", "Relation Name", "Alias", "Index Name", "Plan Rows", "Startup Cost", "Total Cost",
		"Actual Rows", "Actual Loops", "Actual Total Time", "Actual Startup Time", "Plans", "Shared Hit Blocks", "Shared Read Blocks", "Parallel Aware",
		"Async Capable", "Plan Width", "Shared Dirtied Blocks", "Shared Written Blocks", "Local Hit Blocks", "Local Read Blocks", "Local Dirtied Blocks",
		"Local Written Blocks", "Temp Read Blocks", "Temp Written Blocks");
	private static final Pattern TREE_LINE = Pattern.compile("^( *)-> (.*)$");
	private static final Pattern TREE_COST = Pattern.compile("\\(cost=([0-9.e+]+)(?:\\.\\.([0-9.e+]+))? rows=([0-9.e+]+)\\)");
	private static final Pattern TREE_ACTUAL = Pattern
		.compile("\\(actual time=([0-9.e+]+)\\.\\.([0-9.e+]+) rows=([0-9.e+]+) loops=([0-9.e+]+)\\)");
	private static final Pattern TREE_TABLE = Pattern.compile(" on (`?[^ `]+`?)");
	private static final Pattern TREE_INDEX = Pattern.compile(" using (`?[^ `(]+`?)");
	private static final Map<String, String> MYSQL_ACCESS = Map.of("ALL", "Full table scan", "index", "Full index scan", "range", "Index range scan", "ref",
		"Index lookup", "eq_ref", "Unique index lookup", "const", "Constant row", "system", "Constant row", "fulltext", "Full text search", "ref_or_null",
		"Index lookup or null", "index_merge", "Index merge");

	private PlanParser() {
	}

	/** The plan in the text; an {@link EsqlException} when the shape is not one of the known ones. */
	public static Plan parse(String text) {
		String plan = text.strip();
		if (plan.startsWith("->")) {
			return mysqlTree(plan);
		}
		if (plan.startsWith("[") || plan.startsWith("{")) {
			Object json = JsonReader.read(plan);
			if (json instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> top && top.get("Plan") instanceof Map<?, ?> root) {
				return new Plan(postgres(root), number(top.get("Planning Time")), number(top.get("Execution Time")), null);
			}
			if (json instanceof Map<?, ?> top && top.get("query_block") instanceof Map<?, ?> block) {
				return mysqlJson(block);
			}
		}
		throw new EsqlException("This plan has a shape eSQLManager does not know; the Text tab shows it as the server gave it.");
	}

	// PostgreSQL FORMAT JSON

	private static PlanNode postgres(Map<?, ?> node) {
		String operation = text(node.get("Node Type"));
		if (node.get("Join Type") instanceof String join && !operation.contains("Join") && !operation.equals("Nested Loop")) {
			operation += " (" + join + ")";
		} else if (node.get("Join Type") instanceof String join && !"Inner".equals(join)) {
			operation += " " + join;
		}
		String relation = text(node.get("Relation Name"));
		if (relation != null && node.get("Alias") instanceof String alias && !alias.equals(relation)) {
			relation += " " + alias;
		}
		PlanNode.Estimate estimate = new PlanNode.Estimate(number(node.get("Plan Rows")), number(node.get("Startup Cost")), number(node.get("Total Cost")));
		PlanNode.Actual actual = null;
		if (number(node.get("Actual Loops")) instanceof Double loops) {
			double perLoop = number(node.get("Actual Total Time")) instanceof Double time ? time : 0;
			actual = new PlanNode.Actual(orZero(number(node.get("Actual Rows"))), loops, perLoop * loops);
		}
		PlanNode.Buffers buffers = null;
		if (number(node.get("Shared Hit Blocks")) instanceof Double hit) {
			buffers = new PlanNode.Buffers(Math.round(hit), Math.round(orZero(number(node.get("Shared Read Blocks")))));
		}
		List<String> conditions = new ArrayList<>();
		for (String key : POSTGRES_CONDITIONS) {
			if (node.get(key) instanceof String condition) {
				conditions.add(key + ": " + condition);
			}
		}
		Map<String, String> details = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : node.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (!POSTGRES_SHOWN.contains(key) && !POSTGRES_CONDITIONS.contains(key) && entry.getValue() != null) {
				details.put(key, value(entry.getValue()));
			}
		}
		List<PlanNode> children = new ArrayList<>();
		if (node.get("Plans") instanceof List<?> plans) {
			for (Object child : plans) {
				if (child instanceof Map<?, ?> map) {
					children.add(postgres(map));
				}
			}
		}
		return new PlanNode(operation, relation, text(node.get("Index Name")), estimate, actual, buffers, conditions, details, children);
	}

	// MySQL FORMAT=JSON

	private static Plan mysqlJson(Map<?, ?> block) {
		Double cost = block.get("cost_info") instanceof Map<?, ?> info ? number(info.get("query_cost")) : null;
		List<PlanNode> steps = mysqlSteps(block);
		// Without steps MySQL says why ("no matching row in const table").
		String operation = steps.isEmpty() && block.get("message") instanceof String message ? message : "Query block";
		PlanNode root = steps.size() == 1
			? steps.getFirst()
			: new PlanNode(operation, null, null, new PlanNode.Estimate(null, null, cost), null, null, List.of(), Map.of(), steps);
		return new Plan(root, null, null, cost);
	}

	/** The steps inside an object of the MySQL plan: operations that wrap others, nested loops, tables and subqueries. */
	private static List<PlanNode> mysqlSteps(Map<?, ?> object) {
		List<PlanNode> steps = new ArrayList<>();
		for (Map.Entry<?, ?> entry : object.entrySet()) {
			String key = String.valueOf(entry.getKey());
			Object value = entry.getValue();
			if (key.equals("table") && value instanceof Map<?, ?> table) {
				steps.add(mysqlTable(table));
			} else if (key.equals("nested_loop") && value instanceof List<?> tables) {
				// The cost of a joined table is its prefix cost: the cost of the join up to and including it. Each table gets its own part.
				List<PlanNode> joined = new ArrayList<>();
				double before = 0;
				for (Object item : tables) {
					if (item instanceof Map<?, ?> map) {
						for (PlanNode step : mysqlSteps(map)) {
							PlanNode.Estimate estimate = step.estimate();
							if (estimate != null && estimate.totalCost() != null) {
								double prefix = estimate.totalCost();
								estimate = new PlanNode.Estimate(estimate.rows(), estimate.startupCost(), Math.max(0, prefix - before));
								before = prefix;
							}
							joined.add(new PlanNode(step.operation(), step.relation(), step.index(), estimate, step.actual(), step.buffers(), step.conditions(),
								step.details(), step.children()));
						}
					}
				}
				steps.add(new PlanNode("Nested loop", null, null, new PlanNode.Estimate(null, null, before), null, null, List.of(), Map.of(), joined));
			} else if (value instanceof Map<?, ?> nested && (key.endsWith("_operation") || key.equals("duplicates_removal") || key.equals("query_block")
				|| key.endsWith("subquery") || key.equals("materialized_from_subquery"))) {
				steps.add(mysqlOperation(key, nested));
			} else if (value instanceof List<?> list && (key.endsWith("subqueries") || key.equals("query_specifications"))) {
				for (Object item : list) {
					if (item instanceof Map<?, ?> map) {
						steps.add(mysqlOperation(key, map));
					}
				}
			}
		}
		return steps;
	}

	private static PlanNode mysqlOperation(String key, Map<?, ?> nested) {
		String name = key.replace("_operation", "").replace('_', ' ');
		List<String> notes = new ArrayList<>();
		if (Boolean.TRUE.equals(nested.get("using_filesort"))) {
			notes.add("filesort");
		}
		if (Boolean.TRUE.equals(nested.get("using_temporary_table"))) {
			notes.add("temporary table");
		}
		String operation = Character.toUpperCase(name.charAt(0)) + name.substring(1) + (notes.isEmpty() ? "" : " (" + String.join(", ", notes) + ")");
		Double cost = nested.get("cost_info") instanceof Map<?, ?> info ? number(info.get("query_cost")) : null;
		return new PlanNode(operation, null, null, cost == null ? null : new PlanNode.Estimate(null, null, cost), null, null, List.of(), Map.of(),
			mysqlSteps(nested));
	}

	private static PlanNode mysqlTable(Map<?, ?> table) {
		String access = text(table.get("access_type"));
		String operation = access == null ? "Table" : MYSQL_ACCESS.getOrDefault(access, access) + " (" + access + ")";
		Double cost = null;
		if (table.get("cost_info") instanceof Map<?, ?> info) {
			cost = number(info.get("prefix_cost"));
		}
		List<String> conditions = new ArrayList<>();
		if (table.get("attached_condition") instanceof String condition) {
			conditions.add("Condition: " + condition);
		}
		Map<String, String> details = new LinkedHashMap<>();
		for (String key : List.of("possible_keys", "used_key_parts", "ref", "rows_produced_per_join", "filtered", "using_index", "cost_info")) {
			if (table.get(key) != null) {
				details.put(key.replace('_', ' '), value(table.get(key)));
			}
		}
		List<PlanNode> children = mysqlSteps(table);
		return new PlanNode(operation, text(table.get("table_name")), text(table.get("key")),
			new PlanNode.Estimate(number(table.get("rows_examined_per_scan")), null, cost), null, null, conditions, details, children);
	}

	// MySQL EXPLAIN ANALYZE

	private static Plan mysqlTree(String text) {
		List<int[]> levels = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		for (String line : text.lines().toList()) {
			Matcher matcher = TREE_LINE.matcher(line);
			if (matcher.matches()) {
				levels.add(new int[]{matcher.group(1).length()});
				labels.add(matcher.group(2));
			} else if (!labels.isEmpty()) {
				// A step written on several lines: the rest belongs to the step above.
				labels.set(labels.size() - 1, labels.getLast() + " " + line.strip());
			}
		}
		int[] next = {0};
		PlanNode root = treeNode(levels, labels, next);
		Double execution = root.actual() == null ? null : root.actual().totalTime();
		return new Plan(root, null, execution, root.estimate() == null ? null : root.estimate().totalCost());
	}

	private static PlanNode treeNode(List<int[]> levels, List<String> labels, int[] next) {
		int position = next[0]++;
		int indent = levels.get(position)[0];
		List<PlanNode> children = new ArrayList<>();
		while (next[0] < labels.size() && levels.get(next[0])[0] > indent) {
			children.add(treeNode(levels, labels, next));
		}
		String label = labels.get(position);
		Matcher cost = TREE_COST.matcher(label);
		Matcher actual = TREE_ACTUAL.matcher(label);
		int end = label.length();
		PlanNode.Estimate estimate = null;
		PlanNode.Actual measured = null;
		if (cost.find()) {
			end = Math.min(end, cost.start());
			Double total = cost.group(2) != null ? Double.valueOf(cost.group(2)) : Double.valueOf(cost.group(1));
			Double startup = cost.group(2) != null ? Double.valueOf(cost.group(1)) : null;
			estimate = new PlanNode.Estimate(Double.valueOf(cost.group(3)), startup, total);
		}
		if (actual.find()) {
			end = Math.min(end, actual.start());
			double loops = Double.parseDouble(actual.group(4));
			measured = new PlanNode.Actual(Double.parseDouble(actual.group(3)), loops, Double.parseDouble(actual.group(2)) * loops);
		}
		String operation = label.substring(0, end).strip();
		List<String> conditions = new ArrayList<>();
		if (operation.startsWith("Filter: ")) {
			conditions.add(operation);
			operation = "Filter";
		}
		String relation = null;
		Matcher table = TREE_TABLE.matcher(operation);
		if (operation.contains("scan on") || operation.contains("lookup on") || operation.contains("Index range scan")) {
			relation = table.find() ? table.group(1) : null;
		}
		Matcher using = TREE_INDEX.matcher(operation);
		String index = using.find() ? using.group(1) : null;
		return new PlanNode(operation, relation, index, estimate, measured, null, conditions, Map.of(), children);
	}

	// Values

	private static Double number(Object value) {
		if (value instanceof Double number) {
			return number;
		}
		if (value instanceof String text) {
			try {
				return Double.valueOf(text);
			} catch (NumberFormatException e) {
				// A text that is not a number (MySQL writes "34K" for data read) is no number.
				return null;
			}
		}
		return null;
	}

	private static double orZero(Double value) {
		return value == null ? 0 : value;
	}

	private static String text(Object value) {
		return value instanceof String text ? text : null;
	}

	/** A detail as text: a list joined with commas, an object as key=value pairs, a whole number without ".0". */
	private static String value(Object value) {
		if (value instanceof List<?> list) {
			return String.join(", ", list.stream().map(PlanParser::value).toList());
		}
		if (value instanceof Map<?, ?> map) {
			return String.join(", ", map.entrySet().stream().map(entry -> entry.getKey() + "=" + value(entry.getValue())).toList());
		}
		if (value instanceof Double number) {
			return PlanFormat.number(number);
		}
		return String.valueOf(value);
	}
}

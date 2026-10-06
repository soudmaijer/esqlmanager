package nl.errorsoft.esql.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The plan the server gave for a statement, as it came: the rows of the explain result with their columns. Usually one column holding JSON or the
 * MySQL tree ({@code query.plan.PlanParser} reads it); a result of several columns {@link #text()} lines up under a header.
 */
public record QueryPlan(List<String> columns, List<List<String>> rows) {
	private static final Set<String> CHANGES = Set.of("INSERT", "UPDATE", "DELETE", "MERGE", "INTO", "REPLACE", "CREATE", "DROP", "ALTER", "TRUNCATE",
		"CALL", "GRANT", "REVOKE", "LOCK", "COPY");

	/** The plan as text, one line per row. */
	public String text() {
		if (columns.size() == 1) {
			return String.join("\n", rows.stream().map(row -> row.getFirst() == null ? "" : row.getFirst()).toList());
		}
		int[] widths = new int[columns.size()];
		List<List<String>> lines = new ArrayList<>();
		lines.add(columns);
		rows.forEach(row -> lines.add(row.stream().map(value -> value == null ? "NULL" : value).toList()));

		for (List<String> line : lines) {
			for (int i = 0; i < widths.length; i++) {
				widths[i] = Math.max(widths[i], line.get(i).length());
			}
		}
		StringBuilder text = new StringBuilder();
		for (List<String> line : lines) {
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < widths.length; i++) {
				out.append(i == 0 ? "" : "  ").append(String.format("%-" + widths[i] + "s", line.get(i)));
			}
			text.append(out.toString().stripTrailing()).append('\n');
		}
		return text.toString().stripTrailing();
	}

	/**
	 * Whether the statement only reads: a SELECT, or a WITH whose parts are all selects. Explain analyze runs the statement, so anything else (and a SELECT
	 * ... INTO, which creates a table) is confirmed first. Comments and quoted text are ignored.
	 */
	public static boolean isReadOnly(String statement) {
		List<SqlContext.Token> tokens = SqlContext.tokenize(statement);

		if (tokens.isEmpty() || !tokens.getFirst().isName()) {
			return false;
		}
		String first = tokens.getFirst().text().toUpperCase();

		if (!first.equals("SELECT") && !first.equals("WITH")) {
			return false;
		}
		return tokens.stream().filter(SqlContext.Token::isName).noneMatch(token -> CHANGES.contains(token.text().toUpperCase()));
	}
}

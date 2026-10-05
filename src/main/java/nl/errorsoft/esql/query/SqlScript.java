package nl.errorsoft.esql.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a script into statements on {@code ;}. A semicolon inside a string ('...'), a quoted name ("..." or `...`), a PostgreSQL dollar quoted string
 * ($$...$$, $tag$...$tag$), a line comment (--) or a block comment does not end a statement. Statements that hold nothing but whitespace and comments are
 * dropped.
 */
public final class SqlScript {
	/** One statement of a script: its text without the closing semicolon, and where it starts and ends (exclusive) in the script. */
	public record Statement(String sql, int start, int end) {
	}

	private SqlScript() {
	}

	public static List<Statement> split(String script) {
		List<Statement> statements = new ArrayList<>();

		for (int[] range : ranges(script)) {
			Statement statement = trimmed(script, range[0], range[1]);

			if (statement != null) {
				statements.add(statement);
			}
		}
		return statements;
	}

	/**
	 * The statement the caret is in. A caret right after a semicolon, or in the blank space behind the last statement, belongs to the statement before it.
	 * Empty when the script has no statements.
	 */
	public static java.util.Optional<Statement> statementAt(String script, int offset) {
		Statement before = null;

		for (Statement statement : split(script)) {
			if (offset < statement.start()) {
				// The caret is in the space between two statements: the one before it is meant, or this one when it is the first.
				return java.util.Optional.of(before != null ? before : statement);
			}
			if (offset <= statement.end() + 1) {
				return java.util.Optional.of(statement);
			}
			before = statement;
		}
		return java.util.Optional.ofNullable(before);
	}

	/** The raw ranges between the semicolons that end statements, [start, end) each. */
	private static List<int[]> ranges(String s) {
		List<int[]> ranges = new ArrayList<>();
		int start = 0;
		int i = 0;

		while (i < s.length()) {
			char c = s.charAt(i);

			if (c == '\'' || c == '"' || c == '`') {
				i = skipQuoted(s, i, c);
			} else if (c == '-' && next(s, i) == '-') {
				i = skipUntil(s, i + 2, "\n");
			} else if (c == '/' && next(s, i) == '*') {
				i = skipUntil(s, i + 2, "*/");
			} else if (c == '$' && dollarTag(s, i) != null) {
				String tag = dollarTag(s, i);
				i = skipUntil(s, i + tag.length(), tag);
			} else if (c == ';') {
				ranges.add(new int[]{start, i});
				start = i + 1;
				i++;
			} else {
				i++;
			}
		}
		ranges.add(new int[]{start, s.length()});
		return ranges;
	}

	private static char next(String s, int i) {
		return i + 1 < s.length() ? s.charAt(i + 1) : 0;
	}

	/** Skips a quoted text; a doubled quote ('') and, in strings, a backslash escape (MySQL) stay inside it. Returns the index after the closing quote. */
	private static int skipQuoted(String s, int open, char quote) {
		int i = open + 1;

		while (i < s.length()) {
			char c = s.charAt(i);

			if (c == '\\' && quote == '\'') {
				i += 2;
			} else if (c == quote && next(s, i) == quote) {
				i += 2;
			} else if (c == quote) {
				return i + 1;
			} else {
				i++;
			}
		}
		return s.length();
	}

	/** Returns the index after {@code end}, or the end of the text when it does not come. */
	private static int skipUntil(String s, int from, String end) {
		int found = s.indexOf(end, from);
		return found < 0 ? s.length() : found + end.length();
	}

	/** The dollar quote that starts at {@code i} ($$ or $tag$), null when the $ is something else (a parameter such as $1). */
	private static String dollarTag(String s, int i) {
		int j = i + 1;

		while (j < s.length() && (Character.isLetter(s.charAt(j)) || s.charAt(j) == '_' || (j > i + 1 && Character.isDigit(s.charAt(j))))) {
			j++;
		}
		return j < s.length() && s.charAt(j) == '$' ? s.substring(i, j + 1) : null;
	}

	/** The range without surrounding whitespace, null when nothing but whitespace and comments is left. */
	private static Statement trimmed(String s, int start, int end) {
		// Leading comments are left out, so that a statement starts with its first word.
		boolean skipped = true;

		while (skipped) {
			skipped = false;

			while (start < end && Character.isWhitespace(s.charAt(start))) {
				start++;
			}
			if (s.startsWith("--", start) && start < end) {
				start = Math.min(end, skipUntil(s, start + 2, "\n"));
				skipped = true;
			} else if (s.startsWith("/*", start) && start < end) {
				start = Math.min(end, skipUntil(s, start + 2, "*/"));
				skipped = true;
			}
		}
		while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
			end--;
		}
		String sql = s.substring(start, end);
		return onlyComments(sql) ? null : new Statement(sql, start, end);
	}

	private static boolean onlyComments(String sql) {
		String rest = sql.replaceAll("(?s)/\\*.*?(\\*/|$)", " ").replaceAll("--[^\\n]*", " ");
		return rest.isBlank();
	}
}

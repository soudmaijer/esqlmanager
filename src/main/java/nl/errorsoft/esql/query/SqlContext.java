package nl.errorsoft.esql.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What can be typed at the caret of a statement, for auto completion. A small tokenizer, not a parser: it finds the tables a statement names after FROM,
 * JOIN, UPDATE and INTO with their aliases, and looks at the words just before the caret.
 *
 * @param kind what fits at the caret.
 * @param prefix the part of a name typed so far, empty when none.
 * @param table for {@link Kind#COLUMNS}: the table whose columns fit, an alias already resolved; null when the qualifier is unknown.
 * @param tables the tables the statement names, in order, without quotes; a schema qualified table as {@code schema.table}.
 * @param schema for {@link Kind#TABLES}: the schema typed before the dot ({@code FROM sales.|}), null when none.
 */
public record SqlContext(Kind kind, String prefix, String table, List<String> tables, String schema) {
	public enum Kind {
		/** After FROM, JOIN, UPDATE, INTO or TABLE: a table name. */
		TABLES,
		/** After {@code name.}: a column of that table. */
		COLUMNS,
		/** Anywhere else: keywords, functions and the columns of the tables in the statement. */
		ANY
	}

	private static final Set<String> TABLE_KEYWORDS = Set.of("FROM", "JOIN", "UPDATE", "INTO", "TABLE");

	/** Words that end a list of tables, so they are never taken for an alias. */
	private static final Set<String> CLAUSE_WORDS = Set.of("WHERE", "ON", "USING", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "NATURAL",
		"GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET", "SET", "VALUES", "SELECT", "UNION", "EXCEPT", "INTERSECT", "WINDOW", "RETURNING", "FOR", "AS",
		"FROM", "INTO", "UPDATE", "TABLE", "DEFAULT", "LATERAL", "FETCH");

	/**
	 * @param statement the text of one statement.
	 * @param caret the offset of the caret in that text.
	 */
	public static SqlContext at(String statement, int caret) {
		String before = statement.substring(0, Math.max(0, Math.min(caret, statement.length())));
		List<Token> all = tokenize(statement);
		Map<String, String> aliases = new LinkedHashMap<>();
		List<String> tables = tables(all, aliases);

		List<Token> head = tokenize(before);
		String prefix = "";

		// The word being typed is the last token, when the caret touches it.
		if (!head.isEmpty() && head.getLast().isName() && head.getLast().end() == before.length()) {
			prefix = head.getLast().text();
			head = head.subList(0, head.size() - 1);
		}

		if (head.size() >= 2 && head.getLast().is(".") && head.get(head.size() - 2).isName()) {
			String qualifier = head.get(head.size() - 2).name();

			// FROM sales.| : the tables of a schema.
			if (head.size() >= 3 && expectsTable(head.subList(0, head.size() - 2))) {
				return new SqlContext(Kind.TABLES, prefix, null, tables, qualifier);
			}
			return new SqlContext(Kind.COLUMNS, prefix, resolve(qualifier, aliases, tables), tables, null);
		}

		if (!head.isEmpty() && expectsTable(head)) {
			return new SqlContext(Kind.TABLES, prefix, null, tables, null);
		}

		return new SqlContext(Kind.ANY, prefix, null, tables, null);
	}

	/** True when the tokens end where a table name goes: after FROM, JOIN, UPDATE, INTO, TABLE or a comma in a FROM list. */
	private static boolean expectsTable(List<Token> head) {
		return TABLE_KEYWORDS.contains(head.getLast().upper()) || afterTableListComma(head);
	}

	/** True for {@code FROM a, |}: a comma inside the table list of a FROM. */
	private static boolean afterTableListComma(List<Token> head) {
		if (!head.getLast().is(",")) {
			return false;
		}

		for (int i = head.size() - 2; i >= 0; i--) {
			String word = head.get(i).upper();

			if (word.equals("FROM")) {
				return true;
			}
			if (head.get(i).isName() && CLAUSE_WORDS.contains(word) || head.get(i).is("(")) {
				return false;
			}
		}
		return false;
	}

	private static String resolve(String qualifier, Map<String, String> aliases, List<String> tables) {
		String table = aliases.get(qualifier.toLowerCase(Locale.ROOT));

		if (table != null) {
			return table;
		}
		for (String name : tables) {
			if (name.equalsIgnoreCase(qualifier) || name.toLowerCase(Locale.ROOT).endsWith("." + qualifier.toLowerCase(Locale.ROOT))) {
				return name;
			}
		}
		// A table that is not in the FROM yet, the completion can still look it up by name.
		return qualifier;
	}

	/** The tables after FROM, JOIN, UPDATE and INTO, and their aliases (lower case alias to table). */
	private static List<String> tables(List<Token> tokens, Map<String, String> aliases) {
		List<String> tables = new ArrayList<>();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);

			if (!token.isName() || !TABLE_KEYWORDS.contains(token.upper())) {
				continue;
			}

			boolean list = token.upper().equals("FROM");
			int j = i + 1;

			while (j < tokens.size() && tokens.get(j).isName() && !CLAUSE_WORDS.contains(tokens.get(j).upper())) {
				// schema.table is kept qualified, so the completion can find the table in its schema.
				String table = tokens.get(j).name();
				j++;

				while (j + 1 < tokens.size() && tokens.get(j).is(".") && tokens.get(j + 1).isName()) {
					table = table + "." + tokens.get(j + 1).name();
					j += 2;
				}
				tables.add(table);

				if (j < tokens.size() && tokens.get(j).upper().equals("AS")) {
					j++;
				}
				if (j < tokens.size() && tokens.get(j).isName() && !CLAUSE_WORDS.contains(tokens.get(j).upper())) {
					aliases.put(tokens.get(j).name().toLowerCase(Locale.ROOT), table);
					j++;
				}
				if (list && j < tokens.size() && tokens.get(j).is(",")) {
					j++;
				} else {
					break;
				}
			}
			i = j - 1;
		}
		return tables;
	}

	/** A word, a quoted name or a single punctuation character; strings, numbers and comments are left out. */
	record Token(String text, int end, boolean isName) {
		boolean is(String punctuation) {
			return !isName && text.equals(punctuation);
		}

		String upper() {
			return isName ? text.toUpperCase(Locale.ROOT) : "";
		}

		/** The name without its quotes. */
		String name() {
			if (text.length() >= 2 && (text.charAt(0) == '"' || text.charAt(0) == '`')) {
				return text.substring(1, text.length() - 1);
			}
			return text;
		}
	}

	static List<Token> tokenize(String s) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;

		while (i < s.length()) {
			char c = s.charAt(i);

			if (Character.isWhitespace(c)) {
				i++;
			} else if (c == '-' && i + 1 < s.length() && s.charAt(i + 1) == '-') {
				int end = s.indexOf('\n', i);
				i = end < 0 ? s.length() : end + 1;
			} else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
				int end = s.indexOf("*/", i + 2);
				i = end < 0 ? s.length() : end + 2;
			} else if (c == '\'') {
				int end = s.indexOf('\'', i + 1);
				i = end < 0 ? s.length() : end + 1;
			} else if (c == '"' || c == '`') {
				int end = s.indexOf(c, i + 1);
				int stop = end < 0 ? s.length() : end + 1;
				tokens.add(new Token(s.substring(i, stop), stop, true));
				i = stop;
			} else if (Character.isLetter(c) || c == '_') {
				int start = i;

				while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '$')) {
					i++;
				}
				tokens.add(new Token(s.substring(start, i), i, true));
			} else if (Character.isDigit(c)) {
				while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '.')) {
					i++;
				}
			} else {
				tokens.add(new Token(String.valueOf(c), i + 1, false));
				i++;
			}
		}
		return tokens;
	}
}

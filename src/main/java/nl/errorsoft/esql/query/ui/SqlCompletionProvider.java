package nl.errorsoft.esql.query.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

import nl.errorsoft.esql.query.SchemaNames;
import nl.errorsoft.esql.query.SqlContext;
import nl.errorsoft.esql.query.SqlScript;

/**
 * Completion for the SQL editor. After FROM, JOIN, UPDATE and INTO it offers tables, after {@code alias.} the columns of that table, elsewhere keywords,
 * functions and the columns of the tables the statement names. Names that need quotes are inserted quoted.
 */
public class SqlCompletionProvider extends DefaultCompletionProvider {
	private static final Pattern PLAIN_NAME = Pattern.compile("[a-z_][a-z0-9_]*");

	private static final List<String> KEYWORDS = List.of("SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS", "NULL", "LIKE", "BETWEEN", "EXISTS",
		"AS", "DISTINCT", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "ON", "USING", "GROUP BY", "ORDER BY", "HAVING",
		"LIMIT", "OFFSET", "UNION", "UNION ALL", "ASC", "DESC", "CASE", "WHEN", "THEN", "ELSE", "END", "INSERT INTO", "VALUES", "UPDATE", "SET",
		"DELETE FROM", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "TRUNCATE TABLE", "CREATE INDEX", "DROP INDEX", "PRIMARY KEY", "FOREIGN KEY",
		"REFERENCES", "DEFAULT", "UNIQUE", "CHECK", "CONSTRAINT", "ADD COLUMN", "DROP COLUMN", "BEGIN", "COMMIT", "ROLLBACK", "WITH", "RETURNING",
		"TRUE", "FALSE", "USE", "SHOW", "EXPLAIN");

	private static final List<String> FUNCTIONS = List.of("COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "NULLIF", "CAST", "UPPER", "LOWER", "TRIM",
		"LENGTH", "SUBSTRING", "CONCAT", "REPLACE", "ROUND", "ABS", "NOW", "CURRENT_DATE", "CURRENT_TIMESTAMP", "EXTRACT");

	private final SchemaNames names;

	public SqlCompletionProvider(SchemaNames names) {
		this.names = names;
		// A period after a name opens the columns of that table.
		setAutoActivationRules(false, ".");
	}

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent editor) {
		String text = editor.getText();
		int caret = editor.getCaretPosition();
		SqlScript.Statement statement = SqlScript.statementAt(text, caret).filter(s -> caret >= s.start() && caret <= s.end() + 1)
			.orElse(new SqlScript.Statement("", caret, caret));
		SqlContext context = SqlContext.at(statement.sql(), caret - statement.start());

		List<Completion> completions = new ArrayList<>();
		String prefix = context.prefix().toLowerCase(Locale.ROOT);

		switch (context.kind()) {
			case TABLES -> add(completions, names.tables(), "table", prefix, true);
			case COLUMNS -> add(completions, context.table() == null ? List.of() : names.columns(context.table()), context.table(), prefix, true);
			case ANY -> {
				Set<String> statementColumns = new LinkedHashSet<>();
				for (String table : context.tables()) {
					statementColumns.addAll(names.columns(table));
				}
				add(completions, List.copyOf(statementColumns), "column", prefix, true);
				add(completions, FUNCTIONS, "function", prefix, false);
				add(completions, KEYWORDS, "keyword", prefix, false);
				add(completions, names.tables(), "table", prefix, true);
			}
		}
		return completions;
	}

	private void add(List<Completion> completions, List<String> words, String description, String prefix, boolean identifiers) {
		for (String word : words) {
			if (word.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				String replacement = identifiers ? quoteIfNeeded(word) : word;
				completions.add(new BasicCompletion(this, replacement, description));
			}
		}
	}

	/** A name in lower case letters, digits and underscores is inserted as it is, any other name in the quotes of the database. */
	String quoteIfNeeded(String name) {
		boolean keyword = KEYWORDS.contains(name.toUpperCase(Locale.ROOT)) || FUNCTIONS.contains(name.toUpperCase(Locale.ROOT));
		return PLAIN_NAME.matcher(name).matches() && !keyword ? name : names.quote(name);
	}
}

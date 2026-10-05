package nl.errorsoft.esql.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SqlScriptTest {
	private static List<String> sql(String script) {
		return SqlScript.split(script).stream().map(SqlScript.Statement::sql).toList();
	}

	@Test
	void splitsOnSemicolons() {
		assertEquals(List.of("select 1", "select 2"), sql("select 1;\nselect 2"));
	}

	@Test
	void trailingSemicolonAndEmptyStatementsAreDropped() {
		assertEquals(List.of("select 1", "select 2"), sql("select 1;;  ;\n select 2;\n\n"));
	}

	@Test
	void semicolonsInQuotesDoNotSplit() {
		assertEquals(List.of("insert into t values ('a;b', 'it''s;')", "select \"x;y\", `p;q` from t"),
			sql("insert into t values ('a;b', 'it''s;'); select \"x;y\", `p;q` from t;"));
	}

	@Test
	void backslashEscapedQuoteStaysInString() {
		assertEquals(List.of("select 'a\\';b'", "select 2"), sql("select 'a\\';b'; select 2"));
	}

	@Test
	void commentsDoNotSplit() {
		assertEquals(List.of("select 1 -- not; here\n + 1", "select /* a; b */ 2"), sql("select 1 -- not; here\n + 1; select /* a; b */ 2"));
	}

	@Test
	void commentOnlyStatementsAreDropped() {
		assertEquals(List.of("select 1"), sql("-- header;\n/* block; */\nselect 1;\n-- trailer"));
	}

	@Test
	void dollarQuotingDoesNotSplit() {
		String body = "create function f() returns int as $$ begin return 1; end; $$ language plpgsql";
		String tagged = "do $body$ begin perform 1; end $body$";
		assertEquals(List.of(body, tagged, "select $1"), sql(body + ";\n" + tagged + "; select $1"));
	}

	@Test
	void offsetsPointIntoTheScript() {
		String script = "  select 1;\nselect 2  ";
		SqlScript.Statement second = SqlScript.split(script).get(1);
		assertEquals("select 2", script.substring(second.start(), second.end()));
	}

	@Test
	void statementAtCaret() {
		String script = "select 1;\n\nselect 2;\nselect 3";
		assertEquals("select 1", SqlScript.statementAt(script, 3).orElseThrow().sql());
		assertEquals("select 1", SqlScript.statementAt(script, 9).orElseThrow().sql());
		assertEquals("select 2", SqlScript.statementAt(script, script.indexOf("2")).orElseThrow().sql());
		assertEquals("select 3", SqlScript.statementAt(script, script.length()).orElseThrow().sql());
		assertTrue(SqlScript.statementAt("  -- nothing\n", 2).isEmpty());
	}
}

package nl.errorsoft.esql.query.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.error.EsqlException;

/**
 * A small JSON reader for the plans servers give: an object is a {@code Map} in key order, an array a {@code List}, a number a {@code Double}, and
 * strings, booleans and null as themselves. Enough for EXPLAIN output, not a general purpose library.
 */
final class JsonReader {
	private final String text;
	private int at;

	private JsonReader(String text) {
		this.text = text;
	}

	static Object read(String text) {
		JsonReader reader = new JsonReader(text);
		Object value = reader.value();
		reader.skipSpace();
		if (reader.at != text.length()) {
			throw reader.problem("text after the end");
		}
		return value;
	}

	private Object value() {
		skipSpace();
		if (at >= text.length()) {
			throw problem("unexpected end");
		}
		char c = text.charAt(at);
		return switch (c) {
			case '{' -> object();
			case '[' -> array();
			case '"' -> string();
			case 't' -> word("true", Boolean.TRUE);
			case 'f' -> word("false", Boolean.FALSE);
			case 'n' -> word("null", null);
			default -> number();
		};
	}

	private Map<String, Object> object() {
		Map<String, Object> map = new LinkedHashMap<>();
		at++;
		skipSpace();
		if (peek() == '}') {
			at++;
			return map;
		}
		while (true) {
			skipSpace();
			String key = string();
			skipSpace();
			expect(':');
			map.put(key, value());
			skipSpace();
			if (peek() == ',') {
				at++;
			} else {
				expect('}');
				return map;
			}
		}
	}

	private List<Object> array() {
		List<Object> list = new ArrayList<>();
		at++;
		skipSpace();
		if (peek() == ']') {
			at++;
			return list;
		}
		while (true) {
			list.add(value());
			skipSpace();
			if (peek() == ',') {
				at++;
			} else {
				expect(']');
				return list;
			}
		}
	}

	private String string() {
		expect('"');
		StringBuilder out = new StringBuilder();
		while (at < text.length()) {
			char c = text.charAt(at++);
			if (c == '"') {
				return out.toString();
			}
			if (c == '\\' && at < text.length()) {
				char escaped = text.charAt(at++);
				switch (escaped) {
					case 'n' -> out.append('\n');
					case 't' -> out.append('\t');
					case 'r' -> out.append('\r');
					case 'b' -> out.append('\b');
					case 'f' -> out.append('\f');
					case 'u' -> {
						out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
						at += 4;
					}
					default -> out.append(escaped);
				}
			} else {
				out.append(c);
			}
		}
		throw problem("a string is not closed");
	}

	private Object word(String word, Object value) {
		if (!text.startsWith(word, at)) {
			throw problem("unexpected '" + text.charAt(at) + "'");
		}
		at += word.length();
		return value;
	}

	private Double number() {
		int start = at;
		while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
			at++;
		}
		try {
			return Double.valueOf(text.substring(start, at));
		} catch (NumberFormatException e) {
			throw problem("unexpected '" + text.charAt(start) + "'");
		}
	}

	private char peek() {
		return at < text.length() ? text.charAt(at) : 0;
	}

	private void expect(char c) {
		if (peek() != c) {
			throw problem("expected '" + c + "'");
		}
		at++;
	}

	private void skipSpace() {
		while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
			at++;
		}
	}

	private EsqlException problem(String what) {
		return new EsqlException("The plan is not valid JSON: " + what + " at position " + at + ".");
	}
}

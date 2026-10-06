package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import nl.errorsoft.esql.error.EsqlException;
import org.junit.jupiter.api.Test;

class ValueFormatTest {

	@Test
	void theKindIsGuessedFromTheText() {
		assertEquals(ValueFormat.JSON, ValueFormat.detect(" {\"a\": 1} "));
		assertEquals(ValueFormat.JSON, ValueFormat.detect("[1, 2]"));
		assertEquals(ValueFormat.XML, ValueFormat.detect("<order id=\"1\"/>"));
		assertEquals(ValueFormat.SQL, ValueFormat.detect("select * from orders"));
		assertEquals(ValueFormat.PLAIN, ValueFormat.detect("Selected items"));
		assertEquals(ValueFormat.PLAIN, ValueFormat.detect(null));
	}

	@Test
	void jsonIsIndentedOutsideStrings() throws Exception {
		String formatted = ValueFormat.JSON.format("{\"name\":\"a, {b}\",\"tags\":[],\"items\":[1,2]}");

		assertEquals("""
			{
			  "name": "a, {b}",
			  "tags": [],
			  "items": [
			    1,
			    2
			  ]
			}""", formatted);
	}

	@Test
	void anEscapedQuoteDoesNotEndAString() throws Exception {
		assertEquals("{\n  \"q\": \"say \\\"hi\\\"\"\n}", ValueFormat.JSON.format("{\"q\":\"say \\\"hi\\\"\"}"));
	}

	@Test
	void brokenJsonIsRefused() {
		assertThrows(EsqlException.class, () -> ValueFormat.JSON.format("{\"a\": [1}"));
		assertThrows(EsqlException.class, () -> ValueFormat.JSON.format("{\"a\": \"open}"));
	}

	@Test
	void xmlIsIndented() throws Exception {
		assertEquals("<order>\n  <line qty=\"2\">tea</line>\n</order>", ValueFormat.XML.format("<order>  <line qty=\"2\">tea</line></order>"));
	}

	@Test
	void xmlWithADocumentTypeIsRefused() {
		assertThrows(EsqlException.class, () -> ValueFormat.XML.format("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>"));
	}
}

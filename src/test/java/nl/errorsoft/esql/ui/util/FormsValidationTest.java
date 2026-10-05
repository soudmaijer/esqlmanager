package nl.errorsoft.esql.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FormsValidationTest {
	@Test
	void requiredRejectsBlankAndNull() {
		assertEquals("Enter a name.", Validation.required("a name", "  "));
		assertEquals("Enter a name.", Validation.required("a name", null));
		assertNull(Validation.required("a name", "shop"));
	}

	@Test
	void passwordsMustBeTheSame() {
		assertNull(Validation.same("a".toCharArray(), "a".toCharArray(), "no"));
		assertEquals("no", Validation.same("a".toCharArray(), "b".toCharArray(), "no"));
	}

	@Test
	void firstReportsTheFirstMessage() {
		assertNull(Validation.first(null, null));
		assertEquals("two", Validation.first(null, "two", "three"));
	}

	@Test
	void mnemonicIsMarkedByAmpersand() {
		Forms.MnemonicText parsed = Forms.MnemonicText.parse("&Create");
		assertEquals("Create", parsed.text());
		assertEquals('C', parsed.mnemonic());
		assertEquals(0, parsed.index());

		Forms.MnemonicText middle = Forms.MnemonicText.parse("Dr&op table");
		assertEquals("Drop table", middle.text());
		assertEquals(2, middle.index());
	}

	@Test
	void noAmpersandMeansNoMnemonicAndDoubleAmpersandIsLiteral() {
		assertEquals(-1, Forms.MnemonicText.parse("Cancel").index());
		Forms.MnemonicText literal = Forms.MnemonicText.parse("Q&&A");
		assertEquals("Q&A", literal.text());
		assertEquals(-1, literal.index());
	}
}

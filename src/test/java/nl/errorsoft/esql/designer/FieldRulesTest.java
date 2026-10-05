package nl.errorsoft.esql.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import nl.errorsoft.esql.designer.ui.dialog.FieldRules;
import org.junit.jupiter.api.Test;

class FieldRulesTest {
	@Test
	void lengthIsANumberOrPrecisionAndScale() {
		assertNull(FieldRules.lengthProblem("VARCHAR", ""));
		assertNull(FieldRules.lengthProblem("VARCHAR", "255"));
		assertNull(FieldRules.lengthProblem("DECIMAL", "10, 2"));
		assertNotNull(FieldRules.lengthProblem("VARCHAR", "abc"));
		assertNotNull(FieldRules.lengthProblem("INT", "-1"));
	}

	@Test
	void enumListsValues() {
		assertNull(FieldRules.lengthProblem("ENUM", "'a','b'"));
	}

	@Test
	void namesAreUniqueIgnoringCase() {
		assertNull(FieldRules.nameProblem(List.of("id", "name")));
		assertNotNull(FieldRules.nameProblem(List.of("id", "ID")));
		assertNotNull(FieldRules.nameProblem(List.of("id", " ")));
	}

	@Test
	void uniqueNameCounts() {
		assertEquals("new_field", FieldRules.uniqueName("new_field", List.of("id")));
		assertEquals("new_field_3", FieldRules.uniqueName("new_field", List.of("new_field", "new_field_2")));
	}
}

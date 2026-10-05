package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class ColumnOptionsTest {
	@Test
	void theColumnKeepsOnlyWhatTheTypeAllows() {
		DataType text = new DataType("text", EnumSet.of(DataType.Option.NOT_NULL));
		CreateColumn column = new ColumnOptions("note", "", "x", text, false, true, false, false, "a note").toColumn();

		assertEquals("note", column.name);
		assertEquals("x", column.defaultval);
		assertEquals("a note", column.comment);
		assertTrue(column.notnull);
		assertFalse(column.unsigned, "text has no unsigned");
	}
}

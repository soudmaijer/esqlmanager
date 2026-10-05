package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.table.DataType.Option;

class CreateColumnTest {
	private static final DataType INTEGER = new DataType("integer", EnumSet.of(Option.PRIMARY, Option.NOT_NULL, Option.AUTO_INCREMENT, Option.UNSIGNED));
	private static final DataType TEXT = new DataType("text", EnumSet.of(Option.NOT_NULL));

	@Test
	void aTypeSwitchesOffTheOptionsItDoesNotAllow() {
		CreateColumn column = new CreateColumn("id");
		column.applyType(INTEGER);
		column.primary = true;
		column.notnull = true;
		column.autoincrement = true;
		column.unsigned = true;

		column.applyType(TEXT);

		assertEquals(TEXT, column.type);
		assertTrue(column.notnull);
		assertFalse(column.primary);
		assertFalse(column.autoincrement);
		assertFalse(column.unsigned);
	}

	@Test
	void noTypeLeavesTheColumnAlone() {
		CreateColumn column = new CreateColumn("id");
		column.applyType(INTEGER);
		column.primary = true;

		column.applyType(null);

		assertEquals(INTEGER, column.type);
		assertTrue(column.primary);
	}
}

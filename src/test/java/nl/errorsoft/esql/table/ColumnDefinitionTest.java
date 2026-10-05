package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.table.DataType.Option;

class ColumnDefinitionTest {
	private static final DataType INTEGER = new DataType("integer", EnumSet.of(Option.PRIMARY, Option.NOT_NULL, Option.AUTO_INCREMENT, Option.UNSIGNED));
	private static final DataType TEXT = new DataType("text", EnumSet.of(Option.NOT_NULL));

	@Test
	void aTypeSwitchesOffTheOptionsItDoesNotAllow() {
		ColumnDefinition column = new ColumnDefinition("id");
		column.applyType(INTEGER);
		column.primary = true;
		column.notNull = true;
		column.autoIncrement = true;
		column.unsigned = true;

		column.applyType(TEXT);

		assertEquals(TEXT, column.type);
		assertTrue(column.notNull);
		assertFalse(column.primary);
		assertFalse(column.autoIncrement);
		assertFalse(column.unsigned);
	}

	@Test
	void noTypeLeavesTheColumnAlone() {
		ColumnDefinition column = new ColumnDefinition("id");
		column.applyType(INTEGER);
		column.primary = true;

		column.applyType(null);

		assertEquals(INTEGER, column.type);
		assertTrue(column.primary);
	}
}

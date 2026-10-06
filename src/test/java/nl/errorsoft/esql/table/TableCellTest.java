package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TableCellTest {

	@Test
	void sqlNullGivesNoDataAndIsNotTheTextNull() {
		TableCell cell = new TableCell();
		cell.setData(null);

		assertTrue(cell.isNull());
		assertNull(cell.getData());
		assertNull(cell.getNativeData());
		assertEquals("", cell.getEditText());
	}

	@Test
	void theTextNullIsData() {
		TableCell cell = new TableCell();
		cell.setEditedText("null");

		assertFalse(cell.isNull());
		assertEquals("null", cell.getData());
		assertEquals("null", cell.toString());
	}

	@Test
	void anEmptyEditedTextIsNull() {
		TableCell cell = new TableCell();
		cell.setData("x");
		cell.setEditedText("");

		assertTrue(cell.isNull());
	}
}

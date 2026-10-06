package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PendingChangeTest {

	private static TableCell cell(Object data) {
		TableCell cell = new TableCell();
		cell.setData(data);
		return cell;
	}

	@Test
	void theSameTextIsNoChange() {
		assertFalse(cell("shop").isChangedBy("shop"));
		assertFalse(cell(42).isChangedBy("42"));
	}

	@Test
	void anEmptyTextIsNullSoItOnlyChangesACellWithAValue() {
		assertFalse(cell(null).isChangedBy(""));
		assertFalse(cell(null).isChangedBy(null));
		assertTrue(cell("shop").isChangedBy(""));
		assertTrue(cell("").isChangedBy(""));
		assertTrue(cell(null).isChangedBy("null"));
	}

	@Test
	void anEditedCellIsPendingOnlyWhenItsTextDiffers() {
		TableCell cell = cell("shop");

		assertEquals(PendingChange.NONE, PendingChange.of(true, false, cell, "shop"));
		assertEquals(PendingChange.CELL, PendingChange.of(true, false, cell, "shop 2"));
		assertEquals(PendingChange.NONE, PendingChange.of(true, false, null, null));
	}

	@Test
	void aNewRowIsPendingUntilItIsInserted() {
		assertEquals(PendingChange.NEW_ROW, PendingChange.of(true, true, null, null));
		assertEquals(PendingChange.CELL, PendingChange.of(true, true, cell(null), "x"));
	}

	@Test
	void aViewThatCannotBeWrittenBackHasNothingPending() {
		assertEquals(PendingChange.NONE, PendingChange.of(false, true, cell("shop"), "other"));
	}
}

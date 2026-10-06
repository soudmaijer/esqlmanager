package nl.errorsoft.esql.table;

/** What "Update changes" would write for the selected row: nothing, the edited value of a cell, or a new row that was never inserted. */
public enum PendingChange {
	NONE, CELL, NEW_ROW;

	/**
	 * The change waiting in a view. {@code editedCell} is the cell whose text is being edited (null when none), {@code editedText} that text; a view that
	 * cannot be written back has nothing to write.
	 */
	public static PendingChange of(boolean editable, boolean newRowSelected, TableCell editedCell, String editedText) {
		if (!editable) {
			return NONE;
		} else if (editedCell != null && editedCell.isChangedBy(editedText)) {
			return CELL;
		} else if (newRowSelected) {
			return NEW_ROW;
		}
		return NONE;
	}
}

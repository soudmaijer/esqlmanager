package nl.errorsoft.esql.table;

public class TableData {
	private Object data;
	private boolean newRow = false;
	private boolean nullData = true;
	private nl.errorsoft.esql.table.TableColumn tc;

	public TableData() {
	}

	public void setTableColumn(nl.errorsoft.esql.table.TableColumn tc) {
		this.tc = tc;
	}

	public TableColumn getTableColumn() {
		return tc;
	}

	public void setData(Object data) {
		nullData = data == null;
		this.data = data;
	}

	/** Takes text typed into a cell editor: an empty text is SQL NULL. */
	public void setEditedText(String text) {
		setData(text == null || text.isEmpty() ? null : text);
	}

	/** The text a cell editor starts with: empty for NULL, so that it is not mistaken for the text "null". */
	public String getEditText() {
		return nullData ? "" : getData();
	}

	public void setNewRow(boolean newRow) {
		this.newRow = newRow;
	}

	public boolean isNewRow() {
		return newRow;
	}

	public String getData() {
		if (data == null) {
			return "null";
		} else {
			return data.toString();
		}
	}

	public Object getNativeData() {
		if (data == null) {
			return "null";
		} else {
			return data;
		}
	}

	public String toString() {
		if (tc.isBinary()) {
			return "[BINARY]";
		} else if (data == null) {
			return "null";
		} else {
			return data.toString();
		}
	}

	public boolean isNull() {
		return nullData;
	}
}

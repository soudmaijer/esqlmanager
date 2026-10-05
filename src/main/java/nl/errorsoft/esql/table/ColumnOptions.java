package nl.errorsoft.esql.table;

/** What the user filled in for one column in the field properties dialog, before it is turned into a {@link CreateColumn}. */
public record ColumnOptions(String name, String length, String defaultValue, DataType type, boolean autoIncrement, boolean unsigned, boolean nullable,
	boolean primary, String comment) {

	/** The column definition, with the options the type does not allow left off. */
	public CreateColumn toColumn() {
		CreateColumn column = TableService.newColumn(name, length, defaultValue, type, autoIncrement, unsigned, nullable);
		column.primary = primary;
		column.comment = comment;
		return column;
	}
}

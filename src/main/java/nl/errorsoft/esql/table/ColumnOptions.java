package nl.errorsoft.esql.table;

/** What the user filled in for one column in the field properties dialog, before it is turned into a {@link ColumnDefinition}. */
public record ColumnOptions(String name, String length, String defaultValue, DataType type, boolean autoIncrement, boolean unsigned, boolean nullable,
	boolean primary, String comment) {

	/** The column definition, with the options the type does not allow left off. */
	public ColumnDefinition toColumn() {
		ColumnDefinition column = new ColumnDefinition(name);
		column.type = type;
		column.length = length;
		column.defaultValue = defaultValue;
		column.unsigned = type.allows(DataType.Option.UNSIGNED) && unsigned;
		column.notNull = type.allows(DataType.Option.NOT_NULL) && !nullable;
		column.autoIncrement = autoIncrement;
		column.primary = primary;
		column.comment = comment;
		return column;
	}
}

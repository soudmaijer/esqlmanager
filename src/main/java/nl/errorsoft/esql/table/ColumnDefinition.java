package nl.errorsoft.esql.table;

public class ColumnDefinition {
	public String name = "";

	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notNull = false;
	public boolean unsigned = false;
	public boolean autoIncrement = false;
	public boolean zerofill = false;

	public DataType type = null;

	public String defaultValue = "";
	public String length = "";
	/** The comment of the column, written where {@code Dialect.supportsColumnComments}. */
	public String comment = "";

	public ColumnDefinition(String name) {
		this.name = name;
	}

	/** Chooses the type of the column and switches off the options the type does not allow; null leaves the column as it is. */
	public void applyType(DataType type) {
		if (type == null) {
			return;
		}
		this.type = type;
		primary &= type.allows(DataType.Option.PRIMARY);
		notNull &= type.allows(DataType.Option.NOT_NULL);
		unsigned &= type.allows(DataType.Option.UNSIGNED);
		autoIncrement &= type.allows(DataType.Option.AUTO_INCREMENT);
		zerofill &= type.allows(DataType.Option.ZEROFILL);
	}

	public String toString() {
		return name;
	}
}

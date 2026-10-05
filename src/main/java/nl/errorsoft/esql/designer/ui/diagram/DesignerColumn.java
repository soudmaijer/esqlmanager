package nl.errorsoft.esql.designer.ui.diagram;

/** A column of a table drawn on the designer canvas (the canvas counterpart of {@code table.ColumnDefinition}). */
public class DesignerColumn {
	private String name;
	private String length;
	private String defaultValue;
	private String comment;
	private nl.errorsoft.esql.table.DataType type;

	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notNull = false;
	public boolean unsigned = false;
	public boolean autoIncrement = false;
	public boolean zerofill = false;

	public DesignerColumn(String name, nl.errorsoft.esql.table.DataType type, String length, String defaultValue, String comment) {
		this.name = name;
		this.type = type;
		this.length = length;
		this.defaultValue = defaultValue;
		this.comment = comment;
	}

	/** A copy to edit, so that cancelling a dialog leaves the original untouched. */
	public DesignerColumn copy() {
		DesignerColumn copy = new DesignerColumn(name, type, length, defaultValue, comment);
		copy.primary = primary;
		copy.index = index;
		copy.unique = unique;
		copy.binary = binary;
		copy.notNull = notNull;
		copy.unsigned = unsigned;
		copy.autoIncrement = autoIncrement;
		copy.zerofill = zerofill;
		return copy;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public nl.errorsoft.esql.table.DataType getType() {
		return type;
	}

	public void setType(nl.errorsoft.esql.table.DataType type) {
		this.type = type;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	public String getDefault() {
		return defaultValue;
	}

	public void setDefault(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String toString() {
		return name;
	}
}

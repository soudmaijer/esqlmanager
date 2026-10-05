package nl.errorsoft.esql.designer.ui.diagram;

public class Field {
	private String name;
	private String length;
	private String dfault;
	private String comment;
	private nl.errorsoft.esql.table.DataType type;

	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notnull = false;
	public boolean unsigned = false;
	public boolean autoincrement = false;
	public boolean zerofill = false;

	public Field(String name, nl.errorsoft.esql.table.DataType type, String length, String dfault, String comment) {
		this.name = name;
		this.type = type;
		this.length = length;
		this.dfault = dfault;
		this.comment = comment;
	}

	/** A copy to edit, so that cancelling a dialog leaves the original untouched. */
	public Field copy() {
		Field copy = new Field(name, type, length, dfault, comment);
		copy.primary = primary;
		copy.index = index;
		copy.unique = unique;
		copy.binary = binary;
		copy.notnull = notnull;
		copy.unsigned = unsigned;
		copy.autoincrement = autoincrement;
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
		return dfault;
	}

	public void setDefault(String dfault) {
		this.dfault = dfault;
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

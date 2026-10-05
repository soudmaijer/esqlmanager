
package nl.errorsoft.esql.table;

public class TableIndex {
	private boolean unique = false;
	private boolean newindex = false;
	private boolean fulltext = false;
	private TableColumn[] columns;
	private String name;
	private Table table;

	public TableIndex(Table table) {
		columns = new TableColumn[0];
		this.table = table;
	}

	public Table getTable() {
		return this.table;
	}

	public void setNew(boolean newindex) {
		this.newindex = newindex;
	}

	public boolean isNew() {
		return newindex;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public void setFulltext(boolean fulltext) {
		this.fulltext = fulltext;
	}

	public boolean isFulltext() {
		return this.fulltext;
	}

	public void addTableColumn(TableColumn column) {
		TableColumn[] extended = new TableColumn[columns.length + 1];

		for (int i = 0; i < extended.length - 1; i++) {
			extended[i] = columns[i];
		}

		extended[extended.length - 1] = column;
		columns = extended;
		extended = null;
	}

	public TableColumn[] getTableColumns() {
		return this.columns;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean isPrimary() {
		if (this.getName().equals("PRIMARY")) {
			return true;
		}
		return false;
	}

	public String getName() {
		return (this.name);
	}

	public String toString() {

		return name;
	}
}

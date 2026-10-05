package nl.errorsoft.esql.user.ui;

public class CheckListItem {
	private String str;
	private boolean isSelected;

	public CheckListItem(String str) {
		this.str = str;
		isSelected = false;
	}

	public void setSelected(boolean b) {
		isSelected = b;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public String toString() {
		return str;
	}
}

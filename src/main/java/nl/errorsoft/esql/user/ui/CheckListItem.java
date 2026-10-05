package nl.errorsoft.esql.user.ui;

public class CheckListItem {
	private String text;
	private boolean isSelected;

	public CheckListItem(String text) {
		this.text = text;
		isSelected = false;
	}

	public void setSelected(boolean b) {
		isSelected = b;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public String toString() {
		return text;
	}
}

package nl.errorsoft.esql.designer.ui.dialog;

public interface PropertiesPanel {
	public void saveProperties();

	/** The problem with the input, shown in the dialog while it stays open; null when it can be saved. */
	default String inputProblem() {
		return null;
	}
}

package nl.errorsoft.esql.ui.util;

/**
 * A tab of the connection window that edits something (a table, its indexes). The window asks it before the tab is closed, so unsaved work is not lost
 * silently.
 */
public interface EditorTab {
	/** True when the tab may close: nothing changed, or the user chose to discard the changes. */
	boolean confirmClose();
}

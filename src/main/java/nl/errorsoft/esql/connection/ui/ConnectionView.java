package nl.errorsoft.esql.connection.ui;

import java.awt.Component;

import javax.swing.JComponent;

import nl.errorsoft.esql.connection.TreeSelection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.ui.ConnectionBranch;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.table.ui.TableListTab;

/**
 * Where one connection shows its tree, its work windows and its messages: what the controllers of a connection ask of the screen. Called on the event
 * thread, except {@link #setStatus(String)} and {@link #setStatus(Component, String)}, which marshal themselves.
 */
public interface ConnectionView {
	/** The parent of the dialogs and messages of this connection. */
	Component dialogParent();

	// The tree.

	/** The databases, schemas, tables and columns of this connection in the explorer. */
	ConnectionBranch branch();

	/** What is selected in the explorer when it belongs to this connection (a database, schema, table, column or the connection), else null. */
	Object selectedObject();

	/** The selected database, or the database of the selected schema, table or column; null when nothing is selected. */
	default Database getDatabase() {
		return TreeSelection.database(selectedObject());
	}

	/** The selected schema, or the schema of the selected table or column; null when there is none. */
	default Schema getSchema() {
		return TreeSelection.schema(selectedObject());
	}

	/** The selected table, or the table of the selected column; null when nothing or something else is selected. */
	default Table getTable() {
		return TreeSelection.table(selectedObject());
	}

	/** The selected column, null when no column is selected. */
	default TableColumn getTableColumn() {
		return TreeSelection.column(selectedObject());
	}

	// The work windows, each with a tab of its own.

	/** Shows table data in the window opened under this key (a new one when there is none) and puts it in front. */
	void showTableDataTab(String key, String title, TableDataTab tableDataTab);

	/** Shows a table list in the window opened under this key (a new one when there is none) and puts it in front. */
	void showTableListTab(String key, String title, TableListTab tableListTab);

	/** Closes the table data and table lists whose key starts with this prefix, after their database or schema was dropped. */
	void removeViews(String keyPrefix);

	void showQueryTab(QueryTab query);

	/** Puts the editor opened under this key in front, false when there is none. */
	boolean selectEditorTab(String key);

	/** Opens an editor (table, indexes) in a new window, found again by its key, and puts it in front. */
	void showEditorTab(String key, String title, JComponent editor);

	/** Closes a window as its close button does, an editor with unsaved changes asks first. */
	void closeTab(Component tab);

	/** Closes a window without asking, for an editor that has just been saved. */
	void removeTab(Component tab);

	// Messages.

	/** A message about the table data or table list shown last. */
	void setStatus(String text);

	/** Keeps a message with its window; it is shown at the bottom of that window. */
	void setStatus(Component tab, String text);
}

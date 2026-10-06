package nl.errorsoft.esql.connection.ui;

import java.awt.Component;

import javax.swing.JComponent;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.ui.DatabaseTree;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.table.ui.TableListTab;

/**
 * Where one connection shows its tree, its tabs and its messages: what the controllers of a connection ask of the screen. Called on the event thread,
 * except {@link #setStatus(String)} and {@link #setStatus(Component, String)}, which marshal themselves.
 */
public interface ConnectionView {
	/** The parent of the dialogs and messages of this connection. */
	Component dialogParent();

	// The tree.

	void showDatabaseTree(DatabaseTree tree);

	DatabaseTree getDatabaseTree();

	/** What is selected in the tree (a database, schema, table, column or the server), null before anything is selected. */
	Object selectedObject();

	/** The selected database, or the database of the selected schema, table or column; null when nothing is selected. */
	Database getDatabase();

	/** The selected schema, or the schema of the selected table or column; null when there is none. */
	Schema getSchema();

	/** The selected table, or the table of the selected column; null when nothing or something else is selected. */
	Table getTable();

	/** The selected column, null when no column is selected. */
	TableColumn getTableColumn();

	// What the toolbar offers for the kind of node that is selected.

	void rootSelected();

	void databaseSelected();

	void tableSelected();

	void fieldSelected();

	// The tabs.

	void showTableDataTab(String tabTitle, TableDataTab tableDataTab);

	void showTableListTab(String tabTitle, TableListTab tableListTab);

	/** Closes the table data or table list, after its database or schema was dropped. */
	void removeDataTab();

	void showQueryTab(QueryTab query);

	/** Puts the editor tab opened under this key in front, false when there is none. */
	boolean selectEditorTab(String key);

	/** Opens an editor (table, indexes) in a new tab, found again by its key, and puts it in front. */
	void showEditorTab(String key, String title, JComponent editor);

	/** Closes a tab as its close button does, an editor with unsaved changes asks first. */
	void closeTab(Component tab);

	/** Closes a tab without asking, for an editor that has just been saved. */
	void removeTab(Component tab);

	// Messages.

	/** A message about the table data or table list tab. */
	void setStatus(String text);

	/** Keeps a message with its tab; it is shown while that tab is in front. */
	void setStatus(Component tab, String text);
}

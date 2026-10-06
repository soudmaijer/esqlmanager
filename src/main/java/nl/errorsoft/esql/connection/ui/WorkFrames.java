package nl.errorsoft.esql.connection.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ConnectionNode;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.ui.ConnectionBranch;
import nl.errorsoft.esql.query.ui.QueryTab;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.table.ui.TableListTab;
import nl.errorsoft.esql.ui.component.EditorTab;

/**
 * The screen of one connection: its branch in the explorer and the work windows it opened on the desktop of the main window, each a {@link WorkFrame} with
 * a tab of its own.
 */
public class WorkFrames implements ConnectionView {
	private final ConnectionWindowController connection;
	private final MainWindow mainWindow;
	private final ExplorerPanel explorer;
	private final ConnectionNode node;
	private final ConnectionBranch branch;
	private final List<WorkFrame> frames = new ArrayList<>();
	private WorkFrame lastView; // The table data or table list shown last, for setStatus(String)
	private int queryTabs; // Query windows opened so far, for their numbers

	public WorkFrames(ConnectionWindowController connection, MainWindow mainWindow, ConnectionNode node) {
		this.connection = connection;
		this.mainWindow = mainWindow;
		this.explorer = mainWindow.getExplorer();
		this.node = node;
		this.branch = explorer.addConnection(node, connection);
	}

	@Override
	public Component dialogParent() {
		return mainWindow;
	}

	@Override
	public ConnectionBranch branch() {
		return branch;
	}

	@Override
	public Object selectedObject() {
		return explorer.selectedObject(node);
	}

	@Override
	public void showTableDataTab(String key, String title, TableDataTab tableDataTab) {
		WorkFrame frame = show(key, title, tableDataTab);
		frame.setNavigation(tableDataTab.getNavigationBar());
		lastView = frame;
		SwingUtilities.invokeLater(tableDataTab::requestFocusInWindow);
	}

	@Override
	public void showTableListTab(String key, String title, TableListTab tableListTab) {
		lastView = show(key, title, tableListTab);
		SwingUtilities.invokeLater(tableListTab::requestFocusInWindow);
	}

	@Override
	public void removeViews(String keyPrefix) {
		String prefix = keyPrefix.toLowerCase(Locale.ROOT);
		for (WorkFrame frame : List.copyOf(frames)) {
			if (frame.getKey().toLowerCase(Locale.ROOT).startsWith(prefix)) {
				remove(frame);
			}
		}
	}

	/** Opens a query in a new window ("Query", "Query 2", ...), puts it in front and gives its editor the focus. */
	@Override
	public void showQueryTab(QueryTab query) {
		queryTabs++;
		show("query:" + queryTabs, queryTabs == 1 ? "Query" : "Query " + queryTabs, query);
		SwingUtilities.invokeLater(() -> query.getEditor().requestFocusInWindow());
	}

	@Override
	public boolean selectEditorTab(String key) {
		WorkFrame frame = frameWithKey(key);
		if (frame != null) {
			mainWindow.selectWorkFrame(frame);
		}
		return frame != null;
	}

	@Override
	public void showEditorTab(String key, String title, JComponent editor) {
		show(key, title, editor);
	}

	@Override
	public void closeTab(Component tab) {
		WorkFrame frame = frameOf(tab);
		if (frame != null && confirmClose(frame)) {
			remove(frame);
		}
	}

	@Override
	public void removeTab(Component tab) {
		WorkFrame frame = frameOf(tab);
		if (frame != null) {
			remove(frame);
		}
	}

	@Override
	public void setStatus(String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(text));
			return;
		}
		if (lastView != null) {
			lastView.setMessage(text);
		}
	}

	@Override
	public void setStatus(Component tab, String text) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setStatus(tab, text));
			return;
		}
		WorkFrame frame = frameOf(tab);
		if (frame != null) {
			frame.setMessage(text);
		}
	}

	/** How many windows this connection has open. */
	public int frameCount() {
		return frames.size();
	}

	/** Asks every editor with unsaved changes whether to discard them, each brought to the front first; false when the user keeps one. */
	public boolean confirmCloseEditors() {
		for (WorkFrame frame : List.copyOf(frames)) {
			if (frame.getView() instanceof EditorTab) {
				mainWindow.selectWorkFrame(frame);
				if (!confirmClose(frame)) {
					return false;
				}
			}
		}
		return true;
	}

	/** Closes every window of the connection without asking, and takes the connection out of the explorer. */
	public void closeAll() {
		List.copyOf(frames).forEach(this::remove);
		explorer.removeConnection(node);
	}

	/** Shows a view in the window opened under its key, or in a new window, and puts it in front. */
	private WorkFrame show(String key, String title, Component view) {
		WorkFrame frame = frameWithKey(key);
		if (frame == null) {
			frame = new WorkFrame(connection, key, title, view, f -> closeTab(f.getView()));
			frames.add(frame);
			mainWindow.addWorkFrame(frame);
		} else {
			frame.setView(view);
			frame.setTitle(title);
			mainWindow.selectWorkFrame(frame);
		}
		return frame;
	}

	private static boolean confirmClose(WorkFrame frame) {
		return !(frame.getView() instanceof EditorTab editor) || editor.confirmClose();
	}

	private void remove(WorkFrame frame) {
		if (frame.getView() instanceof QueryTab query) {
			query.close();
		}
		frames.remove(frame);
		if (lastView == frame) {
			lastView = null;
		}
		mainWindow.removeWorkFrame(frame);
	}

	private WorkFrame frameWithKey(String key) {
		return frames.stream().filter(frame -> frame.getKey().equals(key)).findFirst().orElse(null);
	}

	private WorkFrame frameOf(Component view) {
		return frames.stream().filter(frame -> frame.getView() == view).findFirst().orElse(null);
	}
}

package nl.errorsoft.esql.app.ui;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.OutputRouting;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.editor.FindBar;

/**
 * The output panel across the bottom of the main window: an Application tab with what belongs to no connection (start up, settings, drivers, errors
 * without a connection) and a tab per open connection, titled with its profile name and server icon, with what was logged for it. Each tab is a read-only
 * SQL coloured log without wrapping, kept below {@link #MAX_CHARS}, with a find bar (menu key+F).
 */
public class OutputPanel extends JPanel {
	static final int MAX_CHARS = 200000;
	private static final String APPLICATION = "Application";

	private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	private final Map<String, Log> connections = new LinkedHashMap<>();
	private final Log application = new Log();

	/** One tab: the text area and its find bar. */
	static final class Log extends JPanel {
		final RSyntaxTextArea text = new RSyntaxTextArea();
		final FindBar findBar;

		Log() {
			super(new BorderLayout());
			// No line wrapping: re-wrapping a long log on every width change made resizing slow.
			// The log shows the statements that were run, so it gets the SQL colours of the query editor.
			text.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			text.setLineWrap(false);
			text.setEditable(false);
			text.setHighlightCurrentLine(false);
			EditorTheme.install(text);
			JScrollPane scroll = new JScrollPane(text);
			scroll.setBorder(null);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			add(scroll, BorderLayout.CENTER);
			findBar = FindBar.install(text, this);
		}

		void append(String line) {
			try {
				text.append(line);
				trim();
				if (!findBar.isVisible()) {
					text.setCaretPosition(text.getDocument().getLength());
				}
			} catch (BadLocationException e) {
				// Cannot happen, trim only reads and removes inside the document.
			}
		}

		/** Keeps the log from growing without limit: past the cap the oldest half goes, at a line end. */
		private void trim() throws BadLocationException {
			Document doc = text.getDocument();
			int length = doc.getLength();

			if (length > MAX_CHARS) {
				int cut = length - MAX_CHARS / 2;
				String head = doc.getText(cut, Math.min(200, length - cut));
				int lineEnd = head.indexOf('\n');
				doc.remove(0, lineEnd < 0 ? cut : cut + lineEnd + 1);
			}
		}
	}

	public OutputPanel() {
		super(new BorderLayout());
		tabs.addTab(APPLICATION, application);
		tabs.putClientProperty("JTabbedPane.tabHeight", 26);
		add(tabs, BorderLayout.CENTER);
	}

	/** Adds the tab of a connection that was opened, after the other tabs. */
	public void addConnection(String name, String serverIcon) {
		if (connections.containsKey(name)) {
			return;
		}
		Log log = new Log();
		connections.put(name, log);
		tabs.addTab(name, ApplicationContext.get().imageLoader().getIcon(serverIcon), log, "Log of " + name);
	}

	/** Removes the tab of a connection that was closed; what it logged afterwards goes to the Application tab with its name in front. */
	public void removeConnection(String name) {
		Log log = connections.remove(name);
		if (log != null) {
			tabs.remove(log);
		}
	}

	/** Brings the tab of a connection to the front without taking the focus. */
	public void showConnection(String name) {
		Log log = connections.get(name);
		if (log != null && tabs.getSelectedComponent() != log) {
			tabs.setSelectedComponent(log);
		}
	}

	/** Adds a log line to the tab of its connection (null: the Application tab), from any thread. */
	public void print(String connection, String line) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> print(connection, line));
			return;
		}
		OutputRouting.Routed routed = OutputRouting.route(connection, line, connections.keySet());
		(routed.tab() == null ? application : connections.get(routed.tab())).append(routed.text());
	}

	/** Opens the find bar of the tab in front. */
	public void find() {
		if (tabs.getSelectedComponent() instanceof Log log) {
			log.findBar.open();
		}
	}
}

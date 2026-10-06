package nl.errorsoft.esql.query.ui;

import nl.errorsoft.esql.ui.util.ToolbarButtons;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.query.QueryPlan;
import nl.errorsoft.esql.query.SqlScript;
import nl.errorsoft.esql.query.control.QueryController;
import nl.errorsoft.esql.table.ui.TableDataTab;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.util.ExtensionFileFilter;
import nl.errorsoft.esql.ui.util.FileChoosers;

/**
 * A query window of a connection: the SQL editor with completion on top and below it a tab for every statement that returned rows, the newest in
 * front. A result tab is named after its statement (the full text is its tooltip) and shows when it ran, on which database, the rows and the time taken.
 */
public class QueryTab extends JPanel {
	private static final int MENU = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	private static final String MENU_KEY = MENU == InputEvent.META_DOWN_MASK ? "Cmd" : "Ctrl";

	private final QueryController controller;
	private final RSyntaxTextArea editor;
	private final JComboBox<Database> databases;
	/** The schema picker, only on servers with schemas. */
	private final JComboBox<String> schemas = new JComboBox<>();
	/** Set while the picker is filled, so that filling it does not switch the schema. */
	private boolean fillingSchemas;
	private final JSplitPane split;
	private final AutoCompletion completion;
	private static final int MAX_RESULTS = 20;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final JLabel noResult = new JLabel("Run a statement to see its result here", JLabel.CENTER);
	private final JTabbedPane results = new JTabbedPane();
	private final JButton runSelectionButton;
	private final JButton runAllButton;
	/** Explain and Explain analyze, hidden on servers without {@code Feature.EXPLAIN}. */
	private final JButton explainButton;
	private final JButton analyzeButton;
	/** Set while statements run on their thread; another run waits until they are done. */
	private boolean running;
	/** Set while the database or schema is changed on its thread. */
	private boolean switching;

	public QueryTab(QueryController controller, List<Database> databaseList, Database selected) {
		super(new BorderLayout());
		this.controller = controller;
		ImageLoader images = ApplicationContext.get().imageLoader();

		editor = new RSyntaxTextArea();
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		editor.setHighlightCurrentLine(false);
		editor.setCodeFoldingEnabled(false);

		// RSyntaxTextArea brings undo and redo with the platform keys.
		RTextScrollPane editorScroll = new RTextScrollPane(editor);
		editorScroll.setLineNumbersEnabled(true);
		editorScroll.setBorder(BorderFactory.createEmptyBorder());
		// After the scroll pane exists, so that the theme also colours the line numbers. The theme listener goes when the tab is closed.
		EditorTheme.install(editor);

		completion = new AutoCompletion(new SqlCompletionProvider(controller));
		completion.setAutoActivationEnabled(true);
		completion.setAutoActivationDelay(300);
		completion.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK));
		completion.install(editor);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(button(images, "imgOpen", "Open query file", e -> open()));
		toolbar.add(button(images, "imgSave", "Save query", e -> save()));
		toolbar.addSeparator();
		runSelectionButton = button(images, "imgRunSelection", "Run selection, or the statement at the caret (" + MENU_KEY + "+Enter)", e -> runSelection());
		runAllButton = button(images, "imgRunAll", "Run all statements (" + MENU_KEY + "+Shift+Enter)", e -> runAll());
		explainButton = button(images, "imgExplain", "Explain the plan of the selection, or the statement at the caret (" + MENU_KEY + "+E)",
			e -> explain(false));
		analyzeButton = button(images, "imgExplainAnalyze", "Explain analyze: run the statement and show its plan with the measured times",
			e -> explain(true));
		toolbar.add(runSelectionButton);
		toolbar.add(runAllButton);
		if (controller.supportsExplain()) {
			toolbar.add(explainButton);
			toolbar.add(analyzeButton);
			bind(KeyStroke.getKeyStroke(KeyEvent.VK_E, MENU), "explain", () -> explain(false));
		}
		toolbar.addSeparator();
		toolbar.add(new JLabel(" " + capitalized(controller.databaseTerm()) + ": "));
		databases = new JComboBox<>(databaseList.toArray(new Database[0]));
		databases.setMaximumSize(new Dimension(200, databases.getPreferredSize().height));

		// Without a selection in the tree the tab starts on the database the connection uses.
		databases.setSelectedItem(controller.startDatabase(databaseList, selected));
		databases.addActionListener(e -> useSelectedDatabase());
		toolbar.add(databases);

		if (controller.hasSchemas()) {
			String term = controller.schemaTerm();
			toolbar.add(new JLabel(" " + capitalized(term) + ": "));
			schemas.setMaximumSize(new Dimension(200, schemas.getPreferredSize().height));
			schemas.setPrototypeDisplayValue("information_schema");
			schemas.setToolTipText("Unqualified table names resolve to this " + term);
			schemas.addActionListener(e -> useSelectedSchema());
			controller.setSchemaListener(this::showSchemas);
			toolbar.add(schemas);
		}

		bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MENU), "runSelection", this::runSelection);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, MENU | InputEvent.SHIFT_DOWN_MASK), "runAll", this::runAll);
		// Cmd+Space as in IntelliJ (macOS gives it to Spotlight unless that shortcut is turned off), and Ctrl+Shift+Space / Cmd+Shift+Space.
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.META_DOWN_MASK), "complete", completion::doCompletion);
		bind(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, MENU | InputEvent.SHIFT_DOWN_MASK), "complete", completion::doCompletion);

		noResult.setEnabled(false);
		results.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		results.putClientProperty("JTabbedPane.tabClosable", true);
		results.putClientProperty("JTabbedPane.tabCloseCallback", (java.util.function.IntConsumer) this::closeResult);
		// The find bar (menu key+F) sits below the editor, above the results.
		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.add(editorScroll, BorderLayout.CENTER);
		nl.errorsoft.esql.ui.editor.FindBar.install(editor, editorPanel);
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, noResult);
		split.setResizeWeight(0.5);
		split.setContinuousLayout(true);
		split.setBorder(BorderFactory.createEmptyBorder());

		add(toolbar, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		useSelectedDatabase();
	}

	public RSyntaxTextArea getEditor() {
		return editor;
	}

	/** Called when the tab is closed: removes what the completion installed on the editor and the window. */
	public void close() {
		completion.uninstall();
	}

	private JButton button(ImageLoader images, String icon, String tooltip, java.awt.event.ActionListener action) {
		JButton button = new JButton(images.getIcon(icon));
		button.setToolTipText(tooltip);
		button.addActionListener(action);
		return button;
	}

	private void bind(KeyStroke key, String name, Runnable action) {
		editor.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
		editor.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	/** Switches the connection off the event thread; the pickers and run buttons wait until it is done. */
	private void useSelectedDatabase() {
		if (databases.getSelectedItem() instanceof Database database) {
			setSwitching(true);
			controller.use(database, () -> setSwitching(false));
		}
	}

	/** Fills the picker with the schemas of the chosen database and selects the one names resolve to now. */
	private void showSchemas(List<String> names, String current) {
		fillingSchemas = true;

		try {
			schemas.removeAllItems();
			names.forEach(schemas::addItem);
			if (current != null) {
				schemas.setSelectedItem(current);
			}
		} finally {
			fillingSchemas = false;
		}
	}

	private void useSelectedSchema() {
		if (!fillingSchemas && schemas.getSelectedItem() instanceof String schema) {
			setSwitching(true);
			controller.useSchema(schema, () -> setSwitching(false));
		}
	}

	/** The selected text, or the statement at the caret when nothing is selected. */
	private void runSelection() {
		String selection = editor.getSelectedText();

		if (selection != null && !selection.isBlank()) {
			run(SqlScript.split(selection));
		} else {
			SqlScript.statementAt(editor.getText(), editor.getCaretPosition()).ifPresent(statement -> run(List.of(statement)));
		}
	}

	/**
	 * Shows the plan of the selection or the statement at the caret in a result tab. Analyze runs the statement, so anything but a query is confirmed
	 * first.
	 */
	private void explain(boolean analyze) {
		if (running || switching) {
			return;
		}
		String selection = editor.getSelectedText();
		List<SqlScript.Statement> statements = selection != null && !selection.isBlank()
			? SqlScript.split(selection)
			: SqlScript.statementAt(editor.getText(), editor.getCaretPosition()).map(List::of).orElse(List.of());

		if (statements.isEmpty()) {
			return;
		}
		if (statements.size() > 1) {
			Dialogs.warn(this, "Explain", "Explain shows the plan of one statement. Select one statement, or put the caret in it.");
			return;
		}
		String sql = statements.getFirst().sql();
		if (analyze && !QueryPlan.isReadOnly(sql)
			&& !Dialogs.confirmDestructive(this, "Explain analyze", "Analyze runs this statement and its changes stay. Run it?", "Run and analyze")) {
			return;
		}
		setRunning(true);
		controller.explain(sql, analyze, this, result -> {
			setRunning(false);
			if (result != null) {
				showPlan(result);
			}
			editor.requestFocusInWindow();
		});
	}

	private void runAll() {
		run(SqlScript.split(editor.getText()));
	}

	/** Runs the statements off the event thread; the run buttons are disabled until they are done. */
	private void run(List<SqlScript.Statement> statements) {
		if (statements.isEmpty() || running || switching) {
			return;
		}

		setRunning(true);
		controller.run(statements.stream().map(SqlScript.Statement::sql).toList(), this, result -> {
			setRunning(false);
			for (QueryController.StatementResult statement : result.results()) {
				showResult(statement);
			}
			editor.requestFocusInWindow();
		});
	}

	private void setRunning(boolean running) {
		this.running = running;
		updateControls();
	}

	private void setSwitching(boolean switching) {
		this.switching = switching;
		updateControls();
	}

	/** Nothing runs or switches while statements run or the database or schema is being changed. */
	private void updateControls() {
		boolean idle = !running && !switching;
		String running = idle ? null : "Wait for the running statements.";
		ToolbarButtons.setAvailable(runSelectionButton, running);
		ToolbarButtons.setAvailable(runAllButton, running);
		ToolbarButtons.setAvailable(explainButton, running);
		ToolbarButtons.setAvailable(analyzeButton, running);
		databases.setEnabled(idle);
		schemas.setEnabled(idle);
	}

	/** Whether statements are running, for a check from a test or harness. */
	boolean isRunning() {
		return running;
	}

	private void showResult(QueryController.StatementResult statement) {
		JLabel details = new JLabel("Ran at " + TIME.format(statement.ranAt()) + (statement.database().isEmpty() ? "" : " on " + statement.database()) + ", "
			+ statement.rowCount() + " row(s) in " + statement.millis() + " ms");
		details.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
		TableDataTab view = new TableDataTab(controller.tableController());
		view.loadData(statement.result().table(), statement.result().table().getColumns(), statement.result().rows());
		JPanel tab = new JPanel(new BorderLayout());
		tab.add(view, BorderLayout.CENTER);
		tab.add(details, BorderLayout.SOUTH);

		addResult(tabTitle(statement.sql()), tab, statement.sql());
	}

	/** The plan in a result tab: the plan viewer, and the plan as text. */
	private void showPlan(QueryController.ExplainResult explained) {
		addResult((explained.analyzed() ? "Analyze: " : "Explain: ") + tabTitle(explained.sql()), new PlanView(explained), explained.sql());
	}

	/** Puts a result in front, dropping the oldest past {@link #MAX_RESULTS}, and shows the results below the editor. */
	private void addResult(String title, JComponent tab, String sql) {
		if (results.getTabCount() >= MAX_RESULTS) {
			results.removeTabAt(0);
		}
		results.addTab(title, null, tab, tooltip(sql));
		results.setSelectedComponent(tab);

		if (split.getBottomComponent() != results) {
			int divider = split.getDividerLocation();
			split.setBottomComponent(results);
			split.setDividerLocation(divider);
		}
	}

	private void closeResult(int index) {
		results.removeTabAt(index);

		if (results.getTabCount() == 0) {
			int divider = split.getDividerLocation();
			split.setBottomComponent(noResult);
			split.setDividerLocation(divider);
		}
	}

	/** The statement on one line, shortened to fit on a tab. */
	static String tabTitle(String sql) {
		String line = sql.replaceAll("\\s+", " ").strip();
		return line.length() > 32 ? line.substring(0, 32) + "..." : line;
	}

	private static String tooltip(String sql) {
		String escaped = sql.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<html><pre>" + escaped + "</pre></html>";
	}

	private void open() {
		JFileChooser chooser = chooser("Open query");

		try {
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				editor.setText(Files.readString(chooser.getSelectedFile().toPath()));
				editor.setCaretPosition(0);
				editor.discardAllEdits();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Open query", e);
		}
	}

	private void save() {
		JFileChooser chooser = chooser("Save query as...");

		try {
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = FileChoosers.withExtension(chooser.getSelectedFile(), ".sql");

				if (file.exists() && !Dialogs.confirmDestructive(this, "Save query", "Overwrite the existing file '" + file + "'?", "Overwrite")) {
					return;
				}
				Files.writeString(file.toPath(), editor.getText() + System.lineSeparator());
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Save query", e);
		}
	}

	private JFileChooser chooser(String title) {
		JFileChooser chooser = FileChoosers.create();
		chooser.addChoosableFileFilter(new ExtensionFileFilter("SQL file", new String[]{".sql"}));
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setDialogTitle(title);
		return chooser;
	}

	private static String capitalized(String term) {
		return Character.toUpperCase(term.charAt(0)) + term.substring(1);
	}
}

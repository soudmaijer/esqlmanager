package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.ColumnNames;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableDefinition;
import nl.errorsoft.esql.table.control.CreateTableController;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.component.EditorTab;
import nl.errorsoft.esql.ui.util.Forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 * Creates a new table or edits the name, type and comment of an existing one, as a tab of the connection window. The properties of the table are at the
 * top, the columns in a grid that can be edited in place, and the details of the selected column in a group below it. Save (also Ctrl/Cmd+S) and Cancel
 * are at the bottom; the tab asks before it discards changes.
 */
public class TableEditorTab extends JPanel implements EditorTab {
	private static final String[] COLUMN_TITLES = {"Name", "Type", "Length", "Not null", "Primary key", "Auto increment", "Default"};
	private static final int NAME = 0;
	private static final int TYPE = 1;
	private static final int LENGTH = 2;
	private static final int NOT_NULL = 3;
	private static final int PRIMARY = 4;
	private static final int AUTO_INCREMENT = 5;
	private static final int DEFAULT = 6;

	private final CreateTableController createTableController;
	private final String title;
	private final Table table;

	// Table properties
	private final JTextField tablename = new JTextField(24);
	private final JTextField comment = new JTextField(24);
	private final JComboBox<Database> dbs;
	private final JComboBox<String> tabletypes;

	// The columns: new ones (CreateColumn) or, when editing, the existing ones (TableColumn) which are only shown
	private final List<Object> columns = new ArrayList<>();
	private final ColumnTableModel columnModel = new ColumnTableModel();
	private final JTable columnTable = new JTable(columnModel);
	private final JButton addcolumn = Forms.button("&Add");
	private final JButton remcolumn = Forms.button("&Remove");
	private final JButton moveup = Forms.button("&Up");
	private final JButton movedown = Forms.button("Do&wn");

	// The details of the selected column
	private final JPanel columnGroup = Forms.titled(new JPanel(new BorderLayout()), "Column");
	private final JTextField columnName = new JTextField(16);
	private final JComboBox<DataType> columnType = new JComboBox<>();
	private final JTextField length = new JTextField(16);
	private final JTextField defaultval = new JTextField(16);
	private final JTextField columnComment = new JTextField(16);
	private final JCheckBox primary = Forms.mnemonic(new JCheckBox(), "&Primary key");
	private final JCheckBox notnull = Forms.mnemonic(new JCheckBox(), "Not &null");
	private final JCheckBox autoincrement = Forms.mnemonic(new JCheckBox(), "Auto &increment");
	private final JCheckBox unsigned = Forms.mnemonic(new JCheckBox(), "Unsi&gned");
	private final JComponent[] columnFields;
	private final JLabel problem = new JLabel(" ");

	// The SQL that Save would run, shown on request
	private final JCheckBox showSql = Forms.mnemonic(new JCheckBox(), "Show S&QL");
	private final RSyntaxTextArea sqlPreview = new RSyntaxTextArea(4, 60);
	private final JScrollPane sqlScroll = new JScrollPane(sqlPreview);

	private final JButton save = Forms.button("&Save");
	private final JButton cancel = Forms.button("&Cancel");

	/** The new column shown in the details, null when none is selected or the columns are the existing ones. */
	private CreateColumn selField = null;
	/** True while the details are filled from a column, so that filling them does not write back. */
	private boolean loading;
	/** After a Save with a problem the message follows the input until the problem is gone. */
	private boolean showProblems;

	// What the table properties were when the tab opened, and whether the columns changed since.
	private final String initialName;
	private final String initialComment;
	private final String initialType;
	private boolean columnsChanged;

	public TableEditorTab(CreateTableController createTableController, String title, Database database, Table table) {
		super(new BorderLayout(0, Forms.PADDING));
		this.createTableController = createTableController;
		this.title = title;
		this.table = table;
		setBorder(BorderFactory.createEmptyBorder(Forms.PADDING, Forms.PADDING, Forms.PADDING, Forms.PADDING));

		DefaultComboBoxModel<Database> dcm = new DefaultComboBoxModel<>();
		List<Database> db = createTableController.getDatabases();
		for (Database candidate : db) {
			dcm.addElement(candidate);
			if (database != null && candidate.toString().equals(database.toString())) {
				database = candidate;
			}
		}
		dcm.setSelectedItem(database);
		dbs = new JComboBox<>(dcm);

		DefaultComboBoxModel<String> ttmodel = new DefaultComboBoxModel<>();
		String[] tbt = createTableController.getTableTypes();
		for (String type : tbt) {
			ttmodel.addElement(type);
			if (table != null && type.equalsIgnoreCase(table.getType())) {
				ttmodel.setSelectedItem(type);
			}
		}
		tabletypes = new JComboBox<>(ttmodel);

		columnFields = new JComponent[]{columnName, columnType, length, defaultval, columnComment, primary, notnull, autoincrement, unsigned};

		JPanel form = new JPanel(new BorderLayout(0, Forms.PADDING));
		form.add(tableProperties(tbt.length > 0, table), BorderLayout.NORTH);
		form.add(columnsPanel(), BorderLayout.CENTER);
		add(Forms.verticalScroll(form), BorderLayout.CENTER);
		add(bottom(), BorderLayout.SOUTH);

		if (table != null) {
			tablename.setText(table.getName());
			comment.setText(table.getComment());
			columns.addAll(java.util.Arrays.asList(table.getColumns()));
			dbs.setEnabled(false);
			for (JButton button : List.of(addcolumn, remcolumn, moveup, movedown)) {
				button.setEnabled(false);
			}
		}
		showColumn();

		initialName = tablename.getText();
		initialComment = comment.getText();
		initialType = selectedTableType();

		tablename.getDocument().addDocumentListener(new Changed(this::edited));
		comment.getDocument().addDocumentListener(new Changed(this::edited));
		tabletypes.addActionListener(e -> refreshPreview());
		dbs.addActionListener(e -> refreshPreview());
		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
			"saveTable");
		getActionMap().put("saveTable", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				save();
			}
		});
	}

	/** Database (and schema), name, type and comment in one column. */
	private JPanel tableProperties(boolean hasTableTypes, Table existing) {
		Forms.Grid grid = new Forms.Grid();
		grid.row("&Database:", dbs);
		if (createTableController.supportsSchemas()) {
			JTextField schema = new JTextField(24);
			schema.setEditable(false);
			schema.setFocusable(false);
			if (existing != null && existing.getSchema() != null) {
				schema.setText(existing.getSchema().getName());
			} else {
				String name = createTableController.targetSchemaName();
				schema.setText(name == null ? "(current schema)" : name);
			}
			grid.row("S&chema:", schema);
		}
		grid.row("&Name:", tablename);
		// Servers without storage engines have nothing to choose here.
		if (hasTableTypes) {
			grid.row("&Type:", tabletypes);
		}
		grid.row("Co&mment:", comment);
		return Forms.titled(grid.panel(), "Table");
	}

	private JPanel columnsPanel() {
		for (DataType type : createTableController.getDatatypes()) {
			columnType.addItem(type);
		}
		JComboBox<DataType> typeEditor = new JComboBox<>();
		for (DataType type : createTableController.getDatatypes()) {
			typeEditor.addItem(type);
		}
		columnTable.setDefaultEditor(DataType.class, new DefaultCellEditor(typeEditor));
		columnTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		columnTable.setFillsViewportHeight(true);
		columnTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		columnTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				showColumn();
			}
		});
		JScrollPane scroll = new JScrollPane(columnTable);
		scroll.setPreferredSize(new Dimension(640, 180));

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, Forms.GAP));
		buttons.add(addcolumn);
		buttons.add(remcolumn);
		buttons.add(moveup);
		buttons.add(movedown);
		JPanel buttonColumn = new JPanel(new BorderLayout());
		buttonColumn.add(buttons, BorderLayout.NORTH);

		JPanel list = Forms.titled(new JPanel(new BorderLayout(Forms.GAP, 0)), "Columns");
		list.add(scroll, BorderLayout.CENTER);
		list.add(buttonColumn, BorderLayout.EAST);

		JPanel checks = new JPanel(new GridLayout(2, 2, Forms.GAP, 0));
		checks.add(primary);
		checks.add(notnull);
		checks.add(autoincrement);
		checks.add(unsigned);
		Forms.Grid grid = new Forms.Grid();
		grid.row("Col&umn name:", columnName).row("Typ&e:", columnType).row("&Length:", length).row("De&fault:", defaultval);
		if (createTableController.supportsColumnComments()) {
			grid.row("C&omment:", columnComment);
		}
		grid.full(checks);
		columnGroup.add(grid.panel(), BorderLayout.CENTER);

		addcolumn.addActionListener(e -> addColumn());
		remcolumn.addActionListener(e -> removeColumn());
		moveup.addActionListener(e -> moveColumn(-1));
		movedown.addActionListener(e -> moveColumn(1));

		columnName.getDocument().addDocumentListener(new Changed(() -> write(() -> {
			String name = columnName.getText().trim();
			boolean taken = ColumnNames.isUsed(name, otherNames());
			columnName.putClientProperty("JComponent.outline", name.isEmpty() || taken ? "error" : null);
			if (!name.isEmpty() && !taken) {
				selField.name = name;
			}
		})));
		columnType.addActionListener(e -> write(() -> selField.applyType((DataType) columnType.getSelectedItem())));
		length.getDocument().addDocumentListener(new Changed(() -> write(() -> selField.length = length.getText())));
		defaultval.getDocument().addDocumentListener(new Changed(() -> write(() -> selField.defaultval = defaultval.getText())));
		columnComment.getDocument().addDocumentListener(new Changed(() -> write(() -> selField.comment = columnComment.getText())));
		primary.addActionListener(e -> write(() -> selField.primary = primary.isSelected()));
		notnull.addActionListener(e -> write(() -> selField.notnull = notnull.isSelected()));
		autoincrement.addActionListener(e -> write(() -> selField.autoincrement = autoincrement.isSelected()));
		unsigned.addActionListener(e -> write(() -> selField.unsigned = unsigned.isSelected()));

		JPanel panel = new JPanel(new BorderLayout(0, Forms.PADDING));
		panel.add(list, BorderLayout.CENTER);
		panel.add(columnGroup, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel bottom() {
		problem.setForeground(Forms.errorColor());
		save.addActionListener(e -> save());
		cancel.addActionListener(e -> createTableController.cancel(this));

		sqlPreview.setEditable(false);
		sqlPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		EditorTheme.install(sqlPreview);
		sqlScroll.setVisible(false);
		showSql.addActionListener(e -> {
			sqlScroll.setVisible(showSql.isSelected());
			refreshPreview();
			revalidate();
		});

		JPanel messages = new JPanel(new BorderLayout(0, Forms.GAP));
		messages.add(sqlScroll, BorderLayout.NORTH);
		messages.add(problem, BorderLayout.CENTER);
		JPanel south = new JPanel(new BorderLayout());
		south.add(messages, BorderLayout.NORTH);
		south.add(Forms.buttonRowWithLeading(showSql, save, cancel), BorderLayout.CENTER);
		return south;
	}

	/** A document listener that runs one action on every change. */
	private record Changed(Runnable action) implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			action.run();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			action.run();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			action.run();
		}
	}

	private String selectedTableType() {
		Object type = tabletypes.getSelectedItem();
		return type == null ? null : type.toString();
	}

	/** True when the table properties or the columns differ from when the tab opened. */
	public boolean isModified() {
		return columnsChanged || !tablename.getText().equals(initialName) || !comment.getText().equals(initialComment)
			|| !Objects.equals(selectedTableType(), initialType);
	}

	@Override
	public boolean confirmClose() {
		return !isModified() || Dialogs.confirmDestructive(this, "Discard changes?", "Discard the changes in " + title + "?", "Discard");
	}

	/** The problem that stops Save, null when the input is fine. */
	private String inputProblem() {
		if (tablename.getText().trim().isEmpty()) {
			return "Enter a table name.";
		}
		if (table == null && dbs.getSelectedItem() == null) {
			return "Choose a database.";
		}
		if (table == null && columns.isEmpty()) {
			return "Add at least one column.";
		}
		return null;
	}

	/** The name or comment of the table was typed in. */
	private void edited() {
		refreshProblem();
		refreshPreview();
	}

	/** Shows the statements Save would run, while the SQL view is open. */
	private void refreshPreview() {
		if (!showSql.isSelected() || !(sqlScroll.isVisible())) {
			return;
		}
		List<CreateColumn> created = new ArrayList<>();
		for (Object column : columns) {
			if (column instanceof CreateColumn c) {
				created.add(c);
			}
		}
		String name = tablename.getText().trim();
		if (table == null && created.isEmpty()) {
			sqlPreview.setText("-- Add a column to see the statements.");
			return;
		}
		List<String> statements = createTableController.previewStatements(table,
			definition(name.isEmpty() ? "table_name" : name, created));
		sqlPreview.setText(statements.isEmpty()
			? "-- No changes."
			: statements.stream().map(statement -> statement.startsWith("--") ? statement : statement + ";")
				.collect(java.util.stream.Collectors.joining("\n")));
		sqlPreview.setCaretPosition(0);
	}

	private void refreshProblem() {
		if (showProblems) {
			String found = inputProblem();
			problem.setText(found == null ? " " : found);
			if (found == null) {
				showProblems = false;
			}
		}
	}

	private void save() {
		if (columnTable.isEditing()) {
			columnTable.getCellEditor().stopCellEditing();
		}
		String found = inputProblem();
		showProblems = found != null;
		problem.setText(found == null ? " " : found);
		if (found != null) {
			(tablename.getText().trim().isEmpty() ? tablename : addcolumn).requestFocusInWindow();
			return;
		}
		if (table == null) {
			List<CreateColumn> cols = new ArrayList<>();
			for (Object column : columns) {
				cols.add((CreateColumn) column);
			}
			createTableController.createTable(definition(tablename.getText(), cols), this);
		} else {
			createTableController.modifyTable(this, table, tablename.getText(), selectedTableType(), comment.getText());
		}
	}

	/** The table as the editor holds it, in the chosen database. */
	private TableDefinition definition(String name, List<CreateColumn> columns) {
		Database database = dbs.getSelectedItem() instanceof Database chosen ? chosen : new Database("");
		return new TableDefinition(database, null, name, selectedTableType(), comment.getText(), columns);
	}

	private void addColumn() {
		if (columnTable.isEditing()) {
			columnTable.getCellEditor().stopCellEditing();
		}
		CreateColumn column = new CreateColumn(ColumnNames.next(allNames()));
		column.type = columnType.getItemAt(0);
		columns.add(column);
		columnModel.fireTableRowsInserted(columns.size() - 1, columns.size() - 1);
		columnTable.setRowSelectionInterval(columns.size() - 1, columns.size() - 1);
		columnTable.scrollRectToVisible(columnTable.getCellRect(columns.size() - 1, 0, true));
		columnsChanged = true;
		refreshProblem();
		refreshPreview();
		columnName.requestFocusInWindow();
		columnName.selectAll();
	}

	private void removeColumn() {
		int row = columnTable.getSelectedRow();
		if (row < 0) {
			return;
		}
		if (columnTable.isEditing()) {
			columnTable.getCellEditor().cancelCellEditing();
		}
		columns.remove(row);
		columnModel.fireTableRowsDeleted(row, row);
		if (!columns.isEmpty()) {
			columnTable.setRowSelectionInterval(Math.min(row, columns.size() - 1), Math.min(row, columns.size() - 1));
		}
		columnsChanged = true;
		showColumn();
		refreshProblem();
		refreshPreview();
	}

	private void moveColumn(int step) {
		int row = columnTable.getSelectedRow();
		int to = row + step;
		if (row < 0 || to < 0 || to >= columns.size()) {
			return;
		}
		columns.add(to, columns.remove(row));
		columnModel.fireTableDataChanged();
		columnTable.setRowSelectionInterval(to, to);
		columnsChanged = true;
		refreshPreview();
	}

	private List<String> allNames() {
		List<String> names = new ArrayList<>();
		for (Object column : columns) {
			names.add(column instanceof TableColumn tc ? tc.getName() : ((CreateColumn) column).name);
		}
		return names;
	}

	/** The names of the columns other than the selected one. */
	private List<String> otherNames() {
		List<String> names = allNames();
		int row = columnTable.getSelectedRow();
		if (row >= 0 && row < names.size()) {
			names.remove(row);
		}
		return names;
	}

	/** Runs a change to the selected new column made in the details, then marks it and updates the grid. */
	private void write(Runnable change) {
		if (loading || selField == null) {
			return;
		}
		change.run();
		columnsChanged = true;
		int row = columnTable.getSelectedRow();
		if (row >= 0) {
			columnModel.fireTableRowsUpdated(row, row);
		}
		enableOptionsFor(selField.type);
		updateGroupTitle();
		refreshPreview();
	}

	/** Fills the details from the selected column. A new column can be edited, an existing one is only shown. */
	private void showColumn() {
		int row = columnTable.getSelectedRow();
		Object selected = row >= 0 && row < columns.size() ? columns.get(row) : null;
		selField = selected instanceof CreateColumn column ? column : null;
		loading = true;
		if (selField != null) {
			columnName.setText(selField.name);
			columnType.setSelectedItem(selField.type);
			length.setText(selField.length);
			defaultval.setText(selField.defaultval);
			columnComment.setText(selField.comment);
			primary.setSelected(selField.primary);
			notnull.setSelected(selField.notnull);
			autoincrement.setSelected(selField.autoincrement);
			unsigned.setSelected(selField.unsigned);
			columnName.putClientProperty("JComponent.outline", null);
		} else if (selected instanceof TableColumn tc) {
			columnName.setText(tc.getName());
			selectTypeNamed(tc.getNativeTypeName());
			length.setText(tc.getLength());
			defaultval.setText(tc.getDefault());
			columnComment.setText(tc.getComment());
			primary.setSelected(tc.isPrimary());
			notnull.setSelected(!tc.isNullable());
			autoincrement.setSelected(tc.isAutoIncrement());
			unsigned.setSelected(!tc.isSigned());
		} else {
			columnName.setText("");
			length.setText("");
			defaultval.setText("");
			columnComment.setText("");
			for (JCheckBox box : List.of(primary, notnull, autoincrement, unsigned)) {
				box.setSelected(false);
			}
		}
		loading = false;
		for (JComponent field : columnFields) {
			field.setEnabled(selField != null);
		}
		if (selField != null) {
			enableOptionsFor(selField.type);
		}
		updateGroupTitle();
	}

	private void selectTypeNamed(String name) {
		for (int i = 0; i < columnType.getItemCount(); i++) {
			if (columnType.getItemAt(i).toString().equalsIgnoreCase(name)) {
				columnType.setSelectedIndex(i);
				return;
			}
		}
	}

	private void enableOptionsFor(DataType type) {
		if (type == null) {
			return;
		}
		primary.setEnabled(type.allows(DataType.Option.PRIMARY));
		notnull.setEnabled(type.allows(DataType.Option.NOT_NULL));
		autoincrement.setEnabled(type.allows(DataType.Option.AUTO_INCREMENT));
		unsigned.setEnabled(type.allows(DataType.Option.UNSIGNED));
		// Fixed in the box when the type does not allow it
		primary.setSelected(primary.isSelected() && type.allows(DataType.Option.PRIMARY));
		notnull.setSelected(notnull.isSelected() && type.allows(DataType.Option.NOT_NULL));
		autoincrement.setSelected(autoincrement.isSelected() && type.allows(DataType.Option.AUTO_INCREMENT));
		unsigned.setSelected(unsigned.isSelected() && type.allows(DataType.Option.UNSIGNED));
	}

	/** The group says which column its fields belong to. */
	private void updateGroupTitle() {
		String name = columnName.getText().trim();
		String text = columnTable.getSelectedRow() < 0 ? "Column (select a column)" : "Column: " + name;
		if (columnGroup.getBorder() instanceof CompoundBorder compound && compound.getOutsideBorder() instanceof TitledBorder titled) {
			titled.setTitle(text);
			columnGroup.repaint();
		}
	}

	/** The columns in a grid. New columns are edited in place, existing ones are only shown. */
	private final class ColumnTableModel extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return columns.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMN_TITLES.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMN_TITLES[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return switch (column) {
				case NOT_NULL, PRIMARY, AUTO_INCREMENT -> Boolean.class;
				case TYPE -> DataType.class;
				default -> String.class;
			};
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (!(columns.get(row) instanceof CreateColumn created)) {
				return false;
			}
			DataType type = created.type;
			return switch (column) {
				case NOT_NULL -> type != null && type.allows(DataType.Option.NOT_NULL);
				case PRIMARY -> type != null && type.allows(DataType.Option.PRIMARY);
				case AUTO_INCREMENT -> type != null && type.allows(DataType.Option.AUTO_INCREMENT);
				default -> true;
			};
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (columns.get(row) instanceof CreateColumn created) {
				return switch (column) {
					case NAME -> created.name;
					case TYPE -> created.type;
					case LENGTH -> created.length;
					case NOT_NULL -> created.notnull;
					case PRIMARY -> created.primary;
					case AUTO_INCREMENT -> created.autoincrement;
					default -> created.defaultval;
				};
			}
			TableColumn existing = (TableColumn) columns.get(row);
			return switch (column) {
				case NAME -> existing.getName();
				case TYPE -> existing.getNativeTypeName();
				case LENGTH -> existing.getLength();
				case NOT_NULL -> !existing.isNullable();
				case PRIMARY -> existing.isPrimary();
				case AUTO_INCREMENT -> existing.isAutoIncrement();
				default -> existing.getDefault();
			};
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (!(columns.get(row) instanceof CreateColumn created)) {
				return;
			}
			switch (column) {
				case NAME -> {
					String name = Objects.toString(value, "").trim();
					List<String> others = allNames();
					others.remove(row);
					if (!name.isEmpty() && !ColumnNames.isUsed(name, others)) {
						created.name = name;
					}
				}
				case TYPE -> created.applyType((DataType) value);
				case LENGTH -> created.length = Objects.toString(value, "");
				case NOT_NULL -> created.notnull = (Boolean) value;
				case PRIMARY -> created.primary = (Boolean) value;
				case AUTO_INCREMENT -> created.autoincrement = (Boolean) value;
				default -> created.defaultval = Objects.toString(value, "");
			}
			columnsChanged = true;
			refreshPreview();
			fireTableRowsUpdated(row, row);
			if (row == columnTable.getSelectedRow()) {
				showColumn();
			}
		}
	}
}

package nl.errorsoft.esql.exporter.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.ui.DatabaseTree;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.exporter.ExportOptions;
import nl.errorsoft.esql.exporter.control.ExportController;
import nl.errorsoft.esql.table.TableName;
import nl.errorsoft.esql.ui.util.Encodings;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.ui.util.FileChoosers;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

/** Exports databases, schemas or tables as SQL statements to a file: the tree to choose from on the left, the options and the file on the right. */
public class ExportSqlDialog extends FormDialog {
	private final ExportController exportController;
	private final JScrollPane treeScroll = new JScrollPane();
	private final JTextField file = new JTextField(24);

	private final JCheckBox structure = Forms.mnemonic(new JCheckBox("", true), "&Structure");
	private final JCheckBox data = Forms.mnemonic(new JCheckBox("", true), "&Data");
	private final JCheckBox createDatabase = Forms.mnemonic(new JCheckBox("", true), "&Create database");
	private final JCheckBox dropTable = Forms.mnemonic(new JCheckBox("", true), "Dr&op table");
	private final JCheckBox useDatabase = Forms.mnemonic(new JCheckBox("", true), "&Use database");
	private final JCheckBox views = Forms.mnemonic(new JCheckBox("", false), "&Views (structure)");
	private final JCheckBox dropIfExists = Forms.mnemonic(new JCheckBox("", true), "DROP ... &IF EXISTS");
	private final JCheckBox createIfNotExists = Forms.mnemonic(new JCheckBox("", false), "CREATE ... IF &NOT EXISTS");
	private final JCheckBox transaction = Forms.mnemonic(new JCheckBox("", false), "Wrap in a &transaction");
	private final JCheckBox foreignKeys = Forms.mnemonic(new JCheckBox("", false), "Disable &foreign key checks");
	private final JSpinner rowsPerInsert = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
	private final JComboBox<Charset> encoding = Encodings.combo(ApplicationContext.get().settings().getDefaultEncoding());

	private final JLabel schemaFilterNote = new JLabel(
		"<html>The profile hides some schemas in this tree. Exporting a whole database still includes them.</html>");
	private DatabaseTree databaseTree;

	public ExportSqlDialog(MainWindow mainWindow, ExportController exportController) {
		super(mainWindow, "Export data", false);
		this.exportController = exportController;
		initComponents();
		setResizable(true);
		showDialog();
		toFront();
	}

	/** Tells that the tree leaves out schemas the profile hides, which an export of a whole database includes anyway. */
	public void showSchemaFilterNote() {
		schemaFilterNote.setVisible(true);
		pack();
	}

	public DatabaseTree getDatabaseTree() {
		return databaseTree;
	}

	// Shows the database tree.
	public void showDatabaseTree(final DatabaseTree tree) {
		databaseTree = tree;
		treeScroll.getViewport().add(databaseTree);
		databaseTree.addTreeSelectionListener(e -> {
			if (e.isAddedPath()) {
				final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				Object selected = selectedNode.getUserObject();

				if ((selected instanceof Database || selected instanceof Schema) && selectedNode.getChildCount() <= 0) {
					exportController.showChildren(this, selected);
				}
				if (selected instanceof Table || selected instanceof Schema) {
					// A selected database or schema exports everything in it already, so a table or schema below it is not selected twice.
					for (DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode
						.getParent(); parent != null; parent = (DefaultMutableTreeNode) parent
							.getParent()) {
						tree.removeSelectionPath(new TreePath(parent.getPath()));
					}
				}
			}
		});
	}

	private void initComponents() {
		Dialect dialect = exportController.dialect();
		views.setEnabled(dialect.showCreateViewSql(TableName.of("v")) != null);
		views.setToolTipText("Writes CREATE VIEW for the views of the selected databases or schemas, after their tables. Needs Structure.");
		transaction.setEnabled(dialect.beginTransactionSql() != null);
		foreignKeys.setEnabled(dialect.disableForeignKeyChecksSql() != null);
		foreignKeys.setToolTipText(foreignKeys.isEnabled()
			? "Loads the tables in any order without checking foreign keys."
			: "This server has no safe way to switch foreign key checks off for a script.");
		rowsPerInsert.setToolTipText("1 writes an INSERT for every row, a larger number puts that many rows in one INSERT.");

		JPanel content = Forms.titled(box(structure, data, views), "Content");
		JPanel statements = Forms.titled(box(createDatabase, dropTable, useDatabase, dropIfExists, createIfNotExists), "Statements");
		JPanel script = Forms.titled(box(transaction, foreignKeys), "Script");
		JPanel format = Forms.titled(new Forms.Grid().row("&Rows per INSERT:", rowsPerInsert).row("E&ncoding:", encoding).panel(), "Format");

		JButton browse = Forms.button("&Browse...");
		browse.addActionListener(e -> chooseFile());
		JPanel fileRow = new JPanel(new BorderLayout(Forms.GAP, 0));
		fileRow.add(file, BorderLayout.CENTER);
		fileRow.add(browse, BorderLayout.EAST);
		// The label belongs to the text field, the row only adds the Browse button next to it.
		JPanel target = Forms.titled(new Forms.Grid().row(Forms.label("&File:", file), fileRow).panel(), "Save as");

		JButton export = Forms.button("&Export");
		JButton close = Forms.button("Close");
		export.addActionListener(e -> export());
		close.addActionListener(e -> dispose());

		JPanel options = new Forms.Grid().full(content).full(statements).full(script).full(format).full(target).done();

		JButton selectAll = Forms.button("Select a&ll");
		JButton clear = Forms.button("Clea&r");
		selectAll.addActionListener(e -> selectAll());
		clear.addActionListener(e -> {
			if (databaseTree != null) {
				databaseTree.clearSelection();
			}
		});
		JPanel treeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		treeButtons.add(selectAll);
		treeButtons.add(clear);

		JPanel tree = new JPanel(new BorderLayout(0, Forms.GAP));
		tree.add(new JLabel("Select the databases, schemas or tables to export:"), BorderLayout.NORTH);
		tree.add(treeScroll, BorderLayout.CENTER);
		JPanel treeSouth = new JPanel(new BorderLayout(0, Forms.GAP));
		treeSouth.add(treeButtons, BorderLayout.NORTH);
		schemaFilterNote.setVisible(false);
		treeSouth.add(schemaFilterNote, BorderLayout.SOUTH);
		tree.add(treeSouth, BorderLayout.SOUTH);
		treeScroll.setPreferredSize(new Dimension(240, 320));

		JPanel main = new JPanel(new BorderLayout(Forms.PADDING, 0));
		main.add(tree, BorderLayout.CENTER);
		main.add(options, BorderLayout.EAST);
		layoutDialog(main, export, close);
		setInitialFocus(file);
	}

	private static JPanel box(JCheckBox... boxes) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (JCheckBox box : boxes) {
			panel.add(box);
		}
		return panel;
	}

	/** Selects every database of the server, which takes everything in them. */
	private void selectAll() {
		if (databaseTree == null) {
			return;
		}
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) databaseTree.getModel().getRoot();
		List<TreePath> paths = new ArrayList<>();
		for (int i = 0; i < root.getChildCount(); i++) {
			paths.add(new TreePath(((DefaultMutableTreeNode) root.getChildAt(i)).getPath()));
		}
		databaseTree.setSelectionPaths(paths.toArray(new TreePath[0]));
	}

	private void chooseFile() {
		JFileChooser chooser = FileChoosers.create();
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setDialogTitle("Save as (a name ending in .gz is compressed)");

		try {
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				file.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		} catch (Exception err) {
			ApplicationContext.get().errors().report(this, "Choose file", err);
		}
	}

	private void export() {
		// The controller gives the tree before the window opens; without one there is nothing to run.
		if (databaseTree == null) {
			return;
		}
		String problem = Validation.required("the file to save to", file.getText());
		showError(problem);
		if (problem == null) {
			ExportOptions options = new ExportOptions(structure.isSelected(), data.isSelected(), createDatabase.isSelected(), dropTable.isSelected(),
				useDatabase.isSelected(), dropIfExists.isSelected(), createIfNotExists.isSelected(), (Charset) encoding.getSelectedItem(),
				(Integer) rowsPerInsert.getValue(), views.isEnabled() && views.isSelected(), transaction.isEnabled() && transaction.isSelected(),
				foreignKeys.isEnabled() && foreignKeys.isSelected());
			exportController.exportNodesAsSQL(this, databaseTree.getSelectionPaths(), file.getText().trim(), options);
		}
	}
}

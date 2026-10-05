package nl.errorsoft.esql.export.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
import nl.errorsoft.esql.export.control.ExportCC;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

/** Exports databases, schemas or tables as SQL statements to a file: the tree to choose from on the left, the options and the file on the right. */
public class ExportAsSQLUI extends FormDialog {
	private final ExportCC ecc;
	private final JScrollPane treeScroll = new JScrollPane();
	private final JTextField file = new JTextField(24);

	private final JCheckBox structure = Forms.mnemonic(new JCheckBox("", true), "&Structure");
	private final JCheckBox data = Forms.mnemonic(new JCheckBox("", true), "&Data");
	private final JCheckBox createDatabase = Forms.mnemonic(new JCheckBox("", true), "&Create database");
	private final JCheckBox dropTable = Forms.mnemonic(new JCheckBox("", true), "Dr&op table");
	private final JCheckBox useDatabase = Forms.mnemonic(new JCheckBox("", true), "&Use database");

	private DatabaseTreeView dtv;

	public ExportAsSQLUI(ESQLManagerUI jm, ExportCC ecc) {
		super(jm, "Export data", false);
		this.ecc = ecc;
		initComponents();
		setResizable(true);
		showDialog();
		toFront();
	}

	public DatabaseTreeView getDatabaseTreeView() {
		return dtv;
	}

	// Shows the database tree.
	public void showDatabaseTreeView(final DatabaseTreeView tv) {
		dtv = tv;
		treeScroll.getViewport().add(dtv);
		dtv.addTreeSelectionListener(e -> {
			if (e.isAddedPath()) {
				final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				Object selected = selectedNode.getUserObject();

				if ((selected instanceof Database || selected instanceof Schema) && selectedNode.getChildCount() <= 0) {
					ecc.showChildren(this, selected);
				}
				if (selected instanceof Table || selected instanceof Schema) {
					// A selected database or schema exports everything in it already, so a table or schema below it is not selected twice.
					for (DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode
						.getParent(); parent != null; parent = (DefaultMutableTreeNode) parent
							.getParent()) {
						tv.removeSelectionPath(new TreePath(parent.getPath()));
					}
				}
			}
		});
	}

	private void initComponents() {
		JPanel content = Forms.titled(box(structure, data), "Content");
		JPanel statements = Forms.titled(box(createDatabase, dropTable, useDatabase), "Statements");

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

		JPanel options = new Forms.Grid().full(content).full(statements).full(target).done();

		JButton selectAll = Forms.button("Select a&ll");
		JButton clear = Forms.button("Clea&r");
		selectAll.addActionListener(e -> selectAll());
		clear.addActionListener(e -> {
			if (dtv != null) {
				dtv.clearSelection();
			}
		});
		JPanel treeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		treeButtons.add(selectAll);
		treeButtons.add(clear);

		JPanel tree = new JPanel(new BorderLayout(0, Forms.GAP));
		tree.add(new JLabel("Select the databases, schemas or tables to export:"), BorderLayout.NORTH);
		tree.add(treeScroll, BorderLayout.CENTER);
		tree.add(treeButtons, BorderLayout.SOUTH);
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
		if (dtv == null) {
			return;
		}
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) dtv.getModel().getRoot();
		List<TreePath> paths = new ArrayList<>();
		for (int i = 0; i < root.getChildCount(); i++) {
			paths.add(new TreePath(((DefaultMutableTreeNode) root.getChildAt(i)).getPath()));
		}
		dtv.setSelectionPaths(paths.toArray(new TreePath[0]));
	}

	private void chooseFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setDialogTitle("Save as");

		try {
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				file.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		} catch (Exception err) {
			ApplicationContext.get().errors().report(this, "Choose file", err);
		}
	}

	private void export() {
		String problem = Validation.required("the file to save to", file.getText());
		showError(problem);
		if (problem == null) {
			ecc.exportNodesAsSQL(this, dtv.getSelectionPaths(), file.getText().trim(), structure.isSelected(), data.isSelected(),
				createDatabase.isSelected(), dropTable.isSelected(), useDatabase.isSelected());
		}
	}
}

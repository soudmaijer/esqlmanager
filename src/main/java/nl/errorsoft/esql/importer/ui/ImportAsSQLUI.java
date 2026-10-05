package nl.errorsoft.esql.importer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
import nl.errorsoft.esql.importer.ImportOptions;
import nl.errorsoft.esql.importer.control.ImportCC;
import nl.errorsoft.esql.ui.util.Encodings;
import nl.errorsoft.esql.ui.util.ExtentionFileFilter;
import nl.errorsoft.esql.ui.util.FileChoosers;
import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

/** Runs the statements of a SQL file in a database or schema: the tree to choose the target on the left, the target and the file on the right. */
public class ImportAsSQLUI extends FormDialog {
	private static final String CURRENT = "the current database";

	private final ImportCC ecc;
	private final JScrollPane treeScroll = new JScrollPane();
	private final JTextField file = new JTextField(24);
	private final JLabel target = new JLabel(CURRENT);
	private final JRadioButton stopOnError = Forms.mnemonic(new JRadioButton("", true), "&Stop on the first error");
	private final JRadioButton continueOnError = Forms.mnemonic(new JRadioButton(), "Co&ntinue and report the errors at the end");
	private final JCheckBox singleTransaction = Forms.mnemonic(new JCheckBox(), "Run in a single &transaction");
	private final JComboBox<Charset> encoding = Encodings.combo(ApplicationContext.get().settings().getDefaultEncoding());

	private DatabaseTreeView dtv;

	public ImportAsSQLUI(ESQLManagerUI jm, ImportCC ecc) {
		super(jm, "Import data", false);
		this.ecc = ecc;
		initComponents();
		setResizable(true);
		showDialog();
		toFront();
	}

	private void initComponents() {
		JButton browse = Forms.button("&Browse...");
		browse.addActionListener(e -> chooseFile());
		JPanel fileRow = new JPanel(new BorderLayout(Forms.GAP, 0));
		fileRow.add(file, BorderLayout.CENTER);
		fileRow.add(browse, BorderLayout.EAST);

		JPanel options = Forms.titled(new Forms.Grid().row(new JLabel("Import into:"), target).row(Forms.label("&File:", file), fileRow).panel(), "Import");
		JLabel hint = new JLabel(
			"<html><body style='width: 260px'>Select the database or schema to run the script in. Without a selection it runs in the current one."
				+ "</body></html>");

		ButtonGroup errors = new ButtonGroup();
		errors.add(stopOnError);
		errors.add(continueOnError);
		singleTransaction.setToolTipText("All statements are rolled back when one fails or the import is cancelled. MySQL commits at every CREATE or DROP.");
		singleTransaction.addActionListener(e -> {
			// A transaction cannot go on after an error.
			continueOnError.setEnabled(!singleTransaction.isSelected());
			if (singleTransaction.isSelected()) {
				stopOnError.setSelected(true);
			}
		});
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.add(stopOnError);
		box.add(continueOnError);
		box.add(singleTransaction);
		JPanel script = Forms.titled(new Forms.Grid().full(box).row("E&ncoding:", encoding).panel(), "Script");

		JButton run = Forms.button("&Import");
		JButton close = Forms.button("Close");
		run.addActionListener(e -> run());
		close.addActionListener(e -> dispose());

		treeScroll.setPreferredSize(new Dimension(240, 300));
		JPanel right = new Forms.Grid().full(hint).full(options).full(script).done();

		JPanel main = new JPanel(new BorderLayout(Forms.PADDING, 0));
		main.add(treeScroll, BorderLayout.CENTER);
		main.add(right, BorderLayout.EAST);
		layoutDialog(main, run, close);
		setInitialFocus(file);
	}

	// Shows the database tree.
	public void showDatabaseTreeView(DatabaseTreeView tv) {
		dtv = tv;
		treeScroll.getViewport().add(dtv);
		dtv.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		dtv.addTreeSelectionListener(e -> {
			target.setText(e.getPath() != null && e.isAddedPath()
				? String.valueOf(((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject())
				: CURRENT);

			if (e.isAddedPath()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

				// A database opens to its schemas, so the script can be run in one of them.
				if (node.getUserObject() instanceof Database && node.getChildCount() <= 0) {
					ecc.showChildren(this, node.getUserObject());
				}
			}
		});
		TreePath selected = dtv.getSelectionPath();
		if (selected != null) {
			target.setText(String.valueOf(((DefaultMutableTreeNode) selected.getLastPathComponent()).getUserObject()));
		}
	}

	public DatabaseTreeView getDatabaseTreeView() {
		return dtv;
	}

	private void chooseFile() {
		JFileChooser chooser = FileChoosers.create();
		ExtentionFileFilter sql = new ExtentionFileFilter("SQL file", new String[]{".sql", ".gz"});
		chooser.addChoosableFileFilter(sql);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileFilter(sql);
		chooser.setDialogTitle("Select file (.sql or compressed .sql.gz)");

		try {
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				file.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		} catch (Exception err) {
			ApplicationContext.get().errors().report(this, "Choose file", err);
		}
	}

	private void run() {
		String problem = Validation.required("the file to import", file.getText());
		showError(problem);
		if (problem == null) {
			ecc.importNodesAsSQL(this, dtv.getSelectionPath(), file.getText().trim(),
				new ImportOptions(!continueOnError.isSelected(), singleTransaction.isSelected(), (Charset) encoding.getSelectedItem()));
		}
	}
}

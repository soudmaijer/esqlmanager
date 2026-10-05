package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.designer.ui.diagram.CommentObject;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.Field;
import nl.errorsoft.esql.designer.ui.diagram.ModelObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;

import nl.errorsoft.esql.table.CreateColumn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import nl.errorsoft.esql.designer.model.Model;
import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.model.ForeignKey;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class Generate extends JDialog {
	private static final Logger log = LogManager.getLogger(Generate.class);
	private static final String[] STEPS = {"Checking Databases", "Checking Tables", "Checking Columns", "Checking Relations", "Checking Model"};

	private final ConnectionWindowUI cwui;
	private final Model m;
	private final JLabel[] checks = new JLabel[STEPS.length];
	private final JTextArea problems = new JTextArea(6, 36);
	private final JProgressBar progress = new JProgressBar();
	private final JButton generate = Forms.button("&Generate");
	private final JButton close = Forms.button("&Close");
	/** True while checking or generating, the dialog cannot be closed then. */
	private boolean busy;

	public Generate(ESQLManagerUI eui, ConnectionWindowUI cwui, Model m) {
		super((JFrame) eui, "Analyze / Generate model", true);
		this.cwui = cwui;
		this.m = m;

		initComponents();
		setLocationRelativeTo(eui);
		startChecking();
		setVisible(true);
	}

	private void initComponents() {
		setResizable(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!busy) {
					dispose();
				}
			}
		});

		JPanel steps = new JPanel();
		steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
		for (int i = 0; i < STEPS.length; i++) {
			checks[i] = new JLabel(STEPS[i], ApplicationContext.get().imageLoader().getIcon("check_pending"), SwingConstants.LEADING);
			checks[i].setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			steps.add(checks[i]);
		}
		steps.add(Box.createVerticalStrut(Forms.GAP));
		progress.setAlignmentX(LEFT_ALIGNMENT);
		steps.add(progress);
		Forms.titled(steps, "Checking Model");

		problems.setEditable(false);
		problems.setLineWrap(true);
		problems.setWrapStyleWord(true);
		JScrollPane problemScroll = new JScrollPane(problems);
		Forms.titled(problemScroll, "Problems");

		close.addActionListener(e -> dispose());
		close.setEnabled(false);
		generate.addActionListener(e -> generateModel());
		generate.setEnabled(false);

		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(steps, BorderLayout.NORTH);
		root.add(problemScroll, BorderLayout.CENTER);
		root.add(Forms.buttonRow(generate, close), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(generate);
		pack();
	}

	/** Checks the model on a virtual thread, the dialog is updated on the event thread. */
	private void startChecking() {
		setBusy(true);
		progress.setMaximum(STEPS.length);
		Thread.ofVirtual().name("generate-check").start(() -> {
			List<List<String>> found = new ArrayList<>();
			for (int step = 0; step < STEPS.length; step++) {
				found.add(check(step));
				int done = step;
				List<String> stepProblems = found.get(step);
				SwingUtilities.invokeLater(() -> {
					checks[done].setIcon(ApplicationContext.get().imageLoader().getIcon(stepProblems.isEmpty() ? "check_good" : "check_error"));
					progress.setValue(done + 1);
				});
			}
			boolean ok = found.stream().allMatch(List::isEmpty);
			String text = String.join("\n", found.stream().flatMap(List::stream).toList());
			SwingUtilities.invokeLater(() -> {
				problems.setText(text);
				setBusy(false);
				generate.setEnabled(ok);
			});
		});
	}

	/** The problems of one step of the check, empty when it passed. */
	private List<String> check(int step) {
		List<ModelObject> objects = m.getObjects();
		List<DatabaseObject> databases = objects.stream().filter(DatabaseObject.class::isInstance).map(DatabaseObject.class::cast).toList();
		List<TableObject> tables = objects.stream().filter(TableObject.class::isInstance).map(TableObject.class::cast).toList();
		List<String> found = new ArrayList<>();

		switch (step) {
			case 0 -> {
				for (int i = 0; i < databases.size(); i++) {
					for (int j = i + 1; j < databases.size(); j++) {
						if (databases.get(i).getName().equalsIgnoreCase(databases.get(j).getName())) {
							found.add("The model has two databases named '" + databases.get(i).getName() + "'.");
						}
					}
				}
			}
			case 1 -> {
				for (DatabaseObject database : databases) {
					List<ModelObject> own = m.getReferences(database);
					for (int i = 0; i < own.size(); i++) {
						for (int j = i + 1; j < own.size(); j++) {
							if (own.get(i) instanceof TableObject a && own.get(j) instanceof TableObject b && a.getName().equalsIgnoreCase(b.getName())) {
								found.add("Database '" + database.getName() + "' has two tables named '" + a.getName() + "'.");
							}
						}
					}
				}
			}
			case 2 -> {
				for (TableObject table : tables) {
					Field[] fields = table.getFields();
					if (fields.length == 0) {
						found.add("Table '" + table.getName() + "' has no columns.");
					}
					for (int k = 0; k < fields.length; k++) {
						for (int l = k + 1; l < fields.length; l++) {
							if (fields[k].getName().equalsIgnoreCase(fields[l].getName())) {
								found.add("Table '" + table.getName() + "' has two columns named '" + fields[k].getName() + "'.");
							}
						}
					}
				}
			}
			case 3 -> {
				for (TableObject table : tables) {
					boolean inDatabase = databases.stream().anyMatch(d -> m.getReferences(d).contains(table));
					if (!inDatabase) {
						found.add("Table '" + table.getName() + "' is not linked to a database.");
					}
				}
			}
			default -> {
				if (databases.isEmpty()) {
					found.add("The model has no database.");
				}
			}
		}
		return found;
	}

	private void setBusy(boolean busy) {
		this.busy = busy;
		close.setEnabled(!busy);
	}

	/** Reads the model on the event thread, creates the databases and tables on a virtual thread. */
	private void generateModel() {
		List<DesignedDatabase> model = new ArrayList<>();
		int count = 0;

		for (ModelObject object : m.getObjects()) {
			if (object instanceof DatabaseObject d) {
				List<DesignedTable> designedTables = new ArrayList<>();
				count++;

				for (ModelObject reference : m.getReferences(d)) {
					TableObject table = (TableObject) reference;
					List<CreateColumn> columns = new ArrayList<>();

					for (Field field : table.getFields()) {
						columns.add(toCreateColumn(field));
					}
					count += 1 + columns.size();
					designedTables.add(new DesignedTable(table.getName(), table.getType(), table.getComment(), columns, foreignKeysOf(table)));
				}
				model.add(new DesignedDatabase(d.getName(), designedTables));
			}
		}

		progress.setValue(0);
		progress.setMaximum(count);
		generate.setEnabled(false);
		setBusy(true);

		Thread.ofVirtual().name("generate-model").start(() -> {
			Exception failure = null;
			try {
				cwui.getControlClass().getContext().designer().generate(model,
					() -> SwingUtilities.invokeLater(() -> progress.setValue(progress.getValue() + 1)));
			} catch (Exception e) {
				log.debug("Model generation failed", e);
				failure = e;
			}
			Exception error = failure;
			SwingUtilities.invokeLater(() -> {
				setBusy(false);
				cwui.getControlClass().showDatabaseTree();
				if (error == null) {
					dispose();
				} else {
					generate.setEnabled(true);
					ApplicationContext.get().errors().report(this, "Generate model", error);
				}
			});
		});
	}

	/** The keys the table has on other tables; the keys other tables have on it are generated with those tables. */
	private List<DesignedForeignKey> foreignKeysOf(TableObject table) {
		List<DesignedForeignKey> keys = new ArrayList<>();

		for (ForeignKey key : m.foreignKeysOf(table)) {
			if (key.from() == table) {
				keys.add(new DesignedForeignKey(key.name(), key.fromColumns(), key.to().getName(), key.toColumns(), key.onDelete(), key.onUpdate()));
			}
		}
		return keys;
	}

	private CreateColumn toCreateColumn(Field f) {
		CreateColumn column = new CreateColumn(f.getName());
		column.type = f.getType();
		column.length = f.getLength();
		column.defaultval = f.getDefault();
		column.primary = f.primary;
		column.index = f.index;
		column.unique = f.unique;
		column.binary = f.binary;
		column.notnull = f.notnull;
		column.unsigned = f.unsigned;
		column.autoincrement = f.autoincrement;
		column.zerofill = f.zerofill;
		return column;
	}
}

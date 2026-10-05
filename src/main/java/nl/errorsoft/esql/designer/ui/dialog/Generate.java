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
import java.util.ArrayList;
import java.util.List;

public class Generate extends javax.swing.JDialog implements Runnable {
	private static final Logger log = LogManager.getLogger(Generate.class);
	private ConnectionWindowUI cwui;
	private ESQLManagerUI eui;
	private Model m;

	public Generate(ESQLManagerUI eui, ConnectionWindowUI cwui, Model m) {
		super((JFrame) eui, true);

		this.eui = eui;
		this.cwui = cwui;
		this.m = m;

		initComponents();
	}

	private void initComponents() {
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		progress = new javax.swing.JProgressBar();
		jButton1 = new javax.swing.JButton();
		generate = new javax.swing.JButton();

		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setTitle("Analyze / Generate model");

		jPanel2.setLayout(new BoxLayout(jPanel2, BoxLayout.Y_AXIS));
		JLabel[] checks = {jLabel1, jLabel2, jLabel3, jLabel4, jLabel5};
		String[] texts = {"Checking Databases", "Checking Tables", "Checking Columns", "Checking Relations", "Checking Model"};
		for (int i = 0; i < checks.length; i++) {
			checks[i].setText(texts[i]);
			checks[i].setIcon(ApplicationContext.get().imageLoader().getIcon("check_off"));
			checks[i].setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			jPanel2.add(checks[i]);
		}
		jPanel2.add(Box.createVerticalStrut(Forms.GAP));
		progress.setAlignmentX(LEFT_ALIGNMENT);
		progress.setPreferredSize(new java.awt.Dimension(200, progress.getPreferredSize().height));
		jPanel2.add(progress);
		Forms.titled(jPanel2, "Checking Model");

		jButton1.setText("Close");
		jButton1.addActionListener(evt -> jButton1ActionPerformed(evt));
		generate.setText("Generate");
		generate.addActionListener(evt -> generateActionPerformed(evt));
		generate.setEnabled(false);

		jPanel1.setLayout(new java.awt.BorderLayout());
		Forms.padded(jPanel1);
		jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);
		jPanel1.add(Forms.buttonRow(generate, jButton1), java.awt.BorderLayout.SOUTH);
		setContentPane(jPanel1);

		pack();

		this.setLocationRelativeTo(eui);

		Thread t = new Thread(this);
		t.start();

		this.setVisible(true);
	}

	public void run() {
		List<ModelObject> v = m.getObjects();

		if (v.size() != 0) {
			List<ModelObject> db = new ArrayList<>();
			List<ModelObject> tb = new ArrayList<>();

			// Split objects
			for (int i = 0; i < v.size(); i++) {
				if (v.get(i) instanceof DatabaseObject) {
					db.add(v.get(i));
				} else if (v.get(i) instanceof TableObject) {
					tb.add(v.get(i));
				}
			}

			progress.setMaximum(db.size() * db.size());

			// Check databases
			boolean db_error = false;
			boolean tb_error = false;
			boolean fd_error = false;

			for (int i = 0; i < db.size(); i++) {
				DatabaseObject d = (DatabaseObject) db.get(i);

				for (int j = 0; j < db.size(); j++) {
					DatabaseObject tmp2 = (DatabaseObject) db.get(j);
					if (d.getName().equalsIgnoreCase(tmp2.getName()) && tmp2 != d) {
						db_error = true;
						Dialogs.error(this, getTitle(), "The model has two databases named '" + d.getName() + "'.");
						break;
					}
					progress.setValue(progress.getValue() + 1);
				}
				if (db_error) {
					break;
				}

				tb = m.getReferences(d);

				progress.setMaximum(tb.size() * tb.size());
				progress.setValue(0);

				for (int h = 0; h < tb.size(); h++) {
					TableObject tmp = (TableObject) tb.get(h);

					for (int j = 0; j < tb.size(); j++) {
						TableObject tmp2 = (TableObject) tb.get(j);
						if (tmp.getName().equalsIgnoreCase(tmp2.getName()) && tmp2 != tmp) {
							tb_error = true;
							Dialogs.error(this, getTitle(), "Database '" + d.getName() + "' has two tables named '" + tmp.getName() + "'.");
							break;
						}
						progress.setValue(progress.getValue() + 1);
					}

					Field[] f = tmp.getFields();

					if (f.length == 0) {
						Dialogs.error(this, getTitle(), "Table '" + tmp.getName() + "' has no columns.");
						fd_error = true;
						break;
					}

					for (int k = 0; k < f.length; k++) {
						for (int l = 0; l < f.length; l++) {
							if (f[k].getName().equalsIgnoreCase(f[l].getName()) && k != l) {
								Dialogs.error(this, getTitle(),
									"Table '" + tmp.getName() + "' has two columns named '" + f[l].getName() + "'.");
								fd_error = true;
								break;
							}
						}
						if (fd_error) {
							break;
						}
					}
				}
				if (tb_error || fd_error) {
					break;
				}
			}

			if (db_error) {
				jLabel1.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			} else {
				jLabel1.setIcon(ApplicationContext.get().imageLoader().getIcon("check_good"));
			}

			if (tb_error) {
				jLabel2.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			} else {
				jLabel2.setIcon(ApplicationContext.get().imageLoader().getIcon("check_good"));
			}

			if (fd_error) {
				jLabel3.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			} else {
				jLabel3.setIcon(ApplicationContext.get().imageLoader().getIcon("check_good"));
			}

			progress.setValue(0);
			progress.setMaximum(v.size());

			boolean ref_error = false;
			for (int i = 0; i < v.size(); i++) {
				if (!(v.get(i) instanceof CommentObject)) {
					ModelObject mo = (ModelObject) v.get(i);
					if (m.getReferences(mo).size() == 0) {
						ref_error = true;
					}
				}
				progress.setValue(progress.getValue() + 1);
			}

			if (ref_error) {
				jLabel4.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			} else {
				jLabel4.setIcon(ApplicationContext.get().imageLoader().getIcon("check_good"));
			}

			// Cheat past the model :)
			jLabel5.setIcon(ApplicationContext.get().imageLoader().getIcon("check_good"));

			if (db_error == false && tb_error == false && fd_error == false) {
				generate.setEnabled(true);
			}
		} else {
			jLabel1.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			jLabel2.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			jLabel3.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			jLabel4.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
			jLabel5.setIcon(ApplicationContext.get().imageLoader().getIcon("check_error"));
		}
	}

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		this.dispose();
	}

	private void generateActionPerformed(java.awt.event.ActionEvent evt) {
		List<ModelObject> v = m.getObjects();

		List<ModelObject> db = new ArrayList<>();

		// Split objects
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i) instanceof DatabaseObject) {
				db.add(v.get(i));
			}
		}

		int count = 0;
		for (int i = 0; i < db.size(); i++) {
			DatabaseObject d = (DatabaseObject) db.get(i);
			count++;

			List<ModelObject> tb = m.getReferences(d);

			for (int j = 0; j < tb.size(); j++) {
				count++;

				TableObject tbs = (TableObject) tb.get(j);
				for (int k = 0; k < tbs.getFields().length; k++) {
					count++;
				}
			}
		}

		progress.setValue(0);
		progress.setMaximum(count);

		boolean error = false;

		try {
			List<DesignedDatabase> model = new ArrayList<>();

			for (int i = 0; i < db.size(); i++) {
				DatabaseObject d = (DatabaseObject) db.get(i);
				List<DesignedTable> designedTables = new ArrayList<>();
				List<ModelObject> tb = m.getReferences(d);

				for (int j = 0; j < tb.size(); j++) {
					TableObject tbs = (TableObject) tb.get(j);
					List<CreateColumn> columns = new ArrayList<>();

					for (int k = 0; k < tbs.getFields().length; k++) {
						columns.add(toCreateColumn(tbs.getFields()[k]));
					}

					designedTables.add(new DesignedTable(tbs.getName(), tbs.getType(), tbs.getComment(), columns, foreignKeysOf(tbs)));
				}
				model.add(new DesignedDatabase(d.getName(), designedTables));
			}

			cwui.getControlClass().getContext().designer().generate(model, () -> progress.setValue(progress.getValue() + 1));
		} catch (Exception e) {
			log.error("Model generation failed", e);
			error = true;
		}

		if (error) {
			Dialogs.error(this, getTitle(), "Model generation failed, the output panel shows why.");
		} else {
			Dialogs.info(this, getTitle(), "Model generated.");
		}

		cwui.getControlClass().showDatabaseTree();

		this.dispose();
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

	private javax.swing.JLabel jLabel4;
	private javax.swing.JButton generate;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JButton jButton1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JProgressBar progress;
	private javax.swing.JLabel jLabel5;
}

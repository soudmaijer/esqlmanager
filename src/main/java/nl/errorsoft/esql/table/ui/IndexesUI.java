package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.control.IndexesCC;
import nl.errorsoft.esql.ui.util.EditorTab;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The indexes of one table, as a tab of the connection window. Save writes the selected index, Drop removes it; the tab stays open and shows the indexes
 * as they are after the change. Cancel closes it, asking first when the selected index has changes that are not saved.
 */
public class IndexesUI extends JPanel implements ActionListener, EditorTab {
	private final IndexesCC tcc;
	private final String title;

	private final JComboBox jcmbIndexes = new JComboBox();
	private final JButton jbtnAdd = new JButton("Add");
	private final JButton jbtnPrimary = new JButton("Add Primary");
	private final JList jlstUsed = new JList(new DefaultListModel());
	private final JList jlstAvail = new JList(new DefaultListModel());
	private final JButton jbtnAddToList = new JButton("<");
	private final JButton jbtnRemoveFromList = new JButton(">");
	private final JCheckBox jrdUnique = new JCheckBox("Unique");
	private final JCheckBox jrdFulltext = new JCheckBox("Fulltext");
	private final JButton jbtnSave = new JButton("Save");
	private final JButton jbtnDrop = new JButton("Drop");
	private final JButton jbtnCancel = new JButton("Cancel");

	// Columns, type or a new index changed since the indexes were loaded.
	private boolean modified;

	public IndexesUI(IndexesCC tcc, String title) {
		super(new BorderLayout(8, 8));
		this.tcc = tcc;
		this.title = title;
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel indexes = new JPanel(new GridBagLayout());
		indexes.setBorder(BorderFactory.createTitledBorder("Indexes"));
		GridBagConstraints c = constraints(0, 0);
		indexes.add(new JLabel("Name:"), c);
		c = constraints(1, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		indexes.add(jcmbIndexes, c);
		c = constraints(2, 0);
		indexes.add(jbtnPrimary, c);
		c = constraints(3, 0);
		indexes.add(jbtnAdd, c);

		JPanel columns = new JPanel(new GridBagLayout());
		columns.setBorder(BorderFactory.createTitledBorder("Columns"));
		columns.add(new JLabel("Used:"), constraints(0, 0));
		columns.add(new JLabel("Available:"), constraints(2, 0));
		c = constraints(0, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(jlstUsed), c);
		JPanel move = new JPanel(new GridLayout(2, 1, 0, 4));
		move.add(jbtnAddToList);
		move.add(jbtnRemoveFromList);
		columns.add(move, constraints(1, 1));
		c = constraints(2, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(jlstAvail), c);

		JPanel type = new JPanel(new FlowLayout(FlowLayout.LEFT));
		type.setBorder(BorderFactory.createTitledBorder("Index type"));
		type.add(jrdUnique);
		type.add(jrdFulltext);

		add(indexes, BorderLayout.NORTH);
		add(columns, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		buttons.add(jbtnSave);
		buttons.add(jbtnDrop);
		buttons.add(jbtnCancel);
		JPanel south = new JPanel(new BorderLayout(0, 8));
		south.add(type, BorderLayout.NORTH);
		south.add(buttons, BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);

		for (JButton button : new JButton[]{jbtnAdd, jbtnPrimary, jbtnAddToList, jbtnRemoveFromList, jbtnSave, jbtnDrop, jbtnCancel}) {
			button.addActionListener(this);
		}
		jrdUnique.addActionListener(this);
		jrdFulltext.addActionListener(this);
		jcmbIndexes.addItemListener(e -> {
			if (jcmbIndexes.getSelectedItem() instanceof TableIndex ti) {
				itemSelected(ti);
			}
		});
	}

	private static GridBagConstraints constraints(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 4, 2, 4);
		return c;
	}

	@Override
	public boolean confirmClose() {
		return !modified || Dialogs.confirmDestructive(this, "Discard changes?", "Discard the changes in " + title + "?", "Discard");
	}

	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();

		if (source == jbtnCancel) {
			tcc.close();
		} else if (source == jbtnRemoveFromList) {
			moveSelected(jlstUsed, jlstAvail);
		} else if (source == jbtnAddToList) {
			moveSelected(jlstAvail, jlstUsed);
		} else if (source == jrdFulltext) {
			jrdUnique.setSelected(false);
			modified = true;
		} else if (source == jrdUnique) {
			jrdFulltext.setSelected(false);
			modified = true;
		} else if (source == jbtnDrop) {
			if (jcmbIndexes.getSelectedItem() instanceof TableIndex ti) {
				tcc.dropIndex(ti);
			}
		} else if (source == jbtnAdd) {
			String input = Dialogs.input(this, "New index", "Name of the new index:");

			if (input != null) {
				tcc.addNew(input);
			}
		} else if (source == jbtnPrimary) {
			tcc.addPrimary();
		} else if (source == jbtnSave) {
			if (jcmbIndexes.getSelectedItem() instanceof TableIndex ti) {
				DefaultListModel dlm = (DefaultListModel) jlstUsed.getModel();
				TableColumn[] tc = new TableColumn[dlm.getSize()];

				for (int i = 0; i < dlm.getSize(); i++) {
					tc[i] = (TableColumn) dlm.elementAt(i);
				}

				String type = "INDEX";

				if (jrdFulltext.isSelected()) {
					type = "FULLTEXT";
				} else if (jrdUnique.isSelected()) {
					type = "UNIQUE";
				}

				tcc.modifyIndex(ti, tc, type);
			}
		}
	}

	private void moveSelected(JList from, JList to) {
		if (from.getSelectedValue() instanceof TableColumn column) {
			((DefaultListModel) from.getModel()).removeElement(column);
			((DefaultListModel) to.getModel()).addElement(column);
			modified = true;
		}
	}

	/** Shows the indexes as they are in the database, nothing is modified any more. */
	public void loadIndexes(TableIndex[] tia) {
		modified = false;
		jbtnPrimary.setEnabled(true);
		jcmbIndexes.setModel(new DefaultComboBoxModel());
		jlstUsed.setModel(new DefaultListModel());
		jlstAvail.setModel(new DefaultListModel());

		if (tia == null || tia.length == 0) {
			return;
		}

		for (TableIndex index : tia) {
			if (index.isPrimary()) {
				jbtnPrimary.setEnabled(false);
			}
			jcmbIndexes.addItem(index);
		}

		itemSelected(tia[0]);
	}

	public void addNewIndex(TableIndex ti, TableColumn[] tc) {
		DefaultListModel dlmAvail = new DefaultListModel();
		jcmbIndexes.addItem(ti);
		jcmbIndexes.setSelectedItem(ti);

		for (TableColumn column : tc) {
			dlmAvail.addElement(column);
		}

		jlstUsed.setModel(new DefaultListModel());
		jlstAvail.setModel(dlmAvail);
		modified = true;
	}

	public void itemSelected(TableIndex index) {
		TableColumn[] used = index.getTableColumns();
		TableColumn[] avail = index.getTable().getColumns();
		DefaultListModel dlmUsed = new DefaultListModel();
		DefaultListModel dlmAvail = new DefaultListModel();

		jrdUnique.setSelected(index.isUnique());
		jrdFulltext.setSelected(!index.isUnique() && index.isFulltext());
		jrdUnique.setEnabled(!index.isPrimary());
		jrdFulltext.setEnabled(!index.isPrimary());

		for (TableColumn column : avail) {
			dlmAvail.addElement(column);
		}

		if (used != null) {
			for (TableColumn column : used) {
				dlmUsed.addElement(column);
				dlmAvail.removeElement(column);
			}
		}

		jlstUsed.setModel(dlmUsed);
		jlstAvail.setModel(dlmAvail);
	}
}

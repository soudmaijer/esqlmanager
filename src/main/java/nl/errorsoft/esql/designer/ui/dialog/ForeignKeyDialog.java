package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.Field;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.designer.model.ForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.dialect.Dialect;

/** Edits a foreign key of the designer: the referenced table, the column pairs, the name and the actions. */
public class ForeignKeyDialog extends JDialog {
	private final TableObject from;

	private final JComboBox<TableObject> referenced = new JComboBox<>();
	private final JTextField name = new JTextField(24);
	private final DefaultTableModel pairs = new DefaultTableModel(new Object[]{"Column", "Referenced column"}, 0);
	private final JTable pairTable = new JTable(pairs);
	private final JComboBox<String> onDelete = actions();
	private final JComboBox<String> onUpdate = actions();

	private String suggestedName;
	private ForeignKey result;

	private ForeignKeyDialog(Window owner, Model model, ForeignKey initial) {
		super(owner, initial.name().isEmpty() ? "Add foreign key" : "Edit foreign key", ModalityType.APPLICATION_MODAL);
		this.from = initial.from();

		for (Object object : model.getObjects()) {
			if (object instanceof TableObject table) {
				referenced.addItem(table);
			}
		}
		referenced.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
				return super.getListCellRendererComponent(list, value instanceof TableObject table ? table.getName() : value, index, selected, focus);
			}
		});
		referenced.setSelectedItem(initial.to());

		for (int i = 0; i < initial.fromColumns().size(); i++) {
			pairs.addRow(new Object[]{initial.fromColumns().get(i), initial.toColumns().get(i)});
		}
		if (pairs.getRowCount() == 0) {
			pairs.addRow(new Object[]{firstColumn(from), primaryColumn(initial.to())});
		}

		suggestedName = ForeignKey.defaultName(from, String.valueOf(pairs.getValueAt(0, 0)));
		name.setText(initial.name().isEmpty() ? suggestedName : initial.name());
		onDelete.setSelectedItem(initial.onDelete() == null ? "" : initial.onDelete());
		onUpdate.setSelectedItem(initial.onUpdate() == null ? "" : initial.onUpdate());

		pairTable.setRowHeight(Math.max(pairTable.getRowHeight(), 24));
		pairTable.setFillsViewportHeight(true);
		pairTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(columnBox(from)));
		updateReferencedEditor();
		referenced.addActionListener(e -> referencedChanged());
		pairs.addTableModelListener(e -> renameIfSuggested());

		setContentPane(content());
		pack();
		setLocationRelativeTo(owner);
	}

	/**
	 * Shows the dialog and returns the key as it was edited, or null when the user cancels. A key that is not valid is reported and the dialog stays open.
	 * @param initial the key to edit; a new key has an empty name and the columns that were dragged (or none).
	 */
	public static ForeignKey edit(Component parent, Model model, ForeignKey initial) {
		ForeignKeyDialog dialog = new ForeignKeyDialog(SwingUtilities.getWindowAncestor(parent), model, initial);
		dialog.setVisible(true);
		return dialog.result;
	}

	private JComponent content() {
		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
		int row = 0;
		row = addRow(form, row, "Table", new JLabel(from.getName()));
		row = addRow(form, row, "References", referenced);
		row = addRow(form, row, "Name", name);

		JScrollPane scroll = new JScrollPane(pairTable);
		scroll.setPreferredSize(new java.awt.Dimension(360, 110));
		JButton add = new JButton("Add pair");
		JButton remove = new JButton("Remove pair");
		add.addActionListener(e -> pairs.addRow(new Object[]{firstColumn(from), primaryColumn(selectedTable())}));
		remove.addActionListener(e -> removeSelectedPair());
		JPanel pairButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pairButtons.add(add);
		pairButtons.add(javax.swing.Box.createHorizontalStrut(6));
		pairButtons.add(remove);
		JPanel columns = new JPanel(new BorderLayout(0, 6));
		columns.add(scroll, BorderLayout.CENTER);
		columns.add(pairButtons, BorderLayout.SOUTH);
		row = addRow(form, row, "Columns", columns);

		row = addRow(form, row, "On delete", onDelete);
		addRow(form, row, "On update", onUpdate);

		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		ok.addActionListener(e -> accept());
		cancel.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(ok);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(ok);
		buttons.add(cancel);

		JPanel content = new JPanel(new BorderLayout());
		content.add(form, BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		return content;
	}

	private static int addRow(JPanel form, int row, String label, JComponent field) {
		GridBagConstraints left = new GridBagConstraints();
		left.gridx = 0;
		left.gridy = row;
		left.anchor = GridBagConstraints.NORTHWEST;
		left.insets = new Insets(4, 0, 4, 10);
		form.add(new JLabel(label), left);

		GridBagConstraints right = new GridBagConstraints();
		right.gridx = 1;
		right.gridy = row;
		right.weightx = 1;
		right.fill = GridBagConstraints.HORIZONTAL;
		right.insets = new Insets(4, 0, 4, 0);
		form.add(field, right);
		return row + 1;
	}

	private void accept() {
		if (pairTable.isEditing()) {
			pairTable.getCellEditor().stopCellEditing();
		}

		List<String> fromColumns = new ArrayList<>();
		List<String> toColumns = new ArrayList<>();
		for (int i = 0; i < pairs.getRowCount(); i++) {
			fromColumns.add(text(pairs.getValueAt(i, 0)));
			toColumns.add(text(pairs.getValueAt(i, 1)));
		}

		ForeignKey key = new ForeignKey(from, fromColumns, selectedTable(), toColumns, name.getText().trim(), text(onDelete.getSelectedItem()),
			text(onUpdate.getSelectedItem()));
		try {
			key.validate();
			result = key;
			dispose();
		} catch (RuntimeException e) {
			ApplicationContext.get().errors().report(this, "Foreign key", e);
		}
	}

	private void referencedChanged() {
		if (pairTable.isEditing()) {
			pairTable.getCellEditor().cancelCellEditing();
		}
		updateReferencedEditor();
		String primary = primaryColumn(selectedTable());
		for (int i = 0; i < pairs.getRowCount(); i++) {
			pairs.setValueAt(i == 0 ? primary : "", i, 1);
		}
	}

	private void updateReferencedEditor() {
		TableObject table = selectedTable();
		pairTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(table == null ? new JComboBox<String>() : columnBox(table)));
	}

	/** The name follows the first column for as long as the user has not typed a name of their own. */
	private void renameIfSuggested() {
		if (pairs.getRowCount() > 0 && name.getText().equals(suggestedName)) {
			suggestedName = ForeignKey.defaultName(from, text(pairs.getValueAt(0, 0)));
			name.setText(suggestedName);
		}
	}

	private void removeSelectedPair() {
		int row = pairTable.getSelectedRow();
		if (row >= 0 && pairs.getRowCount() > 1) {
			if (pairTable.isEditing()) {
				pairTable.getCellEditor().cancelCellEditing();
			}
			pairs.removeRow(row);
		}
	}

	private TableObject selectedTable() {
		return (TableObject) referenced.getSelectedItem();
	}

	private static JComboBox<String> columnBox(TableObject table) {
		JComboBox<String> box = new JComboBox<>();
		for (Field field : table.getFields()) {
			box.addItem(field.getName());
		}
		return box;
	}

	private static JComboBox<String> actions() {
		JComboBox<String> box = new JComboBox<>();
		box.addItem("");
		for (String action : Dialect.REFERENTIAL_ACTIONS) {
			box.addItem(action);
		}
		box.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
				return super.getListCellRendererComponent(list, "".equals(value) ? "Server default" : value, index, selected, focus);
			}
		});
		return box;
	}

	private static String firstColumn(TableObject table) {
		Field[] fields = table.getFields();
		return fields.length == 0 ? "" : fields[0].getName();
	}

	/** The first primary key column of a table, the column a new key most likely refers to. */
	public static String primaryColumn(TableObject table) {
		if (table == null) {
			return "";
		}
		for (Field field : table.getFields()) {
			if (field.primary) {
				return field.getName();
			}
		}
		return firstColumn(table);
	}

	private static String text(Object value) {
		return value == null ? "" : value.toString().trim();
	}
}

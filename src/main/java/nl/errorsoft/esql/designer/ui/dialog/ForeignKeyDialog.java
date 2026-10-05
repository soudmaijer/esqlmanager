package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
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

import nl.errorsoft.esql.designer.model.DesignerForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** Edits a foreign key of the designer: the referenced table, the column pairs, the name and the actions. */
public class ForeignKeyDialog extends FormDialog {
	private final TableObject from;

	private final JComboBox<TableObject> referenced = new JComboBox<>();
	private final JTextField name = new JTextField(24);
	private final DefaultTableModel pairs = new DefaultTableModel(new Object[]{"Column", "Referenced column"}, 0);
	private final JTable pairTable = new JTable(pairs);
	private final JComboBox<String> onDelete = actions();
	private final JComboBox<String> onUpdate = actions();

	private final JButton removePair = Forms.button("Re&move pair");
	private final JLabel referencedHint = hint(" ");

	private String suggestedName;
	private DesignerForeignKey result;
	private DesignerForeignKey candidate;

	private ForeignKeyDialog(Window owner, Model model, DesignerForeignKey initial) {
		super(owner, initial.name().isEmpty() ? "Add foreign key" : "Edit foreign key", true);
		this.from = initial.from();

		for (Object object : model.getObjects()) {
			if (object instanceof TableObject table) {
				referenced.addItem(table);
			}
		}
		referenced.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
				String text = value instanceof TableObject table ? table.getName() + (table == from ? " (this table)" : "") : String.valueOf(value);
				return super.getListCellRendererComponent(list, text, index, selected, focus);
			}
		});
		referenced.setSelectedItem(initial.to());

		for (int i = 0; i < initial.fromColumns().size(); i++) {
			pairs.addRow(new Object[]{initial.fromColumns().get(i), initial.toColumns().get(i)});
		}
		if (pairs.getRowCount() == 0) {
			pairs.addRow(new Object[]{firstColumn(from), primaryColumn(initial.to())});
		}

		suggestedName = DesignerForeignKey.defaultName(from, String.valueOf(pairs.getValueAt(0, 0)));
		name.setText(initial.name().isEmpty() ? suggestedName : initial.name());
		onDelete.setSelectedItem(initial.onDelete() == null ? "" : initial.onDelete());
		onUpdate.setSelectedItem(initial.onUpdate() == null ? "" : initial.onUpdate());

		pairTable.setRowHeight(Math.max(pairTable.getRowHeight(), 24));
		pairTable.setFillsViewportHeight(true);
		pairTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(columnBox(from)));
		updateReferencedEditor();
		referenced.addActionListener(e -> referencedChanged());
		pairs.addTableModelListener(e -> {
			renameIfSuggested();
			removePair.setEnabled(pairs.getRowCount() > 1);
		});
		removePair.setEnabled(pairs.getRowCount() > 1);
		showReferencedHint();

		setOkCancel(content(), initial.name().isEmpty() ? "&Add" : "&Save", "Cancel");
		setValidator(this::check);
		setOnAccept(() -> result = candidate);
		setInitialFocus(referenced);
		setResizable(true);
	}

	/**
	 * Shows the dialog and returns the key as it was edited, or null when the user cancels. A key that is not valid is reported and the dialog stays open.
	 * @param initial the key to edit; a new key has an empty name and the columns that were dragged (or none).
	 */
	public static DesignerForeignKey edit(Component parent, Model model, DesignerForeignKey initial) {
		ForeignKeyDialog dialog = new ForeignKeyDialog(SwingUtilities.getWindowAncestor(parent), model, initial);
		return dialog.showDialog() ? dialog.result : null;
	}

	private JComponent content() {
		JScrollPane scroll = new JScrollPane(pairTable);
		scroll.setPreferredSize(new java.awt.Dimension(360, 110));
		JButton add = Forms.button("Add &pair");
		JButton remove = removePair;
		add.addActionListener(e -> pairs.addRow(new Object[]{firstColumn(from), primaryColumn(selectedTable())}));
		remove.addActionListener(e -> removeSelectedPair());
		JPanel pairButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		pairButtons.add(add);
		pairButtons.add(remove);
		JPanel columns = new JPanel(new BorderLayout(0, Forms.GAP));
		columns.add(scroll, BorderLayout.CENTER);
		columns.add(pairButtons, BorderLayout.SOUTH);

		return new Forms.Grid().row(new JLabel("Table:"), new JLabel(from.getName())).row("&References:", referenced).full(referencedHint).row("&Name:", name)
			.area(Forms.label("Co&lumns:", pairTable), columns).row("On &delete:", onDelete).row("On &update:", onUpdate)
			.full(hint("Server default: the server decides, usually NO ACTION (the change is refused while rows still refer to it).")).panel();
	}

	private static JLabel hint(String text) {
		JLabel label = new JLabel(text);
		java.awt.Color disabled = javax.swing.UIManager.getColor("Label.disabledForeground");
		if (disabled != null) {
			label.setForeground(disabled);
		}
		label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() - 1f));
		return label;
	}

	/** Says when the key refers to its own table, which is the only choice when the model has one table. */
	private void showReferencedHint() {
		boolean self = selectedTable() == from;
		boolean only = referenced.getItemCount() == 1;
		referencedHint.setText(
			self ? (only ? "The model has only this table, so the key refers to a column of the table itself." : "The key refers to the table itself.") : " ");
	}

	/** Builds the key from the dialog and checks it; the message to show, null when the key is valid. */
	private String check() {
		if (pairTable.isEditing()) {
			pairTable.getCellEditor().stopCellEditing();
		}

		List<String> fromColumns = new ArrayList<>();
		List<String> toColumns = new ArrayList<>();
		for (int i = 0; i < pairs.getRowCount(); i++) {
			fromColumns.add(text(pairs.getValueAt(i, 0)));
			toColumns.add(text(pairs.getValueAt(i, 1)));
		}

		DesignerForeignKey key = new DesignerForeignKey(from, fromColumns, selectedTable(), toColumns, name.getText().trim(), text(onDelete.getSelectedItem()),
			text(onUpdate.getSelectedItem()));
		try {
			key.validate();
			candidate = key;
			return null;
		} catch (EsqlException e) {
			// validate() reports what is wrong with the input this way; anything else is unexpected and goes to the error handler.
			return e.getMessage();
		}
	}

	private void referencedChanged() {
		showReferencedHint();
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
			suggestedName = DesignerForeignKey.defaultName(from, text(pairs.getValueAt(0, 0)));
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
		for (DesignerColumn field : table.getFields()) {
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
		DesignerColumn[] fields = table.getFields();
		return fields.length == 0 ? "" : fields[0].getName();
	}

	/** The first primary key column of a table, the column a new key most likely refers to. */
	public static String primaryColumn(TableObject table) {
		if (table == null) {
			return "";
		}
		for (DesignerColumn field : table.getFields()) {
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

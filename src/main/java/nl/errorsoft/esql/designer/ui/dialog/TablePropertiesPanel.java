package nl.errorsoft.esql.designer.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.designer.model.DesignerForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.ui.util.Forms;

/** The properties of a table in the designer: General, Fields (the list of fields and one form for the selected field) and Foreign keys. */
public class TablePropertiesPanel extends JTabbedPane implements PropertiesPanel {
	// General
	private final JTextField txt_name = new JTextField();
	private final JTextField txt_comm = new JTextField();
	private final JTextArea txt_desc = new JTextArea();
	private final JComboBox<String> cmb_type = new JComboBox<>();

	// Fields
	private final JList<DesignerColumn> lst_fields = new JList<>(new DefaultListModel<>());
	private final JTextField txt_fieldname = new JTextField();
	private final JComboBox<DataType> cmb_types = new JComboBox<>();
	private final JTextField txt_length = new JTextField();
	private final JTextField txt_default = new JTextField();
	private final JTextArea txt_fieldcomm = new JTextArea();

	private final JCheckBox primary = Forms.mnemonic(new JCheckBox(), "&Primary Key");
	private final JCheckBox notnull = Forms.mnemonic(new JCheckBox(), "N&ot null");
	private final JCheckBox unique = Forms.mnemonic(new JCheckBox(), "&Unique");
	private final JCheckBox autoincrement = Forms.mnemonic(new JCheckBox(), "&Auto Increment");
	private final JCheckBox index = Forms.mnemonic(new JCheckBox(), "&Index");
	private final JCheckBox unsigned = Forms.mnemonic(new JCheckBox(), "U&nsigned");
	private final JCheckBox binary = Forms.mnemonic(new JCheckBox(), "&Binary");
	private final JCheckBox zerofill = Forms.mnemonic(new JCheckBox(), "&Zerofill");

	private JPanel fieldForm;
	private DesignerColumn selField = null;
	/** True while the form is filled from a field, so that filling it does not write back. */
	private boolean loading;

	private final JList<DesignerForeignKey> lst_keys = new JList<>();

	private final TableObject tb;

	// The model keeps its foreign keys in step with renamed and removed fields.
	private final Model model;
	private final Map<DesignerColumn, String> namesBefore = new IdentityHashMap<>();

	public TablePropertiesPanel(TableObject tb, ServerType serverType, Model model) {
		this.model = model;
		this.tb = tb;

		addTab("General", generalTab(tb, serverType));
		addTab("Fields", fieldsTab(serverType));
		if (model != null) {
			addTab("Foreign keys", foreignKeysTab());
		}

		DefaultListModel<DesignerColumn> dlm = (DefaultListModel<DesignerColumn>) lst_fields.getModel();
		for (DesignerColumn original : tb.getFields()) {
			// The dialog edits copies, the table gets them on OK.
			DesignerColumn copy = original.copy();
			dlm.addElement(copy);
			namesBefore.put(copy, original.getName());
		}
		if (!dlm.isEmpty()) {
			lst_fields.setSelectedIndex(0);
		} else {
			showField(null);
		}
	}

	private JPanel generalTab(TableObject tb, ServerType serverType) {
		txt_name.setText(tb.getName());
		txt_comm.setText(tb.getComment());
		String[] tableTypes = serverType.getDialect().getTableTypes();
		for (String tableType : tableTypes) {
			cmb_type.addItem(tableType);
		}
		cmb_type.setSelectedItem(tb.getType());
		txt_desc.setFont(txt_name.getFont());
		txt_desc.setLineWrap(true);
		txt_desc.setWrapStyleWord(true);
		txt_desc.setText(tb.getDescription());
		JScrollPane notes = new JScrollPane(txt_desc);
		notes.setPreferredSize(new Dimension(300, 120));

		Forms.Grid grid = new Forms.Grid().row("&Name:", txt_name);
		// Servers without storage engines have nothing to choose here.
		if (tableTypes.length > 0) {
			grid.row("&Type:", cmb_type);
		}
		grid.row("&Comment:", txt_comm).area("N&otes:", notes);
		txt_comm.setToolTipText("The comment of the table in the database");
		txt_desc.setToolTipText("Notes that only the model file keeps, they are not written to the database");
		return Forms.padded(grid.panel());
	}

	private JPanel fieldsTab(ServerType serverType) {
		for (DataType type : serverType.getDataTypes()) {
			cmb_types.addItem(type);
		}

		// The type options exist for the servers whose data types have them.
		boolean hasUnsigned = false;
		boolean hasBinary = false;
		boolean hasZerofill = false;
		for (DataType type : serverType.getDataTypes()) {
			hasUnsigned |= type.unsigned;
			hasBinary |= type.binary;
			hasZerofill |= type.zerofill;
		}
		unsigned.setVisible(hasUnsigned);
		binary.setVisible(hasBinary);
		zerofill.setVisible(hasZerofill);

		JPanel constraints = Forms.titled(new JPanel(new GridLayout(0, 2, Forms.GAP, 0)), "Constraints");
		constraints.add(primary);
		constraints.add(notnull);
		constraints.add(unique);
		constraints.add(autoincrement);
		constraints.add(index);

		Forms.Grid grid = new Forms.Grid();
		txt_fieldcomm.setWrapStyleWord(true);
		txt_fieldcomm.setLineWrap(true);
		txt_fieldcomm.setFont(txt_fieldname.getFont());
		JScrollPane comment = new JScrollPane(txt_fieldcomm);
		comment.setPreferredSize(new Dimension(300, 60));
		grid.row("Na&me:", txt_fieldname).row("T&ype:", cmb_types).row("&Length:", txt_length).row("&Default:", txt_default).area("Co&mment:", comment);
		grid.full(constraints);
		if (hasUnsigned || hasBinary || hasZerofill) {
			JPanel options = Forms.titled(new JPanel(new GridLayout(0, 3, Forms.GAP, 0)), "Type options");
			options.add(unsigned);
			options.add(binary);
			options.add(zerofill);
			grid.full(options);
		}
		fieldForm = grid.panel();

		bind(txt_fieldname, text -> {
			if (!text.trim().isEmpty()) {
				selField.setName(text);
			}
			markNameProblem();
		});
		bind(txt_length, text -> {
			selField.setLength(text);
			markLengthProblem();
		});
		bind(txt_default, text -> selField.setDefault(text));
		bind(txt_fieldcomm, text -> selField.setComment(text));
		cmb_types.addActionListener(e -> typeChosen());
		bind(primary, value -> selField.primary = value);
		bind(unique, value -> selField.unique = value);
		bind(index, value -> selField.index = value);
		bind(notnull, value -> selField.notnull = value);
		bind(unsigned, value -> selField.unsigned = value);
		bind(binary, value -> selField.binary = value);
		bind(autoincrement, value -> selField.autoincrement = value);
		bind(zerofill, value -> selField.zerofill = value);
		lst_fields.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lst_fields.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				showField(lst_fields.getSelectedValue());
			}
		});

		JButton add = Forms.button("&Add");
		JButton duplicate = Forms.button("D&uplicate");
		JButton remove = Forms.button("&Remove");
		JButton up = Forms.button("U&p");
		JButton down = Forms.button("Do&wn");
		add.addActionListener(e -> addField());
		duplicate.addActionListener(e -> duplicateField());
		remove.addActionListener(e -> removeField());
		up.addActionListener(e -> moveField(-1));
		down.addActionListener(e -> moveField(1));

		JPanel buttons = new JPanel(new GridLayout(0, 3, Forms.GAP, Forms.GAP));
		buttons.setBorder(BorderFactory.createEmptyBorder(Forms.GAP, 0, 0, 0));
		buttons.add(add);
		buttons.add(duplicate);
		buttons.add(remove);
		buttons.add(up);
		buttons.add(down);

		JScrollPane list = new JScrollPane(lst_fields);
		list.setPreferredSize(new Dimension(200, 200));
		JPanel left = new JPanel(new BorderLayout());
		left.add(Forms.label("&Fields:", lst_fields), BorderLayout.NORTH);
		left.add(list, BorderLayout.CENTER);
		left.add(buttons, BorderLayout.SOUTH);

		JPanel panel = Forms.padded(new JPanel(new BorderLayout(Forms.PADDING, 0)));
		panel.add(left, BorderLayout.WEST);
		panel.add(fieldForm, BorderLayout.CENTER);
		return panel;
	}

	private void bind(javax.swing.text.JTextComponent component, Consumer<String> write) {
		component.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				changed();
			}

			private void changed() {
				if (!loading && selField != null) {
					write.accept(component.getText());
					lst_fields.repaint();
				}
			}
		});
	}

	private void bind(JCheckBox box, Consumer<Boolean> write) {
		box.addActionListener(e -> {
			if (!loading && selField != null) {
				write.accept(box.isSelected());
			}
		});
	}

	private void markNameProblem() {
		List<String> names = fieldNames();
		boolean problem = txt_fieldname.getText().isBlank()
			|| FieldRules.nameProblem(names) != null && names.stream().filter(n -> n.equalsIgnoreCase(txt_fieldname.getText().trim())).count() > 1;
		txt_fieldname.putClientProperty("JComponent.outline", problem ? "error" : null);
	}

	private void markLengthProblem() {
		DataType type = (DataType) cmb_types.getSelectedItem();
		String problem = FieldRules.lengthProblem(type == null ? "" : type.getName(), txt_length.getText());
		txt_length.putClientProperty("JComponent.outline", problem == null ? null : "error");
		txt_length.setToolTipText(problem);
	}

	private List<String> fieldNames() {
		List<String> names = new ArrayList<>();
		DefaultListModel<DesignerColumn> fields = fields();
		for (int i = 0; i < fields.getSize(); i++) {
			names.add(fields.get(i).getName());
		}
		return names;
	}

	private DefaultListModel<DesignerColumn> fields() {
		return (DefaultListModel<DesignerColumn>) lst_fields.getModel();
	}

	private void addField() {
		DesignerColumn field = new DesignerColumn(FieldRules.uniqueName("new_field", fieldNames()), cmb_types.getItemAt(0), "", "", "");
		fields().addElement(field);
		lst_fields.setSelectedIndex(fields().getSize() - 1);
		txt_fieldname.requestFocusInWindow();
		txt_fieldname.selectAll();
	}

	private void duplicateField() {
		DesignerColumn source = lst_fields.getSelectedValue();
		if (source == null) {
			return;
		}
		DesignerColumn copy = source.copy();
		copy.setName(FieldRules.uniqueName(source.getName() + "_copy", fieldNames()));
		// A copy cannot be a second primary key or auto number.
		copy.primary = false;
		copy.autoincrement = false;
		int at = lst_fields.getSelectedIndex() + 1;
		fields().add(at, copy);
		lst_fields.setSelectedIndex(at);
		txt_fieldname.requestFocusInWindow();
		txt_fieldname.selectAll();
	}

	private void removeField() {
		int at = lst_fields.getSelectedIndex();
		if (at < 0) {
			return;
		}
		String name = fields().get(at).getName();
		if (!Dialogs.confirmDestructive(this, "Remove field", "Remove field '" + name + "' from the table?", "Remove")) {
			return;
		}
		fields().remove(at);
		if (!fields().isEmpty()) {
			lst_fields.setSelectedIndex(Math.min(at, fields().getSize() - 1));
		} else {
			showField(null);
		}
	}

	private void moveField(int step) {
		int at = lst_fields.getSelectedIndex();
		int to = at + step;
		if (at < 0 || to < 0 || to >= fields().getSize()) {
			return;
		}
		DesignerColumn field = fields().remove(at);
		fields().add(to, field);
		lst_fields.setSelectedIndex(to);
	}

	/** Fills the form from a field, or disables it when there is none. */
	private void showField(DesignerColumn field) {
		selField = field;
		setEnabledDeep(fieldForm, field != null);
		if (field == null) {
			loading = true;
			txt_fieldname.setText("");
			txt_length.setText("");
			txt_default.setText("");
			txt_fieldcomm.setText("");
			for (JCheckBox box : List.of(primary, notnull, unique, autoincrement, index, unsigned, binary, zerofill)) {
				box.setSelected(false);
			}
			loading = false;
			return;
		}
		loading = true;
		for (int i = 0; i < cmb_types.getItemCount(); i++) {
			if (cmb_types.getItemAt(i).getName().equals(field.getType().getName())) {
				cmb_types.setSelectedIndex(i);
				break;
			}
		}
		txt_fieldname.setText(field.getName());
		txt_length.setText(field.getLength());
		txt_default.setText(field.getDefault());
		txt_fieldcomm.setText(field.getComment());
		primary.setSelected(field.primary);
		notnull.setSelected(field.notnull);
		unique.setSelected(field.unique);
		autoincrement.setSelected(field.autoincrement);
		index.setSelected(field.index);
		unsigned.setSelected(field.unsigned);
		binary.setSelected(field.binary);
		zerofill.setSelected(field.zerofill);
		loading = false;
		enableOptionsFor((DataType) cmb_types.getSelectedItem());
		markNameProblem();
		markLengthProblem();
	}

	/** The type was chosen: options the type does not have are switched off. */
	private void typeChosen() {
		DataType type = (DataType) cmb_types.getSelectedItem();
		if (loading || selField == null || type == null) {
			return;
		}
		selField.setType(type);
		loading = true;
		clearUnless(primary, type.primary, value -> selField.primary = value);
		clearUnless(index, type.index, value -> selField.index = value);
		clearUnless(unique, type.unique, value -> selField.unique = value);
		clearUnless(binary, type.binary, value -> selField.binary = value);
		clearUnless(notnull, type.notnull, value -> selField.notnull = value);
		clearUnless(unsigned, type.unsigned, value -> selField.unsigned = value);
		clearUnless(autoincrement, type.autoincrement, value -> selField.autoincrement = value);
		clearUnless(zerofill, type.zerofill, value -> selField.zerofill = value);
		loading = false;
		enableOptionsFor(type);
		markLengthProblem();
		lst_fields.repaint();
	}

	private static void clearUnless(JCheckBox box, boolean allowed, Consumer<Boolean> write) {
		if (!allowed) {
			box.setSelected(false);
			write.accept(false);
		}
	}

	private void enableOptionsFor(DataType type) {
		if (type == null) {
			return;
		}
		primary.setEnabled(type.primary);
		binary.setEnabled(type.binary);
		unsigned.setEnabled(type.unsigned);
		zerofill.setEnabled(type.zerofill);
		index.setEnabled(type.index);
		unique.setEnabled(type.unique);
		notnull.setEnabled(type.notnull);
		autoincrement.setEnabled(type.autoincrement);
	}

	private static void setEnabledDeep(Container container, boolean enabled) {
		for (Component child : container.getComponents()) {
			child.setEnabled(enabled);
			if (child instanceof Container inner) {
				setEnabledDeep(inner, enabled);
			}
		}
	}

	/** The foreign keys of this table on other tables. A change is made in the model straight away. */
	private JPanel foreignKeysTab() {
		JPanel panel = new JPanel(new BorderLayout(0, 6));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.add(new JScrollPane(lst_keys), BorderLayout.CENTER);
		lst_keys.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int i, boolean selected, boolean focus) {
				DesignerForeignKey key = (DesignerForeignKey) value;
				String text = key.name() + ": " + String.join(", ", key.fromColumns()) + " -> " + key.to().getName() + "(" + String.join(", ", key.toColumns())
					+ ")";
				return super.getListCellRendererComponent(list, text, i, selected, focus);
			}
		});

		JButton add = new JButton("Add...");
		JButton edit = new JButton("Edit...");
		JButton remove = new JButton("Remove");
		add.addActionListener(e -> {
			DesignerColumn[] f = tb.getFields();
			String column = f.length == 0 ? "" : f[0].getName();
			TableObject parent = tb;
			for (Object object : model.getObjects()) {
				if (object instanceof TableObject other && other != tb) {
					parent = other;
					break;
				}
			}
			DesignerForeignKey key = ForeignKeyDialog.edit(this, model,
				new DesignerForeignKey(tb, column.isEmpty() ? java.util.List.of() : java.util.List.of(column), parent,
					java.util.List.of(), "", "", ""));
			if (key != null) {
				model.addForeignKey(key);
			}
			refreshKeys();
		});
		edit.addActionListener(e -> {
			DesignerForeignKey key = lst_keys.getSelectedValue();
			if (key != null) {
				DesignerForeignKey edited = ForeignKeyDialog.edit(this, model, key);
				if (edited != null) {
					model.removeForeignKey(key);
					model.addForeignKey(edited);
				}
				refreshKeys();
			}
		});
		remove.addActionListener(e -> {
			DesignerForeignKey key = lst_keys.getSelectedValue();
			if (key != null) {
				model.removeForeignKey(key);
				refreshKeys();
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		buttons.add(add);
		buttons.add(edit);
		buttons.add(remove);
		panel.add(buttons, BorderLayout.SOUTH);
		refreshKeys();
		return panel;
	}

	private void refreshKeys() {
		DefaultListModel<DesignerForeignKey> keys = new DefaultListModel<>();
		for (DesignerForeignKey key : model.foreignKeysOf(tb)) {
			if (key.from() == tb) {
				keys.addElement(key);
			}
		}
		lst_keys.setModel(keys);
	}

	@Override
	public String inputProblem() {
		String name = txt_name.getText().trim();
		String nameProblem = model == null ? (name.isEmpty() ? "The table needs a name." : null) : ObjectNames.tableProblem(model, tb, name);
		if (nameProblem != null) {
			setSelectedIndex(0);
			return nameProblem;
		}
		String names = FieldRules.nameProblem(fieldNames());
		if (names != null) {
			setSelectedIndex(1);
			return names;
		}
		DefaultListModel<DesignerColumn> fields = fields();
		for (int i = 0; i < fields.getSize(); i++) {
			DesignerColumn field = fields.get(i);
			String length = FieldRules.lengthProblem(field.getType().getName(), field.getLength());
			if (length != null) {
				setSelectedIndex(1);
				lst_fields.setSelectedIndex(i);
				return "DesignerColumn '" + field.getName() + "': " + length;
			}
		}
		return null;
	}

	@Override
	public void saveProperties() {
		tb.setName(txt_name.getText());
		tb.setDescription(txt_desc.getText());
		tb.setType(cmb_type.getSelectedItem() == null ? "" : cmb_type.getSelectedItem().toString());
		tb.setComment(txt_comm.getText());

		tb.removeAllFields();

		DefaultListModel<DesignerColumn> dtm = fields();
		for (int i = 0; i < dtm.getSize(); i++) {
			tb.addField(dtm.getElementAt(i));
		}

		if (model != null) {
			model.fieldsEdited(tb, namesBefore);
		}
	}
}

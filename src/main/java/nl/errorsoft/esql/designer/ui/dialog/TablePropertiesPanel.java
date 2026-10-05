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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.errorsoft.esql.designer.FieldRules;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.designer.model.ModelForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.ui.util.Forms;

/** The properties of a table in the designer: General, Fields (the list of fields and one form for the selected field) and Foreign keys. */
public class TablePropertiesPanel extends JTabbedPane implements PropertiesPanel {
	// General
	private final JTextField nameField = new JTextField();
	private final JTextField commentField = new JTextField();
	private final JTextArea descriptionArea = new JTextArea();
	private final JComboBox<String> typeCombo = new JComboBox<>();

	// Fields
	private final JList<DesignerColumn> fieldList = new JList<>(new DefaultListModel<>());
	private final JTextField fieldNameField = new JTextField();
	private final JComboBox<DataType> dataTypeCombo = new JComboBox<>();
	private final JTextField lengthField = new JTextField();
	private final JTextField defaultValueField = new JTextField();
	private final JTextArea fieldCommentArea = new JTextArea();

	private final JCheckBox primary = Forms.mnemonic(new JCheckBox(), "&Primary key");
	private final JCheckBox notNull = Forms.mnemonic(new JCheckBox(), "N&ot null");
	private final JCheckBox unique = Forms.mnemonic(new JCheckBox(), "&Unique");
	private final JCheckBox autoIncrement = Forms.mnemonic(new JCheckBox(), "&Auto increment");
	private final JCheckBox index = Forms.mnemonic(new JCheckBox(), "&Index");
	private final JCheckBox unsigned = Forms.mnemonic(new JCheckBox(), "U&nsigned");
	private final JCheckBox binary = Forms.mnemonic(new JCheckBox(), "&Binary");
	private final JCheckBox zerofill = Forms.mnemonic(new JCheckBox(), "&Zerofill");

	private JPanel fieldForm;
	private DesignerColumn selectedColumn = null;
	/** True while the form is filled from a field, so that filling it does not write back. */
	private boolean loading;

	private final JList<ModelForeignKey> keyList = new JList<>(new DefaultListModel<>());
	/** The foreign keys of this table as they were, and as edited in the tab; the model gets the edited ones on OK, Cancel leaves it alone. */
	private final List<ModelForeignKey> keysBefore = new ArrayList<>();

	private final TableCard tableCard;

	// The model keeps its foreign keys in step with renamed and removed fields.
	private final Model model;
	private final Map<DesignerColumn, String> namesBefore = new IdentityHashMap<>();

	public TablePropertiesPanel(TableCard tableCard, ServerType serverType, Model model) {
		this.model = model;
		this.tableCard = tableCard;

		addTab("General", generalTab(tableCard, serverType));
		addTab("Fields", fieldsTab(serverType));
		if (model != null) {
			addTab("Foreign keys", foreignKeysTab());
		}

		DefaultListModel<DesignerColumn> fieldModel = (DefaultListModel<DesignerColumn>) fieldList.getModel();
		for (DesignerColumn original : tableCard.getFields()) {
			// The dialog edits copies, the table gets them on OK.
			DesignerColumn copy = original.copy();
			fieldModel.addElement(copy);
			namesBefore.put(copy, original.getName());
		}
		if (!fieldModel.isEmpty()) {
			fieldList.setSelectedIndex(0);
		} else {
			showField(null);
		}
	}

	private JPanel generalTab(TableCard tableCard, ServerType serverType) {
		nameField.setText(tableCard.getName());
		commentField.setText(tableCard.getComment());
		String[] tableTypes = serverType.getDialect().getTableTypes();
		for (String tableType : tableTypes) {
			typeCombo.addItem(tableType);
		}
		typeCombo.setSelectedItem(tableCard.getType());
		descriptionArea.setFont(nameField.getFont());
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setText(tableCard.getDescription());
		JScrollPane notes = new JScrollPane(descriptionArea);
		notes.setPreferredSize(new Dimension(300, 120));

		Forms.Grid grid = new Forms.Grid().row("&Name:", nameField);
		// Servers without storage engines have nothing to choose here.
		if (tableTypes.length > 0) {
			grid.row("&Type:", typeCombo);
		}
		grid.row("&Comment:", commentField).area("N&otes:", notes);
		commentField.setToolTipText("The comment of the table in the database");
		descriptionArea.setToolTipText("Notes that only the model file keeps, they are not written to the database");
		return Forms.padded(grid.panel());
	}

	private JPanel fieldsTab(ServerType serverType) {
		for (DataType type : serverType.getDataTypes()) {
			dataTypeCombo.addItem(type);
		}

		// The type options exist for the servers whose data types have them.
		boolean hasUnsigned = false;
		boolean hasBinary = false;
		boolean hasZerofill = false;
		for (DataType type : serverType.getDataTypes()) {
			hasUnsigned |= type.allows(DataType.Option.UNSIGNED);
			hasBinary |= type.allows(DataType.Option.BINARY);
			hasZerofill |= type.allows(DataType.Option.ZEROFILL);
		}
		unsigned.setVisible(hasUnsigned);
		binary.setVisible(hasBinary);
		zerofill.setVisible(hasZerofill);

		JPanel constraints = Forms.titled(new JPanel(new GridLayout(0, 2, Forms.GAP, 0)), "Constraints");
		constraints.add(primary);
		constraints.add(notNull);
		constraints.add(unique);
		constraints.add(autoIncrement);
		constraints.add(index);

		Forms.Grid grid = new Forms.Grid();
		fieldCommentArea.setWrapStyleWord(true);
		fieldCommentArea.setLineWrap(true);
		fieldCommentArea.setFont(fieldNameField.getFont());
		JScrollPane comment = new JScrollPane(fieldCommentArea);
		comment.setPreferredSize(new Dimension(300, 60));
		grid.row("Na&me:", fieldNameField).row("T&ype:", dataTypeCombo).row("&Length:", lengthField).row("&Default:", defaultValueField).area("Co&mment:",
			comment);
		grid.full(constraints);
		if (hasUnsigned || hasBinary || hasZerofill) {
			JPanel options = Forms.titled(new JPanel(new GridLayout(0, 3, Forms.GAP, 0)), "Type options");
			options.add(unsigned);
			options.add(binary);
			options.add(zerofill);
			grid.full(options);
		}
		fieldForm = grid.panel();

		bind(fieldNameField, text -> {
			if (!text.trim().isEmpty()) {
				selectedColumn.setName(text);
			}
			markNameProblem();
		});
		bind(lengthField, text -> {
			selectedColumn.setLength(text);
			markLengthProblem();
		});
		bind(defaultValueField, text -> selectedColumn.setDefault(text));
		bind(fieldCommentArea, text -> selectedColumn.setComment(text));
		dataTypeCombo.addActionListener(e -> typeChosen());
		bind(primary, value -> selectedColumn.primary = value);
		bind(unique, value -> selectedColumn.unique = value);
		bind(index, value -> selectedColumn.index = value);
		bind(notNull, value -> selectedColumn.notNull = value);
		bind(unsigned, value -> selectedColumn.unsigned = value);
		bind(binary, value -> selectedColumn.binary = value);
		bind(autoIncrement, value -> selectedColumn.autoIncrement = value);
		bind(zerofill, value -> selectedColumn.zerofill = value);
		fieldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fieldList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				showField(fieldList.getSelectedValue());
			}
		});

		JButton addButton = Forms.button("&Add");
		JButton duplicate = Forms.button("D&uplicate");
		JButton remove = Forms.button("&Remove");
		JButton up = Forms.button("U&p");
		JButton down = Forms.button("Do&wn");
		addButton.addActionListener(e -> addField());
		duplicate.addActionListener(e -> duplicateField());
		remove.addActionListener(e -> removeField());
		up.addActionListener(e -> moveField(-1));
		down.addActionListener(e -> moveField(1));

		JPanel buttons = new JPanel(new GridLayout(0, 3, Forms.GAP, Forms.GAP));
		buttons.setBorder(BorderFactory.createEmptyBorder(Forms.GAP, 0, 0, 0));
		buttons.add(addButton);
		buttons.add(duplicate);
		buttons.add(remove);
		buttons.add(up);
		buttons.add(down);

		JScrollPane list = new JScrollPane(fieldList);
		list.setPreferredSize(new Dimension(200, 200));
		JPanel left = new JPanel(new BorderLayout());
		left.add(Forms.label("&Fields:", fieldList), BorderLayout.NORTH);
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
				if (!loading && selectedColumn != null) {
					write.accept(component.getText());
					fieldList.repaint();
				}
			}
		});
	}

	private void bind(JCheckBox box, Consumer<Boolean> write) {
		box.addActionListener(e -> {
			if (!loading && selectedColumn != null) {
				write.accept(box.isSelected());
			}
		});
	}

	private void markNameProblem() {
		List<String> names = fieldNames();
		boolean problem = fieldNameField.getText().isBlank()
			|| FieldRules.nameProblem(names) != null && names.stream().filter(n -> n.equalsIgnoreCase(fieldNameField.getText().trim())).count() > 1;
		fieldNameField.putClientProperty("JComponent.outline", problem ? "error" : null);
	}

	private void markLengthProblem() {
		DataType type = (DataType) dataTypeCombo.getSelectedItem();
		String problem = FieldRules.lengthProblem(type == null ? "" : type.getName(), lengthField.getText());
		lengthField.putClientProperty("JComponent.outline", problem == null ? null : "error");
		lengthField.setToolTipText(problem);
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
		return (DefaultListModel<DesignerColumn>) fieldList.getModel();
	}

	private void addField() {
		DesignerColumn field = new DesignerColumn(FieldRules.uniqueName("new_field", fieldNames()), dataTypeCombo.getItemAt(0), "", "", "");
		fields().addElement(field);
		fieldList.setSelectedIndex(fields().getSize() - 1);
		fieldNameField.requestFocusInWindow();
		fieldNameField.selectAll();
	}

	private void duplicateField() {
		DesignerColumn source = fieldList.getSelectedValue();
		if (source == null) {
			return;
		}
		DesignerColumn copy = source.copy();
		copy.setName(FieldRules.uniqueName(source.getName() + "_copy", fieldNames()));
		// A copy cannot be a second primary key or auto number.
		copy.primary = false;
		copy.autoIncrement = false;
		int at = fieldList.getSelectedIndex() + 1;
		fields().add(at, copy);
		fieldList.setSelectedIndex(at);
		fieldNameField.requestFocusInWindow();
		fieldNameField.selectAll();
	}

	private void removeField() {
		int at = fieldList.getSelectedIndex();
		if (at < 0) {
			return;
		}
		String name = fields().get(at).getName();
		if (!Dialogs.confirmDestructive(this, "Remove field", "Remove field '" + name + "' from the table?", "Remove")) {
			return;
		}
		fields().remove(at);
		if (!fields().isEmpty()) {
			fieldList.setSelectedIndex(Math.min(at, fields().getSize() - 1));
		} else {
			showField(null);
		}
	}

	private void moveField(int step) {
		int at = fieldList.getSelectedIndex();
		int to = at + step;
		if (at < 0 || to < 0 || to >= fields().getSize()) {
			return;
		}
		DesignerColumn field = fields().remove(at);
		fields().add(to, field);
		fieldList.setSelectedIndex(to);
	}

	/** Fills the form from a field, or disables it when there is none. */
	private void showField(DesignerColumn field) {
		selectedColumn = field;
		setEnabledDeep(fieldForm, field != null);
		if (field == null) {
			loading = true;
			fieldNameField.setText("");
			lengthField.setText("");
			defaultValueField.setText("");
			fieldCommentArea.setText("");
			for (JCheckBox box : List.of(primary, notNull, unique, autoIncrement, index, unsigned, binary, zerofill)) {
				box.setSelected(false);
			}
			loading = false;
			return;
		}
		loading = true;
		for (int i = 0; i < dataTypeCombo.getItemCount(); i++) {
			if (dataTypeCombo.getItemAt(i).getName().equals(field.getType().getName())) {
				dataTypeCombo.setSelectedIndex(i);
				break;
			}
		}
		fieldNameField.setText(field.getName());
		lengthField.setText(field.getLength());
		defaultValueField.setText(field.getDefault());
		fieldCommentArea.setText(field.getComment());
		primary.setSelected(field.primary);
		notNull.setSelected(field.notNull);
		unique.setSelected(field.unique);
		autoIncrement.setSelected(field.autoIncrement);
		index.setSelected(field.index);
		unsigned.setSelected(field.unsigned);
		binary.setSelected(field.binary);
		zerofill.setSelected(field.zerofill);
		loading = false;
		enableOptionsFor((DataType) dataTypeCombo.getSelectedItem());
		markNameProblem();
		markLengthProblem();
	}

	/** The type was chosen: options the type does not have are switched off. */
	private void typeChosen() {
		DataType type = (DataType) dataTypeCombo.getSelectedItem();
		if (loading || selectedColumn == null || type == null) {
			return;
		}
		selectedColumn.setType(type);
		loading = true;
		clearUnless(primary, type.allows(DataType.Option.PRIMARY), value -> selectedColumn.primary = value);
		clearUnless(index, type.allows(DataType.Option.INDEX), value -> selectedColumn.index = value);
		clearUnless(unique, type.allows(DataType.Option.UNIQUE), value -> selectedColumn.unique = value);
		clearUnless(binary, type.allows(DataType.Option.BINARY), value -> selectedColumn.binary = value);
		clearUnless(notNull, type.allows(DataType.Option.NOT_NULL), value -> selectedColumn.notNull = value);
		clearUnless(unsigned, type.allows(DataType.Option.UNSIGNED), value -> selectedColumn.unsigned = value);
		clearUnless(autoIncrement, type.allows(DataType.Option.AUTO_INCREMENT), value -> selectedColumn.autoIncrement = value);
		clearUnless(zerofill, type.allows(DataType.Option.ZEROFILL), value -> selectedColumn.zerofill = value);
		loading = false;
		enableOptionsFor(type);
		markLengthProblem();
		fieldList.repaint();
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
		primary.setEnabled(type.allows(DataType.Option.PRIMARY));
		binary.setEnabled(type.allows(DataType.Option.BINARY));
		unsigned.setEnabled(type.allows(DataType.Option.UNSIGNED));
		zerofill.setEnabled(type.allows(DataType.Option.ZEROFILL));
		index.setEnabled(type.allows(DataType.Option.INDEX));
		unique.setEnabled(type.allows(DataType.Option.UNIQUE));
		notNull.setEnabled(type.allows(DataType.Option.NOT_NULL));
		autoIncrement.setEnabled(type.allows(DataType.Option.AUTO_INCREMENT));
	}

	private static void setEnabledDeep(Container container, boolean enabled) {
		for (Component child : container.getComponents()) {
			child.setEnabled(enabled);
			if (child instanceof Container inner) {
				setEnabledDeep(inner, enabled);
			}
		}
	}

	/** The foreign keys of this table on other tables. Changes are kept in the tab and reach the model on OK. */
	private JPanel foreignKeysTab() {
		for (ModelForeignKey key : model.foreignKeysOf(tableCard)) {
			if (key.from() == tableCard) {
				keysBefore.add(key);
				keys().addElement(key);
			}
		}
		keyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		keyList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int i, boolean selected, boolean focus) {
				ModelForeignKey key = (ModelForeignKey) value;
				String text = key.name() + ": " + String.join(", ", key.fromColumns()) + " -> " + key.to().getName() + "(" + String.join(", ", key.toColumns())
					+ ")";
				return super.getListCellRendererComponent(list, text, i, selected, focus);
			}
		});

		JButton addButton = Forms.button("Add &key...");
		JButton edit = Forms.button("&Edit...");
		JButton remove = Forms.button("Remo&ve");
		addButton.addActionListener(e -> addKey());
		edit.addActionListener(e -> editKey());
		remove.addActionListener(e -> removeKey());
		Runnable enable = () -> {
			edit.setEnabled(keyList.getSelectedValue() != null);
			remove.setEnabled(keyList.getSelectedValue() != null);
		};
		keyList.addListSelectionListener(e -> enable.run());
		enable.run();

		// Lined up with the list: no gap before the first button.
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.add(addButton);
		buttons.add(javax.swing.Box.createHorizontalStrut(Forms.GAP));
		buttons.add(edit);
		buttons.add(javax.swing.Box.createHorizontalStrut(Forms.GAP));
		buttons.add(remove);

		JPanel panel = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		panel.add(new JLabel("Foreign keys of this table:"), BorderLayout.NORTH);
		panel.add(new JScrollPane(keyList), BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private DefaultListModel<ModelForeignKey> keys() {
		return (DefaultListModel<ModelForeignKey>) keyList.getModel();
	}

	private void addKey() {
		DesignerColumn[] columns = tableCard.getFields();
		String column = columns.length == 0 ? "" : columns[0].getName();
		TableCard parent = tableCard;
		for (Object object : model.getObjects()) {
			if (object instanceof TableCard other && other != tableCard) {
				parent = other;
				break;
			}
		}
		ModelForeignKey key = ForeignKeyDialog.edit(this, model,
			new ModelForeignKey(tableCard, column.isEmpty() ? List.of() : List.of(column), parent, List.of(), "", "", ""));
		if (key != null) {
			keys().addElement(key);
			keyList.setSelectedValue(key, true);
		}
	}

	private void editKey() {
		int at = keyList.getSelectedIndex();
		if (at < 0) {
			return;
		}
		ModelForeignKey edited = ForeignKeyDialog.edit(this, model, keys().get(at));
		if (edited != null) {
			keys().set(at, edited);
		}
	}

	private void removeKey() {
		ModelForeignKey key = keyList.getSelectedValue();
		if (key != null && Dialogs.confirmDestructive(this, "Remove foreign key",
			"Remove foreign key '" + key.name() + "' from table '" + tableCard.getName() + "'? It is removed from the model when you save the properties.",
			"Remove")) {
			keys().removeElement(key);
		}
	}

	/** Puts the keys of the tab in the model in place of the ones the table had. */
	private void saveKeys() {
		for (ModelForeignKey key : keysBefore) {
			model.removeForeignKey(key);
		}
		for (int i = 0; i < keys().getSize(); i++) {
			model.addForeignKey(keys().get(i));
		}
	}

	@Override
	public String inputProblem() {
		String name = nameField.getText().trim();
		String nameProblem = model == null ? (name.isEmpty() ? "The table needs a name." : null) : ObjectNames.tableProblem(model, tableCard, name);
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
				fieldList.setSelectedIndex(i);
				return "Field '" + field.getName() + "': " + length;
			}
		}
		return null;
	}

	@Override
	public void saveProperties() {
		tableCard.setName(nameField.getText());
		tableCard.setDescription(descriptionArea.getText());
		tableCard.setType(typeCombo.getSelectedItem() == null ? "" : typeCombo.getSelectedItem().toString());
		tableCard.setComment(commentField.getText());

		tableCard.removeAllFields();

		DefaultListModel<DesignerColumn> fieldModel = fields();
		for (int i = 0; i < fieldModel.getSize(); i++) {
			tableCard.addField(fieldModel.getElementAt(i));
		}

		if (model != null) {
			// The keys first, so that renaming or removing a field also reaches the keys added in this dialog.
			saveKeys();
			model.fieldsEdited(tableCard, namesBefore);
		}
	}
}

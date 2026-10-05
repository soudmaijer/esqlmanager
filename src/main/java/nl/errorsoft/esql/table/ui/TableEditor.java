package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.control.CreateTableCC;
import nl.errorsoft.esql.ui.util.EditorTab;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
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
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Creates a new table or edits the name, type and comment of an existing one, as a tab of the connection window. Save and Cancel are at the bottom; the
 * tab asks before it discards changes.
 */
public class TableEditor extends JPanel implements ActionListener, ListSelectionListener, CaretListener, EditorTab {
	private CreateTableCC ctcc;
	private final String title;

	// Selected item in fieldlist
	private CreateColumn selField = null;

	// Input fields
	private JTextField tablename = new JTextField(16);
	private JTextField comment = new JTextField(16);
	private JTextField fieldname = new JTextField(12);
	private JTextField length = new JTextField(12);
	private JTextField defaultval = new JTextField(12);

	// Add fields
	private JButton addfield = new JButton("Add");
	private JButton remfield = new JButton("Remove");
	private JButton edtfield = new JButton("Rename");
	private JButton moveup = new JButton("Up");
	private JButton movedown = new JButton("Down");

	// current databases, tabletypes, and fieldtypes dropdowns
	private JComboBox dbs;
	private JComboBox tabletypes;
	private JComboBox fieldtypes = new JComboBox();

	// List with user created fields
	private JList fieldlist = new JList();

	// Field property checkboxes
	private JCheckBox primary = new JCheckBox("Primary");
	private JCheckBox notnull = new JCheckBox("Not null");
	private JCheckBox autoincrement = new JCheckBox("AutoIncrement");
	private JCheckBox unsigned = new JCheckBox("Unsigned");

	// Panel with the field properties
	private JPanel p3;

	// Listmodel for adding and removing items from list
	private DefaultListModel list = new DefaultListModel();

	private JButton save = new JButton("Save");
	private JButton cancel = new JButton("Cancel");

	private Table table;

	// What the table properties were when the tab opened, and whether the columns changed since.
	private final String initialName;
	private final String initialComment;
	private final String initialType;
	private boolean columnsChanged;

	public TableEditor(CreateTableCC ctcc, String title, Database database, Table table) {
		super(new BorderLayout(8, 8));
		this.ctcc = ctcc;
		this.title = title;
		this.table = table;
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		// TABLE PROPERTIES PANEL - TOP
		DefaultComboBoxModel dcm = new DefaultComboBoxModel();
		Vector db = ctcc.getDatabases();
		for (int i = 0; i < db.size(); i++) {
			dcm.addElement((Database) db.get(i));
			if (database != null && ((Database) db.get(i)).toString().equals(database.toString())) {
				database = (Database) db.get(i);
			}
		}
		dcm.setSelectedItem(database);
		dbs = new JComboBox(dcm);

		DefaultComboBoxModel ttmodel = new DefaultComboBoxModel();
		String[] tbt = ctcc.getTableTypes();
		for (String type : tbt) {
			ttmodel.addElement(type);
			if (table != null && type.equalsIgnoreCase(table.getType())) {
				ttmodel.setSelectedItem(type);
			}
		}
		tabletypes = new JComboBox(ttmodel);

		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Table Properties"));
		addRow(p, 0, 0, "Table name:", tablename);
		addRow(p, 0, 1, "On Database:", dbs);
		addRow(p, 2, 0, "Comment:", comment);
		JLabel tt = addRow(p, 2, 1, "Table type:", tabletypes);
		// Servers without storage engines have nothing to choose here.
		tt.setVisible(tbt.length > 0);
		tabletypes.setVisible(tbt.length > 0);
		add(p, BorderLayout.NORTH);

		// FIELDS: name and buttons on the left, the properties of the selected field on the right
		JPanel p2 = new JPanel(new BorderLayout(6, 6));
		p2.setBorder(BorderFactory.createTitledBorder("Add Fields"));
		fieldname.setText("NewField");
		JPanel nameRow = new JPanel(new BorderLayout(6, 0));
		nameRow.add(fieldname, BorderLayout.CENTER);
		nameRow.add(addfield, BorderLayout.EAST);
		p2.add(nameRow, BorderLayout.NORTH);
		p2.add(new JScrollPane(fieldlist), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
		buttons.add(remfield);
		buttons.add(edtfield);
		buttons.add(moveup);
		buttons.add(movedown);
		JPanel buttonColumn = new JPanel(new BorderLayout());
		buttonColumn.add(buttons, BorderLayout.NORTH);
		p2.add(buttonColumn, BorderLayout.EAST);

		DataType[] ftp = ctcc.getDatatypes();
		for (DataType type : ftp) {
			fieldtypes.addItem(type);
		}

		p3 = new JPanel(new GridBagLayout());
		p3.setBorder(BorderFactory.createTitledBorder("Field Properties"));
		addRow(p3, 0, 0, "Type:", fieldtypes);
		addRow(p3, 0, 1, "Length:", length);
		addRow(p3, 0, 2, "Default:", defaultval);
		JPanel checks = new JPanel(new GridLayout(2, 2, 6, 0));
		checks.add(primary);
		checks.add(notnull);
		checks.add(autoincrement);
		checks.add(unsigned);
		GridBagConstraints c = constraints(0, 3);
		c.gridwidth = 2;
		c.weighty = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(8, 4, 4, 4);
		p3.add(checks, c);

		JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
		center.add(p2);
		center.add(p3);
		add(center, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		bottom.add(save);
		bottom.add(cancel);
		add(bottom, BorderLayout.SOUTH);

		cancel.addActionListener(this);
		save.addActionListener(this);
		addfield.addActionListener(this);
		moveup.addActionListener(this);
		movedown.addActionListener(this);
		remfield.addActionListener(this);
		edtfield.addActionListener(this);
		fieldlist.addListSelectionListener(this);

		primary.addActionListener(this);
		notnull.addActionListener(this);
		unsigned.addActionListener(this);
		autoincrement.addActionListener(this);

		fieldtypes.addActionListener(this);
		defaultval.addCaretListener(this);
		length.addCaretListener(this);

		fieldlist.setModel(list);
		valueChanged(null);

		if (table != null) {
			// Disable buttons and textfields.
			fieldname.setText("");
			fieldname.setEnabled(false);
			addfield.setEnabled(false);
			moveup.setEnabled(false);
			movedown.setEnabled(false);
			remfield.setEnabled(false);
			edtfield.setEnabled(false);
			dbs.setEnabled(false);

			// Add TableColumns to list.
			DefaultListModel dlm = new DefaultListModel();
			for (TableColumn column : table.getColumns()) {
				dlm.addElement(column);
			}
			fieldlist.setModel(dlm);

			// Disable field properties.
			primary.setEnabled(false);
			notnull.setEnabled(false);
			unsigned.setEnabled(false);
			autoincrement.setEnabled(false);

			// Set table properties.
			this.comment.setText(table.getComment());
			this.tablename.setText(table.getName());
		}

		initialName = tablename.getText();
		initialComment = comment.getText();
		initialType = selectedTableType();
	}

	/** A label and its field in two grid cells, the field takes the extra width. */
	private static JLabel addRow(JPanel panel, int x, int y, String text, Component field) {
		JLabel label = new JLabel(text);
		GridBagConstraints c = constraints(x, y);
		panel.add(label, c);
		c = constraints(x + 1, y);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		panel.add(field, c);
		return label;
	}

	private static GridBagConstraints constraints(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 4, 2, 4);
		return c;
	}

	private String selectedTableType() {
		Object type = tabletypes.getSelectedItem();
		return type == null ? null : type.toString();
	}

	/** True when the table properties or the columns differ from when the tab opened. */
	public boolean isModified() {
		return columnsChanged || !tablename.getText().equals(initialName) || !comment.getText().equals(initialComment)
			|| !Objects.equals(selectedTableType(), initialType);
	}

	@Override
	public boolean confirmClose() {
		return !isModified() || Dialogs.confirmDestructive(this, "Discard changes?", "Discard the changes in " + title + "?", "Discard");
	}

	private boolean nameInList(String name) {
		for (int i = 0; i < list.getSize(); i++) {
			if (list.getElementAt(i).toString().trim().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		// Add a field to the fieldlist
		if (source == addfield) {
			String add = fieldname.getText().trim();
			if (!nameInList(add) && add.length() != 0) {
				CreateColumn f = new CreateColumn(add);
				f.type = (DataType) fieldtypes.getSelectedItem();
				list.addElement(f);
				fieldlist.setSelectedIndex(list.indexOf(f));
				columnsChanged = true;
			}
			fieldlist.ensureIndexIsVisible(fieldlist.getSelectedIndex());
		}
		// Remove the selected item from the fieldlist
		if (source == remfield && fieldlist.getSelectedIndex() != -1) {
			list.removeElementAt(fieldlist.getSelectedIndex());
			columnsChanged = true;
		}
		// Rename the selected item
		if (source == edtfield && selField != null) {
			String add = fieldname.getText().trim();
			if (!nameInList(add)) {
				selField.name = add;
				fieldlist.repaint();
				columnsChanged = true;
			}
		}
		if (selField != null) {
			if (source == primary) {
				selField.primary = primary.isSelected();
				columnsChanged = true;
			}
			if (source == notnull) {
				selField.notnull = notnull.isSelected();
				columnsChanged = true;
			}
			if (source == unsigned) {
				selField.unsigned = unsigned.isSelected();
				columnsChanged = true;
			}
			if (source == autoincrement) {
				selField.autoincrement = autoincrement.isSelected();
				columnsChanged = true;
			}
		}
		// Click in fieldtypes window, set the GUI to match the selected item
		if (source == fieldtypes) {
			fieldTypeChosen();
		}
		if (source == save) {
			if (table == null) {
				List<CreateColumn> cols = new ArrayList<>();
				for (int i = 0; i < list.getSize(); i++) {
					cols.add((CreateColumn) list.getElementAt(i));
				}
				ctcc.createTable(tablename.getText(), dbs.getSelectedItem().toString(), comment.getText(), selectedTableType(), this, cols);
			} else {
				ctcc.modifyTable(this, table, tablename.getText(), selectedTableType(), comment.getText());
			}
		}
		if (source == moveup && fieldlist.getSelectedIndex() > 0) {
			moveField(fieldlist.getSelectedIndex(), -1);
		}
		if (source == movedown && fieldlist.getSelectedIndex() != -1 && fieldlist.getSelectedIndex() != list.getSize() - 1) {
			moveField(fieldlist.getSelectedIndex(), 1);
		}
		if (source == cancel) {
			ctcc.cancel(this);
		}
	}

	private void moveField(int id, int step) {
		Object one = list.getElementAt(id);
		list.removeElementAt(id);
		list.insertElementAt(one, id + step);
		fieldlist.setSelectedIndex(id + step);
		columnsChanged = true;
	}

	/** Enables the options the chosen type allows, and clears those it does not. */
	private void fieldTypeChosen() {
		DataType f = (DataType) fieldtypes.getSelectedItem();
		primary.setEnabled(f.primary);
		notnull.setEnabled(f.notnull);
		unsigned.setEnabled(f.unsigned);
		autoincrement.setEnabled(f.autoincrement);

		if (selField == null) {
			return;
		}
		if (!f.primary) {
			primary.setSelected(false);
			selField.primary = false;
		}
		if (!f.notnull) {
			notnull.setSelected(false);
			selField.notnull = false;
		}
		if (!f.unsigned) {
			unsigned.setSelected(false);
			selField.unsigned = false;
		}
		if (!f.autoincrement) {
			autoincrement.setSelected(false);
			selField.autoincrement = false;
		}
		if (!f.zerofill) {
			selField.zerofill = false;
		}
		if (selField.type != f) {
			selField.type = f;
			columnsChanged = true;
		}
	}

	// Keeps track of the current selected item in the field list.
	public void valueChanged(ListSelectionEvent e) {
		if (fieldlist.getSelectedIndex() != -1) {
			if (fieldlist.getSelectedValue() instanceof TableColumn tc) {
				primary.setSelected(tc.isPrimary());
				notnull.setSelected(tc.isNullable());
				unsigned.setSelected(!tc.isSigned());
				autoincrement.setSelected(tc.isAutoIncrement());
				length.setText(String.valueOf(tc.getSize()));
				defaultval.setText(tc.getDefault());

				for (int i = 0; i < fieldtypes.getItemCount(); i++) {
					if (fieldtypes.getModel().getElementAt(i) instanceof DataType
						&& fieldtypes.getModel().getElementAt(i).toString().equalsIgnoreCase(tc.getNativeTypeName())) {
						fieldtypes.setSelectedItem(fieldtypes.getModel().getElementAt(i));
					}
				}

				// An existing column is only shown, not edited here.
				primary.setEnabled(false);
				notnull.setEnabled(false);
				unsigned.setEnabled(false);
				autoincrement.setEnabled(false);
			} else {
				setFieldPropertiesEnabled(true);
				selField = (CreateColumn) list.getElementAt(fieldlist.getSelectedIndex());
				primary.setSelected(selField.primary);
				notnull.setSelected(selField.notnull);
				unsigned.setSelected(selField.unsigned);
				autoincrement.setSelected(selField.autoincrement);
				fieldtypes.setSelectedItem(selField.type);
				defaultval.setText(selField.defaultval);
				length.setText(selField.length);
			}
		} else {
			setFieldPropertiesEnabled(false);
			selField = null;
			primary.setSelected(false);
			notnull.setSelected(false);
			unsigned.setSelected(false);
			autoincrement.setSelected(false);
			defaultval.setText("");
			length.setText("");
		}
	}

	private void setFieldPropertiesEnabled(boolean enabled) {
		for (Component component : new Component[]{fieldtypes, length, defaultval, primary, notnull, autoincrement, unsigned}) {
			component.setEnabled(enabled);
		}
	}

	public void caretUpdate(CaretEvent e) {
		if (table != null || selField == null) {
			return;
		}

		if (e.getSource() == defaultval) {
			if (!Objects.toString(selField.defaultval, "").equals(defaultval.getText())) {
				selField.defaultval = defaultval.getText();
				columnsChanged = true;
			}
		} else if (!Objects.toString(selField.length, "").equals(length.getText())) {
			selField.length = length.getText();
			columnsChanged = true;
		}
	}
}

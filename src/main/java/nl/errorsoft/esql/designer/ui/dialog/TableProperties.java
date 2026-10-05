package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.Field;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import nl.errorsoft.esql.connection.ServerType;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.IdentityHashMap;
import java.util.Map;

import nl.errorsoft.esql.designer.model.ForeignKey;
import nl.errorsoft.esql.designer.model.Model;

public class TableProperties extends JTabbedPane implements PropertiesInterface, ActionListener, ListSelectionListener, CaretListener { // General tab
	private JLabel lbl_name = new JLabel("Name");
	private JLabel lbl_comm = new JLabel("Comment");
	private JLabel lbl_type = new JLabel("Type");
	private JLabel lbl_desc = new JLabel("Description");

	private JTextField txt_name = new JTextField();
	private JTextArea txt_desc = new JTextArea();
	private JTextField txt_comm = new JTextField();
	private JComboBox<String> cmb_type = new JComboBox<>();

	// Field tab
	private JLabel lbl_fields = new JLabel("Fields");
	private JTabbedPane tab_field = new JTabbedPane();
	private JList<Field> lst_fields = new JList<>(new DefaultListModel<>());

	// FieldTab 1
	private JLabel lbl_fieldname = new JLabel("Fieldname");
	private JLabel lbl_fieldcomm = new JLabel("Comment");
	private JTextField txt_fieldname = new JTextField();
	private JTextArea txt_fieldcomm = new JTextArea();
	private JButton btn_new = new JButton("New");
	private JButton btn_rem = new JButton("Remove");

	// FieldTab 2
	private JLabel lbl_types = new JLabel("Type");
	private JLabel lbl_length = new JLabel("Length");
	private JLabel lbl_default = new JLabel("Default");
	private JComboBox<nl.errorsoft.esql.table.DataType> cmb_types = new JComboBox<>();
	private JTextField txt_length = new JTextField();
	private JTextField txt_default = new JTextField();

	// Field property checkboxes
	private JCheckBox primary = new JCheckBox("Primary");
	private JCheckBox index = new JCheckBox("Index");
	private JCheckBox unique = new JCheckBox("Unique");
	private JCheckBox notnull = new JCheckBox("Not null");
	private JCheckBox autoincrement = new JCheckBox("AutoIncrement");
	private JCheckBox binary = new JCheckBox("Binary");
	private JCheckBox unsigned = new JCheckBox("Unsigned");
	private JCheckBox zerofill = new JCheckBox("Zerofill");

	private JPanel properties;
	private Field selField = null;

	private JList<ForeignKey> lst_keys = new JList<>();

	// Tableobject
	private TableObject tb;
	private nl.errorsoft.esql.connection.ServerType serverType;

	// The model keeps its foreign keys in step with renamed and removed fields.
	private final Model model;
	private final Map<Field, String> namesBefore = new IdentityHashMap<>();

	public TableProperties(TableObject tb, nl.errorsoft.esql.connection.ServerType serverType, Model model) {
		this.model = model;
		this.serverType = serverType;

		txt_name.setText(tb.getName());
		txt_comm.setText(tb.getComment());
		String[] tableTypes = serverType.getDialect().getTableTypes();
		for (int i = 0; i < tableTypes.length; i++) {
			cmb_type.addItem(tableTypes[i]);
		}
		cmb_type.setSelectedItem(tb.getType());
		// Servers without storage engines have nothing to choose here.
		lbl_type.setVisible(tableTypes.length > 0);
		cmb_type.setVisible(tableTypes.length > 0);
		txt_desc.setFont(txt_name.getFont());
		txt_desc.setLineWrap(true);
		txt_desc.setWrapStyleWord(true);
		txt_desc.setText(tb.getDescription());
		JScrollPane jsp = new JScrollPane(txt_desc);
		jsp.setPreferredSize(new Dimension(185, 160));

		JPanel general = Forms.padded(
			new Forms.Grid().row(lbl_name, txt_name).row(lbl_comm, txt_comm).row(lbl_type, cmb_type).area(lbl_desc, jsp).panel());
		general.setOpaque(false);
		this.addTab("General", general);

		// Fields tab: the list of fields above the editor of the selected field.
		JScrollPane jsp2 = new JScrollPane(lst_fields);
		jsp2.setPreferredSize(new Dimension(185, 80));
		JPanel fields = Forms.padded(new Forms.Grid().row(lbl_fields, jsp2).fill(tab_field).panel());

		txt_fieldcomm.setWrapStyleWord(true);
		txt_fieldcomm.setLineWrap(true);
		txt_fieldcomm.setFont(lbl_fieldcomm.getFont());
		JScrollPane jsp3 = new JScrollPane(txt_fieldcomm);
		jsp3.setPreferredSize(new Dimension(180, 70));
		JPanel first = Forms
			.padded(new Forms.Grid().row(lbl_fieldname, txt_fieldname).area(lbl_fieldcomm, jsp3).full(Forms.buttonRow(btn_new, btn_rem)).panel());

		nl.errorsoft.esql.table.DataType[] fo = serverType.getDataTypes();
		for (int i = 0; i < fo.length; i++) {
			cmb_types.addItem(fo[i]);
		}
		JPanel flags = new JPanel(new GridLayout(4, 2));
		flags.add(primary);
		flags.add(index);
		flags.add(binary);
		flags.add(notnull);
		flags.add(unsigned);
		flags.add(autoincrement);
		flags.add(zerofill);
		flags.add(unique);
		properties = Forms.padded(new Forms.Grid().row(lbl_types, cmb_types).row(lbl_length, txt_length).row(lbl_default, txt_default).full(flags).done());

		tab_field.addTab("Create/Edit", first);
		tab_field.addTab("Properties", properties);

		this.addTab("Fields", fields);

		this.tb = tb;

		if (model != null) {
			this.addTab("Foreign Keys", foreignKeysTab());
		}

		cmb_types.addActionListener(this);
		cmb_types.setSelectedIndex(0);
		lst_fields.addListSelectionListener(this);
		btn_new.addActionListener(this);
		btn_rem.addActionListener(this);
		primary.addActionListener(this);
		index.addActionListener(this);
		unique.addActionListener(this);
		binary.addActionListener(this);
		notnull.addActionListener(this);
		unsigned.addActionListener(this);
		autoincrement.addActionListener(this);
		zerofill.addActionListener(this);

		txt_fieldname.addCaretListener(this);
		txt_fieldcomm.addCaretListener(this);
		txt_default.addCaretListener(this);
		txt_length.addCaretListener(this);

		this.enableComps(false, properties);

		DefaultListModel<Field> dlm = (DefaultListModel<Field>) lst_fields.getModel();
		Field[] f = tb.getFields();
		for (int i = 0; i < f.length; i++) {
			// The dialog edits copies, the table gets them on OK.
			Field copy = f[i].copy();
			dlm.addElement(copy);
			namesBefore.put(copy, f[i].getName());
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
				ForeignKey key = (ForeignKey) value;
				String text = key.name() + ": " + String.join(", ", key.fromColumns()) + " -> " + key.to().getName() + "(" + String.join(", ", key.toColumns())
					+ ")";
				return super.getListCellRendererComponent(list, text, i, selected, focus);
			}
		});

		JButton add = new JButton("Add...");
		JButton edit = new JButton("Edit...");
		JButton remove = new JButton("Remove");
		add.addActionListener(e -> {
			Field[] f = tb.getFields();
			String column = f.length == 0 ? "" : f[0].getName();
			TableObject parent = tb;
			for (Object object : model.getObjects()) {
				if (object instanceof TableObject other && other != tb) {
					parent = other;
					break;
				}
			}
			ForeignKey key = ForeignKeyDialog.edit(this, model, new ForeignKey(tb, column.isEmpty() ? java.util.List.of() : java.util.List.of(column), parent,
				java.util.List.of(), "", "", ""));
			if (key != null) {
				model.addForeignKey(key);
			}
			refreshKeys();
		});
		edit.addActionListener(e -> {
			ForeignKey key = lst_keys.getSelectedValue();
			if (key != null) {
				ForeignKey edited = ForeignKeyDialog.edit(this, model, key);
				if (edited != null) {
					model.removeForeignKey(key);
					model.addForeignKey(edited);
				}
				refreshKeys();
			}
		});
		remove.addActionListener(e -> {
			ForeignKey key = lst_keys.getSelectedValue();
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
		DefaultListModel<ForeignKey> keys = new DefaultListModel<>();
		for (ForeignKey key : model.foreignKeysOf(tb)) {
			if (key.from() == tb) {
				keys.addElement(key);
			}
		}
		lst_keys.setModel(keys);
	}

	public void saveProperties() {
		tb.setName(txt_name.getText());
		tb.setDescription(txt_desc.getText());
		tb.setType(cmb_type.getSelectedItem() == null ? "" : cmb_type.getSelectedItem().toString());
		tb.setComment(txt_comm.getText());

		tb.removeAllFields();

		DefaultListModel<Field> dtm = (DefaultListModel<Field>) lst_fields.getModel();
		for (int i = 0; i < dtm.getSize(); i++) {
			tb.addField((Field) dtm.getElementAt(i));
		}

		if (model != null) {
			model.fieldsEdited(tb, namesBefore);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btn_new) {
			DefaultListModel<Field> dtm = (DefaultListModel<Field>) lst_fields.getModel();
			dtm.addElement(new Field("new_field", (nl.errorsoft.esql.table.DataType) cmb_types.getItemAt(0), "", "", ""));
		}
		if (e.getSource() == btn_rem) {
			DefaultListModel<Field> dtm = (DefaultListModel<Field>) lst_fields.getModel();
			if (lst_fields.getSelectedIndex() != -1) {
				dtm.removeElementAt(lst_fields.getSelectedIndex());
			}
		} else if (e.getSource() == cmb_types) {
			nl.errorsoft.esql.table.DataType fo = (nl.errorsoft.esql.table.DataType) cmb_types.getSelectedItem();
			primary.setEnabled(fo.primary);
			binary.setEnabled(fo.binary);
			unsigned.setEnabled(fo.unsigned);
			zerofill.setEnabled(fo.zerofill);
			index.setEnabled(fo.index);
			notnull.setEnabled(fo.notnull);
			autoincrement.setEnabled(fo.autoincrement);

			if (selField != null) {
				if (!fo.primary) {
					primary.setSelected(false);
					selField.primary = fo.primary;
				}
				if (!fo.index) {
					index.setSelected(false);
					selField.index = fo.index;
				}
				if (!fo.unique) {
					unique.setSelected(false);
					selField.unique = fo.unique;
				}
				if (!fo.binary) {
					binary.setSelected(false);
					selField.binary = fo.binary;
				}
				if (!fo.notnull) {
					notnull.setSelected(false);
					selField.notnull = fo.notnull;
				}
				if (!fo.unsigned) {
					unsigned.setSelected(false);
					selField.unsigned = fo.unsigned;
				}
				if (!fo.autoincrement) {
					autoincrement.setSelected(false);
					selField.autoincrement = fo.autoincrement;
				}
				if (!fo.zerofill) {
					zerofill.setSelected(false);
					selField.zerofill = fo.zerofill;
				}

				selField.setType(fo);
			}
		} else if (e.getSource() == primary) {
			selField.primary = primary.isSelected();
		} else if (e.getSource() == unique) {
			selField.unique = unique.isSelected();
		} else if (e.getSource() == index) {
			selField.index = index.isSelected();
		} else if (e.getSource() == notnull) {
			selField.notnull = notnull.isSelected();
		} else if (e.getSource() == unsigned) {
			selField.unsigned = unsigned.isSelected();
		} else if (e.getSource() == binary) {
			selField.binary = binary.isSelected();
		} else if (e.getSource() == autoincrement) {
			selField.autoincrement = autoincrement.isSelected();
		} else if (e.getSource() == zerofill) {
			selField.zerofill = zerofill.isSelected();
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		enableComps(true, properties);
		DefaultListModel<Field> dtm = (DefaultListModel<Field>) lst_fields.getModel();
		if (lst_fields.getSelectedIndex() == -1) {
			enableComps(false, properties);
			return;
		} else {
			enableComps(true, properties);
		}
		Field fo = (Field) dtm.getElementAt(lst_fields.getSelectedIndex());
		selField = fo;

		primary.setSelected(selField.primary);
		index.setSelected(selField.index);
		unique.setSelected(selField.unique);
		binary.setSelected(selField.binary);
		notnull.setSelected(selField.notnull);
		unsigned.setSelected(selField.unsigned);
		autoincrement.setSelected(selField.autoincrement);
		zerofill.setSelected(selField.zerofill);
		cmb_types.setSelectedItem(selField.getType());
		txt_default.setText(selField.getDefault());
		txt_length.setText(selField.getLength());
		txt_fieldcomm.setText(selField.getComment());
		txt_fieldname.setText(selField.getName());

		for (int i = 0; i < cmb_types.getItemCount(); i++) {
			if (cmb_types.getItemAt(i).toString().equals(fo.getType().getName())) {
				cmb_types.setSelectedIndex(i);
				break;
			}
		}
	}

	public void enableComps(boolean b, JComponent cmp) {
		for (int i = 0; i < cmp.getComponentCount(); i++) {
			Component child = cmp.getComponent(i);
			if (child instanceof JPanel panel) {
				enableComps(b, panel);
			} else {
				child.setEnabled(b);
			}
		}
	}

	public void caretUpdate(CaretEvent e) {
		if (selField != null) {
			if (e.getSource() == txt_default) {
				selField.setDefault(this.txt_default.getText());
			}
			if (e.getSource() == txt_length) {
				selField.setLength(this.txt_length.getText());
			}
			if (e.getSource() == txt_fieldcomm) {
				selField.setComment(this.txt_fieldcomm.getText());
			}
			if (e.getSource() == txt_fieldname && txt_fieldname.getText().trim().length() != 0) {
				selField.setName(this.txt_fieldname.getText());
			}

			lst_fields.repaint();
		}
	}
}

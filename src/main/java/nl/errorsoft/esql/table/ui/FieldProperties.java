package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.table.TableIndex;

import nl.errorsoft.esql.table.DataType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class FieldProperties extends JDialog implements ActionListener {
	// Control class.
	private ConnectionWindowCC cwcc;

	private JPanel top;
	private JTextField name = new JTextField();
	private JTextField length = new JTextField();
	private JTextField dfault = new JTextField();
	private JComboBox<DataType> fieldtypes = new JComboBox<>();
	private JList<TableIndex> indexList = new JList<>();
	private DefaultListModel<TableIndex> dlm = new DefaultListModel<>();
	private JScrollPane indexListScroll;

	// Field property checkboxes
	private JCheckBox primary = new JCheckBox("Primary Key");
	private JCheckBox unique = new JCheckBox("Unique");
	private JCheckBox notnull = new JCheckBox("Nullable");
	private JCheckBox autoIncrement = new JCheckBox("Auto Increment");
	private JCheckBox binary = new JCheckBox("Binary");
	private JCheckBox unsigned = new JCheckBox("Unsigned");
	private JCheckBox zerofill = new JCheckBox("Zerofill");
	private JCheckBox index = new JCheckBox("Index");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnSave = new JButton("Save");

	// Panel with checkboxes
	private JPanel p3;
	private String db;
	private String table;
	private String field;
	private nl.errorsoft.esql.table.TableColumn column;
	public boolean edit;
	public boolean add;
	private JPanel properties;
	private JPanel indexes;
	private JTabbedPane tabs;
	JButton addIndex;
	JButton dropIndex;

	public FieldProperties(JFrame parent, ConnectionWindowCC cwcc, nl.errorsoft.esql.table.TableColumn column, boolean add, boolean edit) {
		super(parent, true);
		this.cwcc = cwcc;
		this.db = db;
		this.add = add;
		this.edit = edit;
		this.column = column;
		this.setResizable(false);

		if (add) {
			this.setTitle("Add a field");
		} else {
			this.setTitle("Edit field properties");
		}
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		fieldtypes.addActionListener(this);
		DataType[] ftp = cwcc.getConnectionProfile().getServerType().getDataTypes();
		for (int i = 0; i < ftp.length; i++) {
			fieldtypes.addItem(ftp[i]);

			if (edit && column.getNativeTypeName().equalsIgnoreCase(ftp[i].getName())) {
				fieldtypes.setSelectedIndex(i);
			}
		}

		// Properties tab: the field and its options.
		name.setColumns(16);
		name.setText(field);
		top = new Forms.Grid().row(new JLabel("Name "), name).row(new JLabel("Type "), fieldtypes).row(new JLabel("Length "), length)
			.row(new JLabel("Default "), dfault).panel();
		Forms.titled(top, "Field properties");

		p3 = new JPanel(new java.awt.GridLayout(2, 2, Forms.GAP, 0));
		p3.add(primary);
		p3.add(unsigned);
		p3.add(autoIncrement);
		p3.add(notnull);
		Forms.titled(p3, "Options");

		properties = Forms.padded(new Forms.Grid().full(top).full(p3).done());

		tabs = new JTabbedPane();
		tabs.addTab("Properties", properties);
		//tabs.addTab("Indexes", indexes );

		btnCancel.addActionListener(this);
		btnSave.addActionListener(this);
		JPanel root = Forms.padded(new JPanel(new java.awt.BorderLayout()));
		root.add(tabs, java.awt.BorderLayout.CENTER);
		root.add(Forms.buttonRow(btnSave, btnCancel), java.awt.BorderLayout.SOUTH);
		setContentPane(root);

		// Indexes tab (not shown at the moment).
		indexListScroll = new JScrollPane(indexList);
		indexListScroll.setPreferredSize(new java.awt.Dimension(220, 100));
		indexListScroll.setBorder(new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Indexes"));
		indexList.setBorder(new EtchedBorder(BevelBorder.LOWERED));
		unique.addActionListener(this);

		JPanel indexOptionsPanel = new JPanel(new java.awt.BorderLayout());
		indexOptionsPanel.add(unique);
		Forms.titled(indexOptionsPanel, "Options");

		addIndex = new JButton("Add index");
		addIndex.addActionListener(this);
		dropIndex = new JButton("Drop index");
		dropIndex.addActionListener(this);

		indexes = Forms.padded(new Forms.Grid().full(indexListScroll).full(indexOptionsPanel).full(Forms.buttonRow(addIndex, dropIndex)).done());

		if (edit) {
			this.name.setText(column.getName());
			this.primary.setSelected(column.isPrimary());
			this.primary.setEnabled(false);
			this.autoIncrement.setSelected(column.isAutoIncrement());
			this.notnull.setSelected(column.isNullable());
			this.unsigned.setSelected(!column.isSigned());
			this.length.setText(Integer.toString(column.getSize()));
			this.dfault.setText(column.getDefault());
		}

		pack();
		setLocationRelativeTo(parent);

		// Listeners.
		indexList.addListSelectionListener(lse -> {
			if (indexList.getSelectedIndex() > -1) {
				Object obj = dlm.getElementAt(indexList.getSelectedIndex());

				if (obj instanceof TableIndex temp) {
					unique.setSelected(temp.isUnique());
				}
			}
		});
	}

	// Actionlistener
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		// Click in fieldtypes window, set the GUI to match the selected item
		if (source == fieldtypes) {
			DataType f = (DataType) fieldtypes.getSelectedItem();
			primary.setEnabled(f.primary);
			notnull.setEnabled(f.notnull);
			unsigned.setEnabled(f.unsigned);
			autoIncrement.setEnabled(f.autoincrement);
		} else if (source == btnCancel) {
			this.dispose();
		} else if (source == btnSave) {
			if (add && name.getText().trim().length() > 0) {
				DataType f = (DataType) fieldtypes.getSelectedItem();
				cwcc.addTableColumn(this, name.getText(), length.getText(), dfault.getText(), f, primary.isSelected(), unique.isSelected(), index.isSelected(),
					autoIncrement.isSelected(), unsigned.isSelected(), notnull.isSelected());
			}
			if (edit && name.getText().trim().length() > 0) {
				DataType f = (DataType) fieldtypes.getSelectedItem();
				cwcc.editTableColumn(this, column, name.getText(), length.getText(), dfault.getText(), f, primary.isSelected(), unique.isSelected(),
					index.isSelected(), autoIncrement.isSelected(), unsigned.isSelected(), notnull.isSelected());
			}
		} else if (source == addIndex) {
			String input = Dialogs.input(this, "New index", "Name of the new index:");

			if (input != null) {
				TableIndex ti = new TableIndex(null);
				ti.setName(input);
				dlm.addElement(ti);
			}
		} else if (source == dropIndex) {
			if (indexList.getSelectedIndex() > -1 && Dialogs.confirm(this, "Remove index",
				"Remove index '" + indexList.getSelectedValue() + "'? It is dropped when you click Save.", "Remove")) {
				dlm.removeElementAt(indexList.getSelectedIndex());
			}
		} else if (source == unique) {
			if (indexList.getSelectedIndex() > -1) {
				((TableIndex) dlm.getElementAt(indexList.getSelectedIndex())).setUnique(unique.isSelected());
			}
		}
	}
}

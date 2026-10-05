package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.ui.util.Forms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Adds a column to a table or edits one. */
public class FieldProperties extends JDialog implements ActionListener {
	/** A length is a number, or two numbers for the precision and scale of a decimal ("10,2"). */
	private static final Pattern LENGTH = Pattern.compile("\\d+(\\s*,\\s*\\d+)?");

	private final ConnectionWindowCC cwcc;
	private final TableColumn column;
	private final boolean add;
	private final boolean edit;

	private final JTextField name = new JTextField(16);
	private final JTextField length = new JTextField();
	private final JTextField dfault = new JTextField();
	private final JComboBox<DataType> fieldtypes = new JComboBox<>();

	private final JCheckBox primary = new JCheckBox("Primary Key");
	private final JCheckBox unsigned = new JCheckBox("Unsigned");
	private final JCheckBox autoIncrement = new JCheckBox("Auto Increment");
	private final JCheckBox notnull = new JCheckBox("Not null");
	private final JButton btnCancel = new JButton("Cancel");
	private final JButton btnSave = new JButton("Save");

	public FieldProperties(JFrame parent, ConnectionWindowCC cwcc, TableColumn column, boolean add, boolean edit) {
		super(parent, add ? "Add a field" : "Edit field properties", true);
		this.cwcc = cwcc;
		this.add = add;
		this.edit = edit;
		this.column = column;
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		fieldtypes.addActionListener(this);
		DataType[] types = cwcc.getConnectionProfile().getServerType().getDataTypes();
		for (int i = 0; i < types.length; i++) {
			fieldtypes.addItem(types[i]);

			if (edit && column.getNativeTypeName().equalsIgnoreCase(types[i].getName())) {
				fieldtypes.setSelectedIndex(i);
			}
		}

		JPanel top = Forms.titled(new Forms.Grid().row(new JLabel("Name "), name).row(new JLabel("Type "), fieldtypes).row(new JLabel("Length "), length)
			.row(new JLabel("Default "), dfault).panel(), "Field properties");

		JPanel options = new JPanel(new GridLayout(2, 2, Forms.GAP, 0));
		options.add(primary);
		options.add(unsigned);
		options.add(autoIncrement);
		options.add(notnull);
		Forms.titled(options, "Options");

		btnCancel.addActionListener(this);
		btnSave.addActionListener(this);
		JPanel root = Forms.padded(new JPanel(new BorderLayout()));
		root.add(new Forms.Grid().full(top).full(options).done(), BorderLayout.CENTER);
		root.add(Forms.buttonRow(btnSave, btnCancel), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(btnSave);

		if (edit) {
			name.setText(column.getName());
			primary.setSelected(column.isPrimary());
			primary.setEnabled(false);
			autoIncrement.setSelected(column.isAutoIncrement());
			notnull.setSelected(!column.isNullable());
			unsigned.setSelected(!column.isSigned());
			// The server reports a size for every type, only types that are written with a length show it.
			length.setText(takesLength(column.getNativeTypeName()) ? Integer.toString(column.getSize()) : "");
			dfault.setText(column.getDefault());
		}

		pack();
		setLocationRelativeTo(parent);
	}

	/** True for the types that are written with a length, such as varchar(50) or decimal(10,2). */
	private static boolean takesLength(String typeName) {
		String type = typeName == null ? "" : typeName.toLowerCase();
		return type.contains("char") || type.contains("binary") || type.contains("decimal") || type.contains("numeric") || type.equals("bit");
	}

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
			dispose();
		} else if (source == btnSave) {
			save();
		}
	}

	private void save() {
		if (name.getText().isBlank()) {
			Dialogs.warn(this, getTitle(), "Enter a name for the field.");
			return;
		}
		String size = length.getText().trim();
		if (!size.isEmpty() && !LENGTH.matcher(size).matches()) {
			Dialogs.warn(this, getTitle(), "The length must be a number, for example 50, or two numbers for a decimal, for example 10,2.");
			return;
		}

		DataType f = (DataType) fieldtypes.getSelectedItem();
		boolean nullable = !notnull.isSelected();
		if (add) {
			cwcc.addTableColumn(this, name.getText().trim(), size, dfault.getText(), f, primary.isSelected(), false, false, autoIncrement.isSelected(),
				unsigned.isSelected(), nullable);
		} else if (edit) {
			cwcc.editTableColumn(this, column, name.getText().trim(), size, dfault.getText(), f, primary.isSelected(), false, false,
				autoIncrement.isSelected(), unsigned.isSelected(), nullable);
		}
	}
}

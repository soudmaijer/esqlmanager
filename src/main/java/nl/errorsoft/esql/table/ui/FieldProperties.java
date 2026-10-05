package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Adds a column to a table or edits one. */
public class FieldProperties extends FormDialog implements ActionListener {
	/** A length is a number, or two numbers for the precision and scale of a decimal ("10,2"). */
	private static final Pattern LENGTH = Pattern.compile("\\d+(\\s*,\\s*\\d+)?");

	private final ConnectionWindowCC cwcc;
	private final TableColumn column;
	private final boolean add;
	private final boolean edit;

	private final JTextField name = new JTextField(16);
	private final JTextField length = new JTextField();
	private final JTextField dfault = new JTextField();
	private final JTextField comment = new JTextField();
	private final JComboBox<DataType> fieldtypes = new JComboBox<>();

	private final JCheckBox primary = Forms.mnemonic(new JCheckBox(), "&Primary key");
	private final JCheckBox unsigned = Forms.mnemonic(new JCheckBox(), "&Unsigned");
	private final JCheckBox autoIncrement = Forms.mnemonic(new JCheckBox(), "Auto &increment");
	private final JCheckBox notnull = Forms.mnemonic(new JCheckBox(), "N&ot null");
	private final JButton btnCancel = Forms.button("Cancel");
	private final JButton btnSave = Forms.button("&Save");

	public FieldProperties(JFrame parent, ConnectionWindowCC cwcc, TableColumn column, boolean add, boolean edit) {
		super(parent, add ? "Add field" : "Edit field", true);
		this.cwcc = cwcc;
		this.add = add;
		this.edit = edit;
		this.column = column;

		fieldtypes.addActionListener(this);
		DataType[] types = cwcc.getConnectionProfile().getServerType().getDataTypes();
		String currentType = edit ? cwcc.dialect().datatypeName(column.getNativeTypeName()) : null;
		for (int i = 0; i < types.length; i++) {
			fieldtypes.addItem(types[i]);

			if (edit && currentType.equalsIgnoreCase(types[i].getName())) {
				fieldtypes.setSelectedIndex(i);
			}
		}

		Forms.Grid fields = new Forms.Grid().row("&Name:", name).row("&Type:", fieldtypes).row("&Length:", length).row("&Default:", dfault);
		if (cwcc.dialect().supportsColumnComments()) {
			fields.row("Co&mment:", comment);
		}
		JPanel top = Forms.titled(fields.panel(), "Field properties");

		JPanel options = new JPanel(new GridLayout(2, 2, Forms.GAP, 0));
		options.add(primary);
		options.add(unsigned);
		options.add(autoIncrement);
		options.add(notnull);
		Forms.titled(options, "Options");

		btnCancel.addActionListener(this);
		btnSave.addActionListener(this);
		layoutDialog(new Forms.Grid().full(top).full(options).done(), btnSave, btnCancel);
		setInitialFocus(name);

		if (edit) {
			name.setText(column.getName());
			primary.setSelected(column.isPrimary());
			primary.setEnabled(false);
			autoIncrement.setSelected(column.isAutoIncrement());
			notnull.setSelected(!column.isNullable());
			unsigned.setSelected(unsigned.isEnabled() && !column.isSigned());
			length.setText(column.getLength());
			dfault.setText(column.getDefault());
			comment.setText(column.getComment());
		}

		pack();
		setLocationRelativeTo(parent);
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
		String size = length.getText().trim();
		String problem = Validation.first(Validation.required("a name for the field", name.getText()),
			!size.isEmpty() && !LENGTH.matcher(size).matches()
				? "The length must be a number, for example 50, or two numbers for a decimal, for example 10,2."
				: null);
		showError(problem);
		if (problem != null) {
			return;
		}

		DataType f = (DataType) fieldtypes.getSelectedItem();
		CreateColumn definition = TableService.newColumn(name.getText().trim(), size, dfault.getText(), f, autoIncrement.isSelected(), unsigned.isSelected(),
			!notnull.isSelected());
		definition.primary = primary.isSelected();
		definition.comment = comment.getText().trim();
		if (add) {
			cwcc.addTableColumn(this, definition);
		} else if (edit) {
			cwcc.editTableColumn(this, column, definition);
		}
	}
}

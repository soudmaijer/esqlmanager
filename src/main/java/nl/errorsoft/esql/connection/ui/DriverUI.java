package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.connection.DatabaseDriver;
import nl.errorsoft.esql.connection.control.DatabaseDriverCC;
import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.ui.util.Forms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Edits the connection URL, the driver class and the quote characters of a database driver. */
public class DriverUI extends JDialog {
	private final DatabaseDriverCC dbcc;

	private final JComboBox<DatabaseDriver> type = new JComboBox<>();
	private final JTextField className = new JTextField();
	private final JTextField url = new JTextField(28);
	private final JTextField identifierOpen = new JTextField(3);
	private final JTextField identifierClose = new JTextField(3);
	private final JTextField stringOpen = new JTextField(3);
	private final JTextField stringClose = new JTextField(3);
	private final JButton save = new JButton("Save");
	private final JButton cancel = new JButton("Cancel");

	public DriverUI(DatabaseDriverCC dbcc, Frame parent) {
		super(parent, "Driver properties", false);
		this.dbcc = dbcc;
		initComponents();
		setResizable(false);
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	private void initComponents() {
		JPanel properties = Forms.titled(new Forms.Grid().row(new JLabel("Driver class name:"), className).row(new JLabel("Connection URL:"), url)
			.row(new JLabel("Identifier quote:"), pair(identifierOpen, identifierClose)).row(new JLabel("String quote:"), pair(stringOpen, stringClose))
			.done(), "Driver properties");

		JPanel typePanel = Forms.titled(new JPanel(new BorderLayout()), "Database type");
		typePanel.add(type);
		type.addItemListener(e -> show((DatabaseDriver) type.getSelectedItem()));

		save.addActionListener(e -> save());
		cancel.addActionListener(e -> dispose());

		JPanel root = Forms.padded(new JPanel(new BorderLayout()));
		root.add(new Forms.Grid().full(typePanel).full(properties).done(), BorderLayout.CENTER);
		root.add(Forms.buttonRow(save, cancel), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(save);
		pack();
	}

	/** An opening and a closing character on one row. */
	private static JPanel pair(Component open, Component close) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		p.add(open);
		p.add(Box.createHorizontalStrut(Forms.GAP));
		p.add(new JLabel("closing:"));
		p.add(close);
		return p;
	}

	public void loadDrivers(DatabaseDriver[] drivers) {
		if (drivers != null && drivers.length > 0) {
			type.setModel(new DefaultComboBoxModel<>(drivers));
			show(drivers[0]);
		}
	}

	private void show(DatabaseDriver driver) {
		if (driver != null) {
			className.setText(driver.getDriverClassName());
			url.setText(driver.getDriverURL());
			identifierOpen.setText(driver.getFieldOpenChar());
			identifierClose.setText(driver.getFieldCloseChar());
			stringOpen.setText(driver.getDataOpenChar());
			stringClose.setText(driver.getDataCloseChar());
		}
	}

	private void save() {
		if (className.getText().isBlank() || url.getText().isBlank()) {
			Dialogs.warn(this, getTitle(), "Enter the driver class name and the connection URL.");
			return;
		}
		DatabaseDriver driver = (DatabaseDriver) type.getSelectedItem();
		dbcc.saveProperties(driver.getId(), driver.getDriverName(), url.getText().trim(), className.getText().trim(), identifierOpen.getText(),
			identifierClose.getText(), stringOpen.getText(), stringClose.getText());
	}
}

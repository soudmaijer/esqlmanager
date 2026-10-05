package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;

public class DatabaseProperties extends JTabbedPane implements PropertiesInterface { // General tab
	private JLabel lbl_name = new JLabel("Name");
	private JLabel lbl_comm = new JLabel("Description");
	private JTextField txt_name = new JTextField();
	private JTextArea txt_comm = new JTextArea();

	// Databaseobject
	private DatabaseObject db;

	public DatabaseProperties(DatabaseObject db) {
		txt_name.setText(db.getName());
		txt_comm.setFont(txt_name.getFont());
		txt_comm.setLineWrap(true);
		txt_comm.setWrapStyleWord(true);
		txt_comm.setText(db.getDescription());
		JScrollPane jsp = new JScrollPane(txt_comm);
		jsp.setPreferredSize(new Dimension(185, 185));

		JPanel general = Forms.padded(new Forms.Grid().row(lbl_name, txt_name).area(lbl_comm, jsp).panel());

		this.addTab("General", general);

		this.db = db;
	}

	public void saveProperties() {
		db.setName(txt_name.getText());
		db.setDescription(txt_comm.getText());
	}
}

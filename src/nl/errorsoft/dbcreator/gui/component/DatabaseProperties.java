package nl.errorsoft.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;

public class DatabaseProperties extends JTabbedPane implements PropertiesInterface
{	// General tab
	private JLabel lbl_name = new JLabel("Name");
	private JLabel lbl_comm = new JLabel("Description");
	private JTextField txt_name = new JTextField();
	private JTextArea txt_comm = new JTextArea();
	
	// Databaseobject
	private DatabaseObject db;

	public DatabaseProperties ( DatabaseObject db )
	{	JPanel general = new JPanel();
		general.setLayout(null);
		
		lbl_name.setBounds(10,15,60,20);
		general.add(lbl_name);
		
		txt_name.setBounds(70,17,185,20);
		general.add(txt_name);
		txt_name.setText(db.getName());
		
		lbl_comm.setBounds(10,40,60,20);
		general.add(lbl_comm);
		
		JScrollPane jsp = new JScrollPane(txt_comm);
		txt_comm.setFont(txt_name.getFont());
		txt_comm.setLineWrap(true);
		txt_comm.setWrapStyleWord(true);
		jsp.setBounds(70,42,185,185);
		general.add(jsp);	
		txt_comm.setText(db.getDescription());			
		
		this.addTab("General", general);
		
		this.db = db;
	} 
	
	public void saveProperties()
	{	db.setName(txt_name.getText());
		db.setDescription(txt_comm.getText());
	}
}
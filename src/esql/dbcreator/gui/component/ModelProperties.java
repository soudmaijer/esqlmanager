package esql.dbcreator.gui.component;

import esql.dbcreator.gui.model.Model;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelProperties extends JTabbedPane implements PropertiesInterface
{	
	private Model m;
		
	private JLabel lbl_name = new JLabel("Name");
	private JLabel lbl_comm = new JLabel("Description");
	private JLabel lbl_author = new JLabel("Author");
	private JLabel lbl_history = new JLabel("History");
	
	private JTextField txt_name = new JTextField();
	private JTextField txt_author = new JTextField();
	private JTextArea txt_comm = new JTextArea();
	
	private JTable tbl_history = new JTable();
	private JButton btn_add = new JButton("Add");
	private JButton btn_del = new JButton("Delete");

	public ModelProperties ( Model m )
	{	
		this.m = m;
		
		JPanel general = new JPanel();
		general.setLayout(null);
		
		lbl_name.setBounds(10,15,60,20);
		general.add(lbl_name);
		
		txt_name.setBounds(70,17,185,20);
		general.add(txt_name);
		txt_name.setText(m.getName());
		
		lbl_author.setBounds(10,40,60,20);
		general.add(lbl_author);
		
		txt_author.setBounds(70,42,185,20);
		general.add(txt_author);
		txt_author.setText(m.getAuthor());	
		
		if(m.getAuthor().trim().length() != 0 )
			txt_author.setEnabled( false );	
		
		lbl_comm.setBounds(10,65,60,20);
		general.add(lbl_comm);

		JScrollPane jsp = new JScrollPane(txt_comm);
		txt_comm.setFont(txt_name.getFont());
		txt_comm.setLineWrap(true);
		txt_comm.setWrapStyleWord(true);
		jsp.setBounds(70,67,185,135);
		general.add(jsp);	
		txt_comm.setText(m.getComment());	
			
		this.addTab("General", general);		
	}
	
	public void saveProperties ()
	{
		m.setAuthor(txt_author.getText());
		m.setName(txt_name.getText());
		m.setComment(txt_comm.getText());
	}	
}
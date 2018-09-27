package esql.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class TableProperties extends JTabbedPane implements PropertiesInterface, ActionListener, ListSelectionListener, CaretListener
{	// General tab
	private JLabel lbl_name = new JLabel("Name");
	private JLabel lbl_comm = new JLabel("Comment");
	private JLabel lbl_type = new JLabel("Type");
	private JLabel lbl_desc = new JLabel("Description");
	
	private JTextField txt_name = new JTextField();
	private JTextArea txt_desc = new JTextArea();
	private JTextField txt_comm = new JTextField();
	private JComboBox cmb_type = new JComboBox();
	
	// Field tab
	private JLabel lbl_fields = new JLabel("Fields");
	private JTabbedPane tab_field = new JTabbedPane();
	private JList lst_fields = new JList(new DefaultListModel());
	
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
	private JComboBox cmb_types = new JComboBox();
	private JTextField txt_length = new JTextField();
	private JTextField txt_default = new JTextField();
	
	// Field property checkboxes
	private JCheckBox  primary = new JCheckBox("Primary");
	private JCheckBox  index = new JCheckBox("Index");
	private JCheckBox  unique = new JCheckBox("Unique");
	private JCheckBox  notnull = new JCheckBox("Not null");
	private JCheckBox  autoincrement = new JCheckBox("AutoIncrement");
	private JCheckBox  binary = new JCheckBox("Binary");
	private JCheckBox  unsigned = new JCheckBox("Unsigned");
	private JCheckBox  zerofill = new JCheckBox("Zerofill");
	
	private JPanel properties;	
	private Field selField = null;
	
	// Tableobject
	private TableObject tb;

	public TableProperties ( TableObject tb )
	{	JPanel general = new JPanel();
		general.setLayout(null);
		general.setOpaque(false);
		
		lbl_name.setBounds(10,15,60,20);
		general.add(lbl_name);
		
		txt_name.setBounds(70,17,185,20);
		general.add(txt_name);
		txt_name.setText(tb.getName());
		
		lbl_comm.setBounds(10,40,60,20);
		general.add(lbl_comm);
		
		txt_comm.setBounds(70,42,185,20);
		general.add(txt_comm);
		txt_comm.setText(tb.getComment());
		
		lbl_type.setBounds(10,65,60,20);
		general.add(lbl_type);
		
		cmb_type.addItem(new String("MyIsam"));
		cmb_type.addItem(new String("ISAM"));
		cmb_type.addItem(new String("HEAP"));
		cmb_type.addItem(new String("MERGE"));
		cmb_type.addItem(new String("MRGMyISAM"));
		cmb_type.addItem(new String("InnoDB"));
		cmb_type.addItem(new String("BDB"));
		cmb_type.setSelectedItem(tb.getType());
		cmb_type.setBounds(70,67,185,20);
		general.add(cmb_type);		
		
		lbl_desc.setBounds(10,90,60,20);
		general.add(lbl_desc);		
		
		JScrollPane jsp = new JScrollPane(txt_desc);
		txt_desc.setFont(txt_name.getFont());
		txt_desc.setLineWrap(true);
		txt_desc.setWrapStyleWord(true);
		jsp.setBounds(70,92,185,160);
		general.add(jsp);	
		txt_desc.setText(tb.getDescription());			
		
		this.addTab("General", general);
		
		JPanel fields = new JPanel();
		fields.setLayout(null);
		
		lbl_fields.setBounds(10,5,60,20);
		fields.add(lbl_fields);
		
		JScrollPane jsp2 = new JScrollPane(lst_fields);
		jsp2.setBounds(70,9,185,80);
		fields.add(jsp2);
		
		tab_field.setBounds(5, 92, 255 , 175);
		fields.add(tab_field);
		
		JPanel first = new JPanel();
		first.setLayout(null);
		
		lbl_fieldname.setBounds(5,10,55,20);
		first.add(lbl_fieldname);
		
		txt_fieldname.setBounds(60,12,180,20);
		first.add(txt_fieldname);
		
		lbl_fieldcomm.setBounds(5,32,55,20);
		first.add(lbl_fieldcomm);
		
		JScrollPane jsp3 = new JScrollPane(txt_fieldcomm);
		txt_fieldcomm.setWrapStyleWord(true);
		txt_fieldcomm.setLineWrap(true);
		txt_fieldcomm.setFont(lbl_fieldcomm.getFont());
		jsp3.setBounds(60,34,180,80);
		first.add(jsp3);
		
		btn_new.setBounds(95,117,60,25);
		first.add(btn_new);	
		
		btn_rem.setBounds(160,117,80,25);
		first.add(btn_rem);						
		
		properties = new JPanel();
		properties.setLayout(null);
		
		lbl_types.setBounds(5,10,55,20);
		properties.add(lbl_types);
		
		cmb_types.setBounds(60,12,180,20);
		properties.add(cmb_types);
		esql.domain.DataType [] fo = FieldObject.getFieldTypes();
		for(int i = 0 ; i < fo.length; i++ )
		{	cmb_types.addItem(fo[i]);
		}
		
		lbl_length.setBounds(5,32,55,20);
		properties.add(lbl_length);
		
		txt_length.setBounds(60,34,180,20);
		properties.add(txt_length);
		
		lbl_default.setBounds(5,54,55,20);
		properties.add(lbl_default);
		
		txt_default.setBounds(60,56,180,20);
		properties.add(txt_default);
		
		primary.setBounds(5,77,100,18);		
		properties.add(primary);

		binary.setBounds(5,93,100,18);		
		properties.add(binary);		
		
		unsigned.setBounds(5,109,100,18);		
		properties.add(unsigned);	
		
		zerofill.setBounds(5,125,100,18);		
		properties.add(zerofill);
		
		index.setBounds(120,77,100,18);		
		properties.add(index);

		notnull.setBounds(120,93,100,18);		
		properties.add(notnull);		
		
		autoincrement.setBounds(120,109,100,18);		
		properties.add(autoincrement);	
		
		unique.setBounds(120,125,100,18);		
		properties.add(unique);					
	
		tab_field.addTab("Create/Edit", first);
		tab_field.addTab("Properties", properties);
		
		this.addTab("Fields", fields);
		
		this.tb = tb;
		
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
		
		DefaultListModel dlm = (DefaultListModel)lst_fields.getModel();
		Field [] f = tb.getFields();
		for( int i = 0; i < f.length; i ++)
		{	dlm.addElement(f[i]);
		}
	} 
	
	public void saveProperties()
	{	tb.setName(txt_name.getText());
		tb.setDescription(txt_desc.getText());
		tb.setType(cmb_type.getSelectedItem().toString());
		tb.setComment(txt_comm.getText());
		
		tb.removeAllFields();
		
		DefaultListModel dtm = (DefaultListModel)lst_fields.getModel();
		for(int i = 0; i < dtm.getSize(); i++)
		{	tb.addField((Field)dtm.getElementAt(i));
		}
	}
	
	public void actionPerformed( ActionEvent e )
	{	if( e.getSource() == btn_new )
		{	DefaultListModel dtm = (DefaultListModel)lst_fields.getModel();
			dtm.addElement(new Field("new_field", (esql.domain.DataType)cmb_types.getItemAt(0), "", "", ""));
		}
		if( e.getSource() == btn_rem )
		{	DefaultListModel dtm = (DefaultListModel)lst_fields.getModel();
			if( lst_fields.getSelectedIndex() != -1 )
			{	dtm.removeElementAt(lst_fields.getSelectedIndex());
			}
		}		
		else if( e.getSource() == cmb_types )
		{	esql.domain.DataType fo = (esql.domain.DataType)cmb_types.getSelectedItem();
			primary.setEnabled(fo.primary);
			binary.setEnabled(fo.binary);
			unsigned.setEnabled(fo.unsigned);
			zerofill.setEnabled(fo.zerofill);
			index.setEnabled(fo.index);
			notnull.setEnabled(fo.notnull);
			autoincrement.setEnabled(fo.autoincrement);
			
			if(selField != null)
			{
				if(!fo.primary)			{	primary.setSelected(false); selField.primary = fo.primary; }
				if(!fo.index)			{	index.setSelected(false); selField.index = fo.index; }
				if(!fo.unique)			{	unique.setSelected(false); selField.unique = fo.unique; }
				if(!fo.binary)			{	binary.setSelected(false); selField.binary = fo.binary; }
				if(!fo.notnull)			{	notnull.setSelected(false); selField.notnull = fo.notnull; }			
				if(!fo.unsigned)		{	unsigned.setSelected(false); selField.unsigned = fo.unsigned; }
				if(!fo.autoincrement)	{	autoincrement.setSelected(false); selField.autoincrement = fo.autoincrement; }	
				if(!fo.zerofill)		{	zerofill.setSelected(false); selField.zerofill = fo.zerofill; }							
				
				selField.setType(fo);
			}
		}
		else if(e.getSource() == primary)
		{	selField.primary = primary.isSelected();
		}
		else if(e.getSource() == unique)
		{	selField.unique = unique.isSelected();
		}
		else if(e.getSource() == index)
		{	selField.index = index.isSelected();
		}
		else if(e.getSource() == notnull)
		{	selField.notnull = notnull.isSelected();
		}
		else if(e.getSource() == unsigned)
		{	selField.unsigned = unsigned.isSelected();
		}
		else if(e.getSource() == binary)
		{	selField.binary = binary.isSelected();
		}
		else if(e.getSource() == autoincrement)
		{	selField.autoincrement = autoincrement.isSelected();
		}	
		else if(e.getSource() == zerofill)
		{	selField.zerofill = zerofill.isSelected();
		}
	}
	
	public void valueChanged( ListSelectionEvent e )
	{	enableComps(true, properties);
		DefaultListModel dtm = (DefaultListModel)lst_fields.getModel();
		if(lst_fields.getSelectedIndex() == -1)
		{	enableComps(false, properties);
			return;
		}
		else
		{	enableComps(true, properties);
		}
		Field fo = (Field)dtm.getElementAt(lst_fields.getSelectedIndex());
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
		
		for( int i = 0 ; i < cmb_types.getItemCount(); i++)
		{	if(cmb_types.getItemAt(i).toString().equals(fo.getType().getName()) )
			{	cmb_types.setSelectedIndex(i);
				break;
			}
		}
	}
	
	public void enableComps (boolean b, JComponent cmp)
	{	for (int i = 0; i < cmp.getComponentCount(); i++)    
			cmp.getComponent(i).setEnabled(b);
	}
	
	public void caretUpdate(CaretEvent e)
	{	if(selField != null)
		{	if( e.getSource() == txt_default)
				selField.setDefault(this.txt_default.getText());
			if( e.getSource() == txt_length)
				selField.setLength(this.txt_length.getText());
			if( e.getSource() == txt_fieldcomm)
				selField.setComment(this.txt_fieldcomm.getText());
			if(e.getSource() == txt_fieldname && txt_fieldname.getText().trim().length() != 0)
				selField.setName(this.txt_fieldname.getText());
			
			lst_fields.repaint();
		}
	}	
}
package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.tree.*;
import java.sql.*;
import java.util.Vector;

public class CreateTable extends JDialog implements ActionListener, ListSelectionListener, CaretListener
{
	private ESQLManagerUI jm;
	private CreateTableCC ctcc;
	
	// Selected item in fieldlist
	private CreateColumn   selField = null;
	
	// Input fields
	private JTextField tablename = new JTextField();
	private JTextField comment   = new JTextField();
	private JTextField fieldname = new JTextField();
	private JTextField length	 = new JTextField();
	private JTextField defaultval = new JTextField();
	
	// Add fields
	private JButton	   addfield  = new JButton("Add");
	private JButton	   remfield  = new JButton("Remove");
	private JButton	   edtfield  = new JButton("Rename");
	private JButton	   moveup	 = new JButton("Up");
	private JButton	   movedown	 = new JButton("Down");
	
	// current databases, tabletypes, and fieldtypes dropdowns
	private JComboBox  dbs = new JComboBox();
	private JComboBox  tabletypes = new JComboBox();
	private JComboBox  fieldtypes = new JComboBox();
	
	// List with user created fields
	private JList 	   fieldlist = new JList();
	
	// Field property checkboxes
	private JCheckBox  primary = new JCheckBox("Primary");
	private JCheckBox  index = new JCheckBox("Index");
	private JCheckBox  unique = new JCheckBox("Unique");
	private JCheckBox  notnull = new JCheckBox("Not null");
	private JCheckBox  autoincrement = new JCheckBox("AutoIncrement");
	private JCheckBox  binary = new JCheckBox("Binary");
	private JCheckBox  unsigned = new JCheckBox("Unsigned");
	private JCheckBox  zerofill = new JCheckBox("Zerofill");

	// Panel with checkboxes
	private JPanel p3;	
	
	// Listmodel for adding and removing items from list
	private DefaultListModel list = new DefaultListModel();	
	
	// Create button
	private JButton create = new JButton("Create");
	private JButton cancel = new JButton("Cancel");
	
	private Database database;
	private TableCC tcc;
	private Table table;
	
	public CreateTable(ESQLManagerUI jm,ConnectionWindowCC cwcc, TableCC tcc, Database database, Table table ) 
	{	// INIT CODE		
		super((JFrame)jm, true);
		this.jm = jm;
		this.ctcc = new CreateTableCC(cwcc);
		this.table = table;
		this.database = database; 
		this.setSize(620,360);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
		
		if( table != null )
			this.setTitle("Edit table "+ table.getName() );
		else
			this.setTitle("Create new table");
		this.getContentPane().setLayout(null);
		
		// TABLE PROPERTIES PANEL - TOP
		JPanel p = new JPanel();
		TitledBorder b = new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Table Properties");
		p.setBorder(b);
		p.setBounds(5,5,605,85);
		Font font = new Font(p.getFont().getFontName(), Font.PLAIN, p.getFont().getSize());   
		p.setFont(font);
		this.getContentPane().add(p);
		p.setLayout(null);
		
		JLabel tnam = new JLabel("Table name: ");
		tnam.setFont(font);
		tnam.setBounds(10,25,80,15);
		p.add(tnam);

		tablename.setBounds(100,23,190,22);
		tablename.setMargin( new Insets( 1,1,1,1 ) );
		p.add(tablename);
		
		JLabel tdb = new JLabel("On Database: ");
		tdb.setFont(font);
		tdb.setBounds(10,50,80,15);
		p.add(tdb);		
		
		DefaultComboBoxModel dcm = new DefaultComboBoxModel();
		
		Vector db = ctcc.getDatabases();		
		for(int i = 0 ; i < db.size(); i++)
		{	dcm.addElement ((Database)db.get(i));
			if( ((Database)db.get(i)).toString().equals(database.toString()))
			{	database=(Database)db.get(i);
			}	
		}
		dcm.setSelectedItem(database);
		dbs = new JComboBox( dcm );		
		dbs.setBounds(100,48,190,22);
		p.add(dbs);
		
		JLabel com = new JLabel("Comment: ");
		com.setFont(font);
		com.setBounds(310,25,80,15);
		p.add(com);	
		
		JLabel tt = new JLabel("Table type: ");
		tt.setBounds(310,50,80,15);
		tt.setFont(font);
		p.add(tt);				
		
		DefaultComboBoxModel ttmodel = new DefaultComboBoxModel();
		String [] tbt = ctcc.getTableTypes();
		
		for( int i = 0; i < tbt.length; i ++)
		{	
			ttmodel.addElement(tbt[i]);
			
			if( table != null )
				if( tbt[i].equalsIgnoreCase( table.getType() ) )
					ttmodel.setSelectedItem( tbt[i] );
				
		}
		tabletypes = new JComboBox(ttmodel);
		tabletypes.setBounds(395,48,200,22);
		tabletypes.setFont(font);
		p.add(tabletypes);

		comment.setBounds(395,23,200,22);
		comment.setMargin( new Insets( 1,1,1,1 ) );
		p.add(comment);		
		
		// FIELD ADD START
		JPanel p2 = new JPanel();
		p2.setBounds(5,90,300,205);
		p2.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Add Fields"));	
		p2.setLayout(null);
		this.getContentPane().add(p2);
		
		addfield.setBounds(180, 23, 110, 22);
		p2.add(addfield);
		
		fieldname.setBounds(10,23,165,22);
		fieldname.setMargin( new Insets( 1,1,1,1 ) );
		fieldname.setText("NewField");
		p2.add(fieldname);

		remfield.setBounds(180, 48, 110, 22);
		p2.add(remfield);
		
		edtfield.setBounds(180, 73, 110, 22);
		edtfield.setFont(font);
		p2.add(edtfield);
		
		moveup.setBounds(180, 145 ,110, 22);								
		p2.add(moveup);
		movedown.setBounds(180, 170 ,110, 22);								
		p2.add(movedown);
		
		JScrollPane jsp = new JScrollPane(fieldlist);
		jsp.setBounds(10, 50, 165, 142);
		p2.add(jsp);

		// START FIELD TYPE 		
		p3 = new JPanel();
		p3.setBounds(310,90,300,205);
		p3.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Field Properties"));	
		p3.setLayout(null);
		this.getContentPane().add(p3);
		
		JLabel ftype = new JLabel("Type: ");	
		ftype.setFont(font);
		ftype.setBounds(10,25,110,15);
		p3.add(ftype);
		
		fieldtypes.setBounds(90,23,200,22);
		p3.add(fieldtypes);	
		
		DataType [] ftp = ctcc.getDatatypes();
		for(int i=0; i<ftp.length; i++)
		{	
			fieldtypes.addItem(ftp[i]);
		}
		
		JLabel len = new JLabel("Length: ");	
		len.setFont(font);
		len.setBounds(10,50,110,15);
		p3.add(len);	

		JLabel def = new JLabel("Default: ");	
		def.setFont(font);
		def.setBounds(10,75,110,15);
		p3.add(def);				
		
		length.setBounds(90,48,200,22);
		p3.add(length);

		defaultval.setBounds(90,73,200,22);
		p3.add(defaultval);		
		
		primary.setBounds(10,130,70,20);
		primary.setFont(font);
		p3.add(primary);
		
		unsigned.setBounds(130,150,120,20);
		unsigned.setFont(font);
		p3.add(unsigned);
		
		notnull.setBounds(130,130,120,20);
		notnull.setFont(font);
		p3.add(notnull);	
		
		autoincrement.setBounds(10,150,120,20);
		autoincrement.setFont(font);
		p3.add(autoincrement);	

		cancel.setBounds(500,300,105,25);
		this.getContentPane().add(cancel);
		cancel.addActionListener(this);
		
		create.setBounds(390,300,105,25);
		this.getContentPane().add(create);
		create.addActionListener(this);
		
		cancel.setFont(font);
		addfield.setFont(font);
		remfield.setFont(font);
		dbs.setFont(font);
		fieldtypes.setFont(font);
		tabletypes.setFont(font);
		fieldlist.setFont(font);	
		moveup.setFont(font);	
		movedown.setFont(font);	
		
		addfield.addActionListener(this);
		moveup.addActionListener(this);
		movedown.addActionListener(this);
		remfield.addActionListener(this);
		edtfield.addActionListener(this);
		fieldlist.addListSelectionListener(this);
		
		primary.addActionListener(this);
		index.addActionListener(this);
		unique.addActionListener(this);
		binary.addActionListener(this);
		notnull.addActionListener(this);			
		unsigned.addActionListener(this);
		autoincrement.addActionListener(this);
		zerofill.addActionListener(this);
		
		fieldtypes.addActionListener(this);
		defaultval.addCaretListener(this);
		length.addCaretListener(this);
		
		fieldlist.setModel(list);
		valueChanged(null);		
		
		if( table != null )
		{
			// Disable buttons and textfields.
			fieldname.setText("");
			fieldname.setEnabled( false );
			addfield.setEnabled( false );
			moveup.setEnabled( false );
			movedown.setEnabled( false );
			remfield.setEnabled( false );
			edtfield.setEnabled( false );
			dbs.setEnabled( false );
			
			// Add TableColumns to list.			
			DefaultListModel dlm = new DefaultListModel();
			
			for( int i=0; i<table.getColumns().length; i++ )
				dlm.addElement( table.getColumns()[i] );
			
			fieldlist.setModel( dlm );
			
			// Disable field properties.
			primary.setEnabled( false );
			notnull.setEnabled( false );
			unsigned.setEnabled( false );
			autoincrement.setEnabled( false );
			
			// Set table properties.
			this.comment.setText( table.getComment() );
			this.tablename.setText( table.getName() );
			this.create.setText( "Save" );
		}
	}
	
	// Actionlistener
	public void actionPerformed(ActionEvent e)
	{	Object source = e.getSource();

		// Add a field to the fieldlist
		if(source == addfield)
		{	String add = fieldname.getText().trim();
			boolean found = false;
			for(int i = 0; i < list.getSize(); i++)
			{	if(list.getElementAt(i).toString().trim().equalsIgnoreCase(add))
				{	found = true;
				}
			}	
			if(!found && add.trim().length() != 0)
			{	CreateColumn f = new CreateColumn(add);
				f.type = (DataType)fieldtypes.getSelectedItem();
				list.addElement(f);
				fieldlist.setSelectedIndex(list.indexOf(f));
			}
			fieldlist.ensureIndexIsVisible( fieldlist.getSelectedIndex() );
		}
		// Remove the selected item from the fieldlist
		if(source == remfield)
		{	if(fieldlist.getSelectedIndex() != -1)
			{	list.removeElementAt(fieldlist.getSelectedIndex());
			}
		}
		// Rename the selected item
		if(source == edtfield && selField != null)
		{	String add = fieldname.getText().trim();
			boolean found = false;
			for(int i = 0; i < list.getSize(); i++)
			{	if(list.getElementAt(i).toString().trim().equalsIgnoreCase(add))
				{	found = true;
				}
			}	
			if(!found)
			{	CreateColumn f = selField;
				f.name = add;	
				fieldlist.updateUI();				
			}
		}
		// Set field objects 'primary' parameter
		if(source == primary)
		{	selField.primary = primary.isSelected();
		}
		// Set field objects 'unique' parameter		
		if(source == unique)
		{	selField.unique = unique.isSelected();
		}
		// Set field objects 'index' parameter
		if(source == index)
		{	selField.index = index.isSelected();
		}
		// Set field objects 'notnull' parameter
		if(source == notnull)
		{	selField.notnull = notnull.isSelected();
		}
		// Set field objects 'unsigned' parameter
		if(source == unsigned)
		{	selField.unsigned = unsigned.isSelected();
		}
		// Set field objects 'binary' parameter
		if(source == binary)
		{	selField.binary = binary.isSelected();
		}
		// Set field objects 'autoincrement' parameter	
		if(source == autoincrement)
		{	selField.autoincrement = autoincrement.isSelected();
		}	
		// Set field objects 'zerofill' parameter
		if(source == zerofill)
		{	selField.zerofill = zerofill.isSelected();
		}
		// Click in fieldtypes window, set the GUI to match the selected item	
		if(source == fieldtypes)
		{	DataType f = (DataType)fieldtypes.getSelectedItem();
			primary.setEnabled(f.primary);
			notnull.setEnabled(f.notnull);
			unsigned.setEnabled(f.unsigned);
			autoincrement.setEnabled(f.autoincrement);
			
			if(!f.primary)		{	primary.setSelected(false); selField.primary = f.primary; }
			if(!f.notnull)		{	notnull.setSelected(false); selField.notnull = f.notnull; }			
			if(!f.unsigned)		{	unsigned.setSelected(false); selField.unsigned = f.unsigned; }
			if(!f.autoincrement){	autoincrement.setSelected(false); selField.autoincrement = f.autoincrement; }	
			if(!f.zerofill)		{	zerofill.setSelected(false); selField.zerofill = f.zerofill; }				
	
			selField.type = (DataType)fieldtypes.getSelectedItem();
		}
		// Create the table (Create button click)
		if(source == create)
		{	
			// New table!
			if( table == null )
			{
				Vector cols = new Vector();
				for( int i = 0 ; i < fieldlist.getModel().getSize(); i++ )
				{	cols.add((CreateColumn)fieldlist.getModel().getElementAt(i));
				}
				ctcc.createTable( tablename.getText(), dbs.getSelectedItem().toString(), comment.getText(), tabletypes.getSelectedItem().toString(), this, cols);	
			}
			// Existing table.
			else if( table != null )
			{
				ctcc.modifyTable( this, table, tablename.getText(), tabletypes.getSelectedItem().toString(), comment.getText() );
			}
		}
		if( source == moveup && fieldlist.getSelectedIndex() != -1 && fieldlist.getSelectedIndex() != 0 )
		{	int id = fieldlist.getSelectedIndex();
			CreateColumn one = (CreateColumn)list.getElementAt(id);
			list.removeElementAt(id);
			list.insertElementAt(one, id -1);
			fieldlist.setSelectedIndex(id - 1);
			fieldlist.updateUI();
		}
		if( source == movedown && fieldlist.getSelectedIndex() != -1 && fieldlist.getSelectedIndex() != list.getSize() - 1 )
		{	int id = fieldlist.getSelectedIndex();
			CreateColumn one = (CreateColumn)list.getElementAt(id);
			list.removeElementAt(id);
			list.insertElementAt(one, id + 1);
			fieldlist.setSelectedIndex(id + 1);
			fieldlist.updateUI();
		}
		if( source == cancel )
		{	ctcc.closeDialog(this);
		}																																																														
	}
	
	// Eventlistener that keeps track of the current selected item
	// in the field list.
	public void valueChanged(ListSelectionEvent e)
	{	if(fieldlist.getSelectedIndex() != -1)
		{
			if( fieldlist.getSelectedValue() instanceof TableColumn )
			{
				TableColumn tc = (TableColumn)fieldlist.getSelectedValue();

				// Disable field properties.
				primary.setEnabled( false );
				notnull.setEnabled( false );
				unsigned.setEnabled( false );
				autoincrement.setEnabled( false );

				primary.setSelected(tc.isPrimary() );
				notnull.setSelected(tc.isNullable() );			
				unsigned.setSelected(!tc.isSigned() );
				autoincrement.setSelected(tc.isAutoIncrement() );
				length.setText( String.valueOf(tc.getSize()) );	
				defaultval.setText(tc.getDefault());
				
			
				for( int i=0; i<fieldtypes.getItemCount(); i++ )
				{
					if( fieldtypes.getModel().getElementAt(i) instanceof DataType && fieldtypes.getModel().getElementAt(i).toString().equalsIgnoreCase( tc.getNativeTypeName() ) )
						fieldtypes.setSelectedItem( fieldtypes.getModel().getElementAt(i) );
				}
			}
			else
			{
				p3.setEnabled(true);
				for(int i = 0 ; i < p3.getComponentCount(); i++)
				{	p3.getComponent(i).setEnabled(true);
				}				
				selField = (CreateColumn)list.getElementAt(fieldlist.getSelectedIndex());
				primary.setSelected(selField.primary);
				notnull.setSelected(selField.notnull);			
				unsigned.setSelected(selField.unsigned);
				autoincrement.setSelected(selField.autoincrement);
				fieldtypes.setSelectedItem(selField.type);
				defaultval.setText(selField.defaultval);
				length.setText(selField.length);			
				
			}
		}
		else
		{	for(int i = 0 ; i < p3.getComponentCount(); i++)
			{	p3.getComponent(i).setEnabled(false);
			}
			primary.setSelected(false);
			index.setSelected(false);
			unique.setSelected(false);
			binary.setSelected(false);
			notnull.setSelected(false);			
			unsigned.setSelected(false);
			autoincrement.setSelected(false);
			zerofill.setSelected(false);			
			defaultval.setText("");
			length.setText("");
			selField = null;
		}	
	}
	
	public void caretUpdate(CaretEvent e)
	{	
		if( table != null )
			return;
			
		Object source = e.getSource();
		if(source == defaultval)
		{	selField.defaultval = defaultval.getText();
		}
		else
		{	selField.length = length.getText();		
		}
	}
	
	public void showErrorMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.WARNING_MESSAGE );  					
	}	
}
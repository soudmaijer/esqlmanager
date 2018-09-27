package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.sql.*;

public class FieldProperties extends JDialog implements ActionListener
{	
	// Control class.
	private ConnectionWindowCC cwcc;	

	private JPanel top;
	private JTextField name = new JTextField();
	private JTextField length = new JTextField();
	private JTextField dfault = new JTextField();
	private JComboBox  fieldtypes = new JComboBox();
	private JList indexList = new JList();
	private DefaultListModel dlm = new DefaultListModel();
	private JScrollPane indexListScroll;

	// Field property checkboxes
	private JCheckBox  primary = new JCheckBox("Primary Key");
	private JCheckBox  unique = new JCheckBox("Unique");
	private JCheckBox  notnull = new JCheckBox("Nullable");
	private JCheckBox  autoIncrement = new JCheckBox("Auto Increment");
	private JCheckBox  binary = new JCheckBox("Binary");
	private JCheckBox  unsigned = new JCheckBox("Unsigned");
	private JCheckBox  zerofill = new JCheckBox("Zerofill");
	private JCheckBox  index = new JCheckBox("Index");
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnSave = new JButton("Save");

	// Panel with checkboxes
	private JPanel p3;	
	private String db;
	private String table;
	private String field;
	private nl.errorsoft.esqlmanager.domain.TableColumn column;
	public boolean edit;
	public boolean add;
	private JPanel properties;
	private JPanel indexes;
	private JTabbedPane tabs;
	JButton addIndex;
	JButton dropIndex;
	
	public FieldProperties( JFrame parent, ConnectionWindowCC cwcc, nl.errorsoft.esqlmanager.domain.TableColumn column, boolean add, boolean edit )
	{	
		super(parent, true);
		this.cwcc = cwcc;
		this.db = db;
		this.add = add;
		this.edit = edit;
		this.column = column;
		this.setResizable(false);
		
		properties = new JPanel();
		properties.setLayout( null );
		properties.setBounds( 0, 0, 250, 170 );
		
		indexes = new JPanel();
		indexes.setLayout( null );
		indexes.setBounds( 0, 0, 250, 170 );

		tabs = new JTabbedPane();
		tabs.setBounds( 0, 0, 250, 265 );
		tabs.addTab("Properties", properties );
		//tabs.addTab("Indexes", indexes );
		
		if( add )
			this.setTitle("Add a field");
		else
			this.setTitle("Edit field properties");
			
		this.setSize(255,335);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.getContentPane().setLayout(null);
		this.setLocation( parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2));
		
		// Create properties part
		TitledBorder border = new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Field properties");
		top = new JPanel();
		top.setLayout(null);
		top.setBorder(border);
		top.setBounds(5,10,235,135);
		properties.add(top);
		
		name.setBounds(65, 23, 160, 20);			
		name.setMargin(new java.awt.Insets(1, 1, 1, 1));
		top.add(name);
		name.setText(field);
		
		dfault.setBounds(65, 98, 160, 20);			
		dfault.setMargin(new java.awt.Insets(1, 1, 1, 1));
		top.add(dfault);
		
		length.setBounds(65, 73, 160, 20);			
		length.setMargin(new java.awt.Insets(1, 1, 1, 1));
		top.add(length);
		
		JLabel a = new JLabel("Name ");
		a.setBounds(9,25,45, 15);
		top.add(a);
		
		JLabel b = new JLabel("Type ");
		b.setBounds(9,50,45, 15);
		top.add(b);
		
		JLabel c = new JLabel("Length ");
		c.setBounds(9,75,45, 15);
		top.add(c);			
		
		JLabel d = new JLabel("Default ");
		d.setBounds(9,100,45, 15);
		top.add(d);
		
		// START FIELD TYPE 		
		TitledBorder options = new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Options");
		
		p3 = new JPanel();
		p3.setLayout(null);
		p3.setBorder( options );
		p3.setBounds(5, 150,235, 85 );
		properties.add(p3);

		primary.setBounds(5,23,100,20);
		p3.add(primary);
		
		autoIncrement.setBounds( 5, 48,100,20);
		p3.add(autoIncrement);			

		unsigned.setBounds(115,23,70,20);
		p3.add(unsigned);
		
		notnull.setBounds(115,48,70,20);
		p3.add(notnull);	

		fieldtypes.setBounds(65,48,160,20);
		fieldtypes.addActionListener(this);

		DataType [] ftp = cwcc.getConnectionProfile().getServerType().getDataTypes();
		
		for(int i=0; i<ftp.length; i++)
		{	
			fieldtypes.addItem(ftp[i]);
			
			if( edit && column.getNativeTypeName().equalsIgnoreCase( ftp[i].getName() ) )
				fieldtypes.setSelectedIndex(i);
		}
		
		top.add(fieldtypes);	
		
		JPanel pBut = new JPanel();
		pBut.setLayout( null );
		pBut.setBounds( 10, 275, 235, 50 );

		btnCancel.addActionListener( this );
		btnCancel.setBounds( 115, 0, 110, 23 );

		btnSave.addActionListener( this );
		btnSave.setBounds( 0, 0, 110, 23 );
		
		pBut.add( btnSave );
		pBut.add( btnCancel );
		
		this.getContentPane().add( tabs );
		this.getContentPane().add( pBut );
	
		// Indexes tab:
		// START FIELD TYPE 		
		TitledBorder indexBorder = new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Indexes");
		indexListScroll = new JScrollPane( indexList );
		indexListScroll.setBounds( 5, 10, 235, 100 );
		indexListScroll.setBorder( indexBorder );
		indexList.setBorder( new EtchedBorder(BevelBorder.LOWERED) );
		unique.setBounds( 5,23,100,20);
		unique.addActionListener( this );
		
		JPanel indexOptionsPanel = new JPanel();
		indexOptionsPanel.setLayout( null );
		indexOptionsPanel.add(unique);
		indexOptionsPanel.setBorder( new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Options") );
		indexOptionsPanel.setBounds( 5,120,235,55);

		addIndex = new JButton("Add index");
		addIndex.addActionListener( this );
		dropIndex = new JButton("Drop index");
		dropIndex.addActionListener( this );

		addIndex.setBounds( 10,190,110,21);
		dropIndex.setBounds( 125,190,110,21);
		
		indexes.add(addIndex);
		indexes.add(dropIndex);
		indexes.add(indexListScroll);
		indexes.add(indexOptionsPanel);
		
		if( edit )
		{
			this.name.setText( column.getName() );
			this.primary.setSelected( column.isPrimary() );
			this.primary.setEnabled( false );
			this.autoIncrement.setSelected( column.isAutoIncrement() );
			this.notnull.setSelected( column.isNullable() );
			this.unsigned.setSelected( !column.isSigned() );
			this.length.setText( Integer.toString( column.getSize() ) );
			this.dfault.setText( column.getDefault() );
		}
		
		// Listeners.
		indexList.addListSelectionListener( new ListSelectionListener()
		{
			public void valueChanged( ListSelectionEvent lse )
			{
				if( indexList.getSelectedIndex() > -1 )
				{
					Object obj = dlm.getElementAt( indexList.getSelectedIndex() );
					
					if( obj instanceof TableIndex )
					{
						TableIndex temp = (TableIndex)obj;
						unique.setSelected( temp.isUnique() );
					}
				}
			}
		} );
	}

	// Actionlistener
	public void actionPerformed(ActionEvent e)
	{	Object source = e.getSource();	

		// Click in fieldtypes window, set the GUI to match the selected item	
		if(source == fieldtypes)
		{	
			DataType f = (DataType)fieldtypes.getSelectedItem();
			primary.setEnabled(f.primary);
			notnull.setEnabled(f.notnull);
			unsigned.setEnabled(f.unsigned);
			autoIncrement.setEnabled(f.autoincrement);
		}
		else if( source == btnCancel )
		{
			this.dispose();
		}
		else if( source == btnSave )
		{
			if( add && name.getText().trim().length() > 0 )
			{
				DataType f = (DataType)fieldtypes.getSelectedItem();
				cwcc.addTableColumn( this, name.getText(), length.getText(), dfault.getText(), f, primary.isSelected(), unique.isSelected(), index.isSelected(), autoIncrement.isSelected(), unsigned.isSelected(), notnull.isSelected() );
			}
			if( edit && name.getText().trim().length() > 0 )
			{
				DataType f = (DataType)fieldtypes.getSelectedItem();
				cwcc.editTableColumn( this, column, name.getText(), length.getText(), dfault.getText(), f, primary.isSelected(), unique.isSelected(), index.isSelected(), autoIncrement.isSelected(), unsigned.isSelected(), notnull.isSelected() );
			}
		}
		else if( source == addIndex )
		{
			String input = JOptionPane.showInputDialog(this, "Enter index name", "New index", JOptionPane.INFORMATION_MESSAGE );
			
			if( input != null )
			{				
				TableIndex ti = new TableIndex(null);
				ti.setName( input );
				dlm.addElement( ti );
			}				
		}
		else if( source == dropIndex )
		{
			int result = JOptionPane.showConfirmDialog( this, "Drop index from list? Notice that you have to press the \"Save\" button to actually drop the index", "Drop index", JOptionPane.YES_NO_OPTION );
			
			if( result == JOptionPane.YES_OPTION )
			{
				if( indexList.getSelectedIndex() > -1 )
				{
					dlm.removeElementAt( indexList.getSelectedIndex() );	
				}
			}					
		}
		else if( source == unique )
		{
			if( indexList.getSelectedIndex() > -1 )
			{
				((TableIndex)dlm.getElementAt( indexList.getSelectedIndex() ) ).setUnique( unique.isSelected() );
			}
		}
	}
}
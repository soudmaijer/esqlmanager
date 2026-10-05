package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.*;

import java.util.Vector;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

public class UserManagerUI extends javax.swing.JDialog
{		
	private ConnectionWindowCC cwcc;
	private ESQLManagerUI emui;
	private UserManagerCC umcc;
	
	private DefaultListModel user;
	private DefaultListModel expuser;
	private DefaultListModel priv;
	private DatabaseTreeView dtv;
	
	private CheckableItem all		 	= new CheckableItem("[ SELECT ALL ]");
	private CheckableItem select 	 	= new CheckableItem("Select");
	private CheckableItem insert 	 	= new CheckableItem("Insert");
	private CheckableItem update 	 	= new CheckableItem("Update");
	private CheckableItem delete 	 	= new CheckableItem("Delete");
	private CheckableItem create 	 	= new CheckableItem("Create");
	private CheckableItem drop   	 	= new CheckableItem("Drop");
	private CheckableItem reload 	 	= new CheckableItem("Reload");
	private CheckableItem shutdown 	= new CheckableItem("Shutdown");
	private CheckableItem process	 	= new CheckableItem("Process");
	private CheckableItem file		 	= new CheckableItem("File");
	private CheckableItem grant	 	= new CheckableItem("Grant");
	private CheckableItem references = new CheckableItem("References");
	private CheckableItem index 	 	= new CheckableItem("Index");
	private CheckableItem alter		= new CheckableItem("Alter");
	
	private Object selected;
	
	public UserManagerUI(ESQLManagerUI emui, ConnectionWindowCC cwcc) 
	{
		super(emui, true);
		this.cwcc = cwcc;
		this.umcc = new UserManagerCC( cwcc );
		this.emui = emui;
		
		this.getRootPane().setPreferredSize(new java.awt.Dimension(400,455));
		
		initComponents();
		
		this.setLocation(emui.getLocation().x + (int)((emui.getSize().width - this.getSize().width) / 2), emui.getLocation().y + (int)((emui.getSize().height - this.getSize().height) / 2));
		
		this.setDefaultCloseOperation(this.DISPOSE_ON_CLOSE);
		this.jScrollPane2.getViewport().setBackground(java.awt.Color.white);
		this.update_btn.setEnabled(false);
		this.jScrollPane3.setEnabled(false);
		this.jScrollPane2.setEnabled(false);
		
		this.getUserAccounts();
	}
	
	private void initComponents() 
	{
		jButton4 = new javax.swing.JButton();
		jTabbedPane1 = new javax.swing.JTabbedPane();
		jPanel3 = new javax.swing.JPanel();
		jPanel1 = new javax.swing.JPanel();
		jScrollPane1 = new javax.swing.JScrollPane();
		
		users = new javax.swing.JList();
		
		add_btn = new javax.swing.JButton();
		edt_btn = new javax.swing.JButton();
		rem_btn = new javax.swing.JButton();
		jPanel2 = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		jTree1 = new javax.swing.JTree();
		jScrollPane3 = new javax.swing.JScrollPane();
		privList = new javax.swing.JList();
		update_btn = new javax.swing.JButton();
		jPanel4 = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		jTextField1 = new javax.swing.JTextField();
		jButton8 = new javax.swing.JButton();
		jLabel2 = new javax.swing.JLabel();
		jPanel7 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jScrollPane4 = new javax.swing.JScrollPane();
		expUser = new javax.swing.JList();
		jCheckBox2 = new javax.swing.JCheckBox();
		jCheckBox1 = new javax.swing.JCheckBox();
		jCheckBox3 = new javax.swing.JCheckBox();
		jCheckBox4 = new javax.swing.JCheckBox();
		jLabel3 = new javax.swing.JLabel();
		jButton7 = new javax.swing.JButton();
		jButton9 = new javax.swing.JButton();
		
		getContentPane().setLayout(null);
		
		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		setTitle("User Manager");
		setResizable(false);
		
		jButton4.setText("Close");
		getContentPane().add(jButton4);
		jButton4.setBounds(315, 422, 75, 25);
		jButton4.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
		     closeButtonPressed();
		   }
		});
		
		jPanel3.setLayout(null);
		
		jPanel1.setLayout(null);
		
		jPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Manage Users", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		jScrollPane1.setViewportView(users);
		
		jPanel1.add(jScrollPane1);
		jScrollPane1.setBounds(15, 30, 195, 90);
		
		add_btn.setText("New User");
		add_btn.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
	      	addActionPerformed(evt);
		   }
		});
		
		jPanel1.add(add_btn);
		add_btn.setBounds(220, 30, 120, 25);
		
		edt_btn.setText("Edit User");
		edt_btn.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
		     	editActionPerformed(evt);
		   }
		});
		
		jPanel1.add(edt_btn);
		edt_btn.setBounds(220, 62, 120, 25);
		
		rem_btn.setText("Remove User");
		jPanel1.add(rem_btn);
		rem_btn.setBounds(220, 95, 120, 25);
		
		rem_btn.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
		     	removeAccount();
		   }
		});		
		
		jPanel3.add(jPanel1);
		jPanel1.setBounds(10, 10, 355, 140);
		
		jPanel2.setLayout(null);
		
		jPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Manage Priviliges", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		
		jPanel2.add(jScrollPane2);
		jScrollPane2.setBounds(15, 30, 195, 170);
		
		jScrollPane3.setViewportView(privList);
		
		jPanel2.add(jScrollPane3);
		jScrollPane3.setBounds(220, 30, 120, 140);
		
		update_btn.setText("Update Priviliges");
		jPanel2.add(update_btn);
		update_btn.setBounds(220, 174, 120, 25);
		update_btn.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
		     	updatePriviliges();
		   }
		});
		
		jPanel3.add(jPanel2);
		jPanel2.setBounds(10, 150, 355, 220);
		
		jTabbedPane1.addTab("Management", jPanel3);
		
		jPanel4.setLayout(null);
		
		jPanel6.setLayout(null);
		
		jPanel6.setBorder(new javax.swing.border.TitledBorder(null, "Import", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		jTextField1.setEditable(false);
		jPanel6.add(jTextField1);
		jTextField1.setBounds(20, 50, 230, 20);
		
		jButton8.setText("Browse...");
		
		jPanel6.add(jButton8);
		jButton8.setBounds(250, 50, 90, 20);
		
		jLabel2.setFont(new java.awt.Font("MS Sans Serif", 1, 11));
		jLabel2.setText("File to import:");
		jPanel6.add(jLabel2);
		jLabel2.setBounds(20, 30, 260, 15);
		
		jPanel4.add(jPanel6);
		jPanel6.setBounds(10, 10, 355, 100);
		
		jPanel7.setLayout(null);
		
		jPanel7.setBorder(new javax.swing.border.TitledBorder(null, "Export", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		jLabel1.setFont(new java.awt.Font("MS Sans Serif", 1, 11));
		jLabel1.setText("Accounts to export:");
		jPanel7.add(jLabel1);
		jLabel1.setBounds(20, 30, 160, 15);
		
		jScrollPane4.setViewportView(expUser);
		
		jPanel7.add(jScrollPane4);
		jScrollPane4.setBounds(20, 50, 160, 80);
		
		jCheckBox2.setText("Export Database Grants");
		jPanel7.add(jCheckBox2);
		jCheckBox2.setBounds(190, 70, 141, 23);
		
		jCheckBox1.setText("Export Global Grants");
		jPanel7.add(jCheckBox1);
		jCheckBox1.setBounds(190, 50, 140, 23);
		
		jCheckBox3.setText("Export Table Grants");
		
		jPanel7.add(jCheckBox3);
		jCheckBox3.setBounds(190, 90, 130, 23);
		
		jCheckBox4.setText("Export Column Grants");
		jPanel7.add(jCheckBox4);
		jCheckBox4.setBounds(190, 110, 140, 23);
		
		jLabel3.setFont(new java.awt.Font("MS Sans Serif", 1, 11));
		jLabel3.setText("Options:");
		jPanel7.add(jLabel3);
		jLabel3.setBounds(190, 30, 150, 15);
		
		jPanel4.add(jPanel7);
		jPanel7.setBounds(10, 170, 355, 160);
		
		jButton7.setText("Export");
		jPanel4.add(jButton7);
		jButton7.setBounds(270, 340, 90, 25);
		
		jButton9.setText("Import");
		jPanel4.add(jButton9);
		jButton9.setBounds(270, 120, 90, 25);
		
		//jTabbedPane1.addTab("Import / Export", jPanel4);
		
		getContentPane().add(jTabbedPane1);
		jTabbedPane1.setBounds(10, 10, 380, 405);
		
		user = new DefaultListModel();
		users.setModel(user);
		
		expuser = new DefaultListModel();
		expUser.setModel(expuser);
		
		privList.setCellRenderer(new CheckListRenderer());
		priv = new DefaultListModel();
		privList.setModel(priv);
		
		privList.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mousePressed(java.awt.event.MouseEvent evt) 
			{
				privListSelect();
		   }
		});
		
		users.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mousePressed(java.awt.event.MouseEvent evt) 
			{
				userSelected();
		   }
		});				
		
		edt_btn.setEnabled( false );
		rem_btn.setEnabled( false );
		
		pack();
	}
	
	public void closeButtonPressed()
	{
		this.dispose();
	}
	
	public void userSelected ()
	{		
		if( users.getSelectedIndex() != -1 )
		{	umcc.initTree(this);
			rem_btn.setEnabled( true );
			edt_btn.setEnabled( true );
			this.setEnabled(jPanel2, true);
			priv.removeAllElements();
		}
		else
		{
			rem_btn.setEnabled( false );
			edt_btn.setEnabled( false );
			this.setEnabled(jPanel2, false );
		}
	}
	
	public void privListSelect ()
	{
		if( privList.getSelectedIndex() != -1 && privList.isEnabled() )
		{	CheckableItem ci = (CheckableItem)priv.getElementAt( privList.getSelectedIndex() );
			ci.setSelected( !ci.isSelected() );
			
			if( ci == all )
			{
				for( int i = 0 ; i < priv.getSize() ; i ++ )
				{
					((CheckableItem)priv.getElementAt( i )).setSelected(ci.isSelected());
					java.awt.Rectangle rect = privList.getCellBounds(i, i);
	      		privList.repaint(rect);
				}
			}
			
			java.awt.Rectangle rect = privList.getCellBounds(privList.getSelectedIndex(), privList.getSelectedIndex());
	      privList.repaint(rect);
	   }
	}
	
	public void createLevelCheckboxList ( Object level )
	{	
		priv.removeAllElements();
		
		if( level instanceof Database || level instanceof Table)
		{
			priv.addElement(this.all);
			priv.addElement(this.select);
			priv.addElement(this.insert);
			priv.addElement(this.update);
			priv.addElement(this.delete);
			priv.addElement(this.create);
			priv.addElement(this.drop);
			priv.addElement(this.grant);
			priv.addElement(this.references);
			priv.addElement(this.index);
			priv.addElement(this.alter);			
		}
		else if ( level instanceof TableColumn )
		{	
			priv.addElement(this.all);
			priv.addElement(this.select);
			priv.addElement(this.insert);
			priv.addElement(this.update);
			priv.addElement(this.references);			
		}
		else
		{	priv.addElement(this.all);
			priv.addElement(this.select);
			priv.addElement(this.insert);
			priv.addElement(this.update);
			priv.addElement(this.delete);
			priv.addElement(this.create);
			priv.addElement(this.drop);
			priv.addElement(this.reload);
			priv.addElement(this.shutdown);
			priv.addElement(this.process);
			priv.addElement(this.file);
			priv.addElement(this.grant);
			priv.addElement(this.references);
			priv.addElement(this.index);
			priv.addElement(this.alter);
		}
		
		for(int i = 0; i < priv.getSize(); i++)
		{
			CheckableItem ci = (CheckableItem)priv.getElementAt(i);
			ci.setSelected(false);
		}
		
		showPriviliges( level );
	}
	
	public void showPriviliges ( Object level )
	{
		User u = (User)user.getElementAt(users.getSelectedIndex());
		
		Vector grants = new Vector();
		
		if( level instanceof Database )
		{	
			grants = u.getLocationGrants( ((Database)level).getName(), Grant.DATABASE_ACCESS );
		}
		else if ( level instanceof Table )
		{
			grants = u.getLocationGrants( ((Table)level).getDatabase().getName()+ "." +((Table)level).getName(), Grant.TABLE_ACCESS );
		}
		else if ( level instanceof TableColumn )
		{
			grants = u.getLocationGrants( ((TableColumn)level).getTable().getDatabase().getName()+ "." + ((TableColumn)level).getTable().getName() + "." + ((TableColumn)level).getName(), Grant.COLUMN_ACCESS );
		}
		else
		{
			grants = u.getLocationGrants( "GLOBAL", Grant.GLOBAL_ACCESS );
		}
		
		for( int i = 0; i < grants.size() ; i++ )
		{
			Grant tmp = (Grant)grants.get(i);
			
			if(tmp.getName().equalsIgnoreCase("SELECT") && tmp.getTranslatedValue())
				select.setSelected(true);
			if(tmp.getName().equalsIgnoreCase("INSERT") && tmp.getTranslatedValue())
				insert.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("UPDATE") && tmp.getTranslatedValue())
				update.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("DELETE") && tmp.getTranslatedValue())
				delete.setSelected(true);					
			if(tmp.getName().equalsIgnoreCase("CREATE") && tmp.getTranslatedValue())
				create.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("DROP") && tmp.getTranslatedValue())
				drop.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("RELOAD") && tmp.getTranslatedValue())
				reload.setSelected(true);					
			if(tmp.getName().equalsIgnoreCase("SHUTDOWN") && tmp.getTranslatedValue())
				shutdown.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("PROCESS") && tmp.getTranslatedValue())
				process.setSelected(true);					
			if(tmp.getName().equalsIgnoreCase("FILE") && tmp.getTranslatedValue())
				file.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("GRANT") && tmp.getTranslatedValue())
				grant.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("REFERENCES") && tmp.getTranslatedValue())
				references.setSelected(true);			
			if(tmp.getName().equalsIgnoreCase("INDEX") && tmp.getTranslatedValue())
				index.setSelected(true);	
			if(tmp.getName().equalsIgnoreCase("ALTER") && tmp.getTranslatedValue())
				alter.setSelected(true);															
		}	
		
		privList.repaint();
	}
	
	public void showDatabaseTreeView( final DatabaseTreeView tv )
	{
		dtv = tv;
		this.jScrollPane2.setViewportView( dtv );
		
		((DefaultMutableTreeNode)((DefaultTreeModel)dtv.getModel()).getRoot()).setUserObject(("GLOBAL - " + ((User)user.getElementAt(users.getSelectedIndex())).getName()));
		
		dtv.addTreeSelectionListener( new TreeSelectionListener()
		{
			public void valueChanged( TreeSelectionEvent e )
			{
				if( e.isAddedPath() )
				{
					final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)e.getPath().getLastPathComponent();
					
					if( selectedNode.getUserObject() instanceof Database )
					{					
						databaseSelected( (Database)selectedNode.getUserObject() );
					}
					if( selectedNode.getUserObject() instanceof Table )
					{					
						tableSelected( (Table) selectedNode.getUserObject() );
					}	
					createLevelCheckboxList(selectedNode.getUserObject());
					selected = selectedNode;
				}
			}
		});
		dtv.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	}	
	
	public void databaseSelected( Database db )
	{
		umcc.showTables( this, db );
	}
	
	public void tableSelected( Table tb )
	{
		umcc.showFields( this, tb );
	}	
	
	public DatabaseTreeView getDatabaseTreeView()
	{
		return dtv;
	}	
	
	public void getUserAccounts()
	{	user.removeAllElements();
		expuser.removeAllElements();
		
		Vector ua = umcc.getUserAccounts();
		
		for( int i = 0 ; i < ua.size(); i ++ )
		{	user.addElement( (User)ua.get(i) );	
			expuser.addElement( (User)ua.get(i) );	
		}
		
		try
		{	userSelected();
		}
		catch( Exception e )
		{
		}
	}
	
	public boolean exists ( String name, String host )
	{
		for(int i = 0; i < user.getSize(); i++)
		{
			User tmp = (User)user.getElementAt(i);
			if(tmp.getName().equalsIgnoreCase(name) && tmp.getHost().equalsIgnoreCase(host) )
			{
				return true;
			}
		}
		return false;
	}
    
	public void addActionPerformed(java.awt.event.ActionEvent evt) 
	{
		UserManagerAddDialog ua = new UserManagerAddDialog( this, umcc , true, null );  
   }
   
   public void editActionPerformed(java.awt.event.ActionEvent evt)
   {
   	if( users.getSelectedIndex() != -1 )
   	{
   		UserManagerAddDialog ua = new UserManagerAddDialog( this, umcc, false, (User)user.getElementAt(users.getSelectedIndex()) );  
   	}
   }
   
   public void setEnabled ( java.awt.Component c, boolean enable )
   {
   	c.setEnabled(enable);
   	if (c instanceof java.awt.Container)
   	{
   		java.awt.Component [] arr = ((java.awt.Container) c).getComponents();
   		for (int j=0;j<arr.length;j++) 
   		{	
   			setEnabled(arr[j],enable); 
   		}
   	}
   	dtv.setVisible(enable);
   }
   
   public void removeAccount ( )
   {	
   	if( users.getSelectedIndex() != -1 )
   	{
   		String result = umcc.deleteUserAccount( (User)user.getElementAt(users.getSelectedIndex()) );
   		if( result.equals("OK") )
			{
				this.getUserAccounts();
				this.showMessage("Useraccount succesfully removed.");
			}
			else
			{
				this.showErrorMessage("Could not remove useraccount." + '\n' + '\n' + "Reason: " + result);
			}
   	}
   }
   
	public void showMessage(String message)
	{
		javax.swing.JOptionPane pane = new javax.swing.JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), javax.swing.JOptionPane.INFORMATION_MESSAGE );  					
	}	
	
	public void showErrorMessage(String message)
	{
		javax.swing.JOptionPane pane = new javax.swing.JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), javax.swing.JOptionPane.WARNING_MESSAGE );  					
	}
	
	public void updatePriviliges ()
	{
		
		if( selected!= null && users.getSelectedIndex() != -1 )
		{
			User u = (User)user.getElementAt(users.getSelectedIndex());
			Object object = ((DefaultMutableTreeNode)selected).getUserObject();
		
			String location = "GLOBAL";
			int type = Grant.GLOBAL_ACCESS;
			
			if( object instanceof Database )
			{
				Database d = (Database)object;
				location = d.getName();
				type = Grant.DATABASE_ACCESS;
			}
			else if ( object instanceof Table )
			{
				Table d = (Table)object;
				location = d.getDatabase().getName() + "." + d.getName();
				type = Grant.TABLE_ACCESS;
			}
			else if ( object instanceof TableColumn )
			{	
				TableColumn d = (TableColumn)object;
				location = d.getTable().getDatabase().getName() + "." + d.getTable().getName() + "." + d.getName() + "";
				type = Grant.COLUMN_ACCESS;
			}
			
			Vector grants = new Vector();
			
			for( int i = 0; i < priv.size(); i++)
			{	
				CheckableItem ci = (CheckableItem)priv.getElementAt(i);
			
				if(ci.toString().toLowerCase().indexOf("select all") == -1 )
				{	
					Grant g = new Grant(ci.toString().toUpperCase(), ci.isSelected(), location, type );
					grants.add( g );
				}
			}
			
			boolean result = umcc.updateGrants(u, grants);
			
			if( result )
				this.showMessage("Priviliges voor user " + u.toString() + " have been succesfully changed.");
			else
				this.showErrorMessage("Error while trying to alter priviliges for user " + u.toString() + ".");
		}
	}	   
   
    private javax.swing.JButton edt_btn;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JButton add_btn;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton rem_btn;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jButton9;
    private javax.swing.JList expUser;
    private javax.swing.JButton update_btn;
    private javax.swing.JList users;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton jButton4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JList privList;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTree jTree1;
    private javax.swing.JTabbedPane jTabbedPane1;   
}

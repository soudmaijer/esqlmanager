/*package esql.gui;

import esql.control.*;
import javax.swing.*;
import java.awt.*;
import java.awt.LayoutManager.*;
import javax.swing.tree.*;
import java.awt.event.*;
import javax.accessibility.*;
import java.sql.*;

public class UserPriviliges extends JDialog //implements MouseListener
{	private ESQLManagerUI jm;	
	private ConnectionWindowCC cwcc;
	
	private boolean is_update = false;
	private User current;
	
	private JTree view;
	private DefaultMutableTreeNode root;
	
	private JList users;
	private JList grants;
	private DefaultListModel usr;
	private DefaultListModel grt;
	private JList privs;
	private DefaultListModel prv;
	private DefaultListModel prv2;
	private DefaultListModel prv3;
	
	private JTabbedPane jtp;
	
	private JTextField name;
	private JTextField host;
	private JPasswordField pass;
	
	private JButton add = new JButton("Add");
	private JButton edt = new JButton("Edit");
	private JButton del = new JButton("Delete");
	
	private CheckableItem all		 = new CheckableItem("[ SELECT ALL ]");
	private CheckableItem select 	 = new CheckableItem("Select");
	private CheckableItem insert 	 = new CheckableItem("Insert");
	private CheckableItem update 	 = new CheckableItem("Update");
	private CheckableItem delete 	 = new CheckableItem("Delete");
	private CheckableItem create 	 = new CheckableItem("Create");
	private CheckableItem drop   	 = new CheckableItem("Drop");
	private CheckableItem reload 	 = new CheckableItem("Reload");
	private CheckableItem shutdown 	 = new CheckableItem("Shutdown");
	private CheckableItem process	 = new CheckableItem("Process");
	private CheckableItem file		 = new CheckableItem("File");
	private CheckableItem grant		 = new CheckableItem("Grant");
	private CheckableItem references = new CheckableItem("References");
	private CheckableItem index 	 = new CheckableItem("Index");
	private CheckableItem alter		 = new CheckableItem("Alter");
	
	private CheckableItem all1		 = new CheckableItem("[ SELECT ALL ]");
	private CheckableItem select1 	 = new CheckableItem("Select");
	private CheckableItem insert1 	 = new CheckableItem("Insert");
	private CheckableItem update1 	 = new CheckableItem("Update");
	private CheckableItem delete1 	 = new CheckableItem("Delete");
	private CheckableItem create1 	 = new CheckableItem("Create");
	private CheckableItem drop1   	 = new CheckableItem("Drop");
	private CheckableItem grant1	 = new CheckableItem("Grant");
	private CheckableItem references1= new CheckableItem("References");
	private CheckableItem index1 	 = new CheckableItem("Index");
	private CheckableItem alter1	 = new CheckableItem("Alter");
	private CheckableItem reload1 	 = new CheckableItem("Reload");
	private CheckableItem shutdown1	 = new CheckableItem("Shutdown");
	private CheckableItem process1	 = new CheckableItem("Process");
	private CheckableItem file1		 = new CheckableItem("File");	
	
	private JButton grant_btn = new JButton("Grant Privileges");
	private JButton revoke_btn = new JButton("Revoke Privileges");

	public UserPriviliges(ESQLManagerUI jm,ConnectionWindowCC cwcc)
	{	// Make this bitch modal
		super((JFrame)jm, true);
		// Set variables
		this.jm = jm;
		this.cwcc = cwcc;
		
		// Tabbedpane
		jtp = new JTabbedPane();
		jtp.setPreferredSize(new Dimension(400,250));
		jtp.setSize(400,400);
		this.getContentPane().add(jtp);
//		jtp.addMouseListener(this);
		
		// Add 'User' tab
		JPanel user = new JPanel();
		jtp.addTab(" User management ", user);
		user.setLayout(null);

		// Add 'Priviliges' tab
		JPanel tree = new JPanel();
		jtp.addTab(" Privileges ",tree);
		tree.setLayout(null);
		
		// Pack Frame for correct sizing
		this.pack();		
		
// PRIVILIGES TAB COMPONENTS
/*
		// Root node for tree
		root = new DefaultMutableTreeNode(new TreeObject(cwcc.getTitle(), true,false,false,false,false));
		
		// Create tree
		view = new JTree(root);
		view.setCellRenderer(new DBTreeCellRenderer(jm.getImageLoader()));
		view.addMouseListener(this);

		// Scrollpane for tree
		JScrollPane jsp = new JScrollPane(view);
		jsp.setBounds(5,5,225, tree.getHeight() - 10);
		tree.add(jsp);

		// Create Privileges
		privs = new JList();
		privs.setCellRenderer(new CheckListRenderer());
		JScrollPane jsp3 = new JScrollPane(privs);
		privs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		privs.addMouseListener(this);
      	jsp3.setBounds(235,5,160,tree.getHeight() - 70);
		tree.add(jsp3);
		
		prv = new DefaultListModel();
		prv2 = new DefaultListModel();
		prv3 = new DefaultListModel();
		
		prv.addElement(all1);
		prv.addElement(select1);
		prv.addElement(insert1);
		prv.addElement(update1);
		prv.addElement(delete1);
		prv.addElement(create1);
		prv.addElement(drop1);
		prv.addElement(grant1);
		prv.addElement(references1);
		prv.addElement(index1);
		prv.addElement(alter1);
		
		prv2.addElement(all1);
		prv2.addElement(select1);
		prv2.addElement(insert1);
		prv2.addElement(update1);
		prv2.addElement(delete1);
		prv2.addElement(create1);
		prv2.addElement(drop1);
		prv2.addElement(reload1);
		prv2.addElement(shutdown1);
		prv2.addElement(process1);
		prv2.addElement(file1);		
		prv2.addElement(grant1);
		prv2.addElement(references1);
		prv2.addElement(index1);
		prv2.addElement(alter1);
		
		prv3.addElement(all1);
		prv3.addElement(select1);
		prv3.addElement(insert1);
		prv3.addElement(update1);
		prv3.addElement(references1);		
		
		privs.setModel(prv);
		
		grant_btn.setBounds(235,tree.getHeight() - 60,160,25);
		tree.add(grant_btn);
//		grant_btn.addMouseListener(this);
		revoke_btn.setBounds(235,tree.getHeight() - 30,160,25);
		tree.add(revoke_btn);
//		revoke_btn.addMouseListener(this);
		
// ADD USER COMPONENTS	

		users = new JList();
		JScrollPane jsp1 = new JScrollPane(users);
		jsp1.setBounds(5,5,190,user.getHeight() - 10);
		user.add(jsp1);
		usr = new DefaultListModel();
		users.setModel(usr);
	//	users.addMouseListener(this);
		
		grants = new JList();
		grt = new DefaultListModel();
		grants.setCellRenderer(new CheckListRenderer());
		grants.setModel(grt);
		
		JScrollPane jsp2 = new JScrollPane(grants);
		grants.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		grants.addMouseListener(this);
      	jsp2.setBounds(205,110,190,user.getHeight() - 115);
		user.add(jsp2);
		
		grt.addElement(all);
		grt.addElement(select);
		grt.addElement(insert);
		grt.addElement(update);
		grt.addElement(delete);
		grt.addElement(create);
		grt.addElement(drop);
		grt.addElement(reload);
		grt.addElement(shutdown);
		grt.addElement(process);
		grt.addElement(file);
		grt.addElement(grant);
		grt.addElement(references);
		grt.addElement(index);
		grt.addElement(alter);
		
		JLabel jl = new JLabel("Name: ");
		jl.setBounds(205,5,40,19);
		user.add(jl);
		
		name = new JTextField();
		name.setBounds(245, 5, 150,20);
		user.add(name);
		
		JLabel jl1 = new JLabel("Host: ");
		jl1.setBounds(205,30,40,19);
		user.add(jl1);
		
		host = new JTextField();
		host.setBounds(245,30, 150,20);
		user.add(host);
		
		JLabel jl2 = new JLabel("Pass: ");
		jl2.setBounds(205,55,40,19);
		user.add(jl2);			
		
		pass = new JPasswordField();
		pass.setBounds(245,55, 150,20);
		user.add(pass);			
		
		add.setBounds(205,80,55,25);
		user.add(add);	

		edt.setBounds(265,80,55,25);
		user.add(edt);
		
		del.setBounds(325, 80, 69,25);
		user.add(del);
		/*
		add.addMouseListener(this);
		edt.addMouseListener(this);
		del.addMouseListener(this);
				
// CONSTRUCTOR END	
	
		// Frame properties
		this.setTitle("User Privileges");
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
		this.setVisible( true );
		//loadUsers();
	
	}
	
// GENERAL FUNCTIONS
	/*
	public void loadUsers()
	{	usr.removeAllElements();
		root.removeAllChildren();
		try
		{	ResultSet rs = cw.getDBAccess().processSystemQuery("mysql", "SELECT * FROM user");
			while (rs.next())
			{	User u = new User(rs.getString(1), rs.getString(2), rs.getString(3));
				u.select = translateGrant(rs.getString(4));
				u.insert = translateGrant(rs.getString(5));
				u.update = translateGrant(rs.getString(6));
				u.delete = translateGrant(rs.getString(7));
				u.create = translateGrant(rs.getString(8));
				u.drop   = translateGrant(rs.getString(9));
				u.reload = translateGrant(rs.getString(10));
				u.shutdown = translateGrant(rs.getString(11));
				u.process = translateGrant(rs.getString(12));
				u.file = translateGrant(rs.getString(13));
				u.grant = translateGrant(rs.getString(14));
				u.references = translateGrant(rs.getString(15));
				u.index = translateGrant(rs.getString(16));
				u.alter = translateGrant(rs.getString(17));
				
				usr.addElement( u );
				
				TreeObject usr = new TreeObject(u.name+ "@" + u.host,false,false,false,false,false);
				usr.setUser(true);
				DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(usr);
				root.add(dmtn);
			}
			all.setSelected(false);
			select.setSelected(false);
			insert.setSelected(false);
			update.setSelected(false);
			delete.setSelected(false);
			create.setSelected(false);
			drop.setSelected(false);
			reload.setSelected(false);
			shutdown.setSelected(false);
			process.setSelected(false);
			file.setSelected(false);
			grant.setSelected(false);
			references.setSelected(false);
			index.setSelected(false);
			alter.setSelected(false);			
			
			grants.updateUI();
			users.updateUI();
			view.updateUI();
			
			this.show();
		}
		catch(Exception e)
		{	JOptionPane pane = new JOptionPane();  
			pane.setMessageType(JOptionPane.ERROR_MESSAGE);  
			pane.showMessageDialog( jm, "Access denied, not enough rights for this tool.", "Access Denied", JOptionPane.INFORMATION_MESSAGE  );
			this.dispose();
		}
	}
	
	public boolean translateGrant(String name)
	{	if(name.equals("Y"))
			return true;
		return false;
	}
	
// MOUSELISTENER	
	
	public void mousePressed(MouseEvent e)
	{	Object eventSource = e.getSource();
		
		if( eventSource == view )
	  	{	view.setSelectionRow(view.getRowForLocation(e.getX(), e.getY()));
	  	}
	}
	
	public void mouseReleased(MouseEvent e)
	{	Object eventSource = e.getSource();
	
		if( eventSource == view )
		{	DefaultMutableTreeNode node = (DefaultMutableTreeNode)view.getLastSelectedPathComponent();
			if(node != null)
			{	TreeObject selected = (TreeObject)node.getUserObject();
				is_update = false;
				if(selected!= null && selected.isUser() )
				{	cw.getDBAccess().loadDatabases(view, node);
					privs.setModel(prv2);
					String username = node.getUserObject().toString().substring(0, node.getUserObject().toString().indexOf("@"));
					String hostname = node.getUserObject().toString().substring(node.getUserObject().toString().indexOf("@")+1, node.getUserObject().toString().length());
					current = new User(username, hostname, "");
					try
					{	ResultSet rs = cw.getDBAccess().processSystemQuery("mysql","SELECT * FROM user where user='" + username + "' and host='" + hostname + "'");
						while (rs.next())
						{	is_update = true;
							current.select = translateGrant(rs.getString(4));
							current.insert = translateGrant(rs.getString(5));
							current.update = translateGrant(rs.getString(6));
							current.delete = translateGrant(rs.getString(7));
							current.create = translateGrant(rs.getString(8));
							current.drop   = translateGrant(rs.getString(9));
							current.reload = translateGrant(rs.getString(10));
							current.shutdown = translateGrant(rs.getString(11));
							current.process = translateGrant(rs.getString(12));
							current.file = translateGrant(rs.getString(13));							
							current.grant = translateGrant(rs.getString(14));
							current.references = translateGrant(rs.getString(15));
							current.index = translateGrant(rs.getString(16));
							current.alter = translateGrant(rs.getString(17));
						}					
					}
					catch(Exception ex)
					{	System.out.println(ex.getMessage());
					}									
				}					
				// Get database name.
				else if(selected!= null && selected.isDatabase() )
				{	cw.getDBAccess().loadTables(view, node);
					privs.setModel(prv);
					DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					current = new User(username, hostname, "");
					try
					{	ResultSet rs = cw.getDBAccess().processSystemQuery("mysql","SELECT * FROM db WHERE db='" + selected.getName() +  "' AND host='" + hostname + "' AND user='" + username + "'");
						while (rs.next())
						{	is_update = true;
							current.select = translateGrant(rs.getString(4));
							current.insert = translateGrant(rs.getString(5));
							current.update = translateGrant(rs.getString(6));
							current.delete = translateGrant(rs.getString(7));
							current.create = translateGrant(rs.getString(8));
							current.drop   = translateGrant(rs.getString(9));
							current.grant = translateGrant(rs.getString(10));
							current.references = translateGrant(rs.getString(11));
							current.index = translateGrant(rs.getString(12));
							current.alter = translateGrant(rs.getString(13));
						}					
					}
					catch(Exception ex)
					{	
					}				
				}
				else if(selected!= null && selected.isTable() )
				{	cw.getDBAccess().loadFields(view, (DefaultMutableTreeNode)node.getParent(),node);
					privs.setModel(prv);
					DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					current = new User(username, hostname, "");
					try
					{	ResultSet rs = cw.getDBAccess().processSystemQuery("mysql","SELECT * FROM tables_priv WHERE db='" + d.getUserObject() +  "' AND host='" + hostname + "' AND user='" + username + "' AND table_name='" + selected.getName() + "'");						
						while (rs.next())
						{	is_update = true;
							String values= rs.getString(7).toLowerCase();
							if(values.indexOf("select") != -1)
								current.select = true;
							if(values.indexOf("insert") != -1)	
								current.insert = true;
							if(values.indexOf("update") != -1)
								current.update = true;
							if(values.indexOf("delete") != -1)
								current.delete = true;
							if(values.indexOf("create") != -1)
								current.create = true;
							if(values.indexOf("drop") != -1)
								current.drop  = true;
							if(values.indexOf("grant") != -1)
								current.grant = true;
							if(values.indexOf("references") != -1)
								current.references = true;
							if(values.indexOf("index") != -1)
								current.index = true;
							if(values.indexOf("alter") != -1)
								current.alter = true;
						}					
					}
					catch(Exception ex)
					{
					}								
				}	
				else if(selected!= null && selected.isField() )								
				{	cw.getDBAccess().loadFields(view, (DefaultMutableTreeNode)node.getParent(),node);
					privs.setModel(prv3);
					DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode t = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					current = new User(username, hostname, "");
					try
					{	ResultSet rs = cw.getDBAccess().processSystemQuery("mysql","SELECT * FROM columns_priv WHERE db='" + d.getUserObject() +  "' AND host='" + hostname + "' AND user='" + username + "' AND table_name='" + t.getUserObject() + "' AND column_name='" + selected.getName() + "'");						
						while (rs.next())
						{	is_update = true;
							String values= rs.getString(7).toLowerCase();
							if(values.indexOf("select") != -1)
								current.select = true;
							if(values.indexOf("insert") != -1)	
								current.insert = true;
							if(values.indexOf("update") != -1)
								current.update = true;
							if(values.indexOf("references") != -1)
								current.references = true;
						}					
					}
					catch(Exception ex)
					{
					}								
				}
				
				if(current != null)
				{	all1.setSelected(false);
					select1.setSelected(current.select);
					insert1.setSelected(current.insert);
					update1.setSelected(current.update);
					delete1.setSelected(current.delete);
					create1.setSelected(current.create);
					drop1.setSelected(current.drop);
					reload1.setSelected(current.reload);
					process1.setSelected(current.process);
					shutdown1.setSelected(current.shutdown);
					file1.setSelected(current.file);
					grant1.setSelected(current.grant);
					references1.setSelected(current.references);
					index1.setSelected(current.index);
					alter1.setSelected(current.alter);
				}
				
				privs.updateUI();
				
				Runnable doAppend = new Runnable() 
				{	public void run() 
					{	view.updateUI();
					}
				};           
				SwingUtilities.invokeLater(doAppend);
			}
		}
		else if( eventSource == revoke_btn )
		{	DefaultMutableTreeNode node = (DefaultMutableTreeNode)view.getLastSelectedPathComponent();
			if(node != null)
			{	TreeObject selected = (TreeObject)node.getUserObject();
				if( selected.isUser() )
				{	String username = node.getUserObject().toString().substring(0, node.getUserObject().toString().indexOf("@"));
					String hostname = node.getUserObject().toString().substring(node.getUserObject().toString().indexOf("@")+1, node.getUserObject().toString().length());
					cw.getDBAccess().processQuery(cw.getTable(), "mysql", "UPDATE user SET Select_priv = 'N',  Insert_priv = 'N',  Update_priv = 'N',  Delete_priv = 'N',  Create_priv = 'N',  Drop_priv = 'N',  Reload_priv = 'N',  Shutdown_priv = 'N',  Process_priv = 'N',  File_priv = 'N',  Grant_priv = 'N',  References_priv = 'N',  Index_priv = 'N',  Alter_priv = 'N' WHERE Host = '" + hostname + "' AND User = '" + username + "'");	
					cw.getDBAccess().processQuery(cw.getTable(), "mysql", "FLUSH PRIVILEGES");
				}
				else if( selected.isDatabase() )
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					cw.getDBAccess().processQuery(cw.getTable(), "mysql", "DELETE FROM db WHERE host='" + hostname + "' AND user='" + username + "' AND db='" + selected + "'");	
				}
				else if( selected.isTable() )
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					cw.getDBAccess().processQuery(cw.getTable(), "mysql", "DELETE FROM tables_priv WHERE host='" + hostname + "' AND user='" + username + "' AND db='" + d.getUserObject() + "' AND table_name='" + selected + "'");	
				}
				else if( selected.isField() )
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode t = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					cw.getDBAccess().processQuery(cw.getTable(), "mysql", "DELETE FROM columns_priv WHERE host='" + hostname + "' AND user='" + username + "' AND db='" + d.getUserObject() + "' AND table_name='" + t.getUserObject() + "' AND column_name='" + selected + "'");	
				}								
			}
			e.setSource(view);
			mouseReleased(e);
		}
		else if( eventSource == grant_btn )
		{	DefaultMutableTreeNode node = (DefaultMutableTreeNode)view.getLastSelectedPathComponent();
			if(node != null)
			{	TreeObject selected = (TreeObject)node.getUserObject();
				if( selected.isUser() )
				{	String username = node.getUserObject().toString().substring(0, node.getUserObject().toString().indexOf("@"));
					String hostname = node.getUserObject().toString().substring(node.getUserObject().toString().indexOf("@")+1, node.getUserObject().toString().length());
					String grant = "UPDATE mysql.user SET ";
					
					if(select1.isSelected())
						grant = grant + "select_priv='Y', ";
					else
						grant = grant + "select_priv='N', ";
					if(insert1.isSelected())
						grant = grant + "Insert_priv='Y', ";
					else
						grant = grant + "Insert_priv='N', ";
					if(update1.isSelected())
						grant = grant + "Update_priv='Y', ";	
					else
						grant = grant + "Update_priv='N', ";		
					if(delete1.isSelected())
						grant = grant + "Delete_priv='Y', ";
					else
						grant = grant + "Delete_priv='N', ";	
					if(create1.isSelected())
						grant = grant + "Create_priv='Y', ";
					else
						grant = grant + "Create_priv='N', ";	
					if(drop1.isSelected())
						grant = grant + "Drop_priv='Y', ";
					else
						grant = grant + "Drop_priv='N', ";	
					if(reload1.isSelected())
						grant = grant + "Reload_priv='Y', ";	
					else
						grant = grant + "Reload_priv='N', ";		
					if(shutdown1.isSelected())
						grant = grant + "Shutdown_priv='Y', ";
					else
						grant = grant + "Shutdown_priv='N', ";	
					if(process1.isSelected())
						grant = grant + "Process_priv='Y', ";	
					else
						grant = grant + "Process_priv='N', ";	
					if(file1.isSelected())
						grant = grant + "File_priv='Y', ";
					else
						grant = grant + "File_priv='N', ";	
					if(references1.isSelected())
						grant = grant + "References_priv='Y', ";	
					else
						grant = grant + "References_priv='N', ";				
					if(index1.isSelected())
						grant = grant + "Index_priv='Y', ";			
					else
						grant = grant + "Index_priv='N', ";				
					if(alter1.isSelected())
						grant = grant + "Alter_priv='Y', ";
					else
						grant = grant + "Alter_priv='N', ";	
					if(grant1.isSelected())
						grant = grant + "Grant_priv='Y' ";
					else
						grant = grant + "Grant_priv='N' ";						
						
					grant = grant + " WHERE User='" + username + "' AND Host='" + hostname + "'";
					cw.getDBAccess().processQuery(cw.getTable(), "",grant);
					cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
				}
				else if ( selected.isDatabase() ) 
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					String grant;
					
					if(is_update)
						grant = "UPDATE mysql.db SET ";
					else
						grant = "INSERT INTO mysql.db SET ";
						
					if(select1.isSelected())
						grant = grant + "select_priv='Y', ";
					else
						grant = grant + "select_priv='N', ";
					if(insert1.isSelected())
						grant = grant + "Insert_priv='Y', ";
					else
						grant = grant + "Insert_priv='N', ";
					if(update1.isSelected())
						grant = grant + "Update_priv='Y', ";	
					else
						grant = grant + "Update_priv='N', ";		
					if(delete1.isSelected())
						grant = grant + "Delete_priv='Y', ";
					else
						grant = grant + "Delete_priv='N', ";	
					if(create1.isSelected())
						grant = grant + "Create_priv='Y', ";
					else
						grant = grant + "Create_priv='N', ";	
					if(drop1.isSelected())
						grant = grant + "Drop_priv='Y', ";
					else
						grant = grant + "Drop_priv='N', ";	
					if(references1.isSelected())
						grant = grant + "References_priv='Y', ";	
					else
						grant = grant + "References_priv='N', ";				
					if(index1.isSelected())
						grant = grant + "Index_priv='Y', ";			
					else
						grant = grant + "Index_priv='N', ";				
					if(alter1.isSelected())
						grant = grant + "Alter_priv='Y', ";
					else
						grant = grant + "Alter_priv='N', ";	
					if(grant1.isSelected())
						grant = grant + "Grant_priv='Y' ";
					else
						grant = grant + "Grant_priv='N' ";						
						
					if(!grant.equals("UPDATE tables_priv SET table_priv='") && !grant.equals("INSERT INTO tables_priv SET table_priv='"))
						grant = grant.substring(0, grant.length()-1);				
					
					if(is_update)
						grant = grant + " WHERE User='" + username + "' AND Host='" + hostname + "' AND db='" + selected + "'";
					else
						grant = grant + ", User='" + username + "', Host='" + hostname + "', db='" + selected + "'";
						
					cw.getDBAccess().processQuery(cw.getTable(), "",grant);
					cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
				}
				else if ( selected.isTable() )
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					
					String grant;
					if(is_update)
						grant = "UPDATE tables_priv SET table_priv='";
					else
						grant = "INSERT INTO tables_priv SET table_priv='";
					
					if(select1.isSelected())
						grant = grant + "select,";
					if(insert1.isSelected())
						grant = grant + "insert,";
					if(update1.isSelected())
						grant = grant + "update,";	
					if(delete1.isSelected())
						grant = grant + "delete,";
					if(create1.isSelected())
						grant = grant + "create,";
					if(drop1.isSelected())
						grant = grant + "drop,";
					if(references1.isSelected())
						grant = grant + "references,";	
					if(index1.isSelected())
						grant = grant + "index,";			
					if(alter1.isSelected())
						grant = grant + "alter,";
					if(grant1.isSelected())
						grant = grant + "grant ";
						
					if(!grant.equals("UPDATE tables_priv SET table_priv='") && !grant.equals("INSERT INTO tables_priv SET table_priv='"))
						grant = grant.substring(0, grant.length()-1);
	
					if( is_update )
						grant = grant + "' WHERE User='" + username + "' AND Host='" + hostname + "' AND db='" + d.getUserObject() + "' AND table_name='" + selected + "'";
					else
						grant = grant + "', user='" + username + "', host='" + hostname + "', db='" + d.getUserObject() + "', table_name='" + selected + "'";
	
					cw.getDBAccess().processQuery(cw.getTable(), "",grant);
					cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
				}
				else if ( selected.isField() )
				{	DefaultMutableTreeNode u = (DefaultMutableTreeNode)node.getParent().getParent().getParent();
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getParent().getParent();
					DefaultMutableTreeNode t = (DefaultMutableTreeNode)node.getParent();
					String username = u.getUserObject().toString().substring(0, u.getUserObject().toString().indexOf("@"));
					String hostname = u.getUserObject().toString().substring(u.getUserObject().toString().indexOf("@")+1, u.getUserObject().toString().length());
					
					String grant;
					if(is_update)
						grant = "UPDATE columns_priv SET column_priv='";
					else
						grant = "INSERT INTO columns_priv SET column_priv='";
					
					if(select1.isSelected())
						grant = grant + "select,";
					if(insert1.isSelected())
						grant = grant + "insert,";
					if(update1.isSelected())
						grant = grant + "update,";	
					if(references1.isSelected())
						grant = grant + "references,";	
						
					if(!grant.equals("UPDATE columns_priv SET column_priv='") && !grant.equals("INSERT INTO columns_priv SET column_priv='"))
						grant = grant.substring(0, grant.length()-1);
	
					if( is_update )
						grant = grant + "' WHERE User='" + username + "' AND Host='" + hostname + "' AND db='" + d.getUserObject() + "' AND table_name='" + t.getUserObject() + "' AND column_name='" + selected + "'";
					else
						grant = grant + "', user='" + username + "', host='" + hostname + "', db='" + d.getUserObject() + "', table_name='" + t.getUserObject() + "', column_name='" + selected + "'";
	
					cw.getDBAccess().processQuery(cw.getTable(), "",grant);
					cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");								
				}
			}
			e.setSource(view);
			mouseReleased(e);
		}
		else if( eventSource == users )
		{	if(users.getSelectedIndex() != -1)
			{	User u = (User)usr.getElementAt(users.getSelectedIndex());
				
				name.setText(u.name);
				host.setText(u.host);
				pass.setText(u.password);
				
				all.setSelected(false);
				select.setSelected(u.select);
				insert.setSelected(u.insert);
				update.setSelected(u.update);
				delete.setSelected(u.delete);
				create.setSelected(u.create);
				drop.setSelected(u.drop);
				reload.setSelected(u.reload);
				shutdown.setSelected(u.shutdown);
				process.setSelected(u.process);
				file.setSelected(u.file);
				grant.setSelected(u.grant);
				references.setSelected(u.references);
				index.setSelected(u.index);
				alter.setSelected(u.alter);
				grants.updateUI();
			}
		}
		else if(eventSource == add && add.isEnabled())
		{	String query = "GRANT ";
			if(select.isSelected())
				query = query + "Select, ";
			if(insert.isSelected())
				query = query + "Insert, ";
			if(update.isSelected())
				query = query + "Update, ";	
			if(delete.isSelected())
				query = query + "Delete, ";
			if(create.isSelected())
				query = query + "Create, ";
			if(drop.isSelected())
				query = query + "Drop, ";
			if(reload.isSelected())
				query = query + "Reload, ";	
			if(shutdown.isSelected())
				query = query + "Shutdown, ";
			if(process.isSelected())
				query = query + "Process, ";	
			if(file.isSelected())
				query = query + "File, ";
			if(references.isSelected())
				query = query + "References, ";			
			if(index.isSelected())
				query = query + "Index, ";	
			if(alter.isSelected())
				query = query + "Alter, ";
			
			// remove extra comma	
			if(query.length() != 6)
				query = query.substring(0, query.length() - 2) + " ";
			else
				query = "GRANT USAGE ";
			
			String hst = host.getText();
			if(hst.trim().length() == 0)
			{	hst = "%";
			}
			if(new String(pass.getPassword()).trim().length() != 0)
				query = query + "ON *.* TO '" + name.getText() + "'@'" + hst + "' IDENTIFIED BY '" + new String(pass.getPassword()) + "' ";					
			else
				query = query + "ON *.* TO '" + name.getText() + "'@'" + hst + "' ";						
									
			if(grant.isSelected())
				query = query + "WITH GRANT OPTION";															
			
			if(cw.getDBAccess().processQuery(cw.getTable(), "",query).equals("UPDATE"))
			{	JOptionPane pane = new JOptionPane();  
				cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
				name.setText("");
				host.setText("");
				pass.setText("");
				loadUsers();
				pane.setMessageType(JOptionPane.OK_OPTION);  
				pane.showMessageDialog( jm, "User has been succesfully added.", "Add new user", JOptionPane.INFORMATION_MESSAGE  );  
			}
		}
		else if(eventSource == del)
		{	if(users.getSelectedIndex() != -1)
			{	User u = (User)usr.getElementAt(users.getSelectedIndex());
				cw.getDBAccess().processQuery(cw.getTable(), "", "DELETE FROM mysql.user WHERE Host='" + u.host + "' AND User='" + u.name + "'");
				cw.getDBAccess().processQuery(cw.getTable(), "", "DELETE FROM mysql.db WHERE Host='" + u.host + "' AND User='" + u.name + "'");
				cw.getDBAccess().processQuery(cw.getTable(), "", "DELETE FROM mysql.tables_priv WHERE Host='" + u.host + "' AND User='" + u.name + "'");
				cw.getDBAccess().processQuery(cw.getTable(), "", "DELETE FROM mysql.columns_priv WHERE Host='" + u.host + "' AND User='" + u.name + "'");
				cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
				loadUsers();
				name.setText("");
				host.setText("");
				pass.setText("");
				JOptionPane pane = new JOptionPane();  
				pane.setMessageType(JOptionPane.OK_OPTION);  
				pane.showMessageDialog( jm, "User succesfully dropped.", "Delete user", JOptionPane.INFORMATION_MESSAGE  );  
			}
		}
		else if(eventSource == edt)
		{	if(users.getSelectedIndex() != -1)
			{	User u = (User)usr.getElementAt(users.getSelectedIndex());
								
				boolean change = false;
				String q1 = "UPDATE mysql.user SET ";
				
				if(!u.name.equals(name.getText()))
				{	q1 = q1 + "User='" + name.getText() + "', ";
					change = true;
				}
				
				if(!u.host.equals(host.getText()))
				{	q1 = q1 + "Host='" + host.getText() + "', ";
					change = true;
				}				
								
				if(!u.password.equals(new String(pass.getPassword())))
				{	if(new String(pass.getPassword()).trim().length() != 0)
					{	JOptionPane pane = new JOptionPane();
						pane.setMessageType(JOptionPane.WARNING_MESSAGE);
						int returnval = pane.showConfirmDialog( jm, "Are you sure you want to use an empty password for this user? ", "Edit User", JOptionPane.YES_NO_OPTION);		
					
						if(returnval == JOptionPane.YES_OPTION)
						{	q1 = q1 + "Password=password('" + new String(pass.getPassword()) + "'), ";
							change = true;
						}
						else
						{	return;
						}
					}
				}
				
				if(change)
				{	q1 = q1.substring(0, q1.length()-2) + " WHERE User='" + u.name + "' AND Host='" + u.host + "'";
					cw.getDBAccess().processQuery(cw.getTable(), "mysql",q1);
					cw.getDBAccess().processQuery(cw.getTable(), "mysql",q1.replaceAll("mysql.user", "mysql.db"));
					cw.getDBAccess().processQuery(cw.getTable(), "mysql",q1.replaceAll("mysql.user", "mysql.tables_priv"));
					cw.getDBAccess().processQuery(cw.getTable(), "mysql",q1.replaceAll("mysql.user", "mysql.columns_priv"));
				}		
				
				String grant = "UPDATE mysql.user SET ";
				if(select.isSelected())
					grant = grant + "select_priv='Y', ";
				else
					grant = grant + "select_priv='N', ";
				if(insert.isSelected())
					grant = grant + "Insert_priv='Y', ";
				else
					grant = grant + "Insert_priv='N', ";
				if(update.isSelected())
					grant = grant + "Update_priv='Y', ";	
				else
					grant = grant + "Update_priv='N', ";		
				if(delete.isSelected())
					grant = grant + "Delete_priv='Y', ";
				else
					grant = grant + "Delete_priv='N', ";	
				if(create.isSelected())
					grant = grant + "Create_priv='Y', ";
				else
					grant = grant + "Create_priv='N', ";	
				if(drop.isSelected())
					grant = grant + "Drop_priv='Y', ";
				else
					grant = grant + "Drop_priv='N', ";	
				if(reload.isSelected())
					grant = grant + "Reload_priv='Y', ";	
				else
					grant = grant + "Reload_priv='N', ";		
				if(shutdown.isSelected())
					grant = grant + "Shutdown_priv='Y', ";
				else
					grant = grant + "Shutdown_priv='N', ";	
				if(process.isSelected())
					grant = grant + "Process_priv='Y', ";	
				else
					grant = grant + "Process_priv='N', ";	
				if(file.isSelected())
					grant = grant + "File_priv='Y', ";
				else
					grant = grant + "File_priv='N', ";	
				if(references.isSelected())
					grant = grant + "References_priv='Y', ";	
				else
					grant = grant + "References_priv='N', ";				
				if(index.isSelected())
					grant = grant + "Index_priv='Y', ";			
				else
					grant = grant + "Index_priv='N', ";				
				if(alter.isSelected())
					grant = grant + "Alter_priv='Y', ";
				else
					grant = grant + "Alter_priv='N', ";	
				if(this.grant.isSelected())
					grant = grant + "Grant_priv='Y' ";
				else
					grant = grant + "Grant_priv='N' ";						
					
				grant = grant + " WHERE User='" + u.name + "' AND Host='" + u.host + "'";
				if(cw.getDBAccess().processQuery(cw.getTable(), "",grant).equals("UPDATE"))
				{	cw.getDBAccess().processQuery(cw.getTable(), "","FLUSH PRIVILEGES");
					loadUsers();
					name.setText("");
					host.setText("");
					pass.setText("");
					
					String msg = "User privileges succesfully changed.";
					if(change)
					{	msg= "User credentials and privileges succesfully changed.";
					}
					
					JOptionPane pane = new JOptionPane();  
					pane.setMessageType(JOptionPane.OK_OPTION);  
					pane.showMessageDialog( jm, msg, "Edit user privileges", JOptionPane.INFORMATION_MESSAGE  );  
				}
				else
				{	JOptionPane pane = new JOptionPane();  
					pane.setMessageType(JOptionPane.OK_OPTION);  
					pane.showMessageDialog( jm, "Error while changing user privileges. Try reconnecting, or check your privileges", "Edit user privileges", JOptionPane.ERROR_MESSAGE  );  	
				}
			}
		}
		else if( eventSource == jtp)
		{	loadUsers();
		}
	}
	
	public void mouseEntered(MouseEvent e)
	{
	}
	
	public void mouseExited(MouseEvent e)
	{
	}
	
	public void mouseClicked(MouseEvent e)
	{	Object eventSource = e.getSource();
		
		if(eventSource == grants)
		{	int ind = grants.locationToIndex(e.getPoint());
        	CheckableItem item = (CheckableItem)grants.getModel().getElementAt(ind);
        	item.setSelected(! item.isSelected());
        	Rectangle rect = grants.getCellBounds(ind, ind);
        	
        	if( item == all )
        	{	select.setSelected(all.isSelected()); 	 
				insert.setSelected(all.isSelected()); 	  	 
				update.setSelected(all.isSelected()); 	  	 
				delete.setSelected(all.isSelected()); 	  	 
				create.setSelected(all.isSelected()); 	  	 
				drop.setSelected(all.isSelected()); 	    	 
				reload.setSelected(all.isSelected()); 	  	 
				shutdown.setSelected(all.isSelected()); 	  
				process.setSelected(all.isSelected()); 	 	 
				file.setSelected(all.isSelected()); 	 	 
				grant.setSelected(all.isSelected()); 	 	 
				references.setSelected(all.isSelected()); 	 
				index.setSelected(all.isSelected()); 	  	
				alter.setSelected(all.isSelected()); 
				grants.repaint();	 	
        	}
        	else if( !item.isSelected() )
        	{	all.setSelected( false );
        		grants.repaint();
        	}
        	grants.repaint(rect);
        }
		if(eventSource == privs)
		{	int ind = privs.locationToIndex(e.getPoint());
        	CheckableItem item = (CheckableItem)privs.getModel().getElementAt(ind);
        	item.setSelected(! item.isSelected());
        	
        	if( item == all1 )
        	{	select1.setSelected(all1.isSelected()); 	 
				insert1.setSelected(all1.isSelected()); 	  	 
				update1.setSelected(all1.isSelected()); 	  	 
				delete1.setSelected(all1.isSelected()); 	  	 
				create1.setSelected(all1.isSelected()); 	  	 
				drop1.setSelected(all1.isSelected()); 	    	 
				reload1.setSelected(all1.isSelected()); 	  	 
				shutdown1.setSelected(all1.isSelected()); 	  
				process1.setSelected(all1.isSelected()); 	 	 
				file1.setSelected(all1.isSelected()); 	 	 
				grant1.setSelected(all1.isSelected()); 	 	 
				references1.setSelected(all1.isSelected()); 	 
				index1.setSelected(all1.isSelected()); 	  	
				alter1.setSelected(all1.isSelected()); 
				privs.repaint();	 	
        	}
        	else if( !item.isSelected() )
        	{	all1.setSelected( false );
        	}
        	
        	Rectangle rect = privs.getCellBounds(ind, ind);
        	privs.repaint(rect);
        }        
	}
}
*/
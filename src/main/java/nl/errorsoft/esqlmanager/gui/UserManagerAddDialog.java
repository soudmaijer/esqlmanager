package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.User;

public class UserManagerAddDialog extends javax.swing.JDialog 
{
	 private javax.swing.JButton jButton2;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JCheckBox jCheckBox1;
    
    private UserManagerCC umcc;
    private UserManagerUI umui;
    private User u;
    
    public UserManagerAddDialog(UserManagerUI umui, UserManagerCC umcc, boolean add, User u ) 
    {
        super(umui, true);
        
        this.getRootPane().setPreferredSize(new java.awt.Dimension(340, 225));
        this.umcc = umcc;
        this.umui = umui;
        this.u = u;
                
        initComponents( add, u );
        
        this.setLocation(umui.getLocation().x + (int)((umui.getSize().width - this.getSize().width) / 2), umui.getLocation().y + (int)((umui.getSize().height - this.getSize().height) / 2));
        
        this.setResizable(false);
        this.setVisible(true);
    }
    
	private void initComponents( boolean add, User u ) 
	{
		jPanel1 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jTextField3 = new javax.swing.JTextField();
		jCheckBox1 = new javax.swing.JCheckBox();
		jPasswordField1 = new javax.swing.JPasswordField();
		jButton1 = new javax.swing.JButton();
		jButton2 = new javax.swing.JButton();
		
		getContentPane().setLayout(null);
		
		setTitle("Add New User");
		
		jPanel1.setLayout(null);
		
		if( add )
		{	
			jPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Add New User", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		}
		else
		{
			jPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Edit User", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("MS Sans Serif", 1, 11)));
		}
		jLabel1.setText("Name:");
		jLabel1.setToolTipText("null");
		jPanel1.add(jLabel1);
		jLabel1.setBounds(20, 40, 60, 15);
		
		jPanel1.add(jTextField1);
		jTextField1.setBounds(110, 40, 190, 21);
		
		jLabel2.setText("Password:");
		jPanel1.add(jLabel2);
		jLabel2.setBounds(20, 70, 90, 15);
		
		jLabel3.setText("Host:");
		jPanel1.add(jLabel3);
		jLabel3.setBounds(20, 100, 90, 15);
		
		jPanel1.add(jTextField3);
		jTextField3.setBounds(110, 100, 190, 21);
		
		if( add )
		{	
			jCheckBox1.setText("Grant global access (ALL RIGHTS)");
			jPanel1.add(jCheckBox1);
			jCheckBox1.setBounds(110, 130, 200, 23);
		}
		
		jPanel1.add(jPasswordField1);
		jPasswordField1.setBounds(110, 70, 190, 21);
		
		getContentPane().add(jPanel1);
		jPanel1.setBounds(10, 10, 320, 170);
		
		if( add )
		{	
			jButton1.setText("Add");
			jButton1.addActionListener(new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) 
				{
		       	addActionPerformed(evt);
		   	}
			});
		}
		else
		{
			jButton1.setText("Edit");
			jButton1.addActionListener(new java.awt.event.ActionListener() 
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) 
				{
		       	editActionPerformed(evt);
		   	}
			});			
			
			jTextField1.setText(u.getName());
			jPasswordField1.setText(u.getPassword());
			jTextField3.setText(u.getHost());
		}
		getContentPane().add(jButton1);
		jButton1.setBounds(160, 190, 80, 25);
		
		jButton2.setText("Cancel");
		jButton2.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
	       	cancelActionPerformed(evt);
	   	}
		});		
		getContentPane().add(jButton2);
		jButton2.setBounds(250, 190, 80, 25);
		
		pack();
	}
	
	private void addActionPerformed (java.awt.event.ActionEvent evt)
	{
		if( !umui.exists( jTextField1.getText(), jTextField3.getText()) )
		{
			String result = umcc.createUserAccount( new User( jTextField3.getText(), jTextField1.getText(), new String(jPasswordField1.getPassword() ) ), jCheckBox1.isSelected() );
			
			if( result.equals("OK") )
			{
				umui.getUserAccounts();
				this.showMessage("New useraccount succesfully added.");
				this.dispose();
			}
			else
			{
				this.showErrorMessage("Cannot create useraccount." + '\n' + '\n' + "Reason: " + result);
			}
		}
		else
		{
			this.showErrorMessage("Could not create useraccount, account already exists.");
		}
	}
	
	private void editActionPerformed (java.awt.event.ActionEvent evt)
	{	
		String result = umcc.editUserAccount( new User( jTextField3.getText(), jTextField1.getText(), new String(jPasswordField1.getPassword() ) ), u );
		
		if( result.equals("OK") )
		{
			umui.getUserAccounts();
			this.showMessage("The useraccount has been succesfully modified.");
			this.dispose();
		}
		else
		{
			this.showErrorMessage("Could not modify useraccount." + '\n' + '\n' + "Reason: " + result);
		}
	}	

	private void cancelActionPerformed (java.awt.event.ActionEvent evt)
	{	
		this.dispose();
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
}
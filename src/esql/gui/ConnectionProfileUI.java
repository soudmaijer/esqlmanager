package esql.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import esql.domain.*;
import esql.control.*;

public class ConnectionProfileUI extends JDialog implements ItemListener, ActionListener
{	
	private JButton btnConnect;
	private JButton btnSave;
	private JButton btnDelete;
	
	private JPasswordField pw;
	private JTextField un;
	private JTextField ip;
	private JTextField pt;
	private JTextField dbs;
	private String ipt = "";
	private String ptt = "";
	private String unt = "";
	
	private ESQLManagerUI jm;
	private ConnectionProfileCC cpcc;
	private JComboBox jc;
	private JComboBox jcServer;
	private JCheckBox chkAutoConnect;
	private boolean useAutoConnect = true;
	private ServerType [] sta;

 
	public ConnectionProfileUI( ESQLManagerUI jm, ConnectionProfileCC cpcc )
	{	
		super((JFrame)jm, false);

		this.jm = jm;
		this.cpcc = cpcc;
		this.setTitle("Connect to SQL server...");
		this.setResizable(false);
		this.getContentPane().setLayout(null);
		sta = ServerType.getServerTypes();
		
		JLabel j = new JLabel("Profile: ");
		j.setBounds(10,14,100,15);
		this.getContentPane().add(j);
		
		jc = new JComboBox( new DefaultComboBoxModel() );
		jc.setEditable( true );

		jc.setBounds(100,10,205,21 );
		this.getContentPane().add(jc);

		JLabel lblServer = new JLabel("Server type: ");
		lblServer.setBounds(10,44,100,15);
		this.getContentPane().add(lblServer);
		
		jcServer = new JComboBox( new DefaultComboBoxModel() );
		jcServer.setEditable( false );
		jcServer.setBounds(100,40,205,21 );
		this.getContentPane().add(jcServer);
		
		JLabel j1 = new JLabel("Host: ");
		j1.setBounds(10,74,100,15);
		this.getContentPane().add(j1);
		
		ip = new JTextField("");
		ip.setMargin(new java.awt.Insets(1, 1, 1, 1));
		ip.setBounds(100,70,205,21);
		this.getContentPane().add(ip);
		
		JLabel j2 = new JLabel("Username: ");
		j2.setBounds(10,104,100,15);
		this.getContentPane().add(j2);
		
		un = new JTextField("");
		un.setMargin(new java.awt.Insets(1, 1, 1, 1));
		un.setBounds(100,100,205,21);
		this.getContentPane().add(un);
		
		JLabel j3 = new JLabel("Password: ");
		j3.setBounds(10,134,100,15);
		this.getContentPane().add(j3);
		
		pw = new JPasswordField();
		pw.setMargin(new java.awt.Insets(1, 1, 1, 1));
		pw.setBounds(100,130,205,21);
		this.getContentPane().add(pw);
		
		JLabel j4 = new JLabel("Port: ");
		j4.setBounds(10,164,100,15);
		this.getContentPane().add(j4);
		
		pt = new JTextField("");
		pt.setMargin(new java.awt.Insets(1, 1, 1, 1));
		pt.setBounds(100,160,205,21);
		this.getContentPane().add(pt);
		
		JLabel j5 = new JLabel("Database(s) ( Comma seperated, example: db1,db2,db3 ): ");
		j5.setBounds(10,194,300,15);
		this.getContentPane().add(j5);
		
		dbs = new JTextField("");
		dbs.setMargin(new java.awt.Insets(1, 1, 1, 1));
		dbs.setBounds(10,214,295,21);
		this.getContentPane().add(dbs);

		chkAutoConnect = new JCheckBox("Auto-connect to this server on startup");
		chkAutoConnect.setBounds( 6, 249,300,21 );
		this.getContentPane().add(chkAutoConnect);
	
		JPanel jp = new JPanel();
		jp.setLayout( new FlowLayout( FlowLayout.LEFT ) );
		
		btnConnect = new JButton("Connect");
		btnConnect.setPreferredSize( new Dimension( 95, 23 ) );
		jp.add(btnConnect);
		
		btnSave = new JButton("Save");
		btnSave.setPreferredSize( new Dimension( 95, 23 ) );
		jp.add(btnSave);

		btnDelete = new JButton("Delete");
		btnDelete.setPreferredSize( new Dimension( 95, 23 ) );
		jp.add( btnDelete );
		jp.setBounds( 5, 284, 400, 30 );
		this.getContentPane().add(jp);
				
		btnSave.addActionListener(this);		
		btnConnect.addActionListener(this);		
		btnDelete.addActionListener(this);		
		
		for( int t=0; t<sta.length; t++ )
			jcServer.addItem( sta[t] );
		
		this.getRootPane().setPreferredSize( new Dimension( 320, 320 ) );
		this.pack();
		this.setLocation( jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2) );
		
		jc.addItemListener(this);
		jcServer.addItemListener(this);
	}

	public void loadProfiles( ConnectionProfile [] p )
	{
		DefaultComboBoxModel dcm = new DefaultComboBoxModel();
		boolean foundLastUsed = false;
		
		if( p == null || p.length == 0 )
		{
			ip.setText("");
			pt.setText("");
			pw.setText("");
			un.setText("");
			dbs.setText("");
		}
		
		for(int i = 0; i<p.length; i++ )
		{	
			dcm.addElement( p[i] );
			
			if( p[i].isLastUsed() )
			{	
				foundLastUsed = true;
				dcm.setSelectedItem( p[i] );
				pt.setText(p[i].getPort());
				ip.setText(p[i].getHost());
				un.setText(p[i].getUsername());
				pw.setText(p[i].getPassword());
				dbs.setText(p[i].getDatabases());
				chkAutoConnect.setSelected( p[i].isAutoConnect() );
				
				for( int t=0; t<sta.length; t++ )
				{	if( p[i].getServerType().getType() == sta[t].getType() )
					{	jcServer.setSelectedItem( sta[t] );
					}
				}
			}
			else
			{
				if( !foundLastUsed && i == p.length-1 )
				{
					pt.setText(p[0].getPort());
					ip.setText(p[0].getHost());
					un.setText(p[0].getUsername());
					pw.setText(p[0].getPassword());				
					dbs.setText(p[0].getDatabases());
					chkAutoConnect.setSelected( p[0].isAutoConnect() );
				}
			}			
		}
		
		jc.setModel( dcm );
		btnConnect.setRequestFocusEnabled( true );
		btnConnect.requestFocus( true );
	}
	
	public void setSelectedProfile( ConnectionProfile cp )
	{
		DefaultComboBoxModel dcm = (DefaultComboBoxModel)jc.getModel();
		dcm.setSelectedItem( cp );
	}
	
	public void itemStateChanged(ItemEvent e)
	{	
		Object src = e.getSource();

		if( src == jc && jc.getSelectedItem() != null && jc.getSelectedItem() instanceof ConnectionProfile )
		{
			ConnectionProfile cp = (ConnectionProfile)jc.getSelectedItem();
			pt.setText( cp.getPort());
			ip.setText( cp.getHost());
			un.setText( cp.getUsername());
			pw.setText( cp.getPassword());				
			dbs.setText( cp.getDatabases());
			chkAutoConnect.setSelected( cp.isAutoConnect() );
			
			for( int i=0; i<jcServer.getItemCount();i++ )
			{
				ServerType temp = (ServerType)jcServer.getItemAt(i);
				
				if( temp.getType() == cp.getServerType().getType() )
					jcServer.setSelectedIndex( i );
			}
		}
	}

	public String getDatabases()
	{
		return dbs.getText().trim();
	}
	
	public String getName()
	{
		if( jc.getSelectedItem() instanceof ConnectionProfile )
			return ((ConnectionProfile)jc.getSelectedItem()).getName();
		return (String)jc.getSelectedItem();
	}
	
	public int getPort()
	{
		if( pt.getText().trim().length() == 0 )
			return -1;
				
		return Integer.parseInt( pt.getText() );
	}

	public String getPortAsString()
	{
		if( pt.getText().trim().length() == 0 )
			return "";
				
		return pt.getText().trim();
	}

	public String getAddress()
	{
		if( ip.getText().trim().length() == 0 )
			return "";

		return ip.getText().trim();
	}
	
	public String getUsername()
	{
		if( un.getText().trim().length() == 0 )
			return "";
		
		return un.getText().trim();
	}

	public String getPassword()
	{
		if( new String( pw.getPassword() ).trim().length() == 0 )
			return "";
		return new String( pw.getPassword() ).trim();
	}	
	
	public void actionPerformed( java.awt.event.ActionEvent event )
	{
		Object object = event.getSource();

		if (object == btnConnect )
		{
			if( jc.getSelectedItem() != null )
			{
				ConnectionProfile temp =(ConnectionProfile)jc.getSelectedItem();
				temp.setHost( this.getAddress() );
				temp.setPort( Integer.toString( this.getPort() ) );
				temp.setUsername( this.getUsername() );
				temp.setPassword( this.getPassword() );
				temp.setServerType( (ServerType)jcServer.getSelectedItem() );
				temp.setDatabases( this.getDatabases() );
				cpcc.connect( temp );				
			}				
		}
		else if (object == btnSave )
		{
			if( jc.getSelectedItem() instanceof ConnectionProfile )
			{
				cpcc.editProfile( (ConnectionProfile)jc.getSelectedItem(), getName(), (ServerType)jcServer.getSelectedItem(), getAddress(), getPortAsString(), getUsername(), getPassword(), getDatabases(), chkAutoConnect.isSelected() );
			}
			else
			{
				cpcc.addProfile( getName(), (ServerType)jcServer.getSelectedItem(), getAddress(), getPortAsString(), getUsername(), getPassword(), getDatabases(), chkAutoConnect.isSelected() );
			}
		}
		else if (object == btnDelete )
		{
			JOptionPane pane = new JOptionPane();  
			int i = pane.showConfirmDialog( this, "Delete selected profile?", this.getTitle(), JOptionPane.YES_NO_OPTION );  					
			
			if( i == JOptionPane.YES_OPTION && jc.getSelectedIndex() > -1 )
				cpcc.deleteProfile( (ConnectionProfile)jc.getSelectedItem() );
		}			
	}	

	public void showMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.INFORMATION_MESSAGE );  					
	}	
	
	public void showErrorMessage(String message)
	{
		JOptionPane pane = new JOptionPane();  
  		pane.showMessageDialog( this, message, this.getTitle(), JOptionPane.WARNING_MESSAGE );  					
	}	
}
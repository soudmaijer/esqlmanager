package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.domain.*;
import nl.errorsoft.esqlmanager.data.*;
import nl.errorsoft.esqlmanager.control.*;
import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class Processlist extends JDialog implements Runnable, ActionListener
{
	private ESQLManagerUI jm;
	private nl.errorsoft.esqlmanager.domain.ConnectionProfile cp;
	private JScrollPane jsp;
	private JTable jtable;
	private JButton btnKillProcess;
	private JLabel lblInterval;
	private boolean refresh = true;
	private DefaultTableModel dtm;
	ConnectionWindowCC cwcc;
	DatabaseConnection m;
	
	public Processlist( ConnectionWindowCC cwcc, JFrame parent )
	{
		super( parent, false );
		this.cwcc = cwcc;
		initComponents();
		
		this.cp = cwcc.getConnectionProfile();
		this.jm = jm;
		this.addWindowListener( new WindowAdapter()
		{
			public void windowClosing( WindowEvent we )
			{
				refresh = false;
			}
			
		});
		this.setTitle( cp.getUsername() +"@"+ cp.getHost() +" - active processes");
		this.setSize( 400, 200 );
		this.setLocation(parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2));
		this.setVisible( true );
		
		Thread t = new Thread( this );
		t.start();
	}
	
	public void run()
	{
		try
		{
			m = new DatabaseConnection();
			m.connect( cp, "" );			
			int selRow = 0;
			DefaultTableModel dtm = null;

			while( refresh )
			{
				if( !m.getConnection().isClosed() )
				{
					if( jtable.getSelectedRow() > 0 )
						selRow = jtable.getSelectedRow();
					
					dtm = new DefaultTableModel();
					dtm.addColumn("Id");
					dtm.addColumn("User");
					dtm.addColumn("Host");
					dtm.addColumn("Database"); 
					dtm.addColumn("Command");
					dtm.addColumn("Time");
					dtm.addColumn("Info");					
					
					// Get processes and add all.
					ResultSet rs = m.executeQuery("SHOW PROCESSLIST");
					Object [] data = new Object [7];
					
					while( rs.next() )
					{
						data[0] = rs.getString("Id");
						data[1] = rs.getString("User");
						data[2] = rs.getString("Host");
						data[3] = rs.getString("db");
						data[4] = rs.getString("Command");
						data[5] = rs.getString("Time");
						data[6] = rs.getString("Info");
						
						dtm.addRow( data );
					}
					rs.close();
					jtable.setModel( dtm );
					jtable.setRowSelectionInterval( selRow, selRow );
										
					Runnable doAppend = new Runnable() 
					{
						public void run() 
						{	jtable.updateUI();
						}
					};           
					SwingUtilities.invokeLater(doAppend);					
					
					
					for( int i=5; i>0; i-- )
					{
						this.lblInterval.setText( Integer.toString(i) );
						Thread.sleep( 1000 );
						
					}
				}
			}
			m.close();
		}
		catch( Exception e )
		{
			System.out.println( e.getMessage() );
		}
	}
	
	public void initComponents()
	{
		jtable = new JTable()
		{
			public boolean isCellEditable(int row, int col) 
			{
				return false;
			}
		};
		jtable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		jtable.setAutoResizeMode( jtable.AUTO_RESIZE_OFF );
		
		jsp = new JScrollPane( jtable );
		jsp.getViewport().setBackground( java.awt.Color.white );
		this.getContentPane().add( jsp, BorderLayout.CENTER );
		
		JPanel p = new JPanel();
		btnKillProcess = new JButton("Kill process");
		btnKillProcess.addActionListener( this );
		p.add( btnKillProcess );
		this.getContentPane().add( p, BorderLayout.SOUTH );
		
		JPanel p1 = new JPanel();
		JLabel lblIntervalMsg = new JLabel("Refreshing in:");
		p.add( lblIntervalMsg );
		lblInterval = new JLabel();
		p.add( lblInterval );
	}
	
	public void actionPerformed( ActionEvent e )
	{
		Object source = e.getSource();
		
		if( source == btnKillProcess )
		{
			DefaultTableModel d = (DefaultTableModel)jtable.getModel();
			
			if( jtable.getSelectedRow() > -1 )
			{
				try
				{
					m.executeUpdate("KILL "+ jtable.getValueAt( jtable.getSelectedRow(), 0 ) ); 
				}
				catch( Exception ae )
				{
					System.out.println( ae.getMessage() );
				}
			}	
		}
	}
}
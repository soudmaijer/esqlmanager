package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerProcess;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import nl.errorsoft.esql.table.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.data.*;
import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class Processlist extends JDialog implements Runnable, ActionListener
{
	private static final Logger log = LogManager.getLogger( Processlist.class );

	private ESQLManagerUI jm;
	private nl.errorsoft.esql.connection.ConnectionProfile cp;
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
					for( ServerProcess process : cp.getServerType().getDialect().listProcesses( m ) )
					{
						dtm.addRow( new Object [] { process.getId(), process.getUser(), process.getHost(), process.getDatabase(), process.getCommand(), process.getTime(), process.getInfo() } );
					}
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
			log.error( e.getMessage(), e );
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
		jsp.getViewport().setBackground( UIManager.getColor( "Table.background" ) );
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
					cp.getServerType().getDialect().killProcess( m, jtable.getValueAt( jtable.getSelectedRow(), 0 ).toString() );
				}
				catch( Exception ae )
				{
					log.error( ae.getMessage(), ae );
				}
			}	
		}
	}
}
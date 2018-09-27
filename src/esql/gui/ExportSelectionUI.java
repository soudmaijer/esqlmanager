package esql.gui;

import esql.control.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ExportSelectionUI extends JDialog implements ActionListener
{
	JPanel jPanel1 = new JPanel();
	JRadioButton jRadioButton1 = new JRadioButton();
	JRadioButton jRadioButton2 = new JRadioButton();
	JLabel jLabel1 = new JLabel();
	JButton jButton1 = new JButton();
	JButton jButton2 = new JButton();
	private ESQLManagerUI jm;
	private ExportCC ecc;
	
	public ExportSelectionUI( ExportCC ecc, ESQLManagerUI jm ) 
	{
		try 
		{
			this.jm = jm;
			this.ecc = ecc;
			jbInit();
		}
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
  	}
  	
	void jbInit() throws Exception 
	{
		this.setTitle("Export data");
		this.setResizable( false );
		this.getContentPane().setLayout(null);
		this.setSize(new Dimension(265, 200));
		jRadioButton1.setText("As CSV comma-seperated");
		jRadioButton1.setBounds(new Rectangle(10, 30, 175, 23));
		jRadioButton2.setText("As SQL statements");
		jRadioButton2.setBounds(new Rectangle(10, 7, 172, 23));
		jLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
		jLabel1.setText("Export data options:");
		jLabel1.setBounds(new Rectangle(30, 20, 161, 15));
		jPanel1.setLayout(null);
		jPanel1.setBorder(BorderFactory.createEtchedBorder());
		jPanel1.setBounds(new Rectangle(29, 47, 194, 59));
		jButton1.setBounds(new Rectangle(29, 120, 94, 21));
		jButton1.setText("Cancel");
		jButton1.addActionListener( this );
		jButton2.setBounds(new Rectangle(127, 120, 95, 21));
		jButton2.setText("Next");
		jButton2.addActionListener( this );
		jPanel1.add(jRadioButton2, null);
		jPanel1.add(jRadioButton1, null);
		jRadioButton1.setEnabled( false );
		jRadioButton2.setSelected( true );
		this.getContentPane().add(jButton1, null);
		this.getContentPane().add(jButton2, null);
		this.getContentPane().add(jLabel1, null);
		this.getContentPane().add(jPanel1, null);
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
  	}
  	
  	public void actionPerformed( ActionEvent e )
  	{
  		if( e.getSource() == this.jButton1 )
  			dispose();
  		else if( e.getSource() == this.jButton2 )
  		{
  			if( !jRadioButton1.isSelected() && !jRadioButton2.isSelected() )
  			{
  				showErrorMessage( "Select an export option!");
  			}
  			else
  			{
  				// CSV
  				if( jRadioButton1.isSelected() )
  					;
  				// SQL
  				else if( jRadioButton2.isSelected() )
  					ecc.startExportSQLUI();
  				this.dispose();
  			}
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
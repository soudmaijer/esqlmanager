package nl.errorsoft.esqlmanager.gui;

/*
 * DriverUI.java
 *
 * Created on 6 april 2003, 0:51
 */

import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author  CoolKillaH
 */
public class DriverUI extends javax.swing.JDialog implements ActionListener, ItemListener {

	private DatabaseDriverCC dbcc;

    /** Creates new form DriverUI */
    public DriverUI( DatabaseDriverCC dbcc, java.awt.Frame parent )
    {
        	super(parent, false);
        	initComponents();
        	this.dbcc = dbcc;
			this.setResizable(false);
			this.setLocation(parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2));
			this.setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        this.getRootPane().setPreferredSize(new java.awt.Dimension(405, 305));
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jtxtURL = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jtxtLibPath = new javax.swing.JTextField();
        jbtnBrowse = new javax.swing.JButton();
        jbtnBrowse.addActionListener( this );
        jLabel11 = new javax.swing.JLabel();
        jtxtClassName = new javax.swing.JTextField();
        jtxtDataOpenChar = new javax.swing.JTextField();
        jtxtDataCloseChar = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jLabel311 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        jtxtFieldOpenChar = new javax.swing.JTextField();
        jtxtFieldCloseChar = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jcmbType = new javax.swing.JComboBox();
        jbtnClose = new javax.swing.JButton();
        jbtnClose.addActionListener( this );
        jbtnSave = new javax.swing.JButton();
        jbtnSave.addActionListener( this );

        getContentPane().setLayout(null);

        setTitle("Driver properties");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                dispose();
            }
        });

        jPanel1.setLayout(null);
        jPanel2.setLayout(null);

        jPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Driver properties", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        jLabel1.setText("Connection URL:");
        jPanel2.add(jLabel1);
        jLabel1.setBounds(15, 55, 100, 20);

        jPanel2.add(jtxtURL);
        jtxtURL.setBounds(120, 55, 220, 20);

        jLabel11.setText("Driver class-name:");
        jPanel2.add(jLabel11);
        jLabel11.setBounds(15, 30, 100, 20);

        jPanel2.add(jtxtClassName);
        jtxtClassName.setBounds(120, 30, 220, 20);

        jPanel2.add(jtxtDataOpenChar);
        jtxtDataOpenChar.setBounds(120, 105, 30, 21);

        jPanel2.add(jtxtDataCloseChar);
        jtxtDataCloseChar.setBounds(250, 105, 30, 21);

        jLabel3.setText("Data close char:");
        jPanel2.add(jLabel3);
        jLabel3.setBounds(160, 105, 90, 20);

        jLabel31.setText("Data open char:");
        jPanel2.add(jLabel31);
        jLabel31.setBounds(15, 105, 90, 20);

        jLabel311.setText("Field open char:");
        jPanel2.add(jLabel311);
        jLabel311.setBounds(15, 80, 90, 20);

        jLabel32.setText("Field close char:");
        jPanel2.add(jLabel32);
        jLabel32.setBounds(160, 80, 90, 20);

        jPanel2.add(jtxtFieldOpenChar);
        jtxtFieldOpenChar.setBounds(120, 80, 30, 21);

        jPanel2.add(jtxtFieldCloseChar);
        jtxtFieldCloseChar.setBounds(250, 80, 30, 21);

        jPanel1.add(jPanel2);
        jPanel2.setBounds(10, 70, 360, 145);

        jPanel3.setLayout(null);

        jPanel3.setBorder(new javax.swing.border.TitledBorder(null, "Database type", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        jPanel3.add(jcmbType);
        jcmbType.setBounds(15, 20, 330, 21);
        jcmbType.addItemListener( this );

        jPanel1.add(jPanel3);
        jPanel3.setBounds(10, 10, 360, 55);

        jTabbedPane1.addTab("JDBC Settings", jPanel1);

        getContentPane().add(jTabbedPane1);
        jTabbedPane1.setBounds(10, 10, 385, 260);

        jbtnClose.setText("Close");
        getContentPane().add(jbtnClose);
        jbtnClose.setBounds(315, 275, 80, 25);

        jbtnSave.setText("Save");
        getContentPane().add(jbtnSave);
        jbtnSave.setBounds(230, 275, 80, 25);

        pack();
    }
		
	public void loadDrivers( DatabaseDriver[] dbda )
	{
		if( dbda != null && dbda.length > 0 )
		{
			javax.swing.DefaultComboBoxModel dcm = new javax.swing.DefaultComboBoxModel();
			
			for( int i=0; i< dbda.length; i++ )
				dcm.addElement( dbda[i] );
			
			this.jcmbType.setModel( dcm );
			
			this.jtxtClassName.setText( dbda[0].getDriverClassName() );
			this.jtxtDataOpenChar.setText( dbda[0].getDataOpenChar() );
			this.jtxtDataCloseChar.setText( dbda[0].getDataCloseChar() );
			this.jtxtFieldOpenChar.setText( dbda[0].getFieldOpenChar() );
			this.jtxtFieldCloseChar.setText( dbda[0].getFieldCloseChar() );
			this.jtxtLibPath.setText( dbda[0].getDriverFilePath() );
			this.jtxtURL.setText( dbda[0].getDriverURL() );
		}
	}
		
	public void actionPerformed( ActionEvent e )
	{
		if( e.getSource() == this.jbtnClose )
			this.dispose();
		else if( e.getSource() == this.jbtnSave )
		{
			dbcc.saveProperties( ((DatabaseDriver)this.jcmbType.getSelectedItem()).getId(), 
										((DatabaseDriver)this.jcmbType.getSelectedItem()).getDriverName(), 
										this.jtxtURL.getText(),
										this.jtxtClassName.getText(), 
										this.jtxtLibPath.getText(),
										this.jtxtFieldOpenChar.getText(),
										this.jtxtFieldCloseChar.getText(), 
										this.jtxtDataOpenChar.getText(), 
										this.jtxtDataCloseChar.getText() );
		}
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{	
		Object src = e.getSource();

		if( src == jcmbType && jcmbType.getSelectedItem() != null && jcmbType.getSelectedItem() instanceof DatabaseDriver )
		{
			DatabaseDriver temp = (DatabaseDriver)jcmbType.getSelectedItem();
			this.jtxtClassName.setText( temp.getDriverClassName() );
			this.jtxtDataOpenChar.setText( temp.getDataOpenChar() );
			this.jtxtDataCloseChar.setText( temp.getDataCloseChar() );
			this.jtxtFieldOpenChar.setText( temp.getFieldOpenChar() );
			this.jtxtFieldCloseChar.setText( temp.getFieldCloseChar() );
			this.jtxtLibPath.setText( temp.getDriverFilePath() );
			this.jtxtURL.setText( temp.getDriverURL() );
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
	

    // Variables declaration - do not modify
    private javax.swing.JButton jbtnClose;
    private javax.swing.JTextField jtxtClassName;
    private javax.swing.JComboBox jcmbType;
    private javax.swing.JTextField jtxtURL;
    private javax.swing.JTextField jtxtFieldCloseChar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JButton jbtnSave;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel311;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton jbtnBrowse;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField jtxtFieldOpenChar;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JTextField jtxtDataCloseChar;
    private javax.swing.JTextField jtxtDataOpenChar;
    private javax.swing.JTextField jtxtLibPath;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration

}
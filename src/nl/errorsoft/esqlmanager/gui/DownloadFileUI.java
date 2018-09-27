package nl.errorsoft.esqlmanager.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import nl.errorsoft.esqlmanager.control.*;
import nl.errorsoft.esqlmanager.domain.*;

/*
 * JDialog.java
 *
 * Created on 9 april 2003, 20:53
 */

/**
 *
 * @author  CoolKillaH
 */
public class DownloadFileUI extends javax.swing.JDialog implements UDDataIF, ActionListener {
    
    private UDDataCC udcc;
    
    /** Creates new form JDialog */
    public DownloadFileUI( UDDataCC udcc, JFrame parent ) 
    {
        //super(parent, modal);
        this.udcc = udcc;
        initComponents();
        this.setLocation( parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2) );
        this.setVisible( true );
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();

        getContentPane().setLayout(null);

        setTitle("Download data");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        getContentPane().add(jProgressBar1);
        jProgressBar1.setBounds(20, 100, 330, 16);

        jLabel1.setText("Press \"Download\" to start the transfer now!");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(20, 75, 270, 15);

        jButton1.setText("Download now!");
        getContentPane().add(jButton1);
        jButton1.setBounds(20, 135, 110, 23);

        jButton2.setText("Cancel");
        getContentPane().add(jButton2);
        jButton2.setBounds(240, 135, 110, 23);

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel2.setText("Select a location on disk:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 20, 150, 15);

        getContentPane().add(jTextField1);
        jTextField1.setBounds(20, 40, 240, 20);

        jButton3.setText("Save as...");
        getContentPane().add(jButton3);
        jButton3.setBounds(260, 40, 90, 21);

        jButton3.addActionListener( this );
        jButton2.addActionListener( this );
        jButton1.addActionListener( this );

        this.getRootPane().setPreferredSize( new java.awt.Dimension( 375, 175 ) );

        pack();
    }//GEN-END:initComponents
    
 	public void setProgressValue( int percentage )
 	{
 		this.jProgressBar1.setValue( percentage );
 		
 		if( percentage == 100 )
 		{
 			showMessage("Download completed!");
 			this.dispose();
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
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
    
	public void actionPerformed( ActionEvent e )
	{
		if( e.getSource() == jButton3 )
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setAcceptAllFileFilterUsed(true);  
			chooser.setDialogTitle("Save data...");   		
			
			try
		   {
			  	if( chooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) 
			  	{
			  		jTextField1.setText( chooser.getSelectedFile().getAbsolutePath() );
				}
			}
			catch( Exception err )
			{
			   JOptionPane pane = new JOptionPane();  
			   pane.setMessageType(JOptionPane.OK_OPTION);  
				pane.showMessageDialog( this, "An error occured while saving data!\n\n"+ err.getMessage(), "Save query", JOptionPane.WARNING_MESSAGE );  
			}	   		
		}
		else if( e.getSource() == jButton2 )
		{
			this.dispose();
		}
		else if( e.getSource() == jButton1 )
		{
			udcc.downloadFile( jTextField1.getText() );
		}
	}    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables
    
}

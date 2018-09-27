package esql.gui;
/*
 * ExportAsSQLUI.java
 *
 * Created on 29 april 2003, 11:50
 */
 
import esql.control.*;
import esql.domain.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.event.*;

/**
 *
 * @author  CoolKillaH
 */
public class ImportAsSQLUI extends javax.swing.JDialog implements ActionListener 
{
   private ESQLManagerUI jm;
	private ImportCC ecc;	
	private DatabaseTreeView dtv;
    
	public ImportAsSQLUI(ESQLManagerUI jm,ImportCC ecc)
	{	
		super((JFrame)jm, false);
		this.jm = jm;
		this.ecc = ecc;
		this.getRootPane().setPreferredSize( new java.awt.Dimension( 560, 300 ) );
		this.initComponents();
		this.setTitle("Import data");
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
		this.setVisible( true );
		this.toFront();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();

        getContentPane().setLayout(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jPanel1.setLayout(null);

        jPanel2.setLayout(null);

        jPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Info", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        jLabel1.setText("<html>1. Select a database or table you want to import data into. If you don`t want to import data into a specific database or table go to step 2.<br><br>2. Press the \"Browse..\" button to select the file containing the sql-statements.<br><br>3. Press the \"Import\" button to start.</html>");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jPanel2.add(jLabel1);
        jLabel1.setBounds(20, 25, 320, 120);

        jPanel1.add(jPanel2);
        jPanel2.setBounds(10, 10, 370, 170);

        jPanel4.setLayout(null);

        jPanel4.setBorder(new javax.swing.border.TitledBorder(null, "Pick File From Disk", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        jTextField1.setEditable(false);
        jPanel4.add(jTextField1);
        jTextField1.setBounds(10, 20, 260, 21);

        jButton1.setText("Browse...");
        jButton1.addActionListener( this );
        jPanel4.add(jButton1);
        jButton1.setBounds(270, 20, 90, 21);

        jPanel1.add(jPanel4);
        jPanel4.setBounds(10, 190, 370, 50);

        jButton2.setText("Import");
        jButton2.addActionListener( this );
        jPanel1.add(jButton2);
        jButton2.setBounds(175, 260, 100, 21);

        jButton3.setText("Close");
        jButton3.addActionListener( this );
        jPanel1.add(jButton3);
        jButton3.setBounds(280, 260, 100, 21);

        getContentPane().add(jPanel1);
        jPanel1.setBounds(170, 0, 390, 300);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(0, 0, 170, 302);

        pack();
    }
    
	// Shows the database tree.
	public void showDatabaseTreeView( DatabaseTreeView tv )
	{
		dtv = tv;
		this.jScrollPane1.getViewport().add( dtv );
		dtv.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	}	
	
	//public void databaseSelected( Database db )
	//{
	//	ecc.showTables( this, db );
	//}
	
	public DatabaseTreeView getDatabaseTreeView()
	{
		return dtv;
	}
	
	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		
		if( src == jButton3 )
		{
			this.dispose();
		}
		else if( src == jButton1 )
		{
			JFileChooser chooser = new JFileChooser();

  			chooser.addChoosableFileFilter( new ExtentionFileFilter("SQL file",new String[]{".sql"})); 
			chooser.setAcceptAllFileFilterUsed( false );  
 			chooser.setDialogTitle("Select file...");   		
				
		   try
		   {
		    	if( chooser.showOpenDialog( jm ) == JFileChooser.APPROVE_OPTION ) 
		    	{
					this.jTextField1.setText( chooser.getSelectedFile().getAbsolutePath() );
				}
			}
			catch( Exception err )
			{
			}
		}
		else if( src == jButton2 )
		{
			if( jTextField1.getText().trim().length() <= 0 )
				showErrorMessage("No file selected!");
			else
				ecc.importNodesAsSQL( this, dtv.getSelectionPath(), jTextField1.getText() );
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
    private void closeDialog(java.awt.event.WindowEvent evt) {
        setVisible(false);
        dispose();
    }
    
    // Variables declaration - do not modify
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton jButton3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration
    
}
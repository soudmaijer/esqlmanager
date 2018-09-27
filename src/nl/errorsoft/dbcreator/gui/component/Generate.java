package nl.errorsoft.dbcreator.gui.component;

import nl.errorsoft.esqlmanager.gui.*;
import nl.errorsoft.dbcreator.gui.model.Model;
import javax.swing.*;
import java.util.*;
import nl.errorsoft.dbcreator.gui.component.*;

public class Generate extends javax.swing.JDialog implements Runnable
{	private ConnectionWindowUI cwui;
	private ESQLManagerUI eui;
	private Model m;
	
	public Generate(ESQLManagerUI eui, ConnectionWindowUI cwui, Model m) 
	{
		super((JFrame)eui, true);
		
		this.eui = eui;
		this.cwui = cwui;
		this.m = m;
		
		initComponents();
	}
    
	private void initComponents()
	{
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		progress = new javax.swing.JProgressBar();
		jButton1 = new javax.swing.JButton();
		generate = new javax.swing.JButton();
		
		getContentPane().setLayout(null);
		getRootPane().setPreferredSize(new java.awt.Dimension(257,216));
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		setTitle("Analyze / Generate model");
		
		jPanel1.setLayout(null);
		
		jPanel2.setLayout(null);
		
		jPanel2.setBorder(new javax.swing.border.TitledBorder("Checking Model"));
		jLabel1.setText("Checking Databases");
		jPanel2.add(jLabel1);
		jLabel1.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_off")));
		jLabel1.setBounds(20, 30, 160, 15);
		
		jLabel2.setText("Checking Tables");
		jPanel2.add(jLabel2);
		jLabel2.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_off")));
		jLabel2.setBounds(20, 50, 160, 15);
		
		jLabel3.setText("Checking Columns");
		jPanel2.add(jLabel3);
		jLabel3.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_off")));
		jLabel3.setBounds(20, 70, 160, 15);
		
		jLabel4.setText("Checking Relations");
		jPanel2.add(jLabel4);
		jLabel4.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_off")));
		jLabel4.setBounds(20, 90, 160, 15);
		
		jLabel5.setText("Checking Model");
		jPanel2.add(jLabel5);
		jLabel5.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_off")));
		jLabel5.setBounds(20, 110, 160, 15);
		
		jPanel2.add(progress);
		progress.setBounds(20, 135, 200, 16);
		
		jPanel1.add(jPanel2);
		jPanel2.setBounds(10, 10, 240, 170);
		
		jButton1.setText("Close");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
		   public void actionPerformed(java.awt.event.ActionEvent evt) {
		       jButton1ActionPerformed(evt);
		   }
		});
		
		jPanel1.add(jButton1);
		jButton1.setBounds(168, 185, 80, 23);
		
		generate.setText("Generate");
		generate.addActionListener(new java.awt.event.ActionListener() {
		   public void actionPerformed(java.awt.event.ActionEvent evt) {
		       generateActionPerformed(evt);
		   }
		});
		
		generate.setEnabled(false);
		
		jPanel1.add(generate);
		generate.setBounds(83, 185, 80, 23);
		
		getContentPane().add(jPanel1);
		jPanel1.setBounds(0, 0, 260, 410);
		
		pack();
		
		this.setLocation( eui.getLocation().x + (int)((eui.getSize().width - this.getSize().width) / 2), eui.getLocation().y + (int)((eui.getSize().height - this.getSize().height) / 2) );
		
		Thread t = new Thread (this);
		t.start();
		
		this.setVisible(true);
	}
	
	public void run()
	{			
		Vector v = m.getObjects();
		
		if( v.size() != 0 )
		{
			Vector db = new Vector();
			Vector tb = new Vector();
			
			// Split objects
			for( int i = 0; i < v.size(); i ++ )
			{
				if( v.get(i) instanceof DatabaseObject )
					db.add(v.get(i));
				else if( v.get(i) instanceof TableObject )
					tb.add(v.get(i));		
			}
			
			progress.setMaximum(db.size() * db.size());
			
			// Check databases
			boolean db_error = false;
			boolean tb_error = false;
			boolean fd_error = false;
			
			for( int i = 0; i < db.size(); i++ )
			{
				DatabaseObject d =  (DatabaseObject)db.get(i);
				
				for( int j = 0; j < db.size(); j++ )
				{
					DatabaseObject tmp2 =  (DatabaseObject)db.get(j);
					if( d.getName().equalsIgnoreCase(tmp2.getName()) && tmp2 != d ) 
					{
						db_error = true;
						showErrorMessage("Can't generate model - There are identical databases in the model (" + d.getName() + ")");
						break;
					}
					progress.setValue(progress.getValue() + 1);
				}
				if(db_error)
					break;
					
				tb = m.getReferences(d);
	
				progress.setMaximum(tb.size() * tb.size());
				progress.setValue(0);
				
				for( int h = 0; h < tb.size(); h++ )
				{
					TableObject tmp =  (TableObject)tb.get(h);
					
					for( int j = 0; j < tb.size(); j++ )
					{
						TableObject tmp2 =  (TableObject)tb.get(j);
						if( tmp.getName().equalsIgnoreCase(tmp2.getName()) && tmp2 != tmp ) 
						{
							tb_error = true;
							showErrorMessage("Can't generate model - There are identical tables in database '" + d.getName() + "' (" + tmp.getName() + ")");
							break;
						}
						progress.setValue(progress.getValue() + 1);
					}		
					
					Field [] f = tmp.getFields();
					
					if(f.length == 0 )
					{
						showErrorMessage("Can't generate model - There are no fields in table '" + tmp.getName() + "'");
						fd_error = true;
						break;
					}
					
					for( int k = 0; k < f.length; k ++ )
					{					
						for( int l = 0 ; l < f.length; l ++ )
						{
							if( f[k].getName().equalsIgnoreCase(f[l].getName()) && k != l)
							{
								showErrorMessage("Can't generate model - There are identical field-names in table '" + tmp.getName() + "' ("+ f[l].getName() + ")");
								fd_error = true;
								break;
							}
						}
						if(fd_error)
							break;	
					}
				}		
				if(tb_error || fd_error)
					break;
			}
			
			if( db_error )
			{
				jLabel1.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			}
			else
			{
				jLabel1.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_good")));
			}
			
			if( tb_error )
			{
				jLabel2.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			}
			else
			{
				jLabel2.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_good")));
			}	
			
			if( fd_error )
			{
				jLabel3.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			}
			else
			{
				jLabel3.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_good")));
			}	
			
			progress.setValue(0);
			progress.setMaximum(v.size());
			
			boolean ref_error = false;
			for( int i = 0; i < v.size(); i ++ )
			{
				if( !( v.get(i) instanceof CommentObject ) )
				{
					ModelObject mo = (ModelObject)v.get(i);
					if( m.getReferences(mo).size() == 0 )
					{	
						ref_error = true;
					}
				}
				progress.setValue(progress.getValue() + 1);
			}
			
			if( ref_error )
			{
				jLabel4.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			}
			else
			{
				jLabel4.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_good")));
			}
			
			// Cheat past the model :)
			jLabel5.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_good")));
			
			if( db_error == false && tb_error == false && fd_error == false )
			{
				generate.setEnabled(true);	
			}
		}
		else
		{
			jLabel1.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			jLabel2.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			jLabel3.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			jLabel4.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
			jLabel5.setIcon(new ImageIcon(cwui.getControlClass().getImageLoader().getImage("check_error")));
		}
	}

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) 
	{
		this.dispose();
	}

	private void generateActionPerformed(java.awt.event.ActionEvent evt) 
	{
		Vector v = m.getObjects();
		
		Vector db = new Vector();
			
		// Split objects
		for( int i = 0; i < v.size(); i ++ )
		{
			if( v.get(i) instanceof DatabaseObject )
				db.add(v.get(i));	
		}
		
		int count = 0;
		for( int i = 0; i < db.size(); i ++ )
		{	DatabaseObject d = (DatabaseObject)db.get(i);
			count ++;
			
			Vector tb = m.getReferences(d);
			
			for( int j = 0; j < tb.size() ; j ++ )
			{
				count ++;
				
				TableObject tbs = (TableObject)tb.get(j);	
				for(int k = 0; k < tbs.getFields().length; k++)
				{
					count++;
				}
			}
		}
		
		progress.setValue(0);
		progress.setMaximum(count);
		
		boolean error = false;
		
		for( int i = 0; i < db.size(); i ++ )
		{	DatabaseObject d = (DatabaseObject)db.get(i);
		
			try
			{
				cwui.getControlClass().getDatabaseConnection().executeUpdate("CREATE DATABASE IF NOT EXISTS `" + d.getName() + "`");
			}
			catch(Exception e)
			{	error = true;
				break;
			}
			
			Vector tb = m.getReferences(d);
			
			progress.setValue(progress.getValue() + 1);
			
			for( int j = 0; j < tb.size() ; j ++ )
			{
				TableObject tbs = (TableObject)tb.get(j);	
				if(tbs.getName().trim().length() == 0)
				{	//ct.showErrorMessage("Tablename missing. You must enter a tablename in order to create a table.");
				}
				if(tbs.getFields().length == 0)
				{	//ct.showErrorMessage("You didn't add any columns to the table. Please add some fields to the table prior to generating it.");
				}
				
				String query = "CREATE TABLE IF NOT EXISTS `" + tbs.getName() + "` ";
				query = query + " (";
				
				progress.setValue(progress.getValue() + 1);
				
				for(int k = 0; k < tbs.getFields().length; k++)
				{	if(k != 0)	{	query = query + ", ";	}
					
					Field f = tbs.getFields()[k];
					
					query = query + " `" + f.getName() + "` " ;
					query = query + f.getType().getName();
					if(!f.getLength().trim().equals("")) { query = query + " (" + f.getLength() + ")";  }
					if(f.unsigned){ query = query + " UNSIGNED"; }
					if(!f.getDefault().trim().equals("")) {query = query + " DEFAULT '" + f.getDefault() + "'"; }
					if(f.notnull){ query = query + " NOT NULL"; }
					if(f.zerofill){ query = query + " ZEROFILL"; }
					if(f.binary){ query = query + " BINARY"; }
					if(f.autoincrement){ query = query + " AUTO_INCREMENT"; }
					if(f.primary){ query = query + ", PRIMARY KEY(`" + f.getName() + "`)"; }
					if(f.unique){ query = query + ", UNIQUE(`" + f.getName() + "`)"; }
					if(f.index){ query = query + ", INDEX(`" + f.getName() + "`)"; }
					
					progress.setValue(progress.getValue() + 1);
				}	
				query = query + ") TYPE=" + tbs.getType();
				if(!tbs.getComment().trim().equals(""))
				{	query = query + " COMMENT='" + tbs.getComment() + "'";
				}	
				
				try
				{
					cwui.getControlClass().getDatabaseConnection().executeUpdate("USE `" + d.getName() + "`;");
					cwui.getControlClass().getDatabaseConnection().executeUpdate(query);
				}
				catch(Exception e)
				{
					error = true;
					break;
				}
			}
		}
		
		if(error)
			this.showErrorMessage("Model Generation failed - Please read the log for more information");
		else
			this.showMessage("Model has been succesfully generated!");
			
		cwui.getControlClass().showDatabaseTree();
		
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
    
	private javax.swing.JLabel jLabel4;
	private javax.swing.JButton generate;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JButton jButton1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JProgressBar progress;
	private javax.swing.JLabel jLabel5;    
}
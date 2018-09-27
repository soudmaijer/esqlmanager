package nl.errorsoft.dbcreator;

import nl.errorsoft.dbcreator.control.*;
import nl.errorsoft.dbcreator.gui.component.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import nl.errorsoft.dbcreator.gui.model.*;

import nl.errorsoft.esqlmanager.gui.*;

import java.io.*;

public class DBCreator extends JDialog implements MouseListener
{	private JMenuBar menu;
	
	private JMenu file = new JMenu("File");
	private JMenuItem file_new = new JMenuItem("New Model...");
	private JMenuItem file_opn = new JMenuItem("Open Model...");
	private JMenuItem file_sav = new JMenuItem("Save Model");
	private JMenuItem file_sva = new JMenuItem("Save Model As...");
	private JMenuItem file_ext = new JMenuItem("Close");
	
	private JMenu edit = new JMenu("Edit");
	private JMenuItem edit_del = new JMenuItem("Delete Selected");
	private JMenuItem edit_sla = new JMenuItem("Select All");
	private JMenuItem edit_dsa = new JMenuItem("Deselect All");
	
	private ModelBrowser mb;
	private ModelViewer mv;
	
	private Properties properties;
	
	private ESQLManagerUI eui;
	private ConnectionWindowUI cwui;

	public DBCreator (ESQLManagerUI eui, ConnectionWindowUI cwui)
	{	super(eui, true);
	
		this.eui = eui;
		this.cwui = cwui;
	
		this.setSize(640,480);
		this.setTitle("eSQLDesigner");	
		
		mv = new ModelViewer( new ModelViewerControl(this) );
		
		JScrollPane jsp = new JScrollPane(mv);
		
		this.getContentPane().add(jsp);
		jsp.getViewport().setBackground(Color.white);
		jsp.setBorder( null );
		
		menu = new JMenuBar();
		this.setJMenuBar(menu);
		
		file.add(file_new);
		file.add(file_opn);
		file.addSeparator();
		file.add(file_sav);
		file.add(file_sva);
		file.addSeparator();
		file.add(file_ext);
		
		file.setMnemonic('F');
		file_new.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK) );
		file_new.setMnemonic('N');
		file_opn.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK) );
		file_new.setMnemonic('O');
		file_sav.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK) );
		file_new.setMnemonic('S');
		file_new.setMnemonic('A');
		file_ext.setMnemonic('X');

		edit.add(edit_sla);
		edit.add(edit_dsa);
		edit.addSeparator();
		edit.add(edit_del);
		
		edit.setMnemonic('E');
		edit_sla.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK) );
		edit_sla.setMnemonic('S');
		edit_dsa.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK) );
		edit_dsa.setMnemonic('D');		
		edit_del.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0) );
		
		file_opn.addMouseListener(this);		
		file_ext.addMouseListener(this);
		edit_sla.addMouseListener(this);
		edit_dsa.addMouseListener(this);
		edit_del.addMouseListener(this);
		file_sav.addMouseListener(this);
		file_sva.addMouseListener(this);
		file_new.addMouseListener(this);
		
		buildMenu();
		
		properties = new Properties(eui);
		
		this.setLocation( eui.getLocation().x + (int)((eui.getSize().width - this.getSize().width) / 2), eui.getLocation().y + (int)((eui.getSize().height - this.getSize().height) / 2) );
		
		this.updateTitle();
		
		this.setVisible( true );
	}
	
	public void updateTitle()
	{
		this.setTitle("eSQLDesigner - '" + mv.getModel().getName() + "'");
	}
	
	public void buildMenu()
	{	menu.removeAll();
		menu.add(file);
		menu.add(edit);
		menu.add(mv.getModelMenu());		
		
		this.getContentPane().add( mv.getToolbar(), BorderLayout.NORTH );	
	}
	
	public void generate ()
	{
		Generate g = new Generate( eui, cwui, mv.getModel() );
	}
	
	public void showProperties ( Object src )
	{	properties.showProperties(src);
		properties.show();	
	}
	
	public void openModel ()
	{	if( mv.isInPlaceMode() )
		{
			mv.exitPlaceMode();
		}
	
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Open existing model");
		
		ModelFilter mf = new ModelFilter("edm", "eSQLManager Database Models (*.edm)");
		jfc.addChoosableFileFilter( mf );
		
		jfc.showOpenDialog(this);
		
		File f = jfc.getSelectedFile();
		if( f != null && f.exists() )
		{	
			try
			{
				Model m = mv.getModel().loadModel(f);
				if( m != null )
				{	
					mv.setModel( m );
					m.setFile(f);
				}
			}
			catch( Exception ex )
			{	ex.printStackTrace();
			}
		}
		
		mv.resize();
	}
	
	public void saveCurrentModel ( boolean auto )
	{	if( auto && mv.getModel().getFile() != null )
		{	String xml = mv.getModel().getModelXML();
			try
			{
				PrintWriter out = new PrintWriter( new FileWriter(mv.getModel().getFile()) );
				out.println(xml);
				out.close();
			}
			catch( Exception ex )
			{	System.out.println(ex.getMessage());
			}	
		}
		else
		{	JFileChooser jfc = new JFileChooser();
			jfc.setDialogTitle("Save model as");
		
			ModelFilter mf = new ModelFilter("edm", "eSQLManager Database Models (*.edm)");
			jfc.addChoosableFileFilter( mf );
		
			if( mv.getModel().getFile() != null )
			{	
				jfc.setSelectedFile(mv.getModel().getFile());	
			}
			else
			{	
				jfc.setSelectedFile(new File(mv.getModel().getName() + ".edm"));	
			}
			
			jfc.showSaveDialog(this);
			
			File f = jfc.getSelectedFile();
			if( f != null )
			{
				String xml = mv.getModel().getModelXML();
				try
				{
					PrintWriter out = new PrintWriter( new FileWriter(f) );
					out.println(xml);
					out.close();
					
					mv.getModel().setFile( f );
				}
				catch( Exception ex )
				{	System.out.println(ex.getMessage());
				}				
			}
		}
	}
	
	public void newModel ()
	{	int result = JOptionPane.showConfirmDialog(this,"Do you want to save the current model before continuing?","New model",JOptionPane.YES_NO_CANCEL_OPTION);
		if(result == JOptionPane.YES_OPTION)
		{	saveCurrentModel( true );
			mv.resetModel();	
		}
		else if ( result == JOptionPane.NO_OPTION )
		{	mv.resetModel();
		}
	}
	
	public ImageLoader getImageList()
	{
		return cwui.getControlClass().getImageLoader();
	}
	
	public void mousePressed( MouseEvent e ){}
	public void mouseClicked( MouseEvent e ){}
	public void mouseEntered( MouseEvent e ){}
	public void mouseExited( MouseEvent e ){}
	public void mouseReleased( MouseEvent e ) 
	{	if( e.getSource() == file_ext )
		{	int result = JOptionPane.showConfirmDialog(this,"Do you want to save the current model before continuing?","New model",JOptionPane.YES_NO_CANCEL_OPTION);
			if(result == JOptionPane.YES_OPTION)
			{	saveCurrentModel( true );
				this.dispose();
			}
			else if ( result == JOptionPane.NO_OPTION )
			{	
				this.dispose();
			}
		}
		if( e.getSource() == file_new )
		{	newModel();
		}		
		if( e.getSource() == file_opn )
		{	int result = JOptionPane.showConfirmDialog(this,"Do you want to save the current model before continuing?","Open model",JOptionPane.YES_NO_CANCEL_OPTION);
			if(result == JOptionPane.YES_OPTION)
			{	saveCurrentModel( true );
				openModel();
			}
			else if ( result == JOptionPane.NO_OPTION )
			{	openModel();
			}
		}		  
		if( e.getSource() == edit_sla )
		{	mv.getModel().selectAll();
		}
		if( e.getSource() == edit_dsa )
		{	mv.getModel().deselectAll();
		}		
		if( e.getSource() == edit_del )
		{	mv.removeSelectedObjects();
		}	
		if( e.getSource() == file_sav )
		{
			saveCurrentModel( true );
		}		
		if( e.getSource() == file_sva )
		{
			saveCurrentModel( false );
		}
		updateTitle();							 
	}	
}
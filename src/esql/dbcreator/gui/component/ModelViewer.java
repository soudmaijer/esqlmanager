package esql.dbcreator.gui.component;

import esql.dbcreator.control.*;
import esql.dbcreator.gui.*;
import esql.dbcreator.gui.model.*;

import java.awt.*;
import javax.swing.*;
import java.util.Vector;
import java.awt.event.*;

public class ModelViewer extends JLayeredPane implements MouseListener, MouseMotionListener, ActionListener, AWTEventListener
{	//	Model for this component
	private Model model;
	
	// ModelViewerControl
	private ModelViewerControl mvc;
	
	// Variables for positioning
	private int srcx = 0;
	private int srcy = 0;
	
	private int xpos = 0;
	private int ypos = 0;
	
	private int w = 0;
	private int h = 0;
	
	private ModelObject src = null;
	private int refx = 0;
	private int refy = 0;
	
	private boolean placemode = false;
	private ModelObject place = null;
	
	private JPopupMenu objectmenu;
	private JMenu objectrel = new JMenu("Remove Relation");
	private JMenuItem remove = new JMenuItem("Remove");
	private JMenuItem props = new JMenuItem("Properties");
	private JMenuItem create_table_db = new JMenuItem("Attach Table");
	private JMenuItem create_comment_mo = new JMenuItem("Attach Comment");
	
	private Vector refs;
	private ModelObject selected;
	
	private JMenu model_menu = new JMenu("Model");
	private JMenuItem create_database = new JMenuItem("Add New Database"); 
	private JMenuItem create_table = new JMenuItem("Add New Table"); 
	private JMenuItem create_comment = new JMenuItem("Add New Comment");
	private JMenuItem show_properties = new JMenuItem("Show Object Properties"); 
	private JMenuItem show_model_properties = new JMenuItem("Show Model Properties"); 
	
	private JMenuItem attach_table = new JMenuItem("Attach Table");
	private JMenuItem attach_comment = new JMenuItem("Attach Comment");
	
	private JToolBar toolbar = new JToolBar ();
	private JButton btn_add_database = new JButton ();
	private JButton btn_add_table = new JButton ();
	private JButton btn_add_comment = new JButton ();
	private JButton btn_properties = new JButton ();
	
	private JButton btn_new = new JButton ();
	private JButton btn_save = new JButton ();
	private JButton btn_open = new JButton ();
	
	private JButton btn_export = new JButton ();
	
	/*
	 	ModelViewer default constructor 
	 */
	public ModelViewer (ModelViewerControl mvc)
	{	this.mvc = mvc;
		this.setLayout( null );
		model = new Model("New Model");
		
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		
		objectmenu = new JPopupMenu();

		objectmenu.add(props);
		objectmenu.add(objectrel);
		objectmenu.addSeparator();
		objectmenu.add(create_table_db);
		objectmenu.add(create_comment_mo);
		objectmenu.addSeparator();		
		objectmenu.add(remove);
		
		create_comment_mo.addMouseListener(this);
		remove.addMouseListener(this);
		props.addMouseListener(this);
		create_table_db.addMouseListener(this);
		
		model_menu.add(create_database);
		model_menu.add(create_table);
		model_menu.add(create_comment);
		model_menu.addSeparator();
		model_menu.add(attach_table);
		model_menu.add(attach_comment);
		model_menu.addSeparator();		
		model_menu.add(show_properties);
		model_menu.add(show_model_properties);
		
		model_menu.setMnemonic('M');
		create_database.setMnemonic('D');
		create_table.setMnemonic('T');
		create_comment.setMnemonic('C');
		
		create_database.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0) );
		create_table.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0) );
		create_comment.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0) );
		attach_table.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0) );		
		attach_comment.setAccelerator( KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0) );
		attach_table.setEnabled(false);
		attach_comment.setEnabled(false);
		show_properties.setEnabled(false);
		
		attach_table.addMouseListener(this);
		attach_comment.addMouseListener(this);
		create_database.addActionListener(this);
		create_table.addActionListener(this);
		create_comment.addActionListener(this);
		show_properties.addActionListener(this);
		show_model_properties.addActionListener(this);
		
		Toolkit.getDefaultToolkit().addAWTEventListener (this, AWTEvent.KEY_EVENT_MASK);
		
		btn_add_database.setIcon(new ImageIcon(mvc.getImageList().getImage("add_database")));
		btn_add_table.setIcon(new ImageIcon(mvc.getImageList().getImage("add_table")));
		btn_add_comment.setIcon(new ImageIcon(mvc.getImageList().getImage("add_comment")));
		btn_properties.setIcon(new ImageIcon(mvc.getImageList().getImage("des_properties")));
		
		btn_new.setIcon(new ImageIcon(mvc.getImageList().getImage("des_new")));
		btn_save.setIcon(new ImageIcon(mvc.getImageList().getImage("des_save")));
		btn_open.setIcon(new ImageIcon(mvc.getImageList().getImage("des_open")));
		
		btn_export.setIcon(new ImageIcon(mvc.getImageList().getImage("des_check")));
		
		btn_add_database.setToolTipText("Add new database");
		btn_add_table.setToolTipText("Add new table");
		btn_add_comment.setToolTipText("Add new comment");
		
		btn_new.setToolTipText("New model");
		btn_save.setToolTipText("Save model");
		btn_open.setToolTipText("Open model");
		
		btn_add_database.addActionListener(this);
		btn_add_table.addActionListener(this);
		btn_add_comment.addActionListener(this);
		btn_properties.addActionListener(this);
		
		btn_new.addActionListener(this);
		btn_open.addActionListener(this);
		btn_save.addActionListener(this);
		
		btn_export.addActionListener(this);
		
		btn_export.setToolTipText("Generate model in database");
		btn_properties.setToolTipText("selected object/model properties");
		
		toolbar.add(btn_new);
		toolbar.add(btn_save);
		toolbar.add(btn_open);
		toolbar.addSeparator();
		toolbar.add(btn_add_database);
		toolbar.add(btn_add_table);
		toolbar.add(btn_add_comment);
		toolbar.addSeparator();
		toolbar.add(btn_properties);
		toolbar.addSeparator();
		toolbar.add(btn_export);
	}
	
	/*
		Returns the model for this component
	*/
	public Model getModel ( )
	{	return this.model;
	}
	
	/*
		Returns the toolbar for this component
	*/
	public JToolBar getToolbar ()
	{
		return toolbar;
	}
	
	/*
		Sets the model for this component
	*/
	public void setModel ( )
	{	this.model = model;
	}
	
	/*
		Function creates a new databaseobject
	*/
	public void createDatabaseObject ( String name )
	{	DatabaseObject db = model.createDatabaseObject(name);
		db.addMouseListener(this);
		db.addMouseMotionListener(this);		
		this.enterPlaceMode(db);
	}
	
	/*
		Function creates a new tableobject
	*/
	public void createTableObject ( String name )
	{	TableObject tb = model.createTableObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);		
		this.enterPlaceMode(tb);	
	}		

	/*
		Function creates a new tableobject, with reference from parent db
	*/
	public void createTableObject ( String name, DatabaseObject db )
	{	TableObject tb = model.createTableObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);		
		model.addReference(db, tb);
		this.enterPlaceMode(tb);
	}	
	
	/*
		Function creates a new commentobject, with reference to parentobject
	*/
	public void createCommentObject ( String name, ModelObject obj )
	{	CommentObject tb = model.createCommentObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);		
		model.addReference(tb, obj);
		this.enterPlaceMode(tb);
	}		
	
	/*
		Function creates a new databaseobject
	*/
	public void createCommentObject ( String name )
	{	CommentObject cm = model.createCommentObject(name);
		cm.addMouseListener(this);
		cm.addMouseMotionListener(this);
		this.enterPlaceMode(cm);
	}
	
	public void enterPlaceMode(ModelObject mo)
	{	this.place = mo;	
		this.placemode = true;
		model.lock();
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.create_comment.setEnabled(false);
		this.create_table.setEnabled(false);
		this.create_database.setEnabled(false);
		
		this.btn_add_comment.setEnabled(false);
		this.btn_add_database.setEnabled(false);
		this.btn_add_table.setEnabled(false);
	}
	
	public void exitPlaceMode()
	{	this.placemode = false;
		model.unlock();
		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.moveToFront(place);
		this.place = null;
		this.create_comment.setEnabled(true);
		this.create_table.setEnabled(true);
		this.create_database.setEnabled(true);		
		
		this.btn_add_comment.setEnabled(true);
		this.btn_add_database.setEnabled(true);
		this.btn_add_table.setEnabled(true);		
	}
	
	public void resetModel()
	{	Model model = new Model("New Model");
		this.model = model;
		this.removeAll();
		this.repaint();
	}
	
	/*
		Function creates a new databaseobject
	*/
	public boolean isInPlaceMode()
	{	return placemode;
	}
	
	/*
	 	Function to remove all selected objects from the model
	 */
	public void removeSelectedObjects ( )
	{	Vector objects = model.removeSelectedObjects();
		for( int i = 0 ; i < objects.size(); i ++ )
		{	this.remove((ModelObject)objects.get(i));
		}
		this.repaint();
	}
	
	/*
	 	Function to get the menu for this model
	 */	
	public JMenu getModelMenu()
	{	return model_menu;
	}
	
	/*
	 	Paintcomponent method to paint selection rectangles
	 	and connections between components
	 */
	public void paintComponent( Graphics g )
	{	super.paintComponent(g);
		Vector objects = model.getObjects();
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.black);
		for( int i = 0 ; i < objects.size(); i++)
		{	ModelObject tmp = (ModelObject)objects.get(i);
			Vector vect = tmp.getReferences();
			
			for( int j = 0 ; j < vect.size(); j++)
			{	ModelObject tmp2 = (ModelObject)vect.get(j);
				
				if( tmp instanceof CommentObject || tmp2 instanceof CommentObject )
				{	if( !tmp.isHidden() && !tmp2.isHidden() )
					{	float [] dash = {3.0f};
						Stroke s = g2.getStroke();
						g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5.0f, dash, 0.0f));
						g2.drawLine(tmp.getX() + (tmp.getWidth()/2), tmp.getY() + (tmp.getHeight()/2), tmp2.getX() + (tmp2.getWidth()/2), tmp2.getY() + (tmp2.getHeight()/2));
						g2.setStroke(s);
					}
				}
				else
				{	if( !tmp.isHidden() && !tmp2.isHidden() )
						g2.drawLine(tmp.getX() + (tmp.getWidth()/2), tmp.getY() + (tmp.getHeight()/2), tmp2.getX() + (tmp2.getWidth()/2), tmp2.getY() + (tmp2.getHeight()/2));
				}				
			}
		}	
		float [] dash = {3.0f};
		Stroke s = g2.getStroke();
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5.0f, dash, 0.0f));
		g2.drawRect(xpos,ypos,w,h);	
		if(src != null)
		{	g2.drawLine(src.getX() + (src.getWidth() / 2), src.getY()+ (src.getHeight() / 2), refx, refy);
		}
		g2.setStroke(s);
	}
	
	public void resize()
	{	Component [] comps = this.getComponents();
		int w = 0;
		int h = 0;
		for( int i = 0; i < comps.length; i++)
		{	if(comps[i].getX() + comps[i].getWidth() > w)
			{	w = comps[i].getX() + comps[i].getWidth();
			}	
			if(comps[i].getY() + comps[i].getHeight() > h)
			{	h = comps[i].getY() + comps[i].getHeight();
			}				
		}
		if( this.getWidth() != w || this.getHeight() != h )
		{
			this.setPreferredSize(new Dimension(w + 25,h + 25));
			this.revalidate();
		}
	}	
	
	public void setModel ( Model model )
	{	this.removeAll();
		
		this.model = model;	
		
		for( int i = 0 ; i < model.getObjects().size(); i++ )
		{	ModelObject mo = (ModelObject) model.getObjects().get(i);
			
			mo.addMouseListener(this);
			mo.addMouseMotionListener(this);			
			
			if( mo instanceof DatabaseObject )
				this.add( (DatabaseObject)mo );
			if( mo instanceof CommentObject )
				this.add( (CommentObject)mo );	
			if( mo instanceof TableObject )
				this.add( (TableObject)mo );
		}
		
		this.repaint();
	}
	
	public void automateMenus ()
	{	
		if( model.getSelectedObjects().size() == 1 && model.getSelectedObjects().get(0) instanceof DatabaseObject )
			attach_table.setEnabled( true );
		else
			attach_table.setEnabled( false );
			
		if( model.getSelectedObjects().size() == 1 )
		{	if( ! (model.getSelectedObjects().get(0) instanceof CommentObject ) )
				show_properties.setEnabled( true );
			
			attach_comment.setEnabled( true );
		}
		else
		{	show_properties.setEnabled( false );
			attach_comment.setEnabled( false );
		}
	}
	
	public boolean showProperties ()
	{	model.lock();
		Vector v = this.getModel().getSelectedObjects();
		if( v.size() == 1 && ( v.get(0) instanceof DatabaseObject || v.get(0) instanceof TableObject ) )
		{	
			mvc.showPropertiesDialog(model.getSelectedObjects());
			model.unlock();
			return true;
		}
		else
		{	
			model.unlock();
			return false;
		}
	}
	
/*
 *
 *	Start mouselisteners
 *
 */	
	
	public void mousePressed ( MouseEvent e )
	{
		if(!placemode)		
		{	this.moveToFront((Component)e.getSource());
			if( e.isMetaDown() && e.getSource() instanceof ModelObject )
			{	ModelObject t = (ModelObject)e.getSource();
				objectrel.removeAll();
				Vector v = model.getReferences(t);
				if(v.size() == 0)
					objectrel.add(new JMenuItem("- No Relations -"));
				for(int i = 0; i < v.size(); i++ )
				{	ModelObject tmp = ((ModelObject)v.get(i));
					JMenuItem mn;
					if( tmp instanceof DatabaseObject )
						mn = new JMenuItem( "DB: " + tmp.getName());
					else
						mn = new JMenuItem( "TB: " + tmp.getName());
						
					mn.addMouseListener(this);
					objectrel.add(mn);
				}
				
				if( t instanceof TableObject )
				{	props.setVisible(true);
					create_table.setVisible(false);
				}	
				else if ( t instanceof DatabaseObject )
				{	props.setVisible(true);
					create_table.setVisible(true);
				}
				else if( t instanceof ModelObject )
				{	props.setVisible(false);
					create_table.setVisible(false);
				}
				
				if ( !(t instanceof DatabaseObject) )
					create_table_db.setEnabled( false );
				else
					create_table_db.setEnabled( true );
					
				objectmenu.show(t, e.getX(), e.getY());
				selected = t;
				refs = v;
			}		
		
			if( e.getSource() instanceof ModelViewer )
			{	model.deselectAll();
				selected = null;
				src = null;
			}
			else if ( e.isShiftDown() && e.getSource() instanceof ModelObject )
			{	src = (ModelObject)e.getSource();
				refx = src.getX() + e.getX();
				refy = src.getY() + e.getY();
			}
			
			this.srcx = e.getX();
			this.srcy = e.getY();
			
			this.repaint();
		}
		this.repaint();
		
		this.automateMenus();
	}
	
	public void mouseReleased( MouseEvent e )
	{	if( !placemode )
		{	if (e.getSource() instanceof JMenuItem)
			{	JMenuItem tmp = (JMenuItem)e.getSource();
				if( tmp == remove )
				{	model.removeObject(selected);
					this.remove(selected);
				}
				else if( tmp == props )
				{	this.showProperties();
				}
				else if( ( tmp == create_table_db && create_table_db.isEnabled() ) || ( tmp == attach_table && attach_table.isEnabled() )  )
				{	Vector v = this.getModel().getSelectedObjects();
					if( v.size() == 1 && v.get(0) instanceof DatabaseObject )
						this.createTableObject("New Table", (DatabaseObject)v.get(0));
				}
				else if( ( tmp == create_comment_mo && create_comment_mo.isEnabled() ) || ( tmp == attach_comment && attach_comment.isEnabled() ) )
				{	Vector v = this.getModel().getSelectedObjects();
					if( v.size() == 1 && v.get(0) instanceof ModelObject )
							this.createCommentObject("New Table", (ModelObject)v.get(0));
				}				
				else if( tmp == create_table )
				{	this.createTableObject("New Table");
				}								
				else
				{	try
					{	String name = tmp.getText().substring(4,tmp.getText().length());
						for(int i = 0 ; i < refs.size(); i++)
						{	ModelObject mod = (ModelObject)refs.get(i);
							if(mod.getName().equalsIgnoreCase(name))
							{	mod.removeReference(selected);
								selected.removeReference(mod);
								break;
							}
						}
					}
					catch( Exception ex )
					{}
				}
			}	
			
			if(e.getSource() instanceof ModelViewer)
			{	Rectangle rect = new Rectangle(xpos,ypos,w,h);
				Component [] cmps = this.getComponents();
				for( int i = 0 ; i < cmps.length; i++ )
				{	ModelObject tmp = (ModelObject)cmps[i];
					if( rect.contains(tmp.getLocation()) )
					{	tmp.setSelected( true);
					}
					else
					{	tmp.setSelected( false );
					}
				}
				srcx = 0;
				srcy = 0;
				w = 0;
				h = 0;
			}
			else
			{	if( e.isShiftDown() )
				{	ModelObject end = (ModelObject)e.getSource();
					int xloc = end.getX() + e.getX();
					int yloc = end.getY() + e.getY();
					
					if( this.getComponentAt(xloc, yloc) instanceof ModelObject )
						end = (ModelObject)this.getComponentAt(xloc, yloc);
					
					if(src != null && end != null)
						model.addReference(src, end);
				}
			}
			
			src = null;
		}
		else if( placemode && e.getSource() == this)
		{	place.setHidden(false);
			this.add(place);
			this.place.setLocation(e.getX(), e.getY());
			this.exitPlaceMode();
		}
		
		this.resize();
		this.repaint();
		
		this.automateMenus();
	}
	
	public void mouseDragged( MouseEvent e )
	{	if( !placemode )
		{	if( !e.isMetaDown() )
			{	if( e.isShiftDown() && src != null )
				{	refx = src.getX() + e.getX();
					refy = src.getY() + e.getY();
				}
				if( e.getSource() instanceof ModelViewer )
				{	w = e.getX() - srcx;
					h = e.getY() - srcy;
		
					if( w < 0 )
					{	xpos = e.getX();
						w = srcx - xpos;
					}
					else
					{	xpos = srcx;
					}
					if( h < 0 )
					{	ypos = e.getY();
						h = srcy - ypos;
					}
					else
					{	ypos = srcy;
					}	
				}
			}
		}		
		this.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{	if(e.getSource() == create_database || e.getSource() == btn_add_database)
		{	this.createDatabaseObject("New Database");
		}
		else if(e.getSource() == create_table || e.getSource() == btn_add_table)
		{	this.createTableObject("New Table");
		}	
		else if(e.getSource() == create_comment || e.getSource() == btn_add_comment)
		{	this.createCommentObject("New Comment");
		}	
		else if(e.getSource() == btn_open)
		{	mvc.openModel();
		}	
		else if(e.getSource() == btn_save)
		{	mvc.saveModel();
		}	
		else if(e.getSource() == btn_new)
		{	mvc.newModel();
		}	
		else if(e.getSource() == btn_export)
		{	mvc.generate();
		}							
		else if (e.getSource() == show_properties || e.getSource() == btn_properties)
		{	boolean b = this.showProperties();
			if( e.getSource() == btn_properties && !b )
			{
				model.lock();
				mvc.showModelPropertiesDialog(this.getModel());
				mvc.updateTitle();
				model.unlock();
			}
		}
		else if(e.getSource() == show_model_properties  )		
		{	model.lock();
			mvc.showModelPropertiesDialog(this.getModel());
			mvc.updateTitle();
			model.unlock();
		}
	}
	
	public void mouseClicked( MouseEvent e ){	}
	public void mouseEntered( MouseEvent e ){	}
	public void mouseExited( MouseEvent e )	{	}
	public void mouseMoved( MouseEvent e ){	}
	
	public void eventDispatched(AWTEvent event)
	{   KeyEvent e =(KeyEvent) event;
		if(e.getID() == 401)
		{	if(e.isControlDown() && e.getKeyCode() == e.VK_A)
			{	this.getModel().selectAll();
			}
			if(e.isControlDown() && e.getKeyCode() == e.VK_D)
			{	this.getModel().deselectAll();
			}
			if(e.isControlDown() && e.getKeyCode() == e.VK_O)
			{	mvc.openModel();
			}					
			if(e.getKeyCode() == e.VK_DELETE)
			{	this.removeSelectedObjects();
			}
			if(e.isControlDown() && e.getKeyCode() == e.VK_S)
			{	mvc.saveModel(true);
			}	
			if(e.isControlDown() && e.getKeyCode() == e.VK_N)
			{	mvc.newModel();
			}	
			if(e.getKeyCode() == e.VK_F4)
			{	Vector v = this.getModel().getSelectedObjects();
					if( v.size() == 1 && v.get(0) instanceof DatabaseObject )
						this.createTableObject("New Table", (DatabaseObject)v.get(0));
			}	
			if(e.getKeyCode() == e.VK_F5)
			{	Vector v = this.getModel().getSelectedObjects();
					if( v.size() == 1 && v.get(0) instanceof ModelObject )
							this.createCommentObject("New Table", (ModelObject)v.get(0));
			}																
		}
	}	
}
package esql.dbcreator.gui.model;

import esql.dbcreator.gui.component.*;

import java.util.*;
import java.awt.event.*;
import java.io.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class Model implements MouseListener, MouseMotionListener
{	private String name = "";
	private String comment = "";
	private String author = "";
	private Vector modelobjects = new Vector();
	private boolean locked = false;
	private int identifier = 1;
	private File file = null;

	public Model ( String name )
	{	this.name = name;
	}
	
	public String getName()
	{	
		return name;
	}
	
	public void setName ( String name )
	{
		this.name = name;
	}
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment( String comment )
	{
		this.comment = comment;
	}
	
	public DatabaseObject createDatabaseObject( String name )
	{	DatabaseObject db = new DatabaseObject(name, identifier);
		identifier++;
		db.addMouseListener(this);
		db.addMouseMotionListener(this);
		modelobjects.add(db);
		return db;
	}
	
	public TableObject createTableObject( String name )
	{	TableObject tb = new TableObject(name, identifier);
		identifier ++ ;
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		modelobjects.add(tb);
		return tb;
	}	
	
	public CommentObject createCommentObject( String comment )
	{	CommentObject cm = new CommentObject(comment, identifier);
		identifier++;
		cm.addMouseListener(this);
		cm.addMouseMotionListener(this);
		modelobjects.add(cm);
		return cm;
	}
	
	public void addReference( ModelObject src, ModelObject end )
	{	if( src == end || ( src instanceof DatabaseObject && end instanceof DatabaseObject ) || ( src instanceof TableObject && end instanceof TableObject ))
			return;
			
		Vector v = src.getReferences();
		for(int i = 0; i < v.size(); i++)
		{	ModelObject mo = (ModelObject)v.get(i);
			if ( mo == end )
			{	return;
			}
		}
		
		v = end.getReferences();
		for(int i = 0; i < v.size(); i++)
		{	ModelObject mo = (ModelObject)v.get(i);
			if ( mo == src )
			{	return;
			}
		}
		
		src.addReference(end);
	}	
	
	public Vector getObjects()
	{	return modelobjects;
	}
	
	public Vector getReferences( ModelObject m )
	{	Vector refs = new Vector();
		for(int i = 0 ; i < modelobjects.size(); i++ )
		{	ModelObject tmp = (ModelObject)modelobjects.get(i);
			if( tmp.getReferences().contains(m) && !(tmp instanceof CommentObject))
			{	refs.add(tmp);
			}
		}
		for(int i = 0 ; i < m.getReferences().size(); i++ )
		{	ModelObject tmp = (ModelObject)m.getReferences().get(i);
			if(!(tmp instanceof CommentObject) )
				refs.add(tmp);
		}
		return refs;
	}
	
	public Vector removeSelectedObjects()
	{	Vector v = new Vector();
		if( !locked )
		{	Vector sel = getSelectedObjects();
			for(int i = 0 ; i < sel.size(); i++ )
			{	ModelObject tmp = (ModelObject)sel.get(i);
				if(tmp.isSelected())
				{	for(int j = 0 ; j < modelobjects.size(); j++ )
					{	ModelObject tmp2 = (ModelObject)modelobjects.get(j);
						if ( tmp != tmp2 )
						{	tmp2.removeReference(tmp);
						}
					}
				}
				modelobjects.remove(tmp);
				v.add(tmp);
			}
		}
		return v;
	}
	
	public void removeObject( ModelObject m )
	{	if( !locked )
		{	for(int j = 0 ; j < modelobjects.size(); j++ )
			{	ModelObject tmp2 = (ModelObject)modelobjects.get(j);
				if ( m != tmp2 )
				{	tmp2.removeReference(m);
				}
			}
			modelobjects.remove(m);
		}
	}
	
	public Vector getSelectedObjects()
	{	Vector temp = new Vector();
		for(int i = 0 ; i < modelobjects.size(); i++ )
		{	ModelObject tmp = (ModelObject)modelobjects.get(i);
			if(tmp.isSelected())
			{	temp.add(tmp);
			}
		}
		return temp;
	}
	
	public void deselectAll()
	{	for(int i = 0 ; i < modelobjects.size(); i++ )
		{	((ModelObject)modelobjects.get(i)).setSelected(false);		
		}
	}
	
	public void selectAll()
	{	for(int i = 0 ; i < modelobjects.size(); i++ )
		{	((ModelObject)modelobjects.get(i)).setSelected(true);
		}
	}
	
	public void lock()
	{	locked = true;
	}
	
	public void unlock()
	{	locked = false;
	}
	
	public File getFile()
	{	return file;
	}		
	
	public void setFile ( File f )
	{	this.file = f;
	}
	
	public void mousePressed( MouseEvent e )
	{	if( !this.locked )
		{	ModelObject tmp = (ModelObject)e.getSource();
			if( !e.isControlDown() && !tmp.isSelected() )
			{	deselectAll();
				tmp.setSelected(true);
			}
			else if ( e.isControlDown() )
			{	tmp.setSelected(!tmp.isSelected());
			}
			tmp.xc = e.getX();
			tmp.yc = e.getY();
		}
	}
	
	public void mouseDragged( MouseEvent e )
	{	if( !e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && !this.locked )
		{	
			ModelObject tmp = (ModelObject)e.getSource();
	
			int xloc = tmp.getX() + (e.getX() - tmp.xc);
			int yloc = tmp.getY() + (e.getY() - tmp.yc);			
			
			tmp.setLocation(xloc, yloc);
			
			int xadj = (e.getX() - tmp.xc);
			int yadj = (e.getY() - tmp.yc);
			for(int i = 0 ; i < modelobjects.size(); i++ )
			{	ModelObject mo = (ModelObject)modelobjects.get(i);
				if( mo.isSelected() && mo != tmp )
				{	xloc = mo.getX()+xadj;
					yloc = mo.getY()+yadj;
						
					mo.setLocation(xloc, yloc);
				}
			}	
		}
	}	
	
	public void mouseReleased( MouseEvent e )
	{	if( !this.locked )
		{	int x = 0;
			int y = 0;
			for(int i = 0 ; i < modelobjects.size(); i++ )
			{	ModelObject mo = (ModelObject)modelobjects.get(i);
				if( mo.getX() < x )
					x = mo.getX();
				if( mo.getY() < y )
					y = mo.getY();
			}
			if( x < 0 || y < 0 )
			{	for(int i = 0 ; i < modelobjects.size(); i++ )
				{	ModelObject mo = (ModelObject)modelobjects.get(i);
					mo.setLocation( mo.getX() + (-x) + 5, mo.getY() + (-y) + 5);
				}
			}
		}
	}
	
	public ModelObject getObjectByIdentifier( int identifier )
	{	for(int i = 0 ; i < modelobjects.size(); i++ )
		{	ModelObject tmp = (ModelObject)modelobjects.get(i);
			if(tmp.getIdentifier() == identifier )
			{	return tmp;
			}
		}
		return null;
	}
	
	public void setIdentifier ( int identifier )
	{	this.identifier = identifier;
	}

	public int getIdentifier ( )
	{	return identifier;
	}
	
	public String getAuthor ( )	
	{	return author;
	}
	
	public void setAuthor ( String author )
	{	this.author = author;
	}
	
	public String getModelXML()
	{	// XML offset - start string
		String xml = "<?xml version='1.0' encoding='UTF-8' ?>" + '\n';
		
		// Model start
		xml = xml + "<model>" + '\n';
		xml = xml + "	<version>0.1</version>" + '\n';
		xml = xml + "	<name>" + name + "</name>" + '\n';
		xml = xml + "  <comment>" + comment + "</comment>" + '\n';
		xml = xml + "  <identifier_offset>" + identifier + "</identifier_offset>" + '\n';
		xml = xml + "	<author>" + author + "</author>" + '\n';
		xml = xml + "	<last_edited>" + '\n';
		xml = xml + "		<author>" + author + "</author>" + '\n';
		xml = xml + "		<time>" + System.currentTimeMillis() + "</time>" + '\n';
		xml = xml + "	</last_edited>" + '\n';
		
		// Filter all object
		Vector obj = this.getObjects();
		
		String dbs = "";
		String tbs = "";
		String cms = "";
		String rel = "";
		
		for(int i = 0 ; i < obj.size(); i++)
		{	if ( obj.get(i) instanceof DatabaseObject )
			{	DatabaseObject db = (DatabaseObject)obj.get(i);
				dbs = dbs + "		<database>" + '\n';
				dbs = dbs + "			<name>" + db.getName() + "</name>" + '\n';
				dbs = dbs + "			<comment>" + db.getDescription() + "</comment>" + '\n';
				dbs = dbs + "			<identifier>" + db.getIdentifier() + "</identifier>" + '\n';
				dbs = dbs + "			<bounds>" + '\n';
				dbs = dbs + "				<x>" + db.getX() + "</x>" + '\n';
				dbs = dbs + "				<y>" + db.getY() + "</y>" + '\n';
				dbs = dbs + "				<w>" + db.getWidth() + "</w>" + '\n';
				dbs = dbs + "				<h>" + db.getHeight() + "</h>" + '\n';
				dbs = dbs + "			</bounds>" + '\n';
				dbs = dbs + "		</database>" + '\n';
			}
			else if ( obj.get(i) instanceof TableObject )
			{	TableObject tb = (TableObject)obj.get(i);
				tbs = tbs + "		<table>" + '\n';
				tbs = tbs + "			<name>" + tb.getName() + "</name>" + '\n';
				tbs = tbs + "			<comment>" + tb.getComment() + "</comment>" + '\n';
				tbs = tbs + "			<description>" + tb.getDescription() + "</description>" + '\n';
				tbs = tbs + "			<type>" + tb.getType() + "</type>" + '\n';
				tbs = tbs + "			<identifier>" + tb.getIdentifier() + "</identifier>" + '\n';
				tbs = tbs + "			<bounds>" + '\n';
				tbs = tbs + "				<x>" + tb.getX() + "</x>" + '\n';
				tbs = tbs + "				<y>" + tb.getY() + "</y>" + '\n';
				tbs = tbs + "				<w>" + tb.getWidth() + "</w>" + '\n';
				tbs = tbs + "				<h>" + tb.getHeight() + "</h>" + '\n';
				tbs = tbs + "			</bounds>" + '\n';
				tbs = tbs + "			<fields>" + '\n';
				
				Field [] fds = tb.getFields();
				for( int j = 0 ; j < fds.length; j++ )
				{
					tbs = tbs + "				<field>" + '\n';
					tbs = tbs + "					<name>" + fds[j].getName() + "</name>" + '\n';
					tbs = tbs + "					<comment>" + fds[j].getComment() + "</comment>" + '\n';
					tbs = tbs + "					<default>" + fds[j].getDefault() + "</default>" + '\n';
					tbs = tbs + "					<length>" + fds[j].getLength() + "</length>" + '\n';
					tbs = tbs + "					<type>" + fds[j].getType().getName() + "</type>" + '\n';
					tbs = tbs + "					<primary>" + fds[j].primary + "</primary>" + '\n';
					tbs = tbs + "					<autoincrement>" + fds[j].autoincrement + "</autoincrement>" + '\n';
					tbs = tbs + "					<binary>" + fds[j].binary + "</binary>" + '\n';
					tbs = tbs + "					<index>" + fds[j].index + "</index>" + '\n';
					tbs = tbs + "					<notnull>" + fds[j].notnull + "</notnull>" + '\n';
					tbs = tbs + "					<unique>" + fds[j].unique + "</unique>" + '\n';
					tbs = tbs + "					<unsigned>" + fds[j].unsigned + "</unsigned>" + '\n';
					tbs = tbs + "					<zerofill>" + fds[j].zerofill + "</zerofill>" + '\n';
					tbs = tbs + "				</field>" + '\n';
				}
				
				tbs = tbs + "			</fields>" + '\n';
				tbs = tbs + "		</table>" + '\n';
			}
			else if ( obj.get(i) instanceof CommentObject )
			{	CommentObject cm = (CommentObject)obj.get(i);
				cms = cms + "		<comment>" + '\n';
				cms = cms + "			<comment>" + cm.getComment() + "</comment>" + '\n';
				cms = cms + "			<identifier>" + cm.getIdentifier() + "</identifier>" + '\n';
				cms = cms + "			<bounds>" + '\n';
				cms = cms + "				<x>" + cm.getX() + "</x>" + '\n';
				cms = cms + "				<y>" + cm.getY() + "</y>" + '\n';
				cms = cms + "				<w>" + cm.getWidth() + "</w>" + '\n';
				cms = cms + "				<h>" + cm.getHeight() + "</h>" + '\n';
				cms = cms + "			</bounds>" + '\n';				
				cms = cms + "		</comment>" + '\n';
			}
			
			ModelObject src = (ModelObject)obj.get(i);
			Vector trg = src.getReferences();
			for( int j = 0 ; j < trg.size(); j++ )
			{	
				rel = rel + "		<relation>" + '\n';
				rel = rel + "			<source_identifier>" + src.getIdentifier() + "</source_identifier>" + '\n';
				rel = rel + "			<target_identifier>" + ((ModelObject)trg.get(j)).getIdentifier() + "</target_identifier>" + '\n';
				rel = rel + "		</relation>" + '\n';
			}
			
		}
		
		// Databases objects
		xml = xml + "	<databases>" + '\n';
		xml = xml +	dbs;
		xml = xml + "	</databases>" + '\n';
		
		// Tables objects
		xml = xml + "	<tables>" + '\n';
		xml = xml +	tbs;
		xml = xml + "	</tables>" + '\n';
		
		// Comment objects
		xml = xml + "	<comments>" + '\n';
		xml = xml +	cms;
		xml = xml + "	</comments>" + '\n';
		
		// Relations
		xml = xml + "	<relations>" + '\n';
		xml = xml +	rel;
		xml = xml + "	</relations>" + '\n';		
		
		// Close the model tag
		xml = xml + "</model>" + '\n';
		
		return xml;
	}
	
	public Model loadModel( File xml ) throws Exception
	{	SAXBuilder builder = new SAXBuilder();
		Document model = builder.build( xml );
		
		if( model.hasRootElement() )
		{	
			Element root = model.getRootElement();
			
			if ( root.getChild("version").getText().equalsIgnoreCase("0.1") )
			{				
				Model m = new Model (root.getChild("name").getText());
				m.setIdentifier(Integer.parseInt ( root.getChild("identifier_offset").getText() ) );
				m.setAuthor(root.getChild("author").getText());
				m.setComment(root.getChild("comment").getText());
				
				List dbs = root.getChild("databases").getChildren("database");
				
				for( int i = 0 ; i < dbs.size() ; i++ )
				{	Element db = ( Element )dbs.get( i );	
					String name = db.getChild("name").getText();
					String comment = db.getChild("comment").getText();
					int identifier = Integer.parseInt( db.getChild("identifier").getText() );
					
					int x = Integer.parseInt( db.getChild("bounds").getChild("x").getText() );
					int y = Integer.parseInt( db.getChild("bounds").getChild("y").getText() );
					int w = Integer.parseInt( db.getChild("bounds").getChild("w").getText() );
					int h = Integer.parseInt( db.getChild("bounds").getChild("h").getText() );
					
					DatabaseObject d = m.createDatabaseObject( name );
					d.setDescription( comment );
					d.setIdentifier( identifier );
					d.setBounds( x, y, w, h );
					d.setHidden(false);
				}
				
				List cms = root.getChild("comments").getChildren("comment");
				
				for( int i = 0 ; i < cms.size() ; i++ )
				{	Element cm = ( Element )cms.get( i );
					
					String comment = cm.getChild("comment").getText();
					int identifier = Integer.parseInt( cm.getChild("identifier").getText() );	
					
					int x = Integer.parseInt( cm.getChild("bounds").getChild("x").getText() );
					int y = Integer.parseInt( cm.getChild("bounds").getChild("y").getText() );
					int w = Integer.parseInt( cm.getChild("bounds").getChild("w").getText() );
					int h = Integer.parseInt( cm.getChild("bounds").getChild("h").getText() );	
					
					CommentObject c = m.createCommentObject( comment );
					c.setIdentifier( identifier );
					c.setBounds( x, y, w, h );	
					c.setHidden( false );			
				}
				
				List tbs = root.getChild("tables").getChildren("table");
				
				for( int i = 0 ; i < tbs.size() ; i++ )
				{	Element tb = ( Element )tbs.get( i );
					
					String name = tb.getChild("name").getText();
					String comment = tb.getChild("comment").getText();
					String ttype = tb.getChild("type").getText();
					int identifier = Integer.parseInt( tb.getChild("identifier").getText() );
					
					int x = Integer.parseInt( tb.getChild("bounds").getChild("x").getText() );
					int y = Integer.parseInt( tb.getChild("bounds").getChild("y").getText() );
					int w = Integer.parseInt( tb.getChild("bounds").getChild("w").getText() );
					int h = Integer.parseInt( tb.getChild("bounds").getChild("h").getText() );	
					
					TableObject t = m.createTableObject( name );					
					t.setComment( comment );
					t.setIdentifier( identifier );
					t.setType( ttype );
					t.setBounds( x, y, w, h );	
					t.setHidden( false );
					
					List fds = tb.getChild("fields").getChildren("field");
					for ( int j = 0; j < fds.size(); j ++ )
					{	Element fd = ( Element )fds.get(j);
					
						String fd_name = fd.getChild("name").getText();
						String fd_comment = fd.getChild("comment").getText();
						String fd_default = fd.getChild("default").getText();
						String fd_length = fd.getChild("length").getText();
						String fd_type = fd.getChild("type").getText();
						
						boolean fd_primary =  (fd.getChild("primary").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_autoincrement =  (fd.getChild("autoincrement").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_binary =  (fd.getChild("binary").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_index =  (fd.getChild("index").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_notnull =  (fd.getChild("notnull").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_unique =  (fd.getChild("unique").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_unsigned =  (fd.getChild("unsigned").getText().equalsIgnoreCase("true")) ? true : false;
						boolean fd_zerofill =  (fd.getChild("zerofill").getText().equalsIgnoreCase("true")) ? true : false;
						
						//FieldObject type = new FieldObject(fd_type,false,false,false,false,false,false, false,false);
						esql.domain.DataType type = new esql.domain.DataType(fd_type,false,false,false,false,false,false, false,false);
						Field f = new Field(fd_name,type,fd_length,fd_default,fd_comment);
						f.primary = fd_primary;
						f.autoincrement = fd_autoincrement;
						f.binary = fd_binary;
						f.index = fd_index;
						f.notnull = fd_notnull;
						f.unique = fd_unique;
						f.unsigned = fd_unsigned;
						f.zerofill = fd_zerofill;
						
						t.addField( f );
					}
				}
				
				List rls = root.getChild("relations").getChildren("relation");
				
				for( int i = 0 ; i < rls.size() ; i++ )
				{	Element rl = ( Element ) rls.get(i);
					
					int src = Integer.parseInt( rl.getChild("source_identifier").getText() );
					int trg = Integer.parseInt( rl.getChild("target_identifier").getText() );
					
					ModelObject s = m.getObjectByIdentifier(src);
					ModelObject t = m.getObjectByIdentifier(trg);
					
					s.addReference(t);
				}
												
				return m;
			}
		}
		
		return null;
	}
	
	public void mouseClicked( MouseEvent e ){	}
	public void mouseEntered( MouseEvent e ){	}
	public void mouseExited( MouseEvent e )	{	}
	public void mouseMoved( MouseEvent e )	{	}	
}
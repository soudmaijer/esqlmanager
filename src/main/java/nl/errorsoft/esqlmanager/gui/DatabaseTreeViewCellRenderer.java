package nl.errorsoft.esqlmanager.gui;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;
import nl.errorsoft.esqlmanager.domain.*;

class DatabaseTreeViewCellRenderer extends DefaultTreeCellRenderer 
{	
	private ImageLoader imgldr;
	
	public DatabaseTreeViewCellRenderer(ImageLoader imgldr) 
   { 
   	this.imgldr = imgldr;
   }

	public Component getTreeCellRendererComponent( JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) 
	{   
     	super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus );
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
		TreePath tp = new TreePath( node.getPath() );
		
		if( node.getUserObject() instanceof Database )
		{
			if( hasFocus )
				setIcon(new ImageIcon(imgldr.getImage("dbimgsel")));
			else
				setIcon(new ImageIcon(imgldr.getImage("dbimg")));
		}
		else if( node.getUserObject() instanceof Table )
		{
			if( hasFocus )
				setIcon(new ImageIcon(imgldr.getImage("tbimgsel")));
			else
				setIcon(new ImageIcon(imgldr.getImage("tbimg")));
		}
		else if( node.getUserObject() instanceof nl.errorsoft.esqlmanager.domain.TableColumn )
		{
			TableColumn temp = (TableColumn)node.getUserObject();
			
			if( temp.isPrimary() )
				setIcon(new ImageIcon(imgldr.getImage("keyimg")));
			else if( temp.hasIndex() )
				setIcon(new ImageIcon(imgldr.getImage("imgHasIndexSel")));
			else
				setIcon(new ImageIcon(imgldr.getImage("fldimg")));
		}
		else
		{
			setIcon(new ImageIcon(imgldr.getImage("pc")));
		}
		
		return this;
	}
}
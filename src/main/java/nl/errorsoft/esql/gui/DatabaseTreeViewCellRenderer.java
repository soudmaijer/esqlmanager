package nl.errorsoft.esql.gui;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;
import nl.errorsoft.esql.domain.*;

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
		
		if( node.getUserObject() instanceof Database )
		{
			if( hasFocus )
				setIcon(imgldr.getIcon("dbimgsel"));
			else
				setIcon(imgldr.getIcon("dbimg"));
		}
		else if( node.getUserObject() instanceof Table )
		{
			if( hasFocus )
				setIcon(imgldr.getIcon("tbimgsel"));
			else
				setIcon(imgldr.getIcon("tbimg"));
		}
		else if( node.getUserObject() instanceof nl.errorsoft.esql.domain.TableColumn )
		{
			TableColumn temp = (TableColumn)node.getUserObject();
			
			if( temp.isPrimary() )
				setIcon(imgldr.getIcon("keyimg"));
			else if( temp.hasIndex() )
				setIcon(imgldr.getIcon("imgHasIndexSel"));
			else
				setIcon(imgldr.getIcon("fldimg"));
		}
		else
		{
			setIcon(imgldr.getIcon("pc"));
		}
		
		return this;
	}
}
package nl.errorsoft.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

import java.awt.event.*;

public class ModelObject extends JPanel
{	private Vector references;
	private boolean selected;
	
	private boolean hidden = true;
	
	public int xc = 0;
	public int yc = 0;
	
	private int identifier = -1;
	
	public ModelObject ()
	{	references = new Vector();
		this.setLayout(null);
	}
	
	public void addReference ( ModelObject mo )
	{	references.add(mo);
	}
	
	public void removeReference( ModelObject mo )
	{	references.remove(mo);
	}
	
	public Vector getReferences()
	{	return references;
	}
	
	public boolean isSelected()
	{	return selected;
	}	
	
	public void setSelected( boolean selected )
	{	this.selected = selected;
		if(selected)
		{	this.requestFocus();
		}
		this.repaint();
	}
	
	public void setHidden(boolean hidden)
	{	this.hidden = hidden;
	}
	
	public boolean isHidden()
	{	return hidden;
	}
	
	public void setIdentifier( int identifier )
	{	this.identifier = identifier;
	}
	
	public int getIdentifier()
	{	return identifier;
	}
}
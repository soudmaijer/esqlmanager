package esql.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class TableObject extends ModelObject
{	private String name;
	private String description = "";
	private Vector fields = new Vector();
	private String type = "MyIsam";
	private String comment = "";
	
	private int width;
	private int height;
	
	public TableObject ( String name, int identifier )
	{	this.name = name;
		this.setOpaque(false);
		this.setIdentifier(identifier);
		this.reviewSize();
	}
	
	public void paintComponent(Graphics g)
	{	Graphics2D g2 = (Graphics2D)g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if( !this.isSelected() )
		{	g2.setColor(new Color(213,221,236));
			g2.fillRect(0,0,(int)this.getSize().getWidth()-1, (int)this.getSize().getHeight()-1);
		}
		else
		{	g2.setColor(new Color(231,240,255));
			g2.fillRect(0,0,(int)this.getSize().getWidth()-1, (int)this.getSize().getHeight()-1);
		}
		
		g2.setColor(Color.black);
		g2.setFont(new Font("Arial", Font.BOLD, 11));
			
		String nm = name + " (" + type +  ")";
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(nm, ( (this.getWidth() - fm.stringWidth(nm)) / 2 ),12);
		
		g2.setFont(new Font("Arial", Font.PLAIN, 10));
		for(int i= 0 ; i < fields.size(); i++ )
		{	String name = ((Field)fields.get(i)).getName() + " [ "  + ((Field)fields.get(i)).getType().getName() + " (" + ((Field)fields.get(i)).getLength() + ") ]";
			g2.drawString(name, 5, 30 + ( 12 * i ) );
		}
		
		g2.drawLine(0,16,this.getWidth(),16);
		
		g2.drawRect(0,0,(int)this.getSize().getWidth()-1, (int)this.getSize().getHeight()-1);
	}
	
	public void addField( Field f )
	{	
		this.fields.add(f);
		this.reviewSize();
		this.repaint();
	}
	
	private void reviewSize ()
	{	String nm = name + " (" + type +  ")";
		FontMetrics fm = this.getFontMetrics(new Font("Arial", Font.BOLD, 11));
		
		int w = fm.stringWidth(nm);
		int h = (fields.size() * 12) + 25;
		
		int length = w;
		
		fm = this.getFontMetrics(new Font("Arial", Font.PLAIN, 10));
		for(int i= 0 ; i < fields.size(); i++ )
		{	String name = ((Field)fields.get(i)).getName() + " [ "  + ((Field)fields.get(i)).getType().getName() + " (" + ((Field)fields.get(i)).getLength() + ") ]";
			w = fm.stringWidth( name );
			if ( w > length )
				length = w;
		}
			
		if (this.width != (length + 10) || this.height != h)
		{	this.setSize( length + 10, (fields.size() * 12) + 25 );		
			this.width = length + 10;
			this.height = h ;
		}		
		
		this.setSize(this.width, this.height);
	}
	
	public Field [] getFields ()
	{	Field [] f = new Field[fields.size()];
		for(int i= 0 ; i < fields.size(); i++ )
		{	f[i] = (Field)fields.get(i);
		}
		return f;
	}
	
	public void removeAllFields()
	{	fields.removeAllElements();
		this.reviewSize();
	}
	
	public String getName()
	{	return name;
	}
	
	public String getDescription()
	{	return description;
	}
	
	public String getComment()
	{	return comment;
	}
	
	public String getType()
	{	return type;
	}
	
	public void setName(String name)
	{	this.name = name;
		this.reviewSize();
		this.repaint();
	}
	
	public void setDescription( String description )
	{	this.description = description;
	}	
	
	public void setComment(String comment)
	{	this.comment = comment;
	}
	
	public void setType(String type)
	{	this.type = type;
		this.reviewSize();
		this.repaint();
	}
}
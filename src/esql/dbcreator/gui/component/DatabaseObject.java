package esql.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class DatabaseObject extends ModelObject
{	private String name = "";
	private String description = "";
	private int width;
	
	public DatabaseObject ( String name, int identifier )
	{	this.name = name;
		this.setOpaque(false);
		this.setIdentifier(identifier);
		this.reviewSize();
	}
	
	public void paintComponent(Graphics g)
	{	Graphics2D g2 = (Graphics2D)g;
			
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if( !this.isSelected() )
		{	g2.setColor(new Color(220,220,220));
			g2.fillOval(0,(int)this.getSize().getHeight()-21,(int)this.getSize().getWidth()-1, 20);	
			g2.setColor(Color.black);
			g2.drawOval(0,(int)this.getSize().getHeight()-21,(int)this.getSize().getWidth()-1, 20);	
			g2.setColor(new Color(220,220,220));
			g2.fillRect(0,10,(int)this.getSize().getWidth(), (int)this.getSize().getHeight()-20);
			g2.setColor(Color.black);
			g2.drawLine(0,10,0,(int)this.getSize().getHeight()-10);
			g2.drawLine((int)this.getSize().getWidth()-1,10,(int)this.getSize().getWidth()-1,(int)this.getSize().getHeight()-10);
			g2.setColor(new Color(220,220,220));
			g2.fillOval(0,0,(int)this.getSize().getWidth()-1, 20);
			g2.setColor(Color.black);
			g2.drawOval(0,0,(int)this.getSize().getWidth()-1, 20);
		}
		else
		{	g2.setColor(new Color(240,240,240));
			g2.fillOval(0,(int)this.getSize().getHeight()-21,(int)this.getSize().getWidth()-1, 20);	
			g2.setColor(new Color(0,0,0));
			g2.drawOval(0,(int)this.getSize().getHeight()-21,(int)this.getSize().getWidth()-1, 20);	
			g2.setColor(new Color(240,240,240));
			g2.fillRect(0,10,(int)this.getSize().getWidth(), (int)this.getSize().getHeight()-20);
			g2.setColor(new Color(0,0,0));
			g2.drawLine(0,10,0,(int)this.getSize().getHeight()-10);
			g2.drawLine((int)this.getSize().getWidth()-1,10,(int)this.getSize().getWidth()-1,(int)this.getSize().getHeight()-10);
			g2.setColor(new Color(240,240,240));
			g2.fillOval(0,0,(int)this.getSize().getWidth()-1, 20);
			g2.setColor(new Color(0,0,0));
			g2.drawOval(0,0,(int)this.getSize().getWidth()-1, 20);
		}
		
		g2.setColor(Color.black);
		
		g2.setFont(new Font("Arial", Font.BOLD, 11));
		
		FontMetrics fm = g2.getFontMetrics();
		int wstr = fm.stringWidth(name);
		g2.drawString(name, ( (this.getWidth() - wstr) / 2 ),45);
	}
	
	public void reviewSize()
	{	FontMetrics fm = this.getFontMetrics(new Font("Arial", Font.BOLD, 11));
		int wstr = fm.stringWidth(name);
		
		int w = wstr + 20;
		if(w != width)
		{	this.setSize(w , 75 );
			width = w;
		}
	}	
	
	public String getName()
	{	return name;
	}
	
	public String getDescription()
	{	return description;
	}
	
	public void setName(String name)
	{	this.name = name;
		this.reviewSize();
		this.repaint();
	}
	
	public void setDescription( String description )
	{	this.description = description;
	}
}
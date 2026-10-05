package nl.errorsoft.esqlmanager.gui;

import java.awt.*;
import javax.swing.*;

public class StatusLight extends JPanel
{	private boolean red = false;
	private ImageLoader imgldr;
	private Image img = null;
	
	public StatusLight ( ImageLoader imgldr )
	{	this.imgldr = imgldr;	
	}
	
	public void switchRedLight ( boolean red )
	{	this.red = red;
		paintComponent(getComponentGraphics(getGraphics()));
	}
	
	public void paintComponent( Graphics g )
	{	
		g.setColor(this.getBackground());
		g.fillRect(0,0,90,90);
		
		if( red)	
			g.drawImage(imgldr.getImage("redLight"), 0,(this.getHeight() - 15 )/2, this);
		else
			g.drawImage(imgldr.getImage("greenLight"), 0,(this.getHeight() - 15 )/2, this);
	}
}
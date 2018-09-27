package esql.gui;

import java.awt.*;
import java.io.*;

public class Credits extends Canvas implements Runnable
{	private CreditObject root;
	private CreditObject curr;
	
	private Image img = null;
	private int y_offset = 0;
	
	private int nodes = 0;
	private boolean run = true;
	
	private Image bg;
	
	public Credits()
	{	
		try
		{			
			BufferedReader fin = new BufferedReader(new FileReader("credits.txt"));
			String in = fin.readLine();
			root = new CreditObject(in);
			curr = root;
			nodes++;
			while((in = fin.readLine()) != null)
			{	nodes ++;
				CreditObject tmp = new CreditObject(in);
				curr.next = tmp;
				curr = tmp;
			}
			fin.close();
			fin = null;
			
			Thread t = new Thread(this);
			t.start();
		}
		catch(Exception e)
		{
		}	
	}
	
	public void update(Graphics g)
	{	paint(g);
	}
	
	public void paint(Graphics g)
	{	if(img == null)
		{	img = createImage((int)this.getSize().getWidth(),(int)this.getSize().getHeight());
			y_offset = (int)this.getSize().getHeight();
		}
		
		Graphics2D g2 = (Graphics2D)img.getGraphics();
		Composite old = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)0.8));
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,(int)this.getSize().getWidth(),(int)this.getSize().getHeight());
		
		g2.setComposite(old);
		
		g2.setColor( new Color(Integer.parseInt("5B5150", 16)) );		
		g2.drawRect(0,0,(int)this.getSize().getWidth()-1, (int)this.getSize().getHeight()-1);
		
		g2.setColor(Color.black);
	
		Font fb = new Font("Arial", Font.BOLD, 11);
		Font fp = new Font("Arial", Font.PLAIN, 11);
		
		FontMetrics fmb = this.getFontMetrics(fb);
		FontMetrics fmp = this.getFontMetrics(fp);
		
		curr = root;
		int count = 0; 
		while(curr != null)
		{	if(!curr.txt.equalsIgnoreCase("-"))
			{	int width;
				String txt = curr.txt;
				if(curr.txt.startsWith("<h>"))
				{	txt = txt.substring(3, txt.length());
					width = fmb.stringWidth(txt);
					g2.setColor( new Color(Integer.parseInt("A24811", 16)));
					g2.setFont(fb);
				}
				else
				{	width = fmp.stringWidth(txt);
					g2.setColor(new Color(Integer.parseInt("000000", 16)));
					g2.setFont(fp);
				}
				g2.drawString(txt,(((int)this.getSize().getWidth()) - width) / 2,y_offset + ((count+1) * 11));	
			}
			else
			{	g2.setColor(new Color(Integer.parseInt("5B5150", 16)));
				g2.drawLine(10,y_offset + (count * 11) + 7, (int)this.getSize().getWidth()-10,y_offset + (count * 11) + 7);
			}
			curr = curr.next;
			count++;
		}
		
		g = this.getGraphics();
		g.drawImage(img,0,0,this);
	}
	
	public void switchoff()
	{	run = false;
	}
	
	public void run()
	{	while(run)
		{	try
			{	Thread.sleep(45);
				y_offset--;
				repaint();
				
				if(y_offset + nodes * 11 < 0)
				{	y_offset = (int)this.getSize().getHeight();
				}
			}
			catch(Exception e)
			{
			}
		}
	}
		
	public static void main(String args[])
	{	Frame j = new Frame();
		j.setSize(640,480);
		j.setLayout(null);
		j.setBackground(Color.black);
		
		Credits c = new Credits();
		c.setBounds(20,40,200,200);
		j.add(c);
		
		j.setVisible(true);		
	}
}

class CreditObject
{	String txt;
	CreditObject next = null;
	
	public CreditObject(String txt)
	{	this.txt = txt;
	}
}
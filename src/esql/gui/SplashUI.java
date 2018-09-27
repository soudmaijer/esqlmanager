package esql.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import esql.control.*;

public class SplashUI extends javax.swing.JWindow implements MouseListener, Runnable
{	
	private ESQLManagerUI jmui;
	private ESQLManagerCC jmcc;
	private int time = 0;
	private Credits c;
	private Image splash;
	
	public SplashUI( ESQLManagerCC jmcc, ESQLManagerUI jmui, int time ) 
	{
		super((JFrame)jmui);
		
		this.jmui = jmui;
		this.jmcc = jmcc;
		
		this.setSize(400, 240);
		this.setLocation( jmui.getLocation().x + (int)((jmui.getSize().width - this.getSize().width) / 2), jmui.getLocation().y + (int)((jmui.getSize().height - this.getSize().height) / 2) );
		this.time = time;
		this.addMouseListener( this );
		
		if(time == 0)
		{	this.getContentPane().setLayout(null);
			c = new Credits();
			c.setBounds(0,165,400,75);
			this.getContentPane().add(c);
		}
		
		splash = jmcc.getImageLoader().getImage("esql");
		this.setVisible( true );

		Thread t = new Thread( this );
		t.start();
	}
	
	public void run()
	{
		try
		{
			if( time > 0 )
			{
				Thread.sleep( time );
				cleanUp();
				jmcc.splashReady();
			}
		}
		catch( Exception e )
		{
		}		
	}
	
	public void mouseClicked(MouseEvent e) 
   {
  		if( time == 0 )
  		{	
  			cleanUp();
  		}
   }
   
   public void cleanUp()
   {
   	if( c != null )
   	{	c.switchoff();
   		c = null;
   	}
   	dispose();
   }
   	
   public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}   
	public void mousePressed(MouseEvent e){}
	
	public void paint(Graphics g)
	{	
		java.util.Calendar cal = java.util.Calendar.getInstance();
		Graphics2D g1 = (Graphics2D)this.getGraphics();
		g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g1.drawImage(splash,0,0,this);
		g1.setColor( Color.black );
		g1.setFont(new Font("Arial", Font.BOLD, 12));
		g1.drawString( jmcc.getAppName(), 17, 196 );
		g1.setFont(new Font("Arial", Font.PLAIN, 11));
		g1.drawString( "Version "+ jmcc.getAppVersion(), 17, 212 );
		g1.drawString( "Build #"+ jmcc.getAppBuild(), 17, 227 );
		g1.drawString( "http://www.errorsoft.nl", 274, 212 );
		g1.drawString( "© Copyright Errorsoft 2002-"+ cal.get( java.util.Calendar.YEAR ), 230, 227 );
	}
	
	public void update(Graphics g)
	{	paint(g);
	}
}
package nl.errorsoft.dbcreator.gui.component;

import nl.errorsoft.dbcreator.gui.component.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CommentObject extends ModelObject implements MouseListener, FocusListener, AdjustmentListener
{	private String comment;

	private JTextArea jt =  new JTextArea();
	private JScrollPane jsp = new JScrollPane(jt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JButton jb = new JButton();

	public CommentObject( String comment, int identifier )
	{	this.comment = comment;
		this.setSize(90, 55);
		this.setIdentifier(identifier);
		
		jsp.setBounds(3,12,this.getSize().width-6,this.getSize().height-14);
		jsp.setBorder(null);
		jsp.getVerticalScrollBar().setPreferredSize(new Dimension(8, jsp.getVerticalScrollBar().getSize().height));
		
		jt.setLineWrap(true);
		jt.setWrapStyleWord(true);
		
		jb.setVisible(false);
		this.add(jb);
		
		this.addMouseListener(this);
		jsp.getVerticalScrollBar().addAdjustmentListener(this);
		
		jt.setFont(new Font("Arial", Font.PLAIN, 9));
		jt.setOpaque(false);
		jt.addFocusListener(this);
		jt.setText(comment);
		
		jt.setBackground(new Color(255,248,196));

		jsp.getViewport().setOpaque(false);
		jsp.setOpaque(false);
		this.add(jsp);
	}
	
	public void paintComponent(Graphics g)
	{	Graphics2D g2 = (Graphics2D)g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		if( !this.isSelected() )
		{	g2.setColor(new Color(255,248,196));
			Polygon p = new Polygon();
			p.addPoint(0,0);
			p.addPoint((int)this.getSize().width - 11, 0);
			p.addPoint((int)this.getSize().width - 1, 10);
			p.addPoint((int)this.getSize().width - 1, (int)this.getSize().height -1); 
			p.addPoint(0,(int)this.getSize().height -1);
			
			g2.fillPolygon(p);
			g2.setColor(Color.black);
			g2.drawPolygon(p);
			g2.drawLine((int)this.getSize().width - 11,0,(int)this.getSize().width - 11,10);
			g2.drawLine((int)this.getSize().width - 11,10,(int)this.getSize().width - 1,10);
		}
		else
		{	g2.setColor(new Color(255,248,196,175));
			Polygon p = new Polygon();
			p.addPoint(0,0);
			p.addPoint((int)this.getSize().width - 11, 0);
			p.addPoint((int)this.getSize().width - 1, 10);
			p.addPoint((int)this.getSize().width - 1, (int)this.getSize().height -1); 
			p.addPoint(0,(int)this.getSize().height -1);
			
			g2.fillPolygon(p);
			g2.setColor(Color.black);
			g2.drawPolygon(p);			
			g2.drawLine((int)this.getSize().width - 11,0,(int)this.getSize().width - 11,10);
			g2.drawLine((int)this.getSize().width - 11,10,(int)this.getSize().width - 1,10);			
		}
	}
	
	public String getComment()
	{	return jt.getText();
	}
	
	public void adjustmentValueChanged( AdjustmentEvent e )
	{	jt.requestFocus();
		jt.setOpaque(true);
		jt.repaint();
	}
	
	public void focusLost( FocusEvent e )
	{	jt.setOpaque(false);
		jt.repaint();
	}
	
	public void focusGained( FocusEvent e )
	{	jt.setOpaque(true);
		jt.repaint();
	}
	
	public void mousePressed( MouseEvent e )
	{	jb.requestFocus();
		this.repaint();
	}
	
	public void mouseClicked( MouseEvent e )
	{}
	public void mouseReleased( MouseEvent e )
	{}
	public void mouseEntered( MouseEvent e )
	{}
	public void mouseExited( MouseEvent e )
	{}
}
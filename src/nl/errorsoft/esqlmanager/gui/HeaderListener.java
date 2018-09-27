package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.domain.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class HeaderListener implements MouseListener
{	
	private JTableHeader header;	
	private HeaderRenderer renderer;

	public HeaderListener( JTableHeader header, HeaderRenderer renderer )
	{	this.header = header;
		this.renderer = renderer;
	}
	
	public void mousePressed(MouseEvent e)
	{	int x = 0;
		for(int i = 0; i < header.getTable().getColumnModel().getColumnCount(); i++)
		{
			int w = header.getTable().getColumnModel().getColumn(i).getWidth();
			int h = header.getHeight();
			
			x += w;
			
			Rectangle a = new Rectangle(x - 4, 0, 8, h);
			
			if( a.contains( e.getPoint() ))
			{	return;
			}	
		}	
		
		int col = header.columnAtPoint(e.getPoint());
		int sortCol = header.getTable().convertColumnIndexToModel(col);	
		renderer.setPressed(col);
		renderer.setSelectedColumn(col);
		
		SortableTableModel stm = (SortableTableModel)header.getTable().getModel();
		
		if(renderer.getState(col) == HeaderRenderer.DOWN)
		{	stm.sortByColumn(sortCol, true);	
		}
		else if(renderer.getState(col) == HeaderRenderer.UP)
		{	stm.sortByColumn(sortCol, false);
		}
		header.repaint();
	}
	
	public void mouseReleased(MouseEvent e)
	{	renderer.setPressed(-1);
		header.repaint();
	}
	
	public void mouseClicked(MouseEvent e)
	{	
	}
	
	public void mouseEntered(MouseEvent e)
	{
	}
	
	public void mouseExited(MouseEvent e)
	{
	}
}
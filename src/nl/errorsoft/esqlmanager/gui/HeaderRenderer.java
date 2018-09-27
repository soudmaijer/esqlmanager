package nl.errorsoft.esqlmanager.gui;

import java.util.*;
import java.awt.*;
import javax.swing.table.*;
import javax.swing.*;

public class HeaderRenderer extends DefaultTableCellRenderer
{	public static final int NONE = 0;
	public static final int UP = 1;
	public static final int DOWN = 2;
	
	private int pushedColumn;
	private ImageLoader il;
	private Hashtable state;
	private JLabel button;
	
	public HeaderRenderer(ImageLoader il)
	{	
		super();
		pushedColumn = -1;
		state = new Hashtable();
		this.il = il;
	}
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{	
		button = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		button.setOpaque( true );
		button.setHorizontalAlignment( JLabel.CENTER );
		button.setHorizontalTextPosition( JLabel.LEFT );
		super.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		
		if( value!=null )
			button.setText(value.toString());
		else
			value="";
			
		Object obj = state.get(new Integer(column));

		if(obj != null && ((Integer)obj).intValue() == UP)
		{	button.setIcon(new ImageIcon(il.getImage("sortup")));
		}
		else if(obj != null && ((Integer)obj).intValue() == DOWN)
		{	button.setIcon(new ImageIcon(il.getImage("sortdown")));
		}
		else
		{
			button.setIcon(null);
		}

		button.updateUI();
		return button;
	}
	
	public void setPressed(int column)
	{	pushedColumn = column;
	}
	
	public void setSelectedColumn(int column)
	{	if(column < 0) return;
		Integer value = null;
		Object obj = state.get(new Integer(column));
		if(obj == null)
		{	value = new Integer(DOWN);
		}
		else
		{	if(((Integer)obj).intValue() == DOWN)
			{	value = new Integer(UP);
			}
			else
			{	value = new Integer(DOWN);
			}
		}
		state.clear();
		state.put(new Integer(column), value);
	}
	
	public int getState(int column)
	{	int retValue;
		Object obj = state.get(new Integer(column));
		if(obj == null)
		{	retValue= NONE;
		}
		else
		{	if(((Integer)obj).intValue() == DOWN)
			{	retValue = DOWN;
			}
			else
			{	retValue = UP;
			}
		}
		return retValue;
	}
}
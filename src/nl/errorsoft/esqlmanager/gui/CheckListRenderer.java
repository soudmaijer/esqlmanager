package nl.errorsoft.esqlmanager.gui;
import java.awt.*;
import javax.swing.*;

public class CheckListRenderer extends JCheckBox implements ListCellRenderer 
{	public CheckListRenderer() 
	{	setBackground(UIManager.getColor("List.textBackground"));
      	setForeground(UIManager.getColor("List.textForeground"));
    }

    public Component getListCellRendererComponent(JList list, Object value,int index, boolean isSelected, boolean hasFocus) 
    {		setEnabled(list.isEnabled());
      	this.setSelected(((CheckableItem)value).isSelected());
      	if(isSelected)
      	{	setBackground(list.getSelectionBackground());
      		setForeground(list.getSelectionForeground());
      	}
      	else
      	{	setBackground(UIManager.getColor("List.textBackground"));
      		setForeground(UIManager.getColor("List.textForeground"));
      	}
      	setFont(list.getFont());
      	setText(value.toString());
       	return this;
    }
    
  	public Dimension getPreferredSize() 
  	{	return new Dimension (this.getSize().width,15);	
  	}    
} 
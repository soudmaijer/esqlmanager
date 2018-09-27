package nl.errorsoft.esqlmanager.gui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class TableListViewMouseListener extends MouseAdapter
{
	private TableListView tlv;
	
	public TableListViewMouseListener( TableListView tlv )
	{
		this.tlv = tlv;
	}

	public void mouseClicked( MouseEvent e )
	{
		if( e.getClickCount() == 2 )
		{
			tlv.tableSelected();
		}
	}
}
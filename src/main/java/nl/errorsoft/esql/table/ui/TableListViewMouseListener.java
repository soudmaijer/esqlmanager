package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.control.*;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.gui.*;

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
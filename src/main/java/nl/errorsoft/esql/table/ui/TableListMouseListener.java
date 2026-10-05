package nl.errorsoft.esql.table.ui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class TableListMouseListener extends MouseAdapter {
	private TableListTab tlv;

	public TableListMouseListener(TableListTab tlv) {
		this.tlv = tlv;
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			tlv.tableSelected();
		}
	}
}

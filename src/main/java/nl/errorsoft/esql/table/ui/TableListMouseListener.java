package nl.errorsoft.esql.table.ui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class TableListMouseListener extends MouseAdapter {
	private TableListTab tableListTab;

	public TableListMouseListener(TableListTab tableListTab) {
		this.tableListTab = tableListTab;
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			tableListTab.tableSelected();
		}
	}
}

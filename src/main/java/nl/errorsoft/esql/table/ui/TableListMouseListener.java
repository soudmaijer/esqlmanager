package nl.errorsoft.esql.table.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TableListMouseListener extends MouseAdapter {
	private final TableListTab tableListTab;

	public TableListMouseListener(TableListTab tableListTab) {
		this.tableListTab = tableListTab;
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			tableListTab.tableSelected();
		}
	}
}

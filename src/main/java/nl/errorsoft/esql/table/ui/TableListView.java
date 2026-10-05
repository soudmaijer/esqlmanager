package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.database.control.DatabaseCC;
import nl.errorsoft.esql.ui.ColumnWidths;
import nl.errorsoft.esql.ui.SortableTableModel;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.table.*;

import javax.swing.*;
import javax.swing.table.*;
import nl.errorsoft.esql.domain.*;

public class TableListView extends JScrollPane {
	DatabaseCC dbcc;
	SortableTableModel dtm;
	JTable table;

	public TableListView(DatabaseCC dbcc) {
		this.dbcc = dbcc;
		dtm = new SortableTableModel();
		table = new JTable(dtm) {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(table.AUTO_RESIZE_OFF);
		table.setShowGrid(true);
		table.setGridColor(new java.awt.Color(208, 208, 208));
		table.addMouseListener(new TableListViewMouseListener(this));
		table.getTableHeader().setReorderingAllowed(false);
		this.getViewport().add(table);
	}

	public void loadDatabases(java.util.Vector v) {
		dtm = new SortableTableModel();

		dtm.addColumn("Name");
		dtm.addColumn("Rows");
		dtm.addColumn("Type");
		dtm.addColumn("Comment");

		Object[] data = new Object[4];

		for (int i = 0; i < v.size(); i++) {
			data[0] = (Table) v.get(i);
			data[1] = Integer.valueOf(((Table) v.get(i)).getRowCount());
			data[2] = ((Table) v.get(i)).getType();
			data[3] = ((Table) v.get(i)).getComment();

			dtm.addRow(data);
			dtm.fireTableDataChanged();
		}

		table.setModel(dtm);
		ColumnWidths.fitToContent(table);
	}

	public void tableSelected() {
		if (table.getSelectedRow() > -1) {
			if (table.getValueAt(table.getSelectedRow(), 0) instanceof Table) {
				dbcc.tableSelected((Table) table.getValueAt(table.getSelectedRow(), 0));
			}
		}
	}
}

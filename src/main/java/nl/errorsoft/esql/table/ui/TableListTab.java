package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.table.Table;

import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.table.SortableTableModel;

import javax.swing.*;

public class TableListTab extends JScrollPane {
	DatabaseController databaseController;
	SortableTableModel dtm;
	JTable table;

	public TableListTab(DatabaseController databaseController) {
		this.databaseController = databaseController;
		dtm = new SortableTableModel();
		table = new JTable(dtm) {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(table.AUTO_RESIZE_OFF);
		table.setShowGrid(true);
		table.setGridColor(UIManager.getColor("Table.gridColor"));
		table.addMouseListener(new TableListMouseListener(this));
		table.getTableHeader().setReorderingAllowed(false);
		this.getViewport().add(table);
	}

	public void loadTables(java.util.List<Table> v) {
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
				databaseController.tableSelected((Table) table.getValueAt(table.getSelectedRow(), 0));
			}
		}
	}
}

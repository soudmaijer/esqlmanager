package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.table.Table;

import nl.errorsoft.esql.database.control.DatabaseController;
import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.table.SortableTableModel;

import javax.swing.*;

public class TableListTab extends JScrollPane {
	DatabaseController databaseController;
	SortableTableModel tableModel;
	JTable table;

	public TableListTab(DatabaseController databaseController) {
		this.databaseController = databaseController;
		tableModel = new SortableTableModel();
		table = new JTable(tableModel) {
			public boolean isCellEditable(int row, int column) {
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

	public void loadTables(java.util.List<Table> tables) {
		tableModel = new SortableTableModel();

		tableModel.addColumn("Name");
		tableModel.addColumn("Rows");
		tableModel.addColumn("Type");
		tableModel.addColumn("Comment");

		Object[] data = new Object[4];

		for (int i = 0; i < tables.size(); i++) {
			data[0] = (Table) tables.get(i);
			data[1] = Integer.valueOf(((Table) tables.get(i)).getRowCount());
			data[2] = ((Table) tables.get(i)).getType();
			data[3] = ((Table) tables.get(i)).getComment();

			tableModel.addRow(data);
			tableModel.fireTableDataChanged();
		}

		table.setModel(tableModel);
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

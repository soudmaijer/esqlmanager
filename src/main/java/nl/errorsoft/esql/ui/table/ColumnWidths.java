package nl.errorsoft.esql.ui.table;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Sizes table columns to their content, so text is not cut off but wide values do not take over the screen.
 */
public class ColumnWidths {
	private static final int MIN_WIDTH = 50;
	private static final int MAX_WIDTH = 400;
	private static final int PADDING = 12;
	private static final int ROWS_TO_MEASURE = 200;

	public static void fitToContent(JTable table) {
		int rows = Math.min(table.getRowCount(), ROWS_TO_MEASURE);

		for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
			TableColumn column = table.getColumnModel().getColumn(columnIndex);
			int width = headerWidth(table, column, columnIndex);

			for (int row = 0; row < rows; row++) {
				TableCellRenderer renderer = table.getCellRenderer(row, columnIndex);
				Component cell = table.prepareRenderer(renderer, row, columnIndex);
				width = Math.max(width, cell.getPreferredSize().width);
			}

			column.setPreferredWidth(Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width + PADDING)));
		}
	}

	private static int headerWidth(JTable table, TableColumn column, int columnIndex) {
		TableCellRenderer renderer = column.getHeaderRenderer();

		if (renderer == null) {
			renderer = table.getTableHeader().getDefaultRenderer();
		}

		Component header = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, columnIndex);
		return header.getPreferredSize().width;
	}
}

package nl.errorsoft.esql.ui.table;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import nl.errorsoft.esql.table.TableCell;

/** The default renderer of a grid of {@link TableCell}s: SQL NULL is shown as a dimmed, italic NULL, so that it differs from the text "null". */
public class NullCellRenderer extends DefaultTableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		boolean isNull = value instanceof TableCell cell && cell.isNull() && (cell.getTableColumn() == null || !cell.getTableColumn().isBinary());
		Component component = super.getTableCellRendererComponent(table, isNull ? "NULL" : value, isSelected, hasFocus, row, column);
		Font font = table.getFont();
		if (isNull) {
			component.setFont(font.deriveFont(Font.ITALIC));
			if (!isSelected) {
				component.setForeground(UIManager.getColor("Label.disabledForeground"));
			}
		} else {
			component.setFont(font);
		}
		return component;
	}
}

package nl.errorsoft.esql.ui.table;

import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;

public class SortHeaderListener implements MouseListener {
	private JTableHeader header;
	private HeaderRenderer renderer;

	public SortHeaderListener(JTableHeader header, HeaderRenderer renderer) {
		this.header = header;
		this.renderer = renderer;
	}

	public void mousePressed(MouseEvent e) {
		int x = 0;
		for (int i = 0; i < header.getTable().getColumnModel().getColumnCount(); i++) {
			int w = header.getTable().getColumnModel().getColumn(i).getWidth();
			int h = header.getHeight();

			x += w;

			Rectangle a = new Rectangle(x - 4, 0, 8, h);

			if (a.contains(e.getPoint())) {
				return;
			}
		}

		int viewColumn = header.columnAtPoint(e.getPoint());
		int sortColumn = header.getTable().convertColumnIndexToModel(viewColumn);
		renderer.setPressed(viewColumn);
		renderer.setSelectedColumn(viewColumn);

		SortableTableModel sortableModel = (SortableTableModel) header.getTable().getModel();

		if (renderer.getState(viewColumn) == HeaderRenderer.DOWN) {
			sortableModel.sortByColumn(sortColumn, true);
		} else if (renderer.getState(viewColumn) == HeaderRenderer.UP) {
			sortableModel.sortByColumn(sortColumn, false);
		}
		header.repaint();
	}

	public void mouseReleased(MouseEvent e) {
		renderer.setPressed(-1);
		header.repaint();
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}

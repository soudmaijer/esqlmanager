package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.table.SortHeaderListener;
import nl.errorsoft.esql.ui.table.HeaderRenderer;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.util.ToolbarButtons;
import nl.errorsoft.esql.ui.table.MultiLineCellEditor;
import nl.errorsoft.esql.ui.table.SortableTableModel;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.Paging;
import nl.errorsoft.esql.table.TableCell;
import nl.errorsoft.esql.table.PendingChange;
import nl.errorsoft.esql.ui.table.NullCellRenderer;
import nl.errorsoft.esql.table.control.TableController;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

public class TableDataTab extends JPanel implements ActionListener {
	private int skip;
	private int show;
	private Table table;
	private TableController tableController;
	private MultiLineCellEditor multiLineEditor;
	private SortableTableModel sortableModel;
	private TableColumnModel columnModel;
	private JLabel rowsLabel;
	private JTable dataTable;
	private JScrollPane dataScroll;
	private JSplitPane split;
	private JTextArea editorTextArea;
	/** The value of one cell, below the grid while it is open. */
	private CellValueEditor valueEditor;

	private JComponent navigationBar;
	private JToolBar rowToolbar;
	private JButton firstButton;
	private JButton prevButton;
	private JButton runButton;
	private JButton nextButton;
	private JButton lastButton;

	private JTextField skipField;
	private JTextField showField;
	private boolean inserting = false;
	/** Whether the rows can be written back: a table, not a query result or the server status. */
	private boolean editable = false;
	private JButton updateButton;
	private JButton editValueButton;
	private JButton deleteButton;
	private SortHeaderListener headerListener;

	public TableDataTab(TableController tableController) {
		this.tableController = tableController;
		this.setLayout(new BorderLayout());

		dataTable = new JTable(sortableModel) {
			public void editingStopped(javax.swing.event.ChangeEvent ev) {
				// This method is invoked when editing in the current cell is stoped
				// programmatically (by hitting TAB, ENTER, ARROW keys, or using mouse to
				// move to another cell). We override this method to reset beingEdited flag.
				if (dataTable.isEditing()) {
					int row = dataTable.getEditingRow();
					int columnCount = dataTable.getColumnCount();

					TableCell tableCell = (TableCell) dataTable.getValueAt(dataTable.getSelectedRow(), dataTable.getSelectedColumn());
					TableCell[] rowData = new TableCell[columnCount];
					Object newData = dataTable.getCellEditor().getCellEditorValue();

					for (int i = 0; i < columnCount; i++) {
						rowData[i] = (TableCell) dataTable.getValueAt(row, i);

						// New data inserted.
						if (rowData[i].isNewRow()) {
							tableCell.setData(newData);
							dataTable.setValueAt(tableCell, dataTable.getSelectedRow(), dataTable.getSelectedColumn());
							removeEditor();
							return;
						}
					}

					// Data updated: the editor stays open until the server took the value, and stays after a failure so that it can be corrected.
					int selectedRow = dataTable.getSelectedRow();
					int selectedColumn = dataTable.getSelectedColumn();
					changeCell(rowData, tableCell, newData, () -> {
						tableCell.setEditedText(newData == null ? null : newData.toString());
						dataTable.setValueAt(tableCell, selectedRow, selectedColumn);
						removeEditor();
					});
				}
			}
			public boolean isCellEditable(int row, int column) {
				TableCell tableCell = (TableCell) dataTable.getValueAt(row, column);
				return editable && tableCell.getTableColumn().isWritable() && !tableCell.getTableColumn().isBinary();
			}
		};
		dataTable.setAutoCreateColumnsFromModel(false);
		dataTable.setDefaultRenderer(Object.class, new NullCellRenderer());
		dataTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					deleteSelectedRows();
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
					insertNewRow();
					e.consume();
				}
			}
		});
		dataTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					openValueEditor();
				}
			}

			public void mousePressed(MouseEvent e) {
				showPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				showPopup(e);
			}
		});
		// F2 opens the value editor instead of editing in the grid; typing still edits in the grid.
		dataTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "editValue");
		dataTable.getActionMap().put("editValue", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				openValueEditor();
			}
		});
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dataTable.setShowGrid(true);
		dataTable.setGridColor(UIManager.getColor("Table.gridColor"));
		dataScroll = new JScrollPane(dataTable);

		editorTextArea = new JTextArea();
		editorTextArea.setLineWrap(true);
		editorTextArea.setFont(dataTable.getFont());
		editorTextArea.setWrapStyleWord(true);
		editorTextArea.setOpaque(true);
		multiLineEditor = new MultiLineCellEditor(editorTextArea);
		dataTable.setCellEditor(multiLineEditor);
		// The update buttons follow what is pending: text typed in the grid, another row or cell selected, an edit started or stopped.
		editorTextArea.getDocument().addDocumentListener(onChange(this::refreshUpdateButtons));
		dataTable.getSelectionModel().addListSelectionListener(e -> selectionChanged());
		dataTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> selectionChanged());
		dataTable.addPropertyChangeListener("tableCellEditor", e -> refreshUpdateButtons());

		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

		ImageLoader icons = ApplicationContext.get().imageLoader();
		firstButton = new JButton(icons.getIcon("imgFirst"));
		prevButton = new JButton(icons.getIcon("imgPrev"));
		runButton = new JButton(icons.getIcon("imgRun"));
		nextButton = new JButton(icons.getIcon("imgNext"));
		lastButton = new JButton(icons.getIcon("imgLast"));

		firstButton.addActionListener(this);
		prevButton.addActionListener(this);
		runButton.addActionListener(this);
		nextButton.addActionListener(this);
		lastButton.addActionListener(this);

		buttonPanel.add(firstButton);
		buttonPanel.add(prevButton);
		buttonPanel.add(runButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(lastButton);

		skipField = new JTextField("0");
		skipField.setPreferredSize(new Dimension(40, 20));
		showField = new JTextField("50");
		showField.setPreferredSize(new Dimension(40, 20));

		infoPanel.add(new JLabel("Skip:"));
		infoPanel.add(skipField);
		infoPanel.add(new JLabel("Show:"));
		infoPanel.add(showField);

		rowsLabel = new JLabel("Total: 0");
		infoPanel.add(rowsLabel);

		toolbar.add(buttonPanel);
		toolbar.add(infoPanel);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(dataScroll);
		this.add(split, BorderLayout.CENTER);
		rowToolbar = rowToolbar(icons);
		this.add(rowToolbar, BorderLayout.NORTH);
		navigationBar = toolbar;

		valueEditor = new CellValueEditor(new CellValueEditor.Actions(this::applyValue, this::closeValueEditor, () -> transferBlob(true),
			() -> transferBlob(false), this::refreshUpdateButtons));
		setRowEditing(false);
	}

	private static javax.swing.event.DocumentListener onChange(Runnable changed) {
		return new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				changed.run();
			}

			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				changed.run();
			}

			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				changed.run();
			}
		};
	}

	/** Insert, delete and save rows, at the top of the view; only a table (not a query result or the server status) can be edited. */
	private JToolBar rowToolbar(ImageLoader icons) {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		JButton insert = new JButton(icons.getIcon("imgNewRow"));
		insert.setToolTipText("Insert new row");
		insert.addActionListener(e -> insertNewRow());
		JButton delete = new JButton(icons.getIcon("imgDeleteRow"));
		deleteButton = delete;
		delete.setToolTipText("Delete row");
		delete.addActionListener(e -> deleteSelectedRows());
		JButton update = new JButton(icons.getIcon("imgUpdateRow"));
		update.setToolTipText("Update changes");
		update.addActionListener(e -> updateChanges());
		updateButton = update;
		JButton query = new JButton(icons.getIcon("imgRunQuery"));
		query.setToolTipText("New query on this table");
		query.addActionListener(e -> tableController.startQuery(table));
		editValueButton = new JButton(icons.getIcon("imgEditValue"));
		editValueButton.setToolTipText("Edit value (F2)");
		editValueButton.addActionListener(e -> openValueEditor());
		bar.add(insert);
		bar.add(delete);
		bar.add(update);
		bar.addSeparator();
		bar.add(editValueButton);
		bar.addSeparator();
		bar.add(query);
		ToolbarButtons.style(insert, delete, update, editValueButton, query);
		bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
		return bar;
	}

	/**
	 * Shows the row buttons for a table and hides them for rows that can never be written back (query results, server status, variables): an action that
	 * can never run there is hidden, not disabled. Edit value stays, it shows a read-only value.
	 */
	public void setRowEditing(boolean editable) {
		this.editable = editable;

		for (Component button : rowToolbar.getComponents()) {
			if (button != editValueButton) {
				button.setVisible(editable);
			}
		}
		refreshUpdateButtons();
	}

	/** The change the update buttons would write: the text of the value editor, else the cell edited in the grid, else a new row. */
	private PendingChange pendingChange() {
		if (valueEditorVisible() && valueEditor.isChanged()) {
			return PendingChange.CELL;
		}
		TableCell gridCell = null;
		String gridText = null;

		// Not isEditing(): the grid keeps its cell editor set between edits, the editing row tells whether a cell is being edited.
		if (dataTable.getEditingRow() >= 0 && dataTable.getEditingColumn() >= 0) {
			gridCell = (TableCell) dataTable.getValueAt(dataTable.getEditingRow(), dataTable.getEditingColumn());
			gridText = editorTextArea.getText();
		}
		int row = dataTable.getSelectedRow();
		boolean newRow = row >= 0 && dataTable.getColumnCount() > 0 && ((TableCell) dataTable.getValueAt(row, 0)).isNewRow();
		return PendingChange.of(editable, newRow, gridCell, gridText);
	}

	private boolean valueEditorVisible() {
		return valueEditor != null && split.getBottomComponent() == valueEditor;
	}

	/** Enables "Update changes" only when something would be written. */
	private void refreshUpdateButtons() {
		if (updateButton == null || valueEditor == null) {
			return; // still being built
		}
		ToolbarButtons.setAvailable(updateButton, pendingChange() != PendingChange.NONE ? null : "Change a cell or insert a row first.");
		ToolbarButtons.setAvailable(deleteButton, dataTable.getSelectedRowCount() > 0 ? null : "Select a row first.");
		ToolbarButtons.setAvailable(editValueButton, selectedCell() != null ? null : "Select a cell first.");
	}

	/** Writes what is pending: the text of the value editor, or else the cell edited in the grid and a new row. */
	private void updateChanges() {
		if (valueEditorVisible() && valueEditor.isChanged()) {
			applyValue(valueEditor.textArea().getText());
		} else {
			saveSelectedRow();
		}
	}

	private void showPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int row = dataTable.rowAtPoint(e.getPoint());
		int column = dataTable.columnAtPoint(e.getPoint());

		if (row < 0 || column < 0) {
			return;
		}
		dataTable.changeSelection(row, column, false, false);
		JPopupMenu menu = new JPopupMenu();
		JMenuItem edit = Forms.mnemonic(new JMenuItem(), "&Edit value...");
		edit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		edit.addActionListener(a -> openValueEditor());
		menu.add(edit);
		menu.show(dataTable, e.getX(), e.getY());
	}

	/** Another cell selected: the open value editor follows it unless it holds changes, and the buttons follow what is pending. */
	private void selectionChanged() {
		TableCell selected = selectedCell();

		if (valueEditorVisible() && selected != null && selected != valueEditor.getCell() && !valueEditor.isChanged()) {
			valueEditor.showCell(selected, readOnlyReason(selected));
		}
		refreshUpdateButtons();
	}

	private TableCell selectedCell() {
		int row = dataTable.getSelectedRow();
		int column = dataTable.getSelectedColumn();
		return row < 0 || column < 0 ? null : (TableCell) dataTable.getValueAt(row, column);
	}

	/** Null when the value of the cell can be written, else the hint saying why not. */
	private String readOnlyReason(TableCell cell) {
		if (!editable) {
			return "Read only: this view is not written back.";
		} else if (cell.getTableColumn() != null && !cell.getTableColumn().isWritable()) {
			return "Read only: this column cannot be changed.";
		}
		return null;
	}

	/** Opens the value editor below the grid on the selected cell; an edit in the grid is cancelled. */
	public void openValueEditor() {
		TableCell cell = selectedCell();

		if (cell == null) {
			return;
		}
		if (dataTable.getEditingRow() >= 0) {
			dataTable.getCellEditor().cancelCellEditing();
		}
		valueEditor.showCell(cell, readOnlyReason(cell));

		if (split.getBottomComponent() != valueEditor) {
			split.setBottomComponent(valueEditor);
			split.setDividerLocation(0.55);
		}
		valueEditor.textArea().requestFocusInWindow();
		refreshUpdateButtons();
		SwingUtilities.invokeLater(() -> dataTable.scrollRectToVisible(dataTable.getCellRect(dataTable.getSelectedRow(), 0, true)));
	}

	public void closeValueEditor() {
		split.setBottomComponent(null);
		refreshUpdateButtons();
	}

	/** The value editor, for a harness. */
	CellValueEditor valueEditor() {
		return valueEditor;
	}

	/** The grid, for a harness. */
	JTable grid() {
		return dataTable;
	}

	/** Whether "Update changes" is enabled, for a test or harness. */
	boolean isUpdateEnabled() {
		return updateButton.isEnabled();
	}

	/** The row of the grid that holds a cell, -1 when it is gone (another page was loaded). */
	private int rowOf(TableCell cell) {
		for (int row = 0; row < dataTable.getRowCount(); row++) {
			for (int column = 0; column < dataTable.getColumnCount(); column++) {
				if (dataTable.getValueAt(row, column) == cell) {
					return row;
				}
			}
		}
		return -1;
	}

	private TableCell[] rowCells(int row) {
		TableCell[] cells = new TableCell[dataTable.getColumnCount()];

		for (int i = 0; i < cells.length; i++) {
			cells[i] = (TableCell) dataTable.getValueAt(row, i);
		}
		return cells;
	}

	/** Writes the text of the value editor into its cell (empty is NULL); a cell of a new row only keeps it until the row is inserted. */
	private void applyValue(String text) {
		TableCell cell = valueEditor.getCell();
		int row = rowOf(cell);

		if (row < 0) {
			return;
		}
		if (cell.isNewRow()) {
			cell.setEditedText(text);
			sortableModel.fireTableRowsUpdated(row, row);
			valueEditor.showText();
			return;
		}
		changeCell(rowCells(row), cell, text, () -> {
			cell.setEditedText(text);
			sortableModel.fireTableRowsUpdated(row, row);
			valueEditor.showText();
		});
	}

	/** Upload into or download from the binary cell in the value editor, through the blob transfer dialogs. */
	private void transferBlob(boolean upload) {
		TableCell cell = valueEditor.getCell();
		int row = rowOf(cell);

		if (row < 0) {
			return;
		}
		if (upload) {
			tableController.showUploadFileDialog(table, rowCells(row), cell);
		} else {
			tableController.showDownloadFileDialog(table, rowCells(row), cell);
		}
	}

	/** Writes the cell in the background, the grid is disabled meanwhile; {@code saved} runs on the event thread when the server took it. */
	private void changeCell(TableCell[] rowData, TableCell cellData, Object newValue, Runnable saved) {
		dataTable.setEnabled(false);
		tableController.changeCell(cellData.getTableColumn().getTable(), rowData, cellData, newValue, saved, () -> {
			dataTable.setEnabled(true);
			refreshUpdateButtons();
		});
	}

	public void insertNewRow() {
		if (!inserting) {
			this.closeValueEditor();
			inserting = true;
			int columnCount = dataTable.getColumnCount();
			java.util.Vector<Object> newData = new java.util.Vector<>();

			for (int i = 0; i < columnCount; i++) {
				TableCell newCell = new TableCell();
				newCell.setNewRow(true);
				newCell.setData(new String());
				newCell.setTableColumn((nl.errorsoft.esql.table.TableColumn) dataTable.getColumnModel().getColumn(i).getHeaderValue());

				newData.insertElementAt(newCell, i);
			}

			sortableModel.addRow(newData);
			dataTable.setRowSelectionInterval(sortableModel.getRowCount() - 1, sortableModel.getRowCount() - 1);

			SwingUtilities.invokeLater(() -> dataScroll.getVerticalScrollBar().setValue(dataScroll.getVerticalScrollBar().getMaximum()));
		}
	}

	public void saveSelectedRow() {
		int selectedRow = dataTable.getSelectedRow();

		if (dataTable.getSelectedRow() < 0) {
			return;
		}

		if (dataTable.isEditing()) {
			dataTable.getCellEditor().stopCellEditing();
		}

		int columnCount = dataTable.getColumnCount();
		TableCell[] cells = new TableCell[columnCount];

		for (int j = 0; j < columnCount; j++) {
			cells[j] = (TableCell) sortableModel.getValueAt(selectedRow, j);
		}
		if (cells[0].isNewRow()) {
			dataTable.setEnabled(false);
			tableController.insertRow(table, cells, this::refreshData, () -> dataTable.setEnabled(true));
		}
	}

	public void deleteSelectedRows() {
		this.closeValueEditor();
		int[] selectedRows = dataTable.getSelectedRows();

		if (selectedRows == null || selectedRows.length == 0) {
			return;
		}

		String from = "'" + table.getDatabase().getName() + "." + table.qualifiedName() + "'";

		if (Dialogs.confirmDestructive(this, "Delete rows", "Delete " + selectedRows.length + " row(s) from " + from + "? This cannot be undone.", "Delete")) {
			int columnCount = dataTable.getColumnCount();
			// From the bottom up, so that removing a row leaves the index of the next one alone. A new row that was never saved is only removed.
			List<Integer> saved = new ArrayList<>();
			List<TableCell[]> rows = new ArrayList<>();

			for (int i = selectedRows.length - 1; i >= 0; i--) {
				TableCell[] cells = new TableCell[columnCount];

				for (int j = 0; j < columnCount; j++) {
					cells[j] = (TableCell) sortableModel.getValueAt(selectedRows[i], j);
				}
				if (cells[0].isNewRow()) {
					inserting = false;
					sortableModel.removeRow(selectedRows[i]);
				} else {
					saved.add(selectedRows[i]);
					rows.add(cells);
				}
			}
			showRecordCount();

			if (!rows.isEmpty()) {
				dataTable.setEnabled(false);
				tableController.deleteRows(table, rows, deleted -> {
					for (int i = 0; i < deleted; i++) {
						sortableModel.removeRow(saved.get(i));
					}
					showRecordCount();
				}, () -> dataTable.setEnabled(true));
			}
		}
	}

	public int getRowCount() {
		return table.getRowCount();
	}

	public void showRecordCount() {
		rowsLabel.setText("Total: " + table.getRowCount());
	}

	public void loadData(Table table, nl.errorsoft.esql.table.TableColumn[] columns, TableCell[][] cells) {
		this.table = table;
		showRecordCount();
		// The cell in the value editor belongs to the previous page.
		closeValueEditor();

		sortableModel = new SortableTableModel();
		columnModel = new DefaultTableColumnModel();
		HeaderRenderer headerRenderer = new HeaderRenderer();

		for (int i = 0; i < columns.length; i++) {
			javax.swing.table.TableColumn headerColumn = new javax.swing.table.TableColumn(i);
			headerColumn.setHeaderValue(columns[i]);
			headerColumn.setHeaderRenderer(headerRenderer);
			columnModel.addColumn(headerColumn);
		}

		// Every page has its own renderer, the listener of the previous page goes.
		if (headerListener != null) {
			dataTable.getTableHeader().removeMouseListener(headerListener);
		}
		headerListener = new SortHeaderListener(dataTable.getTableHeader(), headerRenderer);
		dataTable.getTableHeader().addMouseListener(headerListener);
		sortableModel.setDataVector(cells, columns);
		dataTable.setColumnModel(columnModel);
		// A new column model has its own selection model.
		columnModel.getSelectionModel().addListSelectionListener(e -> selectionChanged());
		dataTable.setModel(sortableModel);
		ColumnWidths.fitToContent(dataTable);
		dataScroll.getViewport().revalidate();
		selectionChanged();
	}

	/** The paging buttons with skip, show and total. The window shows them in its own status bar, next to what the connection did last. */
	public JComponent getNavigationBar() {
		return navigationBar;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		try {
			skip = Integer.parseInt(this.skipField.getText());
			show = Integer.parseInt(this.showField.getText());
		} catch (Exception ex) {
			Dialogs.error(this, "Show data", "Enter a number in Skip and Show.");
			return;
		}

		try {
			int rows = table.getRowCount();
			Paging page = new Paging(skip, show);

			if (source == firstButton) {
				page = page.first();
			} else if (source == prevButton) {
				page = page.previous();
			} else if (source == runButton) {
				page = page.clamped(rows);
			} else if (source == nextButton) {
				page = page.next(rows);
			} else if (source == lastButton) {
				page = page.last(rows);
			}
			skip = page.skip();

			skipField.setText(Integer.toString(skip));
			showField.setText(Integer.toString(show));

			tableController.showTableData(table, skip, show);
			inserting = false;
		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Show data", ex);
		}
	}

	public void refreshData() {
		int rows = table.getRowCount();

		try {
			skip = Integer.parseInt(this.skipField.getText());
			show = Integer.parseInt(this.showField.getText());
		} catch (Exception ex) {
			Dialogs.error(this, "Refresh data", "Enter a number in Skip and Show.");
			return;
		}

		try {
			// Called after a row was added: the new row is at the end, so the last page is shown.
			skip = new Paging(skip, show).last(rows).skip();

			this.closeValueEditor();
			inserting = false;
			tableController.showTableData(table, skip, show);

		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Refresh data", ex);
		}
	}
}

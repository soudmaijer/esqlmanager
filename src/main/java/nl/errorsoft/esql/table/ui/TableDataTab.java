package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.util.ExtensionFileFilter;
import nl.errorsoft.esql.ui.table.SortHeaderListener;
import nl.errorsoft.esql.ui.table.HeaderRenderer;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.table.MultiLineCellEditor;
import nl.errorsoft.esql.ui.table.SortableTableModel;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableCell;
import nl.errorsoft.esql.table.control.TableController;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.UndoManager;

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
	private JTextArea cellData; // Contains cell data.
	private JScrollPane cellScroll; // ScrollPane for cellData textArea

	private JComponent navigationBar;
	private JButton firstButton;
	private JButton prevButton;
	private JButton runButton;
	private JButton nextButton;
	private JButton lastButton;
	private JButton saveDataButton;
	private JButton addDataButton;

	private JTextField skipField;
	private JTextField showField;
	private boolean inserting = false;

	// CellDataEditor toolbar.
	private JPanel cellEditorPanel;
	private JButton updateRowDataButton;
	private JButton saveCellDataButton;
	private JButton closeCellDataButton;
	private UndoManager undoManager;
	private SortHeaderListener headerListener;
	private TableCell editingCell;
	private int editingRow;
	private int editingCol;

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
					java.util.Vector<?> dataVector = sortableModel.getDataVector();
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

					// Data updated.
					if (dataChanged(rowData, tableCell, newData)) {
						tableCell.setEditedText(newData == null ? null : newData.toString());
						dataTable.setValueAt(tableCell, dataTable.getSelectedRow(), dataTable.getSelectedColumn());
						removeEditor();
					}
				}
			}
			public boolean isCellEditable(int row, int column) {
				TableCell tableCell = (TableCell) dataTable.getValueAt(row, column);
				return tableCell.getTableColumn().isWritable();
			}
		};
		dataTable.setAutoCreateColumnsFromModel(false);
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
				if (e.getClickCount() == 1) {
					if (dataTable.getSelectedColumn() > -1 && dataTable.getSelectedRow() > -1) {
						nl.errorsoft.esql.table.TableColumn clickedColumn = ((nl.errorsoft.esql.table.TableColumn) (dataTable.getTableHeader().getColumnModel()
							.getColumn(dataTable.getSelectedColumn()).getHeaderValue()));

						if (clickedColumn.isBinary()) {
							enableBinaryDataEditor();
						} else {
							Object clickedValue = dataTable.getValueAt(dataTable.getSelectedRow(), dataTable.getSelectedColumn());

							if (clickedValue instanceof TableCell data) {
								if (!data.isNewRow()) {
									enabledCellDataEditor();
								}
							}
						}
					}
				}
			}
		});
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dataTable.setShowGrid(true);
		dataTable.setGridColor(UIManager.getColor("Table.gridColor"));
		dataTable.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (e.getOppositeComponent() != null && e.getOppositeComponent() instanceof JTextField) {
					disableCellDataEditor();
				}
			}
		});
		dataScroll = new JScrollPane(dataTable);

		editorTextArea = new JTextArea();
		editorTextArea.setLineWrap(true);
		editorTextArea.setFont(dataTable.getFont());
		editorTextArea.setWrapStyleWord(true);
		editorTextArea.setOpaque(true);
		multiLineEditor = new MultiLineCellEditor(editorTextArea);
		dataTable.setCellEditor(multiLineEditor);

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

		/*
		 * Info panel
		 */
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

		/*
		 * Add panels to toolbar
		 */
		toolbar.add(buttonPanel);
		toolbar.add(infoPanel);

		/*
		 * Add toolbar to panel
		 */

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(dataScroll);
		this.add(split, BorderLayout.CENTER);
		navigationBar = toolbar;

		// Create TextArea for row data.
		cellData = new JTextArea();
		cellData.setLineWrap(false);
		cellData.setWrapStyleWord(true);
		// One undo history for the editor, cleared whenever another cell is shown in it.
		undoManager = new UndoManager();
		cellData.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
		int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		bindUndo(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu), "undo", () -> {
			if (undoManager.canUndo()) {
				undoManager.undo();
			}
		});
		bindUndo(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menu), "redo", () -> {
			if (undoManager.canRedo()) {
				undoManager.redo();
			}
		});
		bindUndo(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu | InputEvent.SHIFT_DOWN_MASK), "redo", () -> {
			if (undoManager.canRedo()) {
				undoManager.redo();
			}
		});
		cellScroll = new JScrollPane(cellData, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Add to splitpane
		cellEditorPanel = new JPanel();
		cellEditorPanel.setLayout(new BorderLayout());

		ImageLoader images = ApplicationContext.get().imageLoader();
		updateRowDataButton = new JButton(images.getIcon("imgUpdateRow"));
		updateRowDataButton.setToolTipText("Update changes");
		updateRowDataButton.addActionListener(this);

		saveCellDataButton = new JButton(images.getIcon("imgSave"));
		saveCellDataButton.setToolTipText("Save data to file");
		saveCellDataButton.addActionListener(this);

		closeCellDataButton = new JButton(images.getIcon("imgClose"));
		closeCellDataButton.setToolTipText("Close the cell editor");
		closeCellDataButton.addActionListener(this);

		JToolBar cellToolbar = new JToolBar();
		cellToolbar.setFloatable(false);

		cellToolbar.add(saveCellDataButton);
		cellToolbar.add(closeCellDataButton);
		cellToolbar.add(updateRowDataButton);

		cellScroll.getViewport().add(cellData);

		cellEditorPanel.add(cellToolbar, BorderLayout.NORTH);
		cellEditorPanel.add(cellScroll, BorderLayout.CENTER);
	}

	private void bindUndo(KeyStroke key, String name, Runnable action) {
		cellData.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
		cellData.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	/** Keeps a button at its own width in a column that stretches. */
	private static JPanel left(JButton button) {
		JPanel flowPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		flowPanel.add(button);
		return flowPanel;
	}

	public void enableBinaryDataEditor() {
		saveDataButton = Forms.button("&Download...");
		JLabel saveDataLabel = Forms.label("Save the cell data to a file:", saveDataButton);
		addDataButton = Forms.button("&Upload...");
		JLabel addDataLabel = Forms.label("Load a file into the cell:", addDataButton);

		JPanel binaryOptionsPanel = new Forms.Grid().row(addDataLabel, left(addDataButton)).row(saveDataLabel, left(saveDataButton)).panel();
		Forms.titled(binaryOptionsPanel, "Binary data options");
		JPanel binaryPanel = Forms.padded(new JPanel(new BorderLayout()));
		binaryPanel.add(binaryOptionsPanel, BorderLayout.NORTH);

		saveDataButton.addActionListener(this);
		addDataButton.addActionListener(this);

		split.setBottomComponent(binaryPanel);
		split.setDividerLocation(split.getHeight() - 140);

	}

	public void enabledCellDataEditor() {
		// Enable textarea
		editingRow = dataTable.getSelectedRow();
		editingCol = dataTable.getSelectedColumn();
		editingCell = (TableCell) dataTable.getModel().getValueAt(editingRow, editingCol);

		cellData.setEnabled(true);
		cellData.setText(editingCell.getEditText());
		undoManager.discardAllEdits();

		if (editingCell.getTableColumn().isWritable()) {
			updateRowDataButton.setEnabled(true);
			closeCellDataButton.setEnabled(true);
			cellData.setEditable(true);
		} else {
			updateRowDataButton.setEnabled(false);
			closeCellDataButton.setEnabled(false);
			cellData.setEditable(false);
		}
		split.setBottomComponent(cellEditorPanel);
		split.setDividerLocation(0.70);
		cellData.setCaretPosition(0);

		SwingUtilities.invokeLater(() -> dataTable.scrollRectToVisible(dataTable.getCellRect(dataTable.getSelectedRow(), 0, true)));
	}

	public void disableCellDataEditor() {
		split.setBottomComponent(null);
	}

	public boolean dataChanged(TableCell[] rowData, TableCell cellData, Object newValue) {
		try {
			tableController.dataChanged(cellData.getTableColumn().getTable(), rowData, cellData, newValue);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Change cell", e);
			return false;
		}
		return true;
	}

	public void insertNewRow() {
		if (!inserting) {
			this.disableCellDataEditor();
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
		try {
			if (cells[0].isNewRow()) {
				tableController.insertRow(table, cells);
				refreshData();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Save selected row", e);
		}
	}

	public void deleteSelectedRows() {
		this.disableCellDataEditor();
		int[] selectedRows = dataTable.getSelectedRows();

		if (selectedRows == null || selectedRows.length == 0) {
			return;
		}

		String from = "'" + table.getDatabase().getName() + "." + table.getName() + "'";

		if (Dialogs.confirmDestructive(this, "Delete rows", "Delete " + selectedRows.length + " row(s) from " + from + "? This cannot be undone.", "Delete")) {
			int columnCount = dataTable.getColumnCount();
			TableCell[] cells = new TableCell[columnCount];

			for (int i = selectedRows.length - 1; i >= 0; i--) {
				for (int j = 0; j < columnCount; j++) {
					cells[j] = (TableCell) sortableModel.getValueAt(selectedRows[i], j);
				}
				try {
					if (!cells[0].isNewRow()) {
						tableController.deleteRow(table, cells);
					} else {
						inserting = false;
					}
					sortableModel.removeRow(selectedRows[i]);
					showRecordCount();
				} catch (Exception e) {
					ApplicationContext.get().errors().report(this, "Delete row", e);

					if (selectedRows.length > 1) {
						if (!Dialogs.confirm(this, "Delete rows", "Delete failed for a row. Continue with the remaining rows?", "Continue")) {
							break;
						}
					}

				}
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
		dataTable.setModel(sortableModel);
		ColumnWidths.fitToContent(dataTable);
		dataScroll.getViewport().revalidate();
	}

	/** The paging buttons with skip, show and total. The window shows them in its own status bar, next to what the connection did last. */
	public JComponent getNavigationBar() {
		return navigationBar;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == updateRowDataButton) {
			TableCell[] rowData = new TableCell[dataTable.getColumnCount()];

			for (int i = 0; i < dataTable.getColumnCount(); i++) {
				rowData[i] = (TableCell) dataTable.getValueAt(this.editingRow, i);
			}

			if (this.dataChanged(rowData, this.editingCell, this.cellData.getText())) {
				editingCell.setEditedText(this.cellData.getText());
				sortableModel.fireTableDataChanged();
			}
		} else if (source == closeCellDataButton) {
			this.disableCellDataEditor();
		} else if (source == saveCellDataButton) {
			JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(new ExtensionFileFilter("text file", new String[]{".txt"}));
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setDialogTitle("Save cell data...");

			try {
				if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					java.nio.file.Files.writeString(nl.errorsoft.esql.ui.util.FileChoosers.withExtension(chooser.getSelectedFile(), ".txt").toPath(),
						cellData.getText() + System.lineSeparator());
				}
			} catch (Exception err) {
				ApplicationContext.get().errors().report(this, "Save cell data", err);
			}
		} else if (source == saveDataButton) {
			int row = dataTable.getSelectedRow();
			TableCell[] rowData = new TableCell[dataTable.getColumnCount()];

			for (int i = 0; i < dataTable.getColumnCount(); i++) {
				rowData[i] = (TableCell) dataTable.getValueAt(row, i);
			}

			tableController.showDownloadFileDialog(this.table, rowData, (TableCell) dataTable.getValueAt(row, dataTable.getSelectedColumn()));
		} else if (source == addDataButton) {
			int row = dataTable.getSelectedRow();
			TableCell[] rowData = new TableCell[dataTable.getColumnCount()];

			for (int i = 0; i < dataTable.getColumnCount(); i++) {
				rowData[i] = (TableCell) dataTable.getValueAt(row, i);
			}

			tableController.showUploadFileDialog(this.table, rowData, (TableCell) dataTable.getValueAt(row, dataTable.getSelectedColumn()));
		} else {
			try {
				skip = Integer.parseInt(this.skipField.getText());
				show = Integer.parseInt(this.showField.getText());
			} catch (Exception ex) {
				Dialogs.error(this, "Show data", "Enter a number in Skip and Show.");
				return;
			}

			try {
				int rows = table.getRowCount();

				if (source == firstButton) {
					skip = 0;
				} else if (source == prevButton) {
					skip = skip - show;

					if (skip < 0) {
						skip = 0;
					}
				} else if (source == runButton) {
					if (skip + show > rows) {
						skip = rows - show;
					}
					if (rows - show < 0) {
						skip = 0;
					}
				} else if (source == nextButton) {
					skip = skip + show;

					if (skip >= rows) {
						skip = skip - show;
					}
				} else if (source == lastButton) {
					skip = rows - show;

					if (rows < show) {
						skip = 0;
					}
				}

				skipField.setText(Integer.toString(skip));
				showField.setText(Integer.toString(show));

				tableController.showTableData(table, skip, show);
				inserting = false;
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Show data", ex);
			}
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
			skip = rows - show;

			if (rows < show) {
				skip = 0;
			}

			this.disableCellDataEditor();
			inserting = false;
			tableController.showTableData(table, skip, show);

		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Refresh data", ex);
		}
	}
}

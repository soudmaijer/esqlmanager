//Source file: d:\\roseoutput\\esql\\esql\\table\\TableDataView.java

package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.query.ui.UndoHandler;
import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.util.ExtentionFileFilter;
import nl.errorsoft.esql.ui.table.HeaderListener;
import nl.errorsoft.esql.ui.table.HeaderRenderer;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.table.MultiLineCellEditor;
import nl.errorsoft.esql.ui.table.SortableTableModel;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.control.TableCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.UndoManager;
import javax.swing.event.UndoableEditListener;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class TableDataView extends JPanel implements ActionListener {
	private static final Logger log = LogManager.getLogger(TableDataView.class);

	private int skip;
	private int show;
	private Table table;
	private TableCC tcc;
	private MultiLineCellEditor mlce;
	private SortableTableModel stm;
	private TableColumnModel tcm;
	private JLabel lblSpace;
	private JLabel lblRows;
	private JTable tbData;
	private JScrollPane jsp;
	private JSplitPane jsplit;
	private JTextArea jta;
	private JTextArea cellData; // Contains cell data.
	private JScrollPane jspText; // ScrollPane for cellData textArea

	private JComponent navigationBar;
	private JButton btnFirst;
	private JButton btnPrev;
	private JButton btnRun;
	private JButton btnNext;
	private JButton btnLast;
	private JButton btnSaveData;
	private JButton btnAddData;

	private JTextField jtfSkip;
	private JTextField jtfShow;
	private boolean inserting = false;

	// CellDataEditor toolbar.
	private JPanel jcep;
	private JButton btnUpdateRowData;
	private JButton btnSaveCellData;
	private JButton btnCloseCellData;
	private UndoManager ndo;
	private UndoableEditListener undoHandler;
	private ImageLoader imgLoader;
	private TableData editingCell;
	private int editingRow;
	private int editingCol;

	/**
	* @roseuid 3E05A84602EC
	*/
	public TableDataView(TableCC tcc) {
		this.tcc = tcc;
		this.imgLoader = ApplicationContext.get().imageLoader();
		this.setLayout(new BorderLayout());

		tbData = new JTable(stm) {
			public void editingStopped(javax.swing.event.ChangeEvent ev) {
				// This method is invoked when editing in the current cell is stoped
				// programmatically (by hitting TAB, ENTER, ARROW keys, or using mouse to
				// move to another cell). We override this method to reset beingEdited flag.
				if (tbData.isEditing()) {
					int row = tbData.getEditingRow();
					int cols = tbData.getColumnCount();

					TableData td = (TableData) tbData.getValueAt(tbData.getSelectedRow(), tbData.getSelectedColumn());
					TableData[] rowData = new TableData[cols];
					java.util.Vector<?> dataVector = stm.getDataVector();
					Object newData = tbData.getCellEditor().getCellEditorValue();

					for (int i = 0; i < cols; i++) {
						rowData[i] = (TableData) tbData.getValueAt(row, i);

						// New data inserted.
						if (rowData[i].isNewRow()) {
							td.setData(newData);
							tbData.setValueAt(td, tbData.getSelectedRow(), tbData.getSelectedColumn());
							removeEditor();
							return;
						}
					}

					// Data updated.
					if (dataChanged(rowData, td, newData)) {
						td.setData(newData);
						tbData.setValueAt(td, tbData.getSelectedRow(), tbData.getSelectedColumn());
						removeEditor();
					}
				}
			}
			public boolean isCellEditable(int row, int col) {
				TableData td = (TableData) tbData.getValueAt(row, col);
				return td.getTableColumn().isWritable();
			}
		};
		tbData.setAutoCreateColumnsFromModel(false);
		tbData.addKeyListener(new KeyAdapter() {
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
		tbData.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					if (tbData.getSelectedColumn() > -1 && tbData.getSelectedRow() > -1) {
						nl.errorsoft.esql.table.TableColumn tempTc = ((nl.errorsoft.esql.table.TableColumn) (tbData.getTableHeader().getColumnModel()
							.getColumn(tbData.getSelectedColumn()).getHeaderValue()));

						if (tempTc.isBinary()) {
							enableBinaryDataEditor();
						} else {
							Object temp = tbData.getValueAt(tbData.getSelectedRow(), tbData.getSelectedColumn());

							if (temp instanceof TableData data) {
								if (!data.isNewRow()) {
									enabledCellDataEditor();
								}
							}
						}
					}
				}
			}
		});
		tbData.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbData.setShowGrid(true);
		tbData.setGridColor(new java.awt.Color(208, 208, 208));
		tbData.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (e.getOppositeComponent() != null && e.getOppositeComponent() instanceof JTextField) {
					disableCellDataEditor();
				}
			}
		});
		jsp = new JScrollPane(tbData);

		jta = new JTextArea();
		jta.setLineWrap(true);
		jta.setFont(tbData.getFont());
		jta.setWrapStyleWord(true);
		jta.setOpaque(true);
		mlce = new MultiLineCellEditor(jta);
		tbData.setCellEditor(mlce);

		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel jpButton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel jpInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

		ImageLoader icons = ApplicationContext.get().imageLoader();
		btnFirst = new JButton(icons.getIcon("imgFirst"));
		btnPrev = new JButton(icons.getIcon("imgPrev"));
		btnRun = new JButton(icons.getIcon("imgRun"));
		btnNext = new JButton(icons.getIcon("imgNext"));
		btnLast = new JButton(icons.getIcon("imgLast"));

		btnFirst.addActionListener(this);
		btnPrev.addActionListener(this);
		btnRun.addActionListener(this);
		btnNext.addActionListener(this);
		btnLast.addActionListener(this);

		jpButton.add(btnFirst);
		jpButton.add(btnPrev);
		jpButton.add(btnRun);
		jpButton.add(btnNext);
		jpButton.add(btnLast);

		/*
		 * Info panel
		 */
		jtfSkip = new JTextField("0");
		jtfSkip.setPreferredSize(new Dimension(40, 20));
		jtfShow = new JTextField("50");
		jtfShow.setPreferredSize(new Dimension(40, 20));

		jpInfo.add(new JLabel(" Skip:"));
		jpInfo.add(jtfSkip);
		jpInfo.add(new JLabel(" Show:"));
		jpInfo.add(jtfShow);

		lblRows = new JLabel(" Total:  0");
		jpInfo.add(lblRows);

		/*
		 * Add panels to toolbar
		 */
		toolbar.add(jpButton);
		toolbar.add(jpInfo);

		/*
		 * Add toolbar to panel
		 */

		jsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		jsplit.setTopComponent(jsp);
		this.add(jsplit, BorderLayout.CENTER);
		navigationBar = toolbar;

		// Create TextArea for row data.
		cellData = new JTextArea();
		cellData.setLineWrap(false);
		cellData.setWrapStyleWord(true);
		cellData.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				if (evt.getKeyCode() == evt.VK_Z && evt.isControlDown()) {
					if (ndo.canUndo()) {
						ndo.undo();
					}
				} else if (evt.getKeyCode() == evt.VK_Y && evt.isControlDown()) {
					if (ndo.canRedo()) {
						ndo.redo();
					}
				}
			}
		});
		jspText = new JScrollPane(cellData, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Add to splitpane
		jcep = new JPanel();
		jcep.setLayout(new BorderLayout());

		// Make copy.
		btnUpdateRowData = new JButton(imgLoader.getIcon("imgUpdateRow"));
		btnUpdateRowData.setToolTipText("Update changes");
		btnUpdateRowData.addActionListener(this);

		btnSaveCellData = new JButton(imgLoader.getIcon("imgSave"));
		btnSaveCellData.setToolTipText("Save data to file");
		btnSaveCellData.addActionListener(this);

		btnCloseCellData = new JButton(imgLoader.getIcon("imgDeleteRow"));
		btnCloseCellData.setToolTipText("Close");
		btnCloseCellData.addActionListener(this);

		JToolBar jtb = new JToolBar();
		jtb.setFloatable(false);

		jtb.add(btnSaveCellData);
		jtb.add(btnCloseCellData);
		jtb.add(btnUpdateRowData);

		jspText.getViewport().add(cellData);

		jcep.add(jtb, BorderLayout.NORTH);
		jcep.add(jspText, BorderLayout.CENTER);
	}

	public void enableBinaryDataEditor() {
		JPanel jp = new JPanel();
		jp.setLayout(null);

		JPanel jpButtons = new JPanel();
		jpButtons.setLayout(null);
		jpButtons.setBorder(new TitledBorder(new EtchedBorder(BevelBorder.LOWERED), "Binary data options",
			javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
			new java.awt.Font(jpButtons.getFont().getFontName(), java.awt.Font.BOLD, jpButtons.getFont().getSize())));
		jpButtons.setBounds(10, 15, 350, 100);

		JLabel lblSaveData = new JLabel("Save cell-data to file:");
		btnSaveData = new JButton("Download...");
		JLabel lblAddData = new JLabel("Insert data into cell: ");
		btnAddData = new JButton("Upload...");

		lblAddData.setBounds(15, 30, 200, 20);
		lblSaveData.setBounds(15, 55, 200, 20);
		btnAddData.setBounds(215, 30, 90, 21);
		btnSaveData.setBounds(215, 55, 90, 21);

		jpButtons.add(lblSaveData);
		jpButtons.add(lblAddData);
		jpButtons.add(btnSaveData);
		jpButtons.add(btnAddData);
		jp.add(jpButtons);

		btnSaveData.addActionListener(this);
		btnAddData.addActionListener(this);

		jsplit.setBottomComponent(jp);
		jsplit.setDividerLocation(jsplit.getHeight() - 140);

	}

	public void enabledCellDataEditor() {
		// Enable textarea
		editingRow = tbData.getSelectedRow();
		editingCol = tbData.getSelectedColumn();
		editingCell = (TableData) tbData.getModel().getValueAt(editingRow, editingCol);

		cellData.setEnabled(true);
		cellData.setText(editingCell.getData());

		if (editingCell.getTableColumn().isWritable()) {
			btnUpdateRowData.setEnabled(true);
			btnCloseCellData.setEnabled(true);
			cellData.setEditable(true);

			// Listener for edits on a document.
			ndo = new UndoManager();
			undoHandler = new UndoHandler(ndo);
			cellData.getDocument().addUndoableEditListener(undoHandler);
		} else {
			btnUpdateRowData.setEnabled(false);
			btnCloseCellData.setEnabled(false);
			cellData.setEditable(false);
		}
		jsplit.setBottomComponent(jcep);
		jsplit.setDividerLocation(0.70);
		cellData.setCaretPosition(0);

		SwingUtilities.invokeLater(() -> tbData.scrollRectToVisible(tbData.getCellRect(tbData.getSelectedRow(), 0, true)));
	}

	public void disableCellDataEditor() {
		// Listener for edits on a document.
		cellData.getDocument().removeUndoableEditListener(undoHandler);
		jsplit.setBottomComponent(null);
	}

	public boolean dataChanged(TableData[] rowData, TableData cellData, Object newValue) {
		try {
			tcc.dataChanged(cellData.getTableColumn().getTable(), rowData, cellData, newValue);
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
			int cols = tbData.getColumnCount();
			java.util.Vector<Object> newData = new java.util.Vector<>();

			for (int i = 0; i < cols; i++) {
				TableData tempData = new TableData();
				tempData.setNewRow(true);
				tempData.setData(new String());
				tempData.setTableColumn((nl.errorsoft.esql.table.TableColumn) tbData.getColumnModel().getColumn(i).getHeaderValue());

				newData.insertElementAt(tempData, i);
			}

			stm.addRow(newData);
			tbData.setRowSelectionInterval(stm.getRowCount() - 1, stm.getRowCount() - 1);

			SwingUtilities.invokeLater(() -> jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum()));
		}
	}

	public void saveSelectedRow() {
		int ia = tbData.getSelectedRow();

		if (tbData.getSelectedRow() < 0) {
			return;
		}

		if (tbData.isEditing()) {
			tbData.getCellEditor().stopCellEditing();
		}

		int cols = tbData.getColumnCount();
		TableData[] tda = new TableData[cols];

		for (int j = 0; j < cols; j++) {
			tda[j] = (TableData) stm.getValueAt(ia, j);
		}
		try {
			if (tda[0].isNewRow()) {
				tcc.insertRow(table, tda);
				refreshData();
			}
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Save selected row", e);
		}
	}

	public void deleteSelectedRows() {
		this.disableCellDataEditor();
		int[] ia = tbData.getSelectedRows();

		if (ia == null || ia.length == 0) {
			return;
		}

		String from = "'" + table.getDatabase().getName() + "." + table.getName() + "'";

		if (Dialogs.confirmDestructive(this, "Delete rows", "Delete " + ia.length + " row(s) from " + from + "? This cannot be undone.", "Delete")) {
			int cols = tbData.getColumnCount();
			TableData[] tda = new TableData[cols];

			for (int i = ia.length - 1; i >= 0; i--) {
				for (int j = 0; j < cols; j++) {
					tda[j] = (TableData) stm.getValueAt(ia[i], j);
				}
				try {
					if (!tda[0].isNewRow()) {
						tcc.deleteRow(table, tda);
					} else {
						inserting = false;
					}
					stm.removeRow(ia[i]);
					showRecordCount();
				} catch (Exception e) {
					ApplicationContext.get().errors().report(this, "Delete row", e);

					if (ia.length > 1) {
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
		lblRows.setText(" Total:  " + table.getRowCount());
	}

	public void loadData(Table table, nl.errorsoft.esql.table.TableColumn[] columns, TableData[][] tda) {
		this.table = table;
		showRecordCount();

		stm = new SortableTableModel();
		tcm = new DefaultTableColumnModel();
		HeaderRenderer hr = new HeaderRenderer(ApplicationContext.get().imageLoader());

		for (int i = 0; i < columns.length; i++) {
			javax.swing.table.TableColumn tempCol = new javax.swing.table.TableColumn(i);
			tempCol.setHeaderValue(columns[i]);
			tempCol.setHeaderRenderer(hr);
			tcm.addColumn(tempCol);
		}

		tbData.getTableHeader().addMouseListener(new HeaderListener(tbData.getTableHeader(), hr));
		stm.setDataVector(tda, columns);
		tbData.setColumnModel(tcm);
		tbData.setModel(stm);
		ColumnWidths.fitToContent(tbData);
		jsp.getViewport().revalidate();
	}

	/** The paging buttons with skip, show and total. The window shows them in its own status bar, next to what the connection did last. */
	public JComponent getNavigationBar() {
		return navigationBar;
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == btnUpdateRowData) {
			TableData[] rowData = new TableData[tbData.getColumnCount()];

			for (int i = 0; i < tbData.getColumnCount(); i++) {
				rowData[i] = (TableData) tbData.getValueAt(this.editingRow, i);
			}

			this.dataChanged(rowData, this.editingCell, this.cellData.getText());
			editingCell.setData(this.cellData.getText());
			stm.fireTableDataChanged();
		} else if (src == btnCloseCellData) {
			this.disableCellDataEditor();
		} else if (src == btnSaveCellData) {
			JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(new ExtentionFileFilter("text file", new String[]{".txt"}));
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setDialogTitle("Save cell data...");

			try {
				if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					java.nio.file.Files.writeString(java.nio.file.Path.of(chooser.getSelectedFile().getAbsolutePath() + ".txt"),
						cellData.getText() + System.lineSeparator());
				}
			} catch (Exception err) {
				ApplicationContext.get().errors().report(this, "Save cell data", err);
			}
		} else if (src == btnSaveData) {
			int row = tbData.getSelectedRow();
			TableData[] rowData = new TableData[tbData.getColumnCount()];

			for (int i = 0; i < tbData.getColumnCount(); i++) {
				rowData[i] = (TableData) tbData.getValueAt(row, i);
			}

			tcc.dispatchDownloadFileUI(this.table, rowData, (TableData) tbData.getValueAt(row, tbData.getSelectedColumn()));
		} else if (src == btnAddData) {
			int row = tbData.getSelectedRow();
			TableData[] rowData = new TableData[tbData.getColumnCount()];

			for (int i = 0; i < tbData.getColumnCount(); i++) {
				rowData[i] = (TableData) tbData.getValueAt(row, i);
			}

			tcc.dispatchUploadFileUI(this.table, rowData, (TableData) tbData.getValueAt(row, tbData.getSelectedColumn()));
		} else {
			try {
				skip = Integer.parseInt(this.jtfSkip.getText());
				show = Integer.parseInt(this.jtfShow.getText());
			} catch (Exception ex) {
				Dialogs.error(this, "Show data", "Enter a number in Skip and Show.");
				return;
			}

			try {
				int rows = table.getRowCount();

				if (src == btnFirst) {
					skip = 0;
				} else if (src == btnPrev) {
					skip = skip - show;

					if (skip < 0) {
						skip = 0;
					}
				} else if (src == btnRun) {
					if (skip + show > rows) {
						skip = rows - show;
					}
					if (rows - show < 0) {
						skip = 0;
					}
				} else if (src == btnNext) {
					skip = skip + show;

					if (skip >= rows) {
						skip = skip - show;
					}
				} else if (src == btnLast) {
					skip = rows - show;

					if (rows < show) {
						skip = 0;
					}
				}

				jtfSkip.setText(Integer.toString(skip));
				jtfShow.setText(Integer.toString(show));

				tcc.showTableData(table, skip, show);
				inserting = false;
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Show data", ex);
			}
		}
	}

	public void refreshData() {
		int rows = table.getRowCount();

		try {
			skip = Integer.parseInt(this.jtfSkip.getText());
			show = Integer.parseInt(this.jtfShow.getText());
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
			tcc.showTableData(table, skip, show);

		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Refresh data", ex);
		}
	}
}

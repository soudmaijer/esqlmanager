package nl.errorsoft.esql.table.ui;

import nl.errorsoft.esql.ui.util.MouseClicks;

import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.control.IndexesController;
import nl.errorsoft.esql.ui.component.EditorTab;
import nl.errorsoft.esql.ui.util.Forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/**
 * The indexes of one table, as a work window with a tab of its own. The list on the left holds all indexes with their type, the editor on the right the type
 * and the columns of the selected one. Save writes the selected index, Drop removes it; the tab stays open and shows the indexes as they are after the
 * change. Close closes it, asking first when the selected index has changes that are not saved.
 */
public class IndexesTab extends JPanel implements EditorTab {
	private final IndexesController indexesController;
	private final String title;

	private final JList<TableIndex> indexList = new JList<>(new DefaultListModel<>());
	private final JButton addButton = Forms.button("&Add...");
	private final JButton primaryButton = Forms.button("Add &primary");
	private final JList<TableColumn> usedColumnList = new JList<>(new DefaultListModel<>());
	private final JList<TableColumn> availableColumnList = new JList<>(new DefaultListModel<>());
	private final JButton addToListButton = new JButton("<");
	private final JButton removeFromListButton = new JButton(">");
	private final JRadioButton normalRadio = Forms.mnemonic(new JRadioButton(), "&Normal");
	private final JRadioButton uniqueRadio = Forms.mnemonic(new JRadioButton(), "&Unique");
	private final JRadioButton fulltextRadio = Forms.mnemonic(new JRadioButton(), "&Fulltext");
	private final JButton upButton = Forms.button("U&p");
	private final JButton downButton = Forms.button("&Down");
	private final JButton saveButton = Forms.button("&Save");
	private final JButton dropButton = Forms.button("D&rop");
	private final JButton closeButton = Forms.button("&Close");
	/** Whether the table has no primary key yet, so one can be added. */
	private boolean primaryAllowed = true;
	/** Set while a change of the indexes runs in the background. */
	private boolean busy;
	private final JPanel editor = Forms.titled(new JPanel(new BorderLayout(0, Forms.GAP)), "Index");
	private final JLabel hint = new JLabel(" ");
	private boolean hintIsError;

	// Columns, type or a new index changed since the indexes were loaded.
	private boolean modified;
	// The index whose columns are shown, and whether the list is being filled by the code (not chosen by the user).
	private TableIndex shown;
	private boolean loading;

	/** @param indexTypes the kinds of index the server offers (INDEX, UNIQUE, FULLTEXT), a kind that is not in it has no radio button. */
	public IndexesTab(IndexesController indexesController, String title, List<String> indexTypes) {
		super(new BorderLayout(Forms.PADDING, 0));
		this.indexesController = indexesController;
		this.title = title;
		setBorder(BorderFactory.createEmptyBorder(Forms.PADDING, Forms.PADDING, Forms.PADDING, Forms.PADDING));

		add(listPanel(), BorderLayout.WEST);
		add(editorPanel(indexTypes), BorderLayout.CENTER);
		add(bottom(), BorderLayout.SOUTH);

		addButton.addActionListener(e -> addIndex());
		primaryButton.addActionListener(e -> {
			if (!modified || confirmDiscard()) {
				indexesController.addPrimary();
			}
		});
		addToListButton.addActionListener(e -> moveSelected(availableColumnList, usedColumnList));
		removeFromListButton.addActionListener(e -> moveSelected(usedColumnList, availableColumnList));
		upButton.addActionListener(e -> moveUsed(-1));
		downButton.addActionListener(e -> moveUsed(1));
		saveButton.addActionListener(e -> save());
		dropButton.addActionListener(e -> dropIndex());
		closeButton.addActionListener(e -> indexesController.close());
		for (JRadioButton radio : List.of(normalRadio, uniqueRadio, fulltextRadio)) {
			radio.addActionListener(e -> modified = true);
		}
		indexList.addListSelectionListener(e -> {
			if (e.getValueIsAdjusting() || loading || !(indexList.getSelectedValue() instanceof TableIndex chosen) || chosen == shown) {
				return;
			}
			if (modified && !confirmDiscard()) {
				loading = true;
				indexList.setSelectedValue(shown, true);
				loading = false;
				return;
			}
			modified = false;
			itemSelected(chosen);
		});
		showNone();
	}

	private JPanel listPanel() {
		indexList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		indexList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
				TableIndex item = (TableIndex) value;
				return super.getListCellRendererComponent(list, item + "  (" + kind(item) + ")", index, selected, focus);
			}
		});
		JScrollPane scroll = new JScrollPane(indexList);
		scroll.setPreferredSize(new Dimension(220, 240));
		JPanel buttons = new JPanel(new GridLayout(1, 2, Forms.GAP, 0));
		buttons.setBorder(BorderFactory.createEmptyBorder(Forms.GAP, 0, 0, 0));
		buttons.add(addButton);
		buttons.add(primaryButton);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(Forms.label("&Indexes:", indexList), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel editorPanel(List<String> indexTypes) {
		ButtonGroup group = new ButtonGroup();
		group.add(normalRadio);
		group.add(uniqueRadio);
		group.add(fulltextRadio);
		JPanel type = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.PADDING, 0));
		type.add(new JLabel("Type:"));
		type.add(normalRadio);
		if (indexTypes.contains("UNIQUE")) {
			type.add(uniqueRadio);
		}
		if (indexTypes.contains("FULLTEXT")) {
			type.add(fulltextRadio);
		}

		JPanel columns = new JPanel(new GridBagLayout());
		columns.add(Forms.label("U&sed columns:", usedColumnList), constraints(0, 0));
		columns.add(Forms.label("Availa&ble columns:", availableColumnList), constraints(2, 0));
		GridBagConstraints c = constraints(0, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(usedColumnList), c);
		JPanel move = new JPanel(new GridLayout(2, 1, 0, Forms.GAP));
		move.add(addToListButton);
		move.add(removeFromListButton);
		columns.add(move, constraints(1, 1));
		c = constraints(2, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(availableColumnList), c);
		JPanel order = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		order.add(upButton);
		order.add(downButton);
		c = constraints(0, 2);
		c.fill = GridBagConstraints.HORIZONTAL;
		columns.add(order, c);

		addToListButton.setToolTipText("Use the selected available column in the index");
		removeFromListButton.setToolTipText("Remove the selected column from the index");
		upButton.setToolTipText("Move the selected column up: the order of the columns matters for the index");
		downButton.setToolTipText("Move the selected column down");
		availableColumnList.addMouseListener(doubleClick(availableColumnList, usedColumnList));
		usedColumnList.addMouseListener(doubleClick(usedColumnList, availableColumnList));

		colourHint();
		editor.add(type, BorderLayout.NORTH);
		editor.add(columns, BorderLayout.CENTER);
		editor.add(hint, BorderLayout.SOUTH);
		return editor;
	}

	private JPanel bottom() {
		return Forms.buttonRow(saveButton, dropButton, closeButton);
	}

	private static GridBagConstraints constraints(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(Forms.GAP / 2, Forms.GAP / 2, Forms.GAP / 2, Forms.GAP / 2);
		return c;
	}

	private MouseAdapter doubleClick(JList<TableColumn> from, JList<TableColumn> to) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (MouseClicks.isDoubleClick(e) && from.locationToIndex(e.getPoint()) > -1) {
					moveSelected(from, to);
				}
			}
		};
	}

	/** The kind of an index as the list shows it. */
	private static String kind(TableIndex index) {
		if (index.isPrimary()) {
			return "primary key";
		}
		String kind = index.isUnique() ? "unique" : index.isFulltext() ? "fulltext" : "normal";
		return index.isNew() ? "new, " + kind : kind;
	}

	private boolean confirmDiscard() {
		return Dialogs.confirmDestructive(this, "Discard changes?", "Discard the unsaved changes to index '" + shown + "'?", "Discard");
	}

	@Override
	public boolean confirmClose() {
		return !modified || Dialogs.confirmDestructive(this, "Discard changes?", "Discard the changes in " + title + "?", "Discard");
	}

	private void addIndex() {
		if (modified && !confirmDiscard()) {
			return;
		}
		String input = Dialogs.input(this, "New index", "&Name:", "Create");

		if (input != null) {
			indexesController.addNew(input);
		}
	}

	private void dropIndex() {
		if (indexList.getSelectedValue() instanceof TableIndex index
			&& Dialogs.confirmDestructive(this, "Drop index", "Drop index '" + index + "' of " + title + "? This cannot be undone.", "Drop")) {
			indexesController.dropIndex(index);
		}
	}

	private void save() {
		if (indexList.getSelectedValue() instanceof TableIndex index) {
			DefaultListModel<TableColumn> usedModel = (DefaultListModel<TableColumn>) usedColumnList.getModel();
			if (usedModel.isEmpty()) {
				showHint("Select at least one column for the index.", true);
				return;
			}
			TableColumn[] columns = new TableColumn[usedModel.getSize()];

			for (int i = 0; i < usedModel.getSize(); i++) {
				columns[i] = usedModel.elementAt(i);
			}

			String type = "INDEX";

			if (fulltextRadio.isSelected()) {
				type = "FULLTEXT";
			} else if (uniqueRadio.isSelected()) {
				type = "UNIQUE";
			}

			indexesController.modifyIndex(index, columns, type);
		}
	}

	private void showHint(String text, boolean error) {
		hintIsError = error;
		hint.setText(text);
		colourHint();
	}

	/** The hint is dimmed, or the error colour of the theme; taken again when the look and feel changes. */
	private void colourHint() {
		Color colour = hintIsError ? Forms.errorColor() : UIManager.getColor("Label.disabledForeground");
		if (colour != null) {
			hint.setForeground(colour);
		}
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// Called by the JPanel constructor before the fields exist.
		if (hint != null) {
			colourHint();
		}
	}

	private void moveUsed(int step) {
		DefaultListModel<TableColumn> used = (DefaultListModel<TableColumn>) usedColumnList.getModel();
		int from = usedColumnList.getSelectedIndex();
		int to = from + step;

		if (from > -1 && to > -1 && to < used.getSize()) {
			used.add(to, used.remove(from));
			usedColumnList.setSelectedIndex(to);
			modified = true;
		}
	}

	private void moveSelected(JList<TableColumn> from, JList<TableColumn> to) {
		if (from.getSelectedValue() instanceof TableColumn column) {
			((DefaultListModel<TableColumn>) from.getModel()).removeElement(column);
			((DefaultListModel<TableColumn>) to.getModel()).addElement(column);
			modified = true;
		}
	}

	/** Nothing selected: the editor and its buttons are off. */
	private void showNone() {
		shown = null;
		setTitle("Index");
		showHint(" ", false);
		for (Component component : new Component[]{normalRadio, uniqueRadio, fulltextRadio, usedColumnList, availableColumnList, addToListButton,
			removeFromListButton, upButton, downButton,
			saveButton, dropButton}) {
			component.setEnabled(false);
		}
	}

	private void setTitle(String text) {
		if (editor.getBorder() instanceof CompoundBorder compound && compound.getOutsideBorder() instanceof TitledBorder titled) {
			titled.setTitle(text);
			editor.repaint();
		}
	}

	/** Disables the buttons that change the table while a change runs, so that it is not started twice. */
	public void setBusy(boolean busy) {
		this.busy = busy;
		saveButton.setEnabled(!busy);
		dropButton.setEnabled(!busy);
		addButton.setEnabled(!busy);
		primaryButton.setEnabled(!busy && primaryAllowed);
	}

	/** Shows the indexes as they are in the database, nothing is modified any more. */
	public void loadIndexes(TableIndex[] indexes) {
		modified = false;
		loading = true;
		shown = null;
		primaryAllowed = true;
		DefaultListModel<TableIndex> model = new DefaultListModel<>();
		indexList.setModel(model);
		usedColumnList.setModel(new DefaultListModel<>());
		availableColumnList.setModel(new DefaultListModel<>());
		showNone();

		if (indexes != null && indexes.length > 0) {
			for (TableIndex index : indexes) {
				if (index.isPrimary()) {
					primaryAllowed = false;
				}
				model.addElement(index);
			}
			indexList.setSelectedIndex(0);
			itemSelected(indexes[0]);
		}
		primaryButton.setEnabled(!busy && primaryAllowed);
		loading = false;
	}

	public void addNewIndex(TableIndex index, TableColumn[] columns) {
		DefaultListModel<TableColumn> availableModel = new DefaultListModel<>();
		loading = true;
		((DefaultListModel<TableIndex>) indexList.getModel()).addElement(index);
		indexList.setSelectedValue(index, true);
		loading = false;
		itemSelected(index);

		for (TableColumn column : columns) {
			availableModel.addElement(column);
		}

		usedColumnList.setModel(new DefaultListModel<>());
		availableColumnList.setModel(availableModel);
		modified = true;
	}

	public void itemSelected(TableIndex index) {
		shown = index;
		TableColumn[] used = index.getTableColumns();
		TableColumn[] avail = index.getTable().getColumns();
		DefaultListModel<TableColumn> usedModel = new DefaultListModel<>();
		DefaultListModel<TableColumn> availableModel = new DefaultListModel<>();

		uniqueRadio.setSelected(index.isUnique());
		fulltextRadio.setSelected(!index.isUnique() && index.isFulltext());
		normalRadio.setSelected(!index.isUnique() && !index.isFulltext());
		boolean editable = !index.isPrimary();
		for (Component component : new Component[]{normalRadio, uniqueRadio, fulltextRadio}) {
			component.setEnabled(editable);
		}
		for (Component component : new Component[]{usedColumnList, availableColumnList, addToListButton, removeFromListButton, upButton, downButton, saveButton,
			dropButton}) {
			component.setEnabled(true);
		}
		setTitle("Index: " + index);
		showHint(index.isPrimary() ? "The primary key is always unique, only its columns can change." : " ", false);

		for (TableColumn column : avail) {
			availableModel.addElement(column);
		}

		if (used != null) {
			for (TableColumn column : used) {
				usedModel.addElement(column);
				availableModel.removeElement(column);
			}
		}

		usedColumnList.setModel(usedModel);
		availableColumnList.setModel(availableModel);
	}
}

package nl.errorsoft.esql.table.ui;

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
 * The indexes of one table, as a tab of the connection window. The list on the left holds all indexes with their type, the editor on the right the type
 * and the columns of the selected one. Save writes the selected index, Drop removes it; the tab stays open and shows the indexes as they are after the
 * change. Close closes it, asking first when the selected index has changes that are not saved.
 */
public class IndexesTab extends JPanel implements EditorTab {
	private final IndexesController indexesController;
	private final String title;

	private final JList<TableIndex> indexList = new JList<>(new DefaultListModel<>());
	private final JButton jbtnAdd = Forms.button("&Add...");
	private final JButton jbtnPrimary = Forms.button("Add &primary");
	private final JList<TableColumn> jlstUsed = new JList<>(new DefaultListModel<>());
	private final JList<TableColumn> jlstAvail = new JList<>(new DefaultListModel<>());
	private final JButton jbtnAddToList = new JButton("<");
	private final JButton jbtnRemoveFromList = new JButton(">");
	private final JRadioButton jrdNormal = Forms.mnemonic(new JRadioButton(), "&Normal");
	private final JRadioButton jrdUnique = Forms.mnemonic(new JRadioButton(), "&Unique");
	private final JRadioButton jrdFulltext = Forms.mnemonic(new JRadioButton(), "&Fulltext");
	private final JButton jbtnUp = Forms.button("U&p");
	private final JButton jbtnDown = Forms.button("&Down");
	private final JButton jbtnSave = Forms.button("&Save");
	private final JButton jbtnDrop = Forms.button("D&rop");
	private final JButton jbtnClose = Forms.button("&Close");
	private final JPanel editor = Forms.titled(new JPanel(new BorderLayout(0, Forms.GAP)), "Index");
	private final JLabel hint = new JLabel(" ");

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

		jbtnAdd.addActionListener(e -> addIndex());
		jbtnPrimary.addActionListener(e -> {
			if (!modified || confirmDiscard()) {
				indexesController.addPrimary();
			}
		});
		jbtnAddToList.addActionListener(e -> moveSelected(jlstAvail, jlstUsed));
		jbtnRemoveFromList.addActionListener(e -> moveSelected(jlstUsed, jlstAvail));
		jbtnUp.addActionListener(e -> moveUsed(-1));
		jbtnDown.addActionListener(e -> moveUsed(1));
		jbtnSave.addActionListener(e -> save());
		jbtnDrop.addActionListener(e -> dropIndex());
		jbtnClose.addActionListener(e -> indexesController.close());
		for (JRadioButton radio : List.of(jrdNormal, jrdUnique, jrdFulltext)) {
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
		buttons.add(jbtnAdd);
		buttons.add(jbtnPrimary);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(Forms.label("&Indexes:", indexList), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel editorPanel(List<String> indexTypes) {
		ButtonGroup group = new ButtonGroup();
		group.add(jrdNormal);
		group.add(jrdUnique);
		group.add(jrdFulltext);
		JPanel type = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.PADDING, 0));
		type.add(new JLabel("Type:"));
		type.add(jrdNormal);
		if (indexTypes.contains("UNIQUE")) {
			type.add(jrdUnique);
		}
		if (indexTypes.contains("FULLTEXT")) {
			type.add(jrdFulltext);
		}

		JPanel columns = new JPanel(new GridBagLayout());
		columns.add(Forms.label("U&sed columns:", jlstUsed), constraints(0, 0));
		columns.add(Forms.label("Availa&ble columns:", jlstAvail), constraints(2, 0));
		GridBagConstraints c = constraints(0, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(jlstUsed), c);
		JPanel move = new JPanel(new GridLayout(2, 1, 0, Forms.GAP));
		move.add(jbtnAddToList);
		move.add(jbtnRemoveFromList);
		columns.add(move, constraints(1, 1));
		c = constraints(2, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		columns.add(new JScrollPane(jlstAvail), c);
		JPanel order = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		order.add(jbtnUp);
		order.add(jbtnDown);
		c = constraints(0, 2);
		c.fill = GridBagConstraints.HORIZONTAL;
		columns.add(order, c);

		jbtnAddToList.setToolTipText("Use the selected available column in the index");
		jbtnRemoveFromList.setToolTipText("Remove the selected column from the index");
		jbtnUp.setToolTipText("Move the selected column up: the order of the columns matters for the index");
		jbtnDown.setToolTipText("Move the selected column down");
		jlstAvail.addMouseListener(doubleClick(jlstAvail, jlstUsed));
		jlstUsed.addMouseListener(doubleClick(jlstUsed, jlstAvail));

		Color disabled = UIManager.getColor("Label.disabledForeground");
		if (disabled != null) {
			hint.setForeground(disabled);
		}
		editor.add(type, BorderLayout.NORTH);
		editor.add(columns, BorderLayout.CENTER);
		editor.add(hint, BorderLayout.SOUTH);
		return editor;
	}

	private JPanel bottom() {
		return Forms.buttonRow(jbtnSave, jbtnDrop, jbtnClose);
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
				if (e.getClickCount() == 2 && from.locationToIndex(e.getPoint()) > -1) {
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
		if (indexList.getSelectedValue() instanceof TableIndex ti
			&& Dialogs.confirmDestructive(this, "Drop index", "Drop index '" + ti + "' of " + title + "? This cannot be undone.", "Drop")) {
			indexesController.dropIndex(ti);
		}
	}

	private void save() {
		if (indexList.getSelectedValue() instanceof TableIndex ti) {
			DefaultListModel<TableColumn> dlm = (DefaultListModel<TableColumn>) jlstUsed.getModel();
			if (dlm.isEmpty()) {
				hint.setForeground(errorColor());
				hint.setText("Select at least one column for the index.");
				return;
			}
			TableColumn[] tc = new TableColumn[dlm.getSize()];

			for (int i = 0; i < dlm.getSize(); i++) {
				tc[i] = dlm.elementAt(i);
			}

			String type = "INDEX";

			if (jrdFulltext.isSelected()) {
				type = "FULLTEXT";
			} else if (jrdUnique.isSelected()) {
				type = "UNIQUE";
			}

			indexesController.modifyIndex(ti, tc, type);
		}
	}

	private static Color errorColor() {
		return Forms.errorColor();
	}

	private void moveUsed(int step) {
		DefaultListModel<TableColumn> used = (DefaultListModel<TableColumn>) jlstUsed.getModel();
		int from = jlstUsed.getSelectedIndex();
		int to = from + step;

		if (from > -1 && to > -1 && to < used.getSize()) {
			used.add(to, used.remove(from));
			jlstUsed.setSelectedIndex(to);
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
		hint.setText(" ");
		for (Component component : new Component[]{jrdNormal, jrdUnique, jrdFulltext, jlstUsed, jlstAvail, jbtnAddToList, jbtnRemoveFromList, jbtnUp, jbtnDown,
			jbtnSave, jbtnDrop}) {
			component.setEnabled(false);
		}
	}

	private void setTitle(String text) {
		if (editor.getBorder() instanceof CompoundBorder compound && compound.getOutsideBorder() instanceof TitledBorder titled) {
			titled.setTitle(text);
			editor.repaint();
		}
	}

	/** Shows the indexes as they are in the database, nothing is modified any more. */
	public void loadIndexes(TableIndex[] tia) {
		modified = false;
		loading = true;
		shown = null;
		jbtnPrimary.setEnabled(true);
		DefaultListModel<TableIndex> model = new DefaultListModel<>();
		indexList.setModel(model);
		jlstUsed.setModel(new DefaultListModel<>());
		jlstAvail.setModel(new DefaultListModel<>());
		showNone();

		if (tia != null && tia.length > 0) {
			for (TableIndex index : tia) {
				if (index.isPrimary()) {
					jbtnPrimary.setEnabled(false);
				}
				model.addElement(index);
			}
			indexList.setSelectedIndex(0);
			itemSelected(tia[0]);
		}
		loading = false;
	}

	public void addNewIndex(TableIndex ti, TableColumn[] tc) {
		DefaultListModel<TableColumn> dlmAvail = new DefaultListModel<>();
		loading = true;
		((DefaultListModel<TableIndex>) indexList.getModel()).addElement(ti);
		indexList.setSelectedValue(ti, true);
		loading = false;
		itemSelected(ti);

		for (TableColumn column : tc) {
			dlmAvail.addElement(column);
		}

		jlstUsed.setModel(new DefaultListModel<>());
		jlstAvail.setModel(dlmAvail);
		modified = true;
	}

	public void itemSelected(TableIndex index) {
		shown = index;
		TableColumn[] used = index.getTableColumns();
		TableColumn[] avail = index.getTable().getColumns();
		DefaultListModel<TableColumn> dlmUsed = new DefaultListModel<>();
		DefaultListModel<TableColumn> dlmAvail = new DefaultListModel<>();

		jrdUnique.setSelected(index.isUnique());
		jrdFulltext.setSelected(!index.isUnique() && index.isFulltext());
		jrdNormal.setSelected(!index.isUnique() && !index.isFulltext());
		boolean editable = !index.isPrimary();
		for (Component component : new Component[]{jrdNormal, jrdUnique, jrdFulltext}) {
			component.setEnabled(editable);
		}
		for (Component component : new Component[]{jlstUsed, jlstAvail, jbtnAddToList, jbtnRemoveFromList, jbtnUp, jbtnDown, jbtnSave, jbtnDrop}) {
			component.setEnabled(true);
		}
		setTitle("Index: " + index);
		hint.setForeground(UIManager.getColor("Label.disabledForeground"));
		hint.setText(index.isPrimary() ? "The primary key is always unique, only its columns can change." : " ");

		for (TableColumn column : avail) {
			dlmAvail.addElement(column);
		}

		if (used != null) {
			for (TableColumn column : used) {
				dlmUsed.addElement(column);
				dlmAvail.removeElement(column);
			}
		}

		jlstUsed.setModel(dlmUsed);
		jlstAvail.setModel(dlmAvail);
	}
}

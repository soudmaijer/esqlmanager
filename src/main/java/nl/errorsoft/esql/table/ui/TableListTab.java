package nl.errorsoft.esql.table.ui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.TreeMenu;
import nl.errorsoft.esql.connection.TreeMenu.Item;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.icon.ImageLoader;
import nl.errorsoft.esql.ui.table.ColumnWidths;
import nl.errorsoft.esql.ui.table.SortableTableModel;
import nl.errorsoft.esql.ui.util.ToolbarButtons;

/**
 * The tables of a database or schema (name, rows, type, comment) with a toolbar: what the database or schema itself offers (add table, reload, new query)
 * and what the selected tables offer (open, edit, indexes, drop, maintenance). Which items the server has comes from {@link TreeMenu}, which also says
 * which ones fit the number of selected rows. A double click or Enter opens the data of a table.
 */
public class TableListTab extends JPanel {
	private final ConnectionWindowController connection;
	private final Database database;
	private final Schema schema;
	private final JTable table;
	private final Map<Item, JComponent> buttons = new LinkedHashMap<>();
	private final JPopupMenu maintenanceMenu = new JPopupMenu();

	/** @param schema the schema of the list on servers with schemas, null for the tables of a database */
	public TableListTab(ConnectionWindowController connection, Database database, Schema schema) {
		super(new BorderLayout());
		this.connection = connection;
		this.database = database;
		this.schema = schema;

		table = new JTable(new SortableTableModel()) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(true);
		table.setGridColor(UIManager.getColor("Table.gridColor"));
		table.getTableHeader().setReorderingAllowed(false);
		table.getSelectionModel().addListSelectionListener(e -> updateButtons());
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
					perform(Item.OPEN_TABLE);
				}
			}
		});
		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openTable");
		table.getActionMap().put("openTable", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				perform(Item.OPEN_TABLE);
			}
		});

		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(null);
		add(toolbar(), BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		updateButtons();
	}

	private JToolBar toolbar() {
		ImageLoader icons = ApplicationContext.get().imageLoader();
		Dialect dialect = connection.dialect();
		JToolBar bar = ToolbarButtons.toolbar();

		addButton(bar, Item.CREATE_TABLE, icons, "imgCreateTable", "Add table");
		addButton(bar, Item.RELOAD_TABLES, icons, "imgRun", "Reload");
		addButton(bar, Item.NEW_QUERY, icons, "imgRunQuery", "New query");
		bar.add(ToolbarButtons.separator());
		addButton(bar, Item.OPEN_TABLE, icons, "imgOpen", "Open data");
		addButton(bar, Item.EDIT_TABLE, icons, "des_properties", "Edit table");
		addButton(bar, Item.INDEXES, icons, "imgHasIndex", "Indexes");
		addButton(bar, Item.DROP_TABLE, icons, "imgDropTable", "Drop table");

		// The maintenance commands of the server in one drop down button, left out when the server has none.
		List<Item> maintenance = TreeMenu.maintenanceItems(dialect);
		if (!maintenance.isEmpty()) {
			JButton button = new JButton(icons.getIcon("imgMaintenance"));
			button.setToolTipText("Table maintenance");
			button.getAccessibleContext().setAccessibleName("Table maintenance");
			for (Item item : maintenance) {
				JMenuItem menuItem = new JMenuItem(item.label(dialect));
				menuItem.addActionListener(e -> perform(item));
				maintenanceMenu.add(menuItem);
			}
			button.addActionListener(e -> maintenanceMenu.show(button, 0, button.getHeight()));
			ToolbarButtons.style(button);
			bar.add(button);
			buttons.put(Item.OPTIMIZE, button); // Stands for every maintenance command: they need the same selection
		}
		bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
			bar.getBorder()));
		return bar;
	}

	/** A button for an item the server offers ({@link TreeMenu#supported}); an item it never offers gets no button. */
	private void addButton(JToolBar bar, Item item, ImageLoader icons, String icon, String tooltip) {
		if (!TreeMenu.supported(item, connection.dialect())) {
			return;
		}
		JButton button = new JButton(icons.getIcon(icon));
		button.setToolTipText(tooltip);
		button.getAccessibleContext().setAccessibleName(tooltip);
		button.addActionListener(e -> perform(item));
		ToolbarButtons.style(button);
		bar.add(button);
		buttons.put(item, button);
	}

	/** The buttons follow the number of selected tables: open and edit need one, drop and maintenance one or more. */
	private void updateButtons() {
		int selected = table.getSelectedRowCount();
		buttons.forEach((item, button) -> ToolbarButtons.setAvailable(button, TreeMenu.missingForTables(item, selected)));
	}

	private List<Table> selectedTables() {
		List<Table> tables = new ArrayList<>();
		for (int row : table.getSelectedRows()) {
			if (table.getValueAt(row, 0) instanceof Table t) {
				tables.add(t);
			}
		}
		return tables;
	}

	/** Runs an item through the controller of the connection, on the selected tables or on the database or schema of the list. */
	private void perform(Item item) {
		List<Table> tables = selectedTables();
		if (TreeMenu.missingForTables(item, tables.size()) != null) {
			return;
		}
		switch (item) {
			case CREATE_TABLE -> connection.showCreateTableTab(database, schema);
			case RELOAD_TABLES -> reload();
			case NEW_QUERY -> connection.startQueryTab(database, schema);
			case OPEN_TABLE -> connection.openTable(tables.getFirst());
			case EDIT_TABLE -> connection.showEditTableTab(tables.getFirst());
			case INDEXES -> connection.showIndexesTab(tables.getFirst());
			case DROP_TABLE -> drop(tables);
			default -> {
				Dialect.Maintenance command = TreeMenu.maintenanceCommand(item);
				if (command != null) {
					connection.maintainTables(command, tables);
				}
			}
		}
	}

	private void reload() {
		connection.openTableList(schema != null ? schema : database);
	}

	private void drop(List<Table> tables) {
		String what = tables.size() == 1 ? "table '" + tables.getFirst().getName() + "'" : tables.size() + " tables";
		String data = tables.size() == 1 ? "its" : "their";
		if (Dialogs.confirmDestructive(this, "Drop table", "Drop " + what + "? All " + data + " data will be lost.", "Drop")) {
			connection.dropTables(tables, this::reload);
		}
	}

	public void loadTables(List<Table> tables) {
		SortableTableModel tableModel = new SortableTableModel();
		tableModel.addColumn("Name");
		tableModel.addColumn("Rows");
		tableModel.addColumn("Type");
		tableModel.addColumn("Comment");

		for (Table t : tables) {
			tableModel.addRow(new Object[]{t, Integer.valueOf(t.getRowCount()), t.getType(), t.getComment()});
		}
		table.setModel(tableModel);
		ColumnWidths.fitToContent(table);
		updateButtons();
	}

	@Override
	public boolean requestFocusInWindow() {
		return table.requestFocusInWindow();
	}
}

package nl.errorsoft.esql.connection;

import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.Dialect.Feature;
import nl.errorsoft.esql.dialect.Dialect.Maintenance;

/**
 * The items of the context menu of a node in the database tree, only those the server of the connection supports. Without Swing, so it can be tested.
 * {@link Item#SEPARATOR} separates groups; a menu never starts or ends with one and never has two in a row.
 */
public final class TreeMenu {
	/** What kind of node was clicked. */
	public enum Node {
		/** A saved profile that is not connected ({@link ProfileNode}). */
		PROFILE,
		/** The node of an open connection ({@link ConnectionNode}): the server, and disconnecting it. */
		CONNECTION, SERVER, DATABASE, SCHEMA, TABLE, COLUMN
	}

	public enum Item {
		// Profile and connection.
		CONNECT("Connect"), EDIT_PROFILE("Edit connection..."), DISCONNECT("Disconnect"),
		// Server.
		CREATE_DATABASE("Create {database}..."), NEW_QUERY("New query"), USERS("Users..."), PROCESS_LIST("Process list"), SERVER_STATUS(
			"Show status"), SERVER_VARIABLES("Show variables"), EXPORT("Export..."), IMPORT("Import..."), RELOAD_DATABASES("Reload {database}s"),
		// Database.
		OPEN_DATABASE("Show tables"), CREATE_TABLE("Create table..."), OPEN_IN_DESIGNER("Open in designer"), DROP_DATABASE("Drop {database}..."), RELOAD_TABLES(
			"Reload tables"), CREATE_SCHEMA("Create {schema}..."), RELOAD_SCHEMAS("Reload {schema}s"),
		// Schema.
		RENAME_SCHEMA("Rename {schema}..."), DROP_SCHEMA("Drop {schema}..."),
		// Table.
		OPEN_TABLE("Open"), EDIT_TABLE("Edit table..."), INDEXES("Indexes..."), ADD_FIELD("Add field..."), EMPTY_TABLE("Empty table..."), DROP_TABLE(
			"Drop table..."), RENAME_TABLE("Rename table..."), DUPLICATE_TABLE("Duplicate table..."), PROPERTIES("Properties..."), OPTIMIZE(
				"Optimize table"), ANALYZE("Analyze table"), CHECK("Check table"), REPAIR("Repair table"), RELOAD_COLUMNS("Reload columns"),
		// Column.
		EDIT_FIELD("Edit field..."), DROP_FIELD("Drop field..."), SEPARATOR("");

		private final String label;

		Item(String label) {
			this.label = label;
		}

		/** The text of the item, in the words of the server: "Reload schemas" on PostgreSQL. */
		public String label(Dialect dialect) {
			return label.replace("{database}", dialect.databaseTerm()).replace("{schema}", dialect.schemaTerm());
		}
	}

	private TreeMenu() {
	}

	public static List<Item> itemsFor(Node node, Dialect dialect) {
		List<Item> items = new ArrayList<>();

		switch (node) {
			case PROFILE -> {
				items.add(Item.CONNECT);
				items.add(Item.EDIT_PROFILE);
			}
			case CONNECTION -> {
				addServerItems(items, dialect);
				items.add(Item.SEPARATOR);
				items.add(Item.DISCONNECT);
			}
			case SERVER -> addServerItems(items, dialect);
			case DATABASE -> {
				boolean schemas = dialect.supports(Feature.SCHEMAS);
				items.add(Item.OPEN_DATABASE);
				items.add(Item.NEW_QUERY);
				items.add(Item.SEPARATOR);
				addIf(items, schemas, Item.CREATE_SCHEMA);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.CREATE_TABLE);
				addIf(items, dialect.supports(Feature.DESIGNER), Item.OPEN_IN_DESIGNER);
				items.add(Item.SEPARATOR);
				addImportExport(items, dialect);
				addIf(items, dialect.supports(Feature.CREATE_DATABASE), Item.DROP_DATABASE);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_DATABASE), Item.PROPERTIES);
				items.add(schemas ? Item.RELOAD_SCHEMAS : Item.RELOAD_TABLES);
			}
			case SCHEMA -> {
				items.add(Item.OPEN_DATABASE);
				items.add(Item.NEW_QUERY);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.CREATE_TABLE);
				addIf(items, dialect.supports(Feature.DESIGNER), Item.OPEN_IN_DESIGNER);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.EXPORT), Item.EXPORT);
				addIf(items, dialect.supports(Feature.IMPORT), Item.IMPORT);
				addIf(items, dialect.supports(Feature.SCHEMAS), Item.RENAME_SCHEMA);
				addIf(items, dialect.supports(Feature.SCHEMAS), Item.DROP_SCHEMA);
				items.add(Item.SEPARATOR);
				items.add(Item.RELOAD_TABLES);
			}
			case TABLE -> {
				items.add(Item.OPEN_TABLE);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.EDIT_TABLE);
				addIf(items, dialect.supports(Feature.INDEXES), Item.INDEXES);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.ADD_FIELD);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.RENAME_TABLE);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.DUPLICATE_TABLE);
				items.add(Item.SEPARATOR);
				items.addAll(maintenanceItems(dialect));
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.EXPORT), Item.EXPORT);
				items.add(Item.EMPTY_TABLE);
				items.add(Item.DROP_TABLE);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.PROPERTIES);
				items.add(Item.RELOAD_COLUMNS);
			}
			case COLUMN -> {
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.ADD_FIELD);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.EDIT_FIELD);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.DROP_FIELD);
				items.add(Item.SEPARATOR);
				items.add(Item.RELOAD_COLUMNS);
			}
		}
		return tidy(items);
	}

	/**
	 * Whether the server ever offers the item, on any node. A toolbar button or menu item of an item it never offers is hidden, not disabled (the same
	 * decision as the context menus, which leave the item out).
	 */
	public static boolean supported(Item item, Dialect dialect) {
		for (Node node : Node.values()) {
			if (itemsFor(node, dialect).contains(item)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * What the selection lacks for the item to run, as the sentence for the tooltip of its disabled button ("Select a database first."); null when it can
	 * run. The toolbars, the menu bar and the context menus all ask this, so they agree.
	 *
	 * @param path the objects of the nodes from the root of the tree to the selected node, or null when nothing is selected
	 * @param dialect the server of the selected connection, for its words; may be null when no connection is selected
	 */
	public static String missing(Item item, List<?> path, Dialect dialect) {
		Object selected = path == null || path.isEmpty() ? null : path.getLast();
		String database = dialect == null ? "database" : dialect.databaseTerm();
		String schema = dialect == null ? "schema" : dialect.schemaTerm();

		if (item == Item.CONNECT || item == Item.EDIT_PROFILE) {
			return selected instanceof ProfileNode ? null : "Select a saved connection first.";
		}
		if (TreeSelection.connection(path) == null) {
			return "Select a connection first.";
		}
		return switch (item) {
			case OPEN_DATABASE, NEW_QUERY, CREATE_TABLE, OPEN_IN_DESIGNER, DROP_DATABASE, RELOAD_TABLES, CREATE_SCHEMA, RELOAD_SCHEMAS -> TreeSelection
				.database(selected) != null ? null : "Select a " + database + " first.";
			case RENAME_SCHEMA, DROP_SCHEMA -> TreeSelection.schema(selected) != null ? null : "Select a " + schema + " first.";
			case OPEN_TABLE, EDIT_TABLE, INDEXES, ADD_FIELD, EMPTY_TABLE, DROP_TABLE, RENAME_TABLE, DUPLICATE_TABLE, OPTIMIZE, ANALYZE, CHECK, REPAIR,
				RELOAD_COLUMNS -> TreeSelection.table(selected) != null ? null : "Select a table first.";
			case PROPERTIES -> TreeSelection.table(selected) != null || selected instanceof nl.errorsoft.esql.database.Database
				? null
				: "Select a table or " + database + " first.";
			case EDIT_FIELD, DROP_FIELD -> TreeSelection.column(selected) != null ? null : "Select a column first.";
			default -> null;
		};
	}

	/**
	 * What the selected rows of a list of tables lack for the item to run, null when it can run: opening and editing work on one table, dropping and
	 * maintenance on one or more, the items of the database or schema itself (create table, reload, new query) always run.
	 */
	public static String missingForTables(Item item, int selectedTables) {
		return switch (item) {
			case OPEN_TABLE, EDIT_TABLE, INDEXES -> selectedTables == 1 ? null : selectedTables == 0 ? "Select a table first." : "Select one table.";
			case DROP_TABLE, OPTIMIZE, ANALYZE, CHECK, REPAIR -> selectedTables > 0 ? null : "Select a table first.";
			default -> null;
		};
	}

	/** The table maintenance items the server offers, in menu order. */
	public static List<Item> maintenanceItems(Dialect dialect) {
		List<Item> items = new ArrayList<>();
		for (Maintenance command : Maintenance.values()) {
			addIf(items, dialect.maintenanceCommands().contains(command), maintenanceItem(command));
		}
		return items;
	}

	/** The maintenance command of an item, null for other items. */
	public static Maintenance maintenanceCommand(Item item) {
		for (Maintenance command : Maintenance.values()) {
			if (maintenanceItem(command) == item) {
				return command;
			}
		}
		return null;
	}

	/** What the server itself offers: on the server node of a dialog's tree and on the node of a connection. */
	private static void addServerItems(List<Item> items, Dialect dialect) {
		addIf(items, dialect.supports(Feature.CREATE_DATABASE), Item.CREATE_DATABASE);
		items.add(Item.SEPARATOR);
		addIf(items, dialect.supports(Feature.USER_MANAGER), Item.USERS);
		addIf(items, dialect.supports(Feature.PROCESS_LIST), Item.PROCESS_LIST);
		addIf(items, dialect.supports(Feature.SERVER_STATUS), Item.SERVER_STATUS);
		addIf(items, dialect.supports(Feature.SERVER_STATUS), Item.SERVER_VARIABLES);
		items.add(Item.SEPARATOR);
		addImportExport(items, dialect);
		items.add(Item.RELOAD_DATABASES);
	}

	private static void addImportExport(List<Item> items, Dialect dialect) {
		addIf(items, dialect.supports(Feature.EXPORT), Item.EXPORT);
		addIf(items, dialect.supports(Feature.IMPORT), Item.IMPORT);
		items.add(Item.SEPARATOR);
	}

	private static Item maintenanceItem(Maintenance command) {
		return switch (command) {
			case OPTIMIZE -> Item.OPTIMIZE;
			case ANALYZE -> Item.ANALYZE;
			case CHECK -> Item.CHECK;
			case REPAIR -> Item.REPAIR;
		};
	}

	private static void addIf(List<Item> items, boolean condition, Item item) {
		if (condition) {
			items.add(item);
		}
	}

	/** Drops separators at the start, at the end and next to each other, left behind by groups without supported items. */
	private static List<Item> tidy(List<Item> items) {
		List<Item> tidy = new ArrayList<>();

		for (Item item : items) {
			boolean separator = item == Item.SEPARATOR;

			if (separator && (tidy.isEmpty() || tidy.getLast() == Item.SEPARATOR)) {
				continue;
			}
			tidy.add(item);
		}
		if (!tidy.isEmpty() && tidy.getLast() == Item.SEPARATOR) {
			tidy.removeLast();
		}
		return tidy;
	}
}

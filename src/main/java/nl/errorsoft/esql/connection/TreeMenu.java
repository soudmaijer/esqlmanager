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
		SERVER, DATABASE, TABLE, COLUMN
	}

	public enum Item {
		// Server.
		CREATE_DATABASE("Create database..."), NEW_QUERY("New query"), USERS("Users..."), PROCESS_LIST("Process list"), SERVER_STATUS(
			"Show status"), SERVER_VARIABLES("Show variables"), EXPORT("Export..."), IMPORT("Import..."), RELOAD_DATABASES("Reload databases"),
		// Database.
		OPEN_DATABASE("Open"), CREATE_TABLE("Create table..."), OPEN_IN_DESIGNER("Open in designer"), DROP_DATABASE("Drop database..."), RELOAD_TABLES(
			"Reload tables"),
		// Table.
		OPEN_TABLE("Open"), EDIT_TABLE("Edit table..."), INDEXES("Indexes..."), ADD_FIELD("Add field..."), EMPTY_TABLE("Empty table..."), DROP_TABLE(
			"Drop table..."), OPTIMIZE(
				"Optimize table"), ANALYZE("Analyze table"), CHECK("Check table"), REPAIR("Repair table"), RELOAD_COLUMNS("Reload columns"),
		// Column.
		EDIT_FIELD("Edit field..."), DROP_FIELD("Drop field..."), SEPARATOR("");

		private final String label;

		Item(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	private TreeMenu() {
	}

	public static List<Item> itemsFor(Node node, Dialect dialect) {
		List<Item> items = new ArrayList<>();

		switch (node) {
			case SERVER -> {
				addIf(items, dialect.supports(Feature.CREATE_DATABASE), Item.CREATE_DATABASE);
				items.add(Item.NEW_QUERY);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.USER_MANAGER), Item.USERS);
				addIf(items, dialect.supports(Feature.PROCESS_LIST), Item.PROCESS_LIST);
				addIf(items, dialect.supports(Feature.SERVER_STATUS), Item.SERVER_STATUS);
				addIf(items, dialect.supports(Feature.SERVER_STATUS), Item.SERVER_VARIABLES);
				items.add(Item.SEPARATOR);
				addImportExport(items, dialect);
				items.add(Item.RELOAD_DATABASES);
			}
			case DATABASE -> {
				items.add(Item.OPEN_DATABASE);
				items.add(Item.NEW_QUERY);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.CREATE_TABLE);
				addIf(items, dialect.supports(Feature.DESIGNER), Item.OPEN_IN_DESIGNER);
				items.add(Item.SEPARATOR);
				addImportExport(items, dialect);
				addIf(items, dialect.supports(Feature.CREATE_DATABASE), Item.DROP_DATABASE);
				items.add(Item.SEPARATOR);
				items.add(Item.RELOAD_TABLES);
			}
			case TABLE -> {
				items.add(Item.OPEN_TABLE);
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.EDIT_TABLE);
				addIf(items, dialect.supports(Feature.INDEXES), Item.INDEXES);
				addIf(items, dialect.supports(Feature.CREATE_TABLE), Item.ADD_FIELD);
				items.add(Item.SEPARATOR);
				for (Maintenance command : Maintenance.values()) {
					addIf(items, dialect.maintenanceCommands().contains(command), maintenanceItem(command));
				}
				items.add(Item.SEPARATOR);
				addIf(items, dialect.supports(Feature.EXPORT), Item.EXPORT);
				items.add(Item.EMPTY_TABLE);
				items.add(Item.DROP_TABLE);
				items.add(Item.SEPARATOR);
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

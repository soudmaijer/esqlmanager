package nl.errorsoft.esql.connection;

import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

/**
 * What an action finds in the node selected in the tree of the connection window. Every method accepts null (nothing selected yet, or the tree has
 * just been loaded) and then returns null, so that the caller can ask the user to select something first.
 */
public final class TreeSelection {
	private TreeSelection() {
	}

	/** The selected database, or the database of the selected schema, table or column. */
	public static Database database(Object selected) {
		return switch (selected) {
			case Database database -> database;
			case Schema schema -> schema.getDatabase();
			case Table table -> table.getDatabase();
			case TableColumn column -> column.getTable().getDatabase();
			case null, default -> null;
		};
	}

	/** The selected schema, or the schema of the selected table or column. */
	public static Schema schema(Object selected) {
		if (selected instanceof Schema schema) {
			return schema;
		}
		Table table = table(selected);
		return table != null ? table.getSchema() : null;
	}

	/** The selected table, or the table of the selected column. */
	public static Table table(Object selected) {
		return switch (selected) {
			case Table table -> table;
			case TableColumn column -> column.getTable();
			case null, default -> null;
		};
	}

	/** The selected column. */
	public static TableColumn column(Object selected) {
		return selected instanceof TableColumn column ? column : null;
	}

	/**
	 * The connection a node belongs to: the {@link ConnectionNode} nearest to the selected node on its path.
	 *
	 * @param path the objects of the nodes from the root of the tree to the selected node (the user objects of a {@code TreePath}), or null
	 * @return null when nothing is selected or the node is not part of a connection (a saved profile, the hidden root)
	 */
	public static ConnectionNode connection(List<?> path) {
		if (path == null) {
			return null;
		}
		for (Object node : path.reversed()) {
			if (node instanceof ConnectionNode connection) {
				return connection;
			}
		}
		return null;
	}
}

package nl.errorsoft.esql.designer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.ui.diagram.Field;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.dialect.Dialect;

/**
 * A foreign key drawn in the designer: the columns of {@code from} refer to the columns of {@code to}, pair by pair.
 * The actions are NO ACTION, CASCADE, SET NULL, RESTRICT or SET DEFAULT, empty for the server default.
 */
public record ForeignKey(TableObject from, List<String> fromColumns, TableObject to, List<String> toColumns, String name, String onDelete, String onUpdate) {
	public ForeignKey {
		fromColumns = List.copyOf(fromColumns);
		toColumns = List.copyOf(toColumns);
	}

	/** The name a new key gets: fk_&lt;table&gt;_&lt;first column&gt;. */
	public static String defaultName(TableObject from, String column) {
		return "fk_" + from.getName() + "_" + column;
	}

	/**
	 * Checks that the key can be created: a name, at least one column pair, columns that exist, types that fit together and allowed actions.
	 * @throws EsqlException describing the first problem.
	 */
	public void validate() {
		if (name == null || name.isBlank()) {
			throw new EsqlException("A foreign key needs a name.");
		}
		if (to == null) {
			throw new EsqlException("Choose the table the foreign key refers to.");
		}
		if (fromColumns.isEmpty() || fromColumns.size() != toColumns.size()) {
			throw new EsqlException("A foreign key needs at least one pair of columns.");
		}
		for (int i = 0; i < fromColumns.size(); i++) {
			Field column = field(from, fromColumns.get(i));
			Field referenced = field(to, toColumns.get(i));
			if (!compatible(column, referenced)) {
				throw new EsqlException("Column " + from.getName() + "." + column.getName() + " (" + column.getType() + ") can't refer to " + to.getName() + "."
					+ referenced.getName() + " (" + referenced.getType() + "), the types differ.");
			}
		}
		for (String action : List.of(onDelete, onUpdate)) {
			if (action != null && !action.isBlank() && !Dialect.REFERENTIAL_ACTIONS.contains(action)) {
				throw new EsqlException("'" + action + "' is not a foreign key action, use one of " + String.join(", ", Dialect.REFERENTIAL_ACTIONS) + ".");
			}
		}
	}

	private static Field field(TableObject table, String column) {
		Field field = column == null ? null : table.getField(column);
		if (field == null) {
			throw new EsqlException("Table " + table.getName() + " has no column '" + (column == null ? "" : column) + "'.");
		}
		return field;
	}

	/** The same type, or two types of the same kind (whole numbers, text, decimals): an int may refer to a bigint, a varchar to a char. */
	static boolean compatible(Field a, Field b) {
		String typeA = a.getType() == null ? "" : a.getType().getName().toUpperCase();
		String typeB = b.getType() == null ? "" : b.getType().getName().toUpperCase();
		return typeA.equals(typeB) || (!kind(typeA).isEmpty() && kind(typeA).equals(kind(typeB)));
	}

	private static String kind(String type) {
		if (type.contains("INT") || type.contains("SERIAL")) {
			return "integer";
		}
		if (type.contains("CHAR") || type.contains("TEXT")) {
			return "text";
		}
		if (type.contains("NUMERIC") || type.contains("DECIMAL")) {
			return "decimal";
		}
		return "";
	}

	boolean involves(TableObject table) {
		return from == table || to == table;
	}

	/** This key with the columns of the given table renamed (old name to new name), all at once so that swapped names stay right. */
	ForeignKey withColumnsRenamed(TableObject table, Map<String, String> renames) {
		return new ForeignKey(from, from == table ? replace(fromColumns, renames) : fromColumns, to, to == table ? replace(toColumns, renames) : toColumns,
			name, onDelete, onUpdate);
	}

	boolean usesColumn(TableObject table, String column) {
		return (from == table && fromColumns.contains(column)) || (to == table && toColumns.contains(column));
	}

	private static List<String> replace(List<String> columns, Map<String, String> renames) {
		List<String> renamed = new ArrayList<>();

		for (String column : columns) {
			renamed.add(renames.getOrDefault(column, column));
		}
		return renamed;
	}
}

package nl.errorsoft.esql.designer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.ui.TableObject;

/**
 * A foreign key drawn in the designer: the columns of {@code from} refer to the columns of {@code to}, pair by pair.
 * The actions are NO ACTION, CASCADE, SET NULL, RESTRICT or SET DEFAULT, empty for the server default.
 */
public record ForeignKey(TableObject from, List<String> fromColumns, TableObject to, List<String> toColumns, String name, String onDelete, String onUpdate) {
	public ForeignKey {
		fromColumns = List.copyOf(fromColumns);
		toColumns = List.copyOf(toColumns);
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

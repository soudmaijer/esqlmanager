package nl.errorsoft.esql.designer;

import java.util.List;

/** A foreign key of a designed table on another table of the same designed database. */
public record DesignedForeignKey(String name, List<String> columns, String referencedTable, List<String> referencedColumns, String onDelete,
	String onUpdate) {
}

package nl.errorsoft.esql.designer;

import java.util.List;

/** Input of the generation (not the canvas model, that is {@code designer.model.ModelForeignKey}): a foreign key of a designed table on another table of the same designed database. */
public record DesignedForeignKey(String name, List<String> columns, String referencedTable, List<String> referencedColumns, String onDelete,
	String onUpdate) {
}

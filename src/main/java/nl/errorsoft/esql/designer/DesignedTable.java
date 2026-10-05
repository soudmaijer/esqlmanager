package nl.errorsoft.esql.designer;

import java.util.List;

import nl.errorsoft.esql.table.ColumnDefinition;

/** Input of the generation: a table as drawn in the designer, ready to be created. */
public record DesignedTable(String name, String type, String comment, List<ColumnDefinition> columns, List<DesignedForeignKey> foreignKeys) {
}

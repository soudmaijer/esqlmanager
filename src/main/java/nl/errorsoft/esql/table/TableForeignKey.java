package nl.errorsoft.esql.table;

import java.util.List;

/**
 * A foreign key from columns of a table to columns of another table in the same database.
 * The actions are NO ACTION, CASCADE, SET NULL, RESTRICT or SET DEFAULT, empty for the server default.
 */
public record TableForeignKey(String name, List<String> columns, String referencedTable, List<String> referencedColumns, String onDelete, String onUpdate) {
}

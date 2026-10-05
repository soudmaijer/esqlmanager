package nl.errorsoft.esql.designer;

import java.util.List;

/** Input of the generation: a database as drawn in the designer, with its tables. The canvas objects are turned into these plain records (Designed*) before the service creates anything. */
public record DesignedDatabase(String name, List<DesignedTable> tables) {
}

package nl.errorsoft.esql.designer;

import java.util.List;

/**
 * A snapshot of the designer model for the check and the generation, taken on the event thread: the databases with their tables, and the tables that
 * are not linked to any database (those cannot be generated).
 */
public record DesignedModel(List<DesignedDatabase> databases, List<DesignedTable> unlinkedTables) {
	public DesignedModel {
		databases = List.copyOf(databases);
		unlinkedTables = List.copyOf(unlinkedTables);
	}

	/** How many steps the generation reports: one per database, table and column. */
	public int generationSteps() {
		int steps = 0;
		for (DesignedDatabase database : databases) {
			steps++;
			for (DesignedTable table : database.tables()) {
				steps += 1 + table.columns().size();
			}
		}
		return steps;
	}
}

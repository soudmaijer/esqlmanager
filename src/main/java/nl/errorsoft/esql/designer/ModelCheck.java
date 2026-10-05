package nl.errorsoft.esql.designer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** The checks a designer model passes before it is generated, one step at a time so that the dialog can show each. */
public enum ModelCheck {
	DATABASES("Checking databases"), TABLES("Checking tables"), COLUMNS("Checking columns"), RELATIONS("Checking relations"), MODEL("Checking model");

	private final String label;

	ModelCheck(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** The problems this step finds, empty when it passes. */
	public List<String> problems(DesignedModel model) {
		List<String> found = new ArrayList<>();

		switch (this) {
			case DATABASES -> {
				List<String> names = model.databases().stream().map(DesignedDatabase::name).toList();
				for (String name : duplicates(names)) {
					found.add("The model has two databases named '" + name + "'.");
				}
			}
			case TABLES -> {
				for (DesignedDatabase database : model.databases()) {
					for (String name : duplicates(database.tables().stream().map(DesignedTable::name).toList())) {
						found.add("Database '" + database.name() + "' has two tables named '" + name + "'.");
					}
				}
			}
			case COLUMNS -> {
				for (DesignedTable table : allTables(model)) {
					if (table.columns().isEmpty()) {
						found.add("Table '" + table.name() + "' has no columns.");
					}
					for (String name : duplicates(table.columns().stream().map(column -> column.name).toList())) {
						found.add("Table '" + table.name() + "' has two columns named '" + name + "'.");
					}
				}
			}
			case RELATIONS -> {
				for (DesignedTable table : model.unlinkedTables()) {
					found.add("Table '" + table.name() + "' is not linked to a database.");
				}
			}
			case MODEL -> {
				if (model.databases().isEmpty()) {
					found.add("The model has no database.");
				}
			}
		}
		return found;
	}

	private static List<DesignedTable> allTables(DesignedModel model) {
		return Stream.concat(model.databases().stream().flatMap(database -> database.tables().stream()), model.unlinkedTables().stream()).toList();
	}

	/** The names that occur more than once, ignoring case, each named once in the order of its second occurrence. */
	private static List<String> duplicates(List<String> names) {
		List<String> seen = new ArrayList<>();
		List<String> twice = new ArrayList<>();

		for (String name : names) {
			if (seen.stream().anyMatch(name::equalsIgnoreCase)) {
				if (twice.stream().noneMatch(name::equalsIgnoreCase)) {
					twice.add(name);
				}
			} else {
				seen.add(name);
			}
		}
		return twice;
	}
}

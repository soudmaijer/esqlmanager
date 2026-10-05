package nl.errorsoft.esql.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The databases and schemas a profile shows. Nothing selected shows everything. A selected database without selected schemas shows all its schemas
 * (an empty set), otherwise only the named ones. The order is the order of ticking, the first database is the one connected to. Database names are
 * compared without regard to case (as the old filter did), schema names exactly. Database names cannot contain a comma because the profile stores them
 * as a comma separated list.
 */
public record DatabaseSelection(Map<String, Set<String>> schemas) {
	public static final DatabaseSelection NONE = new DatabaseSelection(Map.of());

	public DatabaseSelection {
		Map<String, Set<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Set<String>> entry : schemas.entrySet()) {
			if (!isStorable(entry.getKey())) {
				throw new IllegalArgumentException("A database name cannot be empty or contain a comma: " + entry.getKey());
			}
			copy.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
		}
		schemas = Collections.unmodifiableMap(copy);
	}

	/** Whether the name can be part of a selection: not blank and without a comma. */
	public static boolean isStorable(String databaseName) {
		return databaseName != null && !databaseName.isBlank() && databaseName.indexOf(',') < 0;
	}

	/** The selection of the names in a comma separated list, all with all their schemas. Blank entries are skipped. */
	public static DatabaseSelection parse(String csv) {
		Map<String, Set<String>> parsed = new LinkedHashMap<>();
		if (csv != null) {
			for (String name : csv.split(",")) {
				if (!name.isBlank()) {
					parsed.putIfAbsent(name.trim(), Set.of());
				}
			}
		}
		return new DatabaseSelection(parsed);
	}

	/** The selected databases in order. */
	public List<String> databases() {
		return new ArrayList<>(schemas.keySet());
	}

	public String toCsv() {
		return String.join(",", schemas.keySet());
	}

	/** True when nothing is selected, which shows everything. */
	public boolean isEmpty() {
		return schemas.isEmpty();
	}

	/** Whether the selection names any schema, so that some schemas of a selected database are hidden. */
	public boolean hasSchemaFilter() {
		return schemas.values().stream().anyMatch(names -> !names.isEmpty());
	}

	public boolean contains(String database) {
		return key(database) != null;
	}

	public boolean showsDatabase(String database) {
		return isEmpty() || contains(database);
	}

	public boolean showsSchema(String database, String schema) {
		if (isEmpty()) {
			return true;
		}
		String key = key(database);
		if (key == null) {
			return false;
		}
		Set<String> names = schemas.get(key);
		return names.isEmpty() || names.contains(schema);
	}

	/** The schemas selected in the database, empty when all are shown (or the database is not selected). */
	public Set<String> schemasOf(String database) {
		String key = key(database);
		return key == null ? Set.of() : schemas.get(key);
	}

	/** Selects the database with all its schemas, or removes it together with its schemas. */
	public DatabaseSelection withDatabase(String database, boolean selected) {
		Map<String, Set<String>> changed = new LinkedHashMap<>(schemas);
		String key = key(database);
		if (selected && key == null) {
			changed.put(database, Set.of());
		} else if (!selected && key != null) {
			changed.remove(key);
		}
		return new DatabaseSelection(changed);
	}

	/** Selects a schema, which selects its database too, or removes the schema from the selection. */
	public DatabaseSelection withSchema(String database, String schema, boolean selected) {
		Map<String, Set<String>> changed = new LinkedHashMap<>(schemas);
		String key = key(database);
		if (key == null) {
			if (!selected) {
				return this;
			}
			key = database;
		}
		Set<String> names = new LinkedHashSet<>(changed.getOrDefault(key, Set.of()));
		if (selected) {
			names.add(schema);
		} else {
			names.remove(schema);
		}
		changed.put(key, names);
		return new DatabaseSelection(changed);
	}

	/** The selection limited to databases for which the test is true, used to drop databases that no longer exist. */
	public DatabaseSelection onlyDatabases(java.util.function.Predicate<String> exists) {
		Map<String, Set<String>> kept = new LinkedHashMap<>();
		schemas.forEach((database, names) -> {
			if (exists.test(database)) {
				kept.put(database, names);
			}
		});
		return new DatabaseSelection(kept);
	}

	private String key(String database) {
		for (String name : schemas.keySet()) {
			if (name.equalsIgnoreCase(database)) {
				return name;
			}
		}
		return null;
	}
}

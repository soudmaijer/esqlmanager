package nl.errorsoft.esql.designer;

import nl.errorsoft.esql.table.TableDefinition;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.table.ColumnDefinition;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableForeignKey;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.TableService;

/** Creates the databases, tables and foreign keys of a designed model on the server, and reads an existing database back as a model. */
public class DesignerService {
	private final DatabaseService databases;
	private final TableService tables;
	private final DesignerRepository repository;

	public DesignerService(DatabaseService databases, TableService tables, DesignerRepository repository) {
		this.databases = databases;
		this.tables = tables;
		this.repository = repository;
	}

	/**
	 * Reads every table of the database (views are left out) with its columns, primary key, single column indexes and the foreign keys
	 * between the tables of the database. Generating the result again leaves the existing tables and keys alone.
	 */
	public DesignedDatabase reverseEngineer(Database database) throws Exception {
		return reverseEngineer(database, null, databases.getTables(database));
	}

	/** As {@link #reverseEngineer(Database)}, for the tables of one schema. */
	public DesignedDatabase reverseEngineer(Schema schema) throws Exception {
		return reverseEngineer(schema.getDatabase(), schema.getName(), databases.getTables(schema));
	}

	private DesignedDatabase reverseEngineer(Database database, String schema, List<Table> listed) throws Exception {
		List<String> names = repository.loadTableNames(database, schema);
		List<String> engines = repository.tableTypes();
		List<DesignedTable> designed = new ArrayList<>();

		for (Table table : listed) {
			if (!names.contains(table.getName())) {
				continue;
			}

			List<ColumnDefinition> columns = repository.loadColumns(database, schema, table.getName());
			markIndexes(table, columns);
			List<DesignedForeignKey> keys = new ArrayList<>();
			for (DesignedForeignKey key : repository.loadForeignKeys(database, schema, table.getName())) {
				// A key on a table in another schema or database can't be drawn in this model.
				if (names.contains(key.referencedTable())) {
					keys.add(key);
				}
			}

			String engine = engines.contains(table.getType()) ? table.getType() : "";
			designed.add(new DesignedTable(table.getName(), engine, table.getComment() == null ? "" : table.getComment(), columns, keys));
		}
		return new DesignedDatabase(database.getName(), designed);
	}

	/** The designer has an index and a unique flag per column, so only indexes on one column are kept; the primary key is already marked. */
	private void markIndexes(Table table, List<ColumnDefinition> columns) throws Exception {
		for (TableIndex index : tables.loadIndexes(table)) {
			if (index.isPrimary() || index.getTableColumns().length != 1) {
				continue;
			}
			for (ColumnDefinition column : columns) {
				if (column.name.equals(index.getTableColumns()[0].getName())) {
					column.unique |= index.isUnique();
					column.index |= !index.isUnique();
				}
			}
		}
	}

	/**
	 * Creates what is missing, generating a model again leaves the databases, tables and foreign keys that are already there alone.
	 * Foreign keys are added in a second pass, once every table of the database exists.
	 * The step callback is called for every database, table and column handled.
	 */
	public void generate(List<DesignedDatabase> model, Runnable step) throws Exception {
		for (DesignedDatabase designed : model) {
			Database database = new Database(designed.name());

			if (!databases.exists(database)) {
				databases.createDatabase(database.getName());
			}

			databases.use(database);
			step.run();

			for (DesignedTable table : designed.tables()) {
				step.run();

				if (!tables.exists(database, table.name())) {
					tables.createTable(new TableDefinition(database, null, table.name(), table.type(), table.comment(), table.columns()));
				}

				for (int i = 0; i < table.columns().size(); i++) {
					step.run();
				}
			}

			for (DesignedTable table : designed.tables()) {
				addForeignKeys(database, table);
			}
		}
	}

	private void addForeignKeys(Database database, DesignedTable designed) throws Exception {
		if (designed.foreignKeys().isEmpty()) {
			return;
		}

		Table table = table(database, designed.name());
		List<String> existing = tables.foreignKeyNames(table);

		for (DesignedForeignKey key : designed.foreignKeys()) {
			if (existing.stream().anyMatch(name -> name.equalsIgnoreCase(key.name()))) {
				continue;
			}

			checkColumnsExist(table, key.columns(), key.name());
			checkColumnsExist(table(database, key.referencedTable()), key.referencedColumns(), key.name());
			tables.addForeignKey(table,
				new TableForeignKey(key.name(), key.columns(), key.referencedTable(), key.referencedColumns(), key.onDelete(), key.onUpdate()));
		}
	}

	private void checkColumnsExist(Table table, List<String> columns, String keyName) throws Exception {
		if (!tables.exists(table.getDatabase(), table.getName())) {
			throw new EsqlException("Foreign key " + keyName + " refers to table " + table.getName() + ", which does not exist.");
		}

		TableColumn[] existing = tables.loadColumns(table);

		for (String column : columns) {
			boolean found = false;
			for (TableColumn candidate : existing) {
				found |= candidate.getName().equalsIgnoreCase(column);
			}

			if (!found) {
				throw new EsqlException("Foreign key " + keyName + " uses column " + column + ", which table " + table.getName() + " does not have.");
			}
		}
	}

	private Table table(Database database, String name) {
		Table table = new Table(database);
		table.setName(name);
		return table;
	}
}

package nl.errorsoft.esql.designer;

import java.util.List;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.domain.EsqlException;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableForeignKey;
import nl.errorsoft.esql.table.TableService;

/** Creates the databases, tables and foreign keys of a designed model on the server. */
public class DesignerService {
	private final DatabaseService databases;
	private final TableService tables;

	public DesignerService(DatabaseService databases, TableService tables) {
		this.databases = databases;
		this.tables = tables;
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
					tables.createTable(database, table.name(), table.columns(), table.type(), table.comment());
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

package nl.errorsoft.esql.designer.ui.diagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.model.ModelForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.table.ColumnDefinition;
import nl.errorsoft.esql.table.DataType;

/**
 * Draws a database that was read from the server as a designer model (a database card attached to a card per table, and the foreign keys), and turns
 * a model back into the plain records the check and the generation use.
 */
public final class ModelFactory {
	private ModelFactory() {
	}

	/**
	 * The model of the database, not yet arranged.
	 * @param dataTypes the types of the server, a column gets the matching one so that editing it offers the right options.
	 */
	public static Model fromDatabase(DesignedDatabase database, DataType[] dataTypes) {
		Model model = new Model(database.name());
		DatabaseCard databaseCard = model.createDatabaseCard(database.name());
		databaseCard.setHidden(false);
		Map<String, TableCard> tables = new HashMap<>();

		for (DesignedTable designed : database.tables()) {
			TableCard table = model.createTableCard(designed.name());
			table.setType(designed.type());
			table.setComment(designed.comment());
			table.setHidden(false);
			for (ColumnDefinition column : designed.columns()) {
				table.addField(field(column, dataTypes));
			}
			model.addReference(databaseCard, table);
			tables.put(designed.name(), table);
		}

		for (DesignedTable designed : database.tables()) {
			for (DesignedForeignKey key : designed.foreignKeys()) {
				model.addForeignKey(
					new ModelForeignKey(tables.get(designed.name()), key.columns(), tables.get(key.referencedTable()), key.referencedColumns(),
						key.name(), key.onDelete(), key.onUpdate()));
			}
		}
		return model;
	}

	/**
	 * A snapshot of the model as plain records, for the check and the generation; read it on the event thread, the records can then be used on any
	 * thread. A table linked to no database is listed apart.
	 */
	public static DesignedModel toDesigned(Model model) {
		List<DesignedDatabase> databases = new ArrayList<>();
		List<TableCard> linked = new ArrayList<>();

		for (ModelCard object : model.getObjects()) {
			if (object instanceof DatabaseCard database) {
				List<DesignedTable> tables = new ArrayList<>();
				for (ModelCard reference : model.getReferences(database)) {
					if (reference instanceof TableCard table) {
						linked.add(table);
						tables.add(designed(model, table));
					}
				}
				databases.add(new DesignedDatabase(database.getName(), tables));
			}
		}

		List<DesignedTable> unlinked = new ArrayList<>();
		for (ModelCard object : model.getObjects()) {
			if (object instanceof TableCard table && !linked.contains(table)) {
				unlinked.add(designed(model, table));
			}
		}
		return new DesignedModel(databases, unlinked);
	}

	private static DesignedTable designed(Model model, TableCard table) {
		List<ColumnDefinition> columns = new ArrayList<>();
		for (DesignerColumn field : table.getFields()) {
			columns.add(column(field));
		}

		// The keys the table has on other tables; the keys other tables have on it are generated with those tables.
		List<DesignedForeignKey> keys = new ArrayList<>();
		for (ModelForeignKey key : model.foreignKeysOf(table)) {
			if (key.from() == table) {
				keys.add(new DesignedForeignKey(key.name(), key.fromColumns(), key.to().getName(), key.toColumns(), key.onDelete(), key.onUpdate()));
			}
		}
		return new DesignedTable(table.getName(), table.getType(), table.getComment(), columns, keys);
	}

	private static ColumnDefinition column(DesignerColumn field) {
		ColumnDefinition column = new ColumnDefinition(field.getName());
		column.type = field.getType();
		column.length = field.getLength();
		column.defaultValue = field.getDefault();
		column.primary = field.primary;
		column.index = field.index;
		column.unique = field.unique;
		column.binary = field.binary;
		column.notNull = field.notNull;
		column.unsigned = field.unsigned;
		column.autoIncrement = field.autoIncrement;
		column.zerofill = field.zerofill;
		return column;
	}

	private static DesignerColumn field(ColumnDefinition column, DataType[] dataTypes) {
		DesignerColumn field = new DesignerColumn(column.name, dataType(column.type, dataTypes), column.length, column.defaultValue, "");
		field.primary = column.primary;
		field.index = column.index;
		field.unique = column.unique;
		field.binary = column.binary;
		field.notNull = column.notNull;
		field.unsigned = column.unsigned;
		field.autoIncrement = column.autoIncrement;
		field.zerofill = column.zerofill;
		return field;
	}

	private static DataType dataType(DataType read, DataType[] dataTypes) {
		for (DataType type : dataTypes) {
			if (type.getName().equalsIgnoreCase(read.getName())) {
				return type;
			}
		}
		return read;
	}
}

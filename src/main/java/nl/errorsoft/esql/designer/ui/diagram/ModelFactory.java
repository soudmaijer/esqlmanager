package nl.errorsoft.esql.designer.ui.diagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.model.DesignerForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.table.CreateColumn;
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
		DatabaseObject databaseObject = model.createDatabaseObject(database.name());
		databaseObject.setHidden(false);
		Map<String, TableObject> tables = new HashMap<>();

		for (DesignedTable designed : database.tables()) {
			TableObject table = model.createTableObject(designed.name());
			table.setType(designed.type());
			table.setComment(designed.comment());
			table.setHidden(false);
			for (CreateColumn column : designed.columns()) {
				table.addField(field(column, dataTypes));
			}
			model.addReference(databaseObject, table);
			tables.put(designed.name(), table);
		}

		for (DesignedTable designed : database.tables()) {
			for (DesignedForeignKey key : designed.foreignKeys()) {
				model.addForeignKey(
					new DesignerForeignKey(tables.get(designed.name()), key.columns(), tables.get(key.referencedTable()), key.referencedColumns(),
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
		List<TableObject> linked = new ArrayList<>();

		for (ModelObject object : model.getObjects()) {
			if (object instanceof DatabaseObject database) {
				List<DesignedTable> tables = new ArrayList<>();
				for (ModelObject reference : model.getReferences(database)) {
					if (reference instanceof TableObject table) {
						linked.add(table);
						tables.add(designed(model, table));
					}
				}
				databases.add(new DesignedDatabase(database.getName(), tables));
			}
		}

		List<DesignedTable> unlinked = new ArrayList<>();
		for (ModelObject object : model.getObjects()) {
			if (object instanceof TableObject table && !linked.contains(table)) {
				unlinked.add(designed(model, table));
			}
		}
		return new DesignedModel(databases, unlinked);
	}

	private static DesignedTable designed(Model model, TableObject table) {
		List<CreateColumn> columns = new ArrayList<>();
		for (DesignerColumn field : table.getFields()) {
			columns.add(column(field));
		}

		// The keys the table has on other tables; the keys other tables have on it are generated with those tables.
		List<DesignedForeignKey> keys = new ArrayList<>();
		for (DesignerForeignKey key : model.foreignKeysOf(table)) {
			if (key.from() == table) {
				keys.add(new DesignedForeignKey(key.name(), key.fromColumns(), key.to().getName(), key.toColumns(), key.onDelete(), key.onUpdate()));
			}
		}
		return new DesignedTable(table.getName(), table.getType(), table.getComment(), columns, keys);
	}

	private static CreateColumn column(DesignerColumn field) {
		CreateColumn column = new CreateColumn(field.getName());
		column.type = field.getType();
		column.length = field.getLength();
		column.defaultval = field.getDefault();
		column.primary = field.primary;
		column.index = field.index;
		column.unique = field.unique;
		column.binary = field.binary;
		column.notnull = field.notnull;
		column.unsigned = field.unsigned;
		column.autoincrement = field.autoincrement;
		column.zerofill = field.zerofill;
		return column;
	}

	private static DesignerColumn field(CreateColumn column, DataType[] dataTypes) {
		DesignerColumn field = new DesignerColumn(column.name, dataType(column.type, dataTypes), column.length, column.defaultval, "");
		field.primary = column.primary;
		field.index = column.index;
		field.unique = column.unique;
		field.binary = column.binary;
		field.notnull = column.notnull;
		field.unsigned = column.unsigned;
		field.autoincrement = column.autoincrement;
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

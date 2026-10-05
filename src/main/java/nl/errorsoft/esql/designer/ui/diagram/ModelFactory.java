package nl.errorsoft.esql.designer.ui.diagram;

import java.util.HashMap;
import java.util.Map;

import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.model.DesignerForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;

/** Draws a database that was read from the server as a designer model: a database card attached to a card per table, and the foreign keys. */
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

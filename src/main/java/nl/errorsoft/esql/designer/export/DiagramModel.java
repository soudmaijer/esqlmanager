package nl.errorsoft.esql.designer.export;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.errorsoft.esql.designer.model.DesignerForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

/** What a text diagram shows of a model: the databases, the tables with their columns and the foreign keys. Plain data, no Swing. */
public record DiagramModel(List<String> databases, List<Table> tables, List<Relation> relations) {
	public record Table(String name, List<Column> columns) {
	}

	/** @param type as the designer shows it, for example varchar(100). */
	public record Column(String name, String type, boolean primary, boolean foreign) {
	}

	/** The columns of {@code child} refer to the columns of {@code parent}. */
	public record Relation(String child, List<String> childColumns, String parent, List<String> parentColumns, String name) {
	}

	public static DiagramModel of(Model model) {
		List<String> databases = new ArrayList<>();
		List<Table> tables = new ArrayList<>();
		List<Relation> relations = new ArrayList<>();

		for (DesignerForeignKey key : model.getForeignKeys()) {
			relations.add(new Relation(key.from().getName(), key.fromColumns(), key.to().getName(), key.toColumns(), key.name()));
		}

		for (Object object : model.getObjects()) {
			if (object instanceof DatabaseObject database) {
				databases.add(database.getName());
			} else if (object instanceof TableObject table) {
				Set<String> foreign = new HashSet<>();
				for (DesignerForeignKey key : model.foreignKeysOf(table)) {
					if (key.from() == table) {
						foreign.addAll(key.fromColumns());
					}
				}

				List<Column> columns = new ArrayList<>();
				for (DesignerColumn field : table.getFields()) {
					columns.add(new Column(field.getName(), TableObject.typeText(field), field.primary, foreign.contains(field.getName())));
				}
				tables.add(new Table(table.getName(), columns));
			}
		}
		return new DiagramModel(databases, tables, relations);
	}
}

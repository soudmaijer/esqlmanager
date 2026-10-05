package nl.errorsoft.esql.designer.export;

import java.util.List;

/** What a text diagram shows of a model: the databases, the tables with their columns and the foreign keys. Plain data, no Swing; {@code Model.toDiagram()} makes one
 * from the canvas objects. */
public record DiagramModel(List<String> databases, List<Table> tables, List<Relation> relations) {
	public record Table(String name, List<Column> columns) {
	}

	/** @param type as the designer shows it, for example varchar(100). */
	public record Column(String name, String type, boolean primary, boolean foreign) {
	}

	/** The columns of {@code child} refer to the columns of {@code parent}. */
	public record Relation(String child, List<String> childColumns, String parent, List<String> parentColumns, String name) {
	}
}

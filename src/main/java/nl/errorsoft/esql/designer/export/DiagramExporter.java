package nl.errorsoft.esql.designer.export;

import java.util.ArrayList;
import java.util.List;

/** Writes a model as a PlantUML or a Mermaid entity relationship diagram, so it can be put in documentation. */
public final class DiagramExporter {
	private DiagramExporter() {
	}

	public static String plantUml(DiagramModel model) {
		StringBuilder out = new StringBuilder("@startuml\n");
		for (String database : model.databases()) {
			out.append("' database: ").append(database).append('\n');
		}
		out.append("hide circle\n");
		out.append("skinparam linetype ortho\n");

		for (DiagramModel.Table table : model.tables()) {
			out.append('\n');
			out.append("entity \"").append(table.name().replace("\"", "'")).append("\" as ").append(identifier(table.name())).append(" {\n");

			List<String> keys = new ArrayList<>();
			List<String> others = new ArrayList<>();
			for (DiagramModel.Column column : table.columns()) {
				String line = (column.primary() ? "  * " : "  ") + column.name() + " : " + column.type() + markers(column, " <<PK>>", " <<FK>>", "");
				(column.primary() ? keys : others).add(line);
			}
			keys.forEach(line -> out.append(line).append('\n'));
			if (!keys.isEmpty() && !others.isEmpty()) {
				out.append("  --\n");
			}
			others.forEach(line -> out.append(line).append('\n'));
			out.append("}\n");
		}

		if (!model.relations().isEmpty()) {
			out.append('\n');
		}
		for (DiagramModel.Relation relation : model.relations()) {
			out.append(identifier(relation.child())).append(" }o--|| ").append(identifier(relation.parent())).append(" : ").append(relation.name())
				.append('\n');
		}
		return out.append("@enduml\n").toString();
	}

	public static String mermaid(DiagramModel model) {
		StringBuilder out = new StringBuilder("erDiagram\n");
		for (String database : model.databases()) {
			out.append("    %% database: ").append(database).append('\n');
		}

		for (DiagramModel.Table table : model.tables()) {
			out.append("    ").append(identifier(table.name())).append(" {\n");
			for (DiagramModel.Column column : table.columns()) {
				out.append("        ").append(mermaidType(column.type())).append(' ').append(identifier(column.name()))
					.append(markers(column, " PK", " FK", " PK, FK")).append('\n');
			}
			out.append("    }\n");
		}

		for (DiagramModel.Relation relation : model.relations()) {
			out.append("    ").append(identifier(relation.parent())).append(" ||--o{ ").append(identifier(relation.child())).append(" : \"")
				.append(relation.name().replace("\"", "'")).append("\"\n");
		}
		return out.toString();
	}

	private static String markers(DiagramModel.Column column, String primary, String foreign, String both) {
		if (column.primary() && column.foreign()) {
			return both.isEmpty() ? primary + foreign : both;
		}
		return column.primary() ? primary : column.foreign() ? foreign : "";
	}

	/** Mermaid allows letters, digits, -, _ and brackets in a type; a length with a comma (numeric(10,2)) is left out. */
	private static String mermaidType(String type) {
		String plain = type.contains(",") && type.contains("(") ? type.substring(0, type.indexOf('(')) : type;
		String cleaned = plain.replaceAll("[^A-Za-z0-9_()\\-\\[\\]]", "_");
		return cleaned.isEmpty() ? "unknown" : cleaned;
	}

	/** A name both tools accept without quotes. */
	private static String identifier(String name) {
		String cleaned = name.replaceAll("[^A-Za-z0-9_]", "_");
		return cleaned.isEmpty() || Character.isDigit(cleaned.charAt(0)) ? "_" + cleaned : cleaned;
	}
}

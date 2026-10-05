package nl.errorsoft.esql.designer.model;

import java.io.File;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import nl.errorsoft.esql.designer.ui.diagram.NoteCard;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.ModelCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Reads and writes a designer model as an .edm file. JDOM escapes the values, so names with {@code < & "} survive a round trip.
 * Version 0.1 files (written before foreign keys existed) still load; new files are written as {@value #VERSION}.
 */
final class ModelXml {
	static final String VERSION = "0.2";

	private ModelXml() {
	}

	static String write(Model model) {
		Element root = new Element("model");
		root.addContent(text("version", VERSION));
		root.addContent(text("name", model.getName()));
		root.addContent(text("comment", model.getComment()));
		root.addContent(text("identifier_offset", model.getIdentifier()));
		root.addContent(text("author", model.getAuthor()));
		Element edited = new Element("last_edited");
		edited.addContent(text("author", model.getAuthor()));
		edited.addContent(text("time", System.currentTimeMillis()));
		root.addContent(edited);

		Element databases = new Element("databases");
		Element tables = new Element("tables");
		Element comments = new Element("comments");
		Element relations = new Element("relations");

		for (Object object : model.getObjects()) {
			switch (object) {
				case DatabaseCard databaseCard -> databases.addContent(database(databaseCard));
				case TableCard tableCard -> tables.addContent(table(tableCard));
				case NoteCard noteCard -> comments.addContent(comment(noteCard));
				default -> {
				}
			}

			ModelCard source = (ModelCard) object;
			for (Object target : source.getReferences()) {
				Element relation = new Element("relation");
				relation.addContent(text("source_identifier", source.getIdentifier()));
				relation.addContent(text("target_identifier", ((ModelCard) target).getIdentifier()));
				relations.addContent(relation);
			}
		}

		root.addContent(databases);
		root.addContent(tables);
		root.addContent(comments);
		root.addContent(relations);
		root.addContent(foreignKeys(model));

		return new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8")).outputString(new Document(root));
	}

	private static Element database(DatabaseCard card) {
		Element element = new Element("database");
		element.addContent(text("name", card.getName()));
		element.addContent(text("comment", card.getDescription()));
		element.addContent(text("identifier", card.getIdentifier()));
		element.addContent(bounds(card));
		return element;
	}

	private static Element table(TableCard card) {
		Element element = new Element("table");
		element.addContent(text("name", card.getName()));
		element.addContent(text("comment", card.getComment()));
		element.addContent(text("description", card.getDescription()));
		element.addContent(text("type", card.getType()));
		element.addContent(text("identifier", card.getIdentifier()));
		element.addContent(bounds(card));

		Element fields = new Element("fields");
		for (DesignerColumn column : card.getFields()) {
			Element field = new Element("field");
			field.addContent(text("name", column.getName()));
			field.addContent(text("comment", column.getComment()));
			field.addContent(text("default", column.getDefault()));
			field.addContent(text("length", column.getLength()));
			field.addContent(text("type", column.getType().getName()));
			field.addContent(text("primary", column.primary));
			field.addContent(text("autoincrement", column.autoIncrement));
			field.addContent(text("binary", column.binary));
			field.addContent(text("index", column.index));
			field.addContent(text("notnull", column.notNull));
			field.addContent(text("unique", column.unique));
			field.addContent(text("unsigned", column.unsigned));
			field.addContent(text("zerofill", column.zerofill));
			fields.addContent(field);
		}
		element.addContent(fields);
		return element;
	}

	private static Element comment(NoteCard card) {
		Element element = new Element("comment");
		element.addContent(text("comment", card.getComment()));
		element.addContent(text("identifier", card.getIdentifier()));
		element.addContent(bounds(card));
		return element;
	}

	private static Element foreignKeys(Model model) {
		Element keys = new Element("foreignkeys");
		for (ModelForeignKey key : model.getForeignKeys()) {
			Element element = new Element("foreignkey");
			element.addContent(text("name", key.name()));
			element.addContent(text("from_identifier", key.from().getIdentifier()));
			element.addContent(text("to_identifier", key.to().getIdentifier()));
			element.addContent(text("on_delete", key.onDelete()));
			element.addContent(text("on_update", key.onUpdate()));

			for (int i = 0; i < key.fromColumns().size(); i++) {
				Element column = new Element("column");
				column.setAttribute("from", key.fromColumns().get(i));
				column.setAttribute("to", key.toColumns().get(i));
				element.addContent(column);
			}
			keys.addContent(element);
		}
		return keys;
	}

	private static Element bounds(ModelCard object) {
		// The card, not the shadow margin around it, so that models written before the cards had a shadow keep their positions.
		Rectangle card = object.cardBounds();
		Element bounds = new Element("bounds");
		bounds.addContent(text("x", card.x));
		bounds.addContent(text("y", card.y));
		bounds.addContent(text("w", card.width));
		bounds.addContent(text("h", card.height));
		return bounds;
	}

	private static Element text(String name, Object value) {
		return new Element(name).setText(value == null ? "" : String.valueOf(value));
	}

	// Reading

	static Model read(File file) throws Exception {
		Document document = new SAXBuilder().build(file);
		Element root = document.getRootElement();
		String version = text(root, "version", "");

		if (!version.equals("0.1") && !version.equals(VERSION)) {
			throw new EsqlException("The model file has version '" + version + "', only 0.1 and " + VERSION + " can be opened.");
		}

		Model model = new Model(text(root, "name", "Model"));
		model.setAuthor(text(root, "author", ""));
		model.setComment(text(root, "comment", ""));

		for (Element databaseElement : children(root, "databases", "database")) {
			DatabaseCard databaseCard = model.createDatabaseCard(text(databaseElement, "name", ""));
			databaseCard.setDescription(text(databaseElement, "comment", ""));
			databaseCard.setIdentifier(number(databaseElement, "identifier", -1));
			place(databaseCard, databaseElement);
		}

		for (Element noteElement : children(root, "comments", "comment")) {
			NoteCard noteCard = model.createNoteCard(text(noteElement, "comment", ""));
			noteCard.setIdentifier(number(noteElement, "identifier", -1));
			place(noteCard, noteElement);
		}

		for (Element tableElement : children(root, "tables", "table")) {
			TableCard tableCard = model.createTableCard(text(tableElement, "name", ""));
			tableCard.setComment(text(tableElement, "comment", ""));
			tableCard.setDescription(text(tableElement, "description", ""));
			tableCard.setIdentifier(number(tableElement, "identifier", -1));
			tableCard.setType(text(tableElement, "type", ""));
			place(tableCard, tableElement);

			for (Element fieldElement : children(tableElement, "fields", "field")) {
				DataType type = DataType.named(text(fieldElement, "type", ""));
				DesignerColumn column = new DesignerColumn(text(fieldElement, "name", ""), type, text(fieldElement, "length", ""),
					text(fieldElement, "default", ""), text(fieldElement, "comment", ""));
				column.primary = flag(fieldElement, "primary");
				column.autoIncrement = flag(fieldElement, "autoincrement");
				column.binary = flag(fieldElement, "binary");
				column.index = flag(fieldElement, "index");
				column.notNull = flag(fieldElement, "notnull");
				column.unique = flag(fieldElement, "unique");
				column.unsigned = flag(fieldElement, "unsigned");
				column.zerofill = flag(fieldElement, "zerofill");
				tableCard.addField(column);
			}
		}

		for (Element relationElement : children(root, "relations", "relation")) {
			ModelCard source = model.getObjectByIdentifier(number(relationElement, "source_identifier", -1));
			ModelCard target = model.getObjectByIdentifier(number(relationElement, "target_identifier", -1));

			// A relation to an object that is not in the file can't be drawn, it is left out.
			if (source != null && target != null) {
				source.addReference(target);
			}
		}

		readForeignKeys(model, root);

		// Set last, creating the objects above moved the counter on.
		model.setIdentifier(number(root, "identifier_offset", model.getIdentifier()));
		return model;
	}

	private static void readForeignKeys(Model model, Element root) {
		for (Element foreignKeyElement : children(root, "foreignkeys", "foreignkey")) {
			ModelCard from = model.getObjectByIdentifier(number(foreignKeyElement, "from_identifier", -1));
			ModelCard to = model.getObjectByIdentifier(number(foreignKeyElement, "to_identifier", -1));

			// A key between tables that are not in the file can't be restored, it is left out.
			if (!(from instanceof TableCard fromTable) || !(to instanceof TableCard toTable)) {
				continue;
			}

			List<String> fromColumns = new ArrayList<>();
			List<String> toColumns = new ArrayList<>();
			for (Object child : foreignKeyElement.getChildren("column")) {
				Element column = (Element) child;
				fromColumns.add(column.getAttributeValue("from", ""));
				toColumns.add(column.getAttributeValue("to", ""));
			}

			model.addForeignKey(new ModelForeignKey(fromTable, fromColumns, toTable, toColumns, text(foreignKeyElement, "name", ""),
				text(foreignKeyElement, "on_delete", ""), text(foreignKeyElement, "on_update", "")));
		}
	}

	private static void place(ModelCard object, Element element) {
		Element bounds = element.getChild("bounds");
		if (bounds != null) {
			Rectangle card = object.cardBounds();
			object.setCardLocation(number(bounds, "x", 0), number(bounds, "y", 0));
			// Tables and databases size themselves to their content, only a note keeps the size it was given.
			if (object instanceof NoteCard) {
				object.setCardSize(Math.max(120, number(bounds, "w", card.width)), Math.max(60, number(bounds, "h", card.height)));
			}
		}
		object.setHidden(false);
	}

	private static List<Element> children(Element parent, String group, String name) {
		Element container = parent.getChild(group);
		List<Element> elements = new ArrayList<>();

		if (container != null) {
			for (Object child : container.getChildren(name)) {
				elements.add((Element) child);
			}
		}
		return elements;
	}

	private static String text(Element parent, String name, String fallback) {
		Element child = parent.getChild(name);
		return child == null ? fallback : child.getText();
	}

	private static int number(Element parent, String name, int fallback) {
		try {
			return Integer.parseInt(text(parent, name, "").trim());
		} catch (NumberFormatException e) {
			// A missing or broken number falls back to the default, the rest of the model still loads.
			return fallback;
		}
	}

	private static boolean flag(Element parent, String name) {
		return text(parent, name, "false").trim().equalsIgnoreCase("true");
	}
}

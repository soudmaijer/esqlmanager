package nl.errorsoft.esql.designer.model;

import java.io.File;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import nl.errorsoft.esql.designer.ui.diagram.CommentObject;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.ModelObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;
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
				case DatabaseObject db -> databases.addContent(database(db));
				case TableObject tb -> tables.addContent(table(tb));
				case CommentObject cm -> comments.addContent(comment(cm));
				default -> {
				}
			}

			ModelObject source = (ModelObject) object;
			for (Object target : source.getReferences()) {
				Element relation = new Element("relation");
				relation.addContent(text("source_identifier", source.getIdentifier()));
				relation.addContent(text("target_identifier", ((ModelObject) target).getIdentifier()));
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

	private static Element database(DatabaseObject db) {
		Element element = new Element("database");
		element.addContent(text("name", db.getName()));
		element.addContent(text("comment", db.getDescription()));
		element.addContent(text("identifier", db.getIdentifier()));
		element.addContent(bounds(db));
		return element;
	}

	private static Element table(TableObject tb) {
		Element element = new Element("table");
		element.addContent(text("name", tb.getName()));
		element.addContent(text("comment", tb.getComment()));
		element.addContent(text("description", tb.getDescription()));
		element.addContent(text("type", tb.getType()));
		element.addContent(text("identifier", tb.getIdentifier()));
		element.addContent(bounds(tb));

		Element fields = new Element("fields");
		for (DesignerColumn fd : tb.getFields()) {
			Element field = new Element("field");
			field.addContent(text("name", fd.getName()));
			field.addContent(text("comment", fd.getComment()));
			field.addContent(text("default", fd.getDefault()));
			field.addContent(text("length", fd.getLength()));
			field.addContent(text("type", fd.getType().getName()));
			field.addContent(text("primary", fd.primary));
			field.addContent(text("autoincrement", fd.autoincrement));
			field.addContent(text("binary", fd.binary));
			field.addContent(text("index", fd.index));
			field.addContent(text("notnull", fd.notnull));
			field.addContent(text("unique", fd.unique));
			field.addContent(text("unsigned", fd.unsigned));
			field.addContent(text("zerofill", fd.zerofill));
			fields.addContent(field);
		}
		element.addContent(fields);
		return element;
	}

	private static Element comment(CommentObject cm) {
		Element element = new Element("comment");
		element.addContent(text("comment", cm.getComment()));
		element.addContent(text("identifier", cm.getIdentifier()));
		element.addContent(bounds(cm));
		return element;
	}

	private static Element foreignKeys(Model model) {
		Element keys = new Element("foreignkeys");
		for (DesignerForeignKey key : model.getForeignKeys()) {
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

	private static Element bounds(ModelObject object) {
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

		for (Element db : children(root, "databases", "database")) {
			DatabaseObject d = model.createDatabaseObject(text(db, "name", ""));
			d.setDescription(text(db, "comment", ""));
			d.setIdentifier(number(db, "identifier", -1));
			place(d, db);
		}

		for (Element cm : children(root, "comments", "comment")) {
			CommentObject c = model.createCommentObject(text(cm, "comment", ""));
			c.setIdentifier(number(cm, "identifier", -1));
			place(c, cm);
		}

		for (Element tb : children(root, "tables", "table")) {
			TableObject t = model.createTableObject(text(tb, "name", ""));
			t.setComment(text(tb, "comment", ""));
			t.setDescription(text(tb, "description", ""));
			t.setIdentifier(number(tb, "identifier", -1));
			t.setType(text(tb, "type", ""));
			place(t, tb);

			for (Element fd : children(tb, "fields", "field")) {
				DataType type = new DataType(text(fd, "type", ""), false, false, false, false, false, false, false, false);
				DesignerColumn f = new DesignerColumn(text(fd, "name", ""), type, text(fd, "length", ""), text(fd, "default", ""), text(fd, "comment", ""));
				f.primary = flag(fd, "primary");
				f.autoincrement = flag(fd, "autoincrement");
				f.binary = flag(fd, "binary");
				f.index = flag(fd, "index");
				f.notnull = flag(fd, "notnull");
				f.unique = flag(fd, "unique");
				f.unsigned = flag(fd, "unsigned");
				f.zerofill = flag(fd, "zerofill");
				t.addField(f);
			}
		}

		for (Element rl : children(root, "relations", "relation")) {
			ModelObject source = model.getObjectByIdentifier(number(rl, "source_identifier", -1));
			ModelObject target = model.getObjectByIdentifier(number(rl, "target_identifier", -1));

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
		for (Element fk : children(root, "foreignkeys", "foreignkey")) {
			ModelObject from = model.getObjectByIdentifier(number(fk, "from_identifier", -1));
			ModelObject to = model.getObjectByIdentifier(number(fk, "to_identifier", -1));

			// A key between tables that are not in the file can't be restored, it is left out.
			if (!(from instanceof TableObject fromTable) || !(to instanceof TableObject toTable)) {
				continue;
			}

			List<String> fromColumns = new ArrayList<>();
			List<String> toColumns = new ArrayList<>();
			for (Object child : fk.getChildren("column")) {
				Element column = (Element) child;
				fromColumns.add(column.getAttributeValue("from", ""));
				toColumns.add(column.getAttributeValue("to", ""));
			}

			model.addForeignKey(new DesignerForeignKey(fromTable, fromColumns, toTable, toColumns, text(fk, "name", ""),
				text(fk, "on_delete", ""), text(fk, "on_update", "")));
		}
	}

	private static void place(ModelObject object, Element element) {
		Element bounds = element.getChild("bounds");
		if (bounds != null) {
			Rectangle card = object.cardBounds();
			object.setCardLocation(number(bounds, "x", 0), number(bounds, "y", 0));
			// Tables and databases size themselves to their content, only a note keeps the size it was given.
			if (object instanceof CommentObject) {
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

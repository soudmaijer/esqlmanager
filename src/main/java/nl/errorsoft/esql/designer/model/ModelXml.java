package nl.errorsoft.esql.designer.model;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import nl.errorsoft.esql.designer.ui.diagram.NoteCard;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.ModelCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.xml.XmlFiles;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Reads and writes a designer model as an .edm file. {@link XmlFiles} escapes the values, so names with {@code < & "} survive a round trip.
 * New files are written as {@value #VERSION}, with the server type the model is designed for ({@code <servertype>}, the number of
 * {@code ServerType}, empty when unknown). Version 0.1 (before foreign keys) and 0.2 (before the server type) still load, with an unknown server.
 */
final class ModelXml {
	static final String VERSION = "0.3";
	private static final java.util.Set<String> READABLE = java.util.Set.of("0.1", "0.2", VERSION);

	private ModelXml() {
	}

	static String write(Model model) {
		return XmlFiles.toString(document(model));
	}

	/** Writes the model to the file in one step, so a failure leaves the old file as it was. */
	static void save(Model model, File file) {
		XmlFiles.write(file.toPath(), document(model));
	}

	private static Document document(Model model) {
		Document document = XmlFiles.newDocument("model");
		Element root = document.getDocumentElement();
		add(root, "version", VERSION);
		add(root, "name", model.getName());
		add(root, "servertype", model.getServerType() == null ? "" : model.getServerType().getType());
		add(root, "comment", model.getComment());
		add(root, "identifier_offset", model.getIdentifier());
		add(root, "author", model.getAuthor());
		Element edited = XmlFiles.addChild(root, "last_edited");
		add(edited, "author", model.getAuthor());
		add(edited, "time", System.currentTimeMillis());

		Element databases = XmlFiles.addChild(root, "databases");
		Element tables = XmlFiles.addChild(root, "tables");
		Element comments = XmlFiles.addChild(root, "comments");
		Element relations = XmlFiles.addChild(root, "relations");

		for (Object object : model.getObjects()) {
			switch (object) {
				case DatabaseCard databaseCard -> database(databases, databaseCard);
				case TableCard tableCard -> table(tables, tableCard);
				case NoteCard noteCard -> comment(comments, noteCard);
				default -> {
				}
			}

			ModelCard source = (ModelCard) object;
			for (Object target : source.getReferences()) {
				Element relation = XmlFiles.addChild(relations, "relation");
				add(relation, "source_identifier", source.getIdentifier());
				add(relation, "target_identifier", ((ModelCard) target).getIdentifier());
			}
		}

		foreignKeys(root, model);

		return document;
	}

	private static void database(Element parent, DatabaseCard card) {
		Element element = XmlFiles.addChild(parent, "database");
		add(element, "name", card.getName());
		add(element, "comment", card.getDescription());
		add(element, "identifier", card.getIdentifier());
		bounds(element, card);
	}

	private static void table(Element parent, TableCard card) {
		Element element = XmlFiles.addChild(parent, "table");
		add(element, "name", card.getName());
		add(element, "comment", card.getComment());
		add(element, "description", card.getDescription());
		add(element, "type", card.getType());
		add(element, "identifier", card.getIdentifier());
		bounds(element, card);

		Element fields = XmlFiles.addChild(element, "fields");
		for (DesignerColumn column : card.getFields()) {
			Element field = XmlFiles.addChild(fields, "field");
			add(field, "name", column.getName());
			add(field, "comment", column.getComment());
			add(field, "default", column.getDefault());
			add(field, "length", column.getLength());
			add(field, "type", column.getType().getName());
			add(field, "primary", column.primary);
			add(field, "autoincrement", column.autoIncrement);
			add(field, "binary", column.binary);
			add(field, "index", column.index);
			add(field, "notnull", column.notNull);
			add(field, "unique", column.unique);
			add(field, "unsigned", column.unsigned);
			add(field, "zerofill", column.zerofill);
		}
	}

	private static void comment(Element parent, NoteCard card) {
		Element element = XmlFiles.addChild(parent, "comment");
		add(element, "comment", card.getComment());
		add(element, "identifier", card.getIdentifier());
		bounds(element, card);
	}

	private static void foreignKeys(Element root, Model model) {
		Element keys = XmlFiles.addChild(root, "foreignkeys");
		for (ModelForeignKey key : model.getForeignKeys()) {
			Element element = XmlFiles.addChild(keys, "foreignkey");
			add(element, "name", key.name());
			add(element, "from_identifier", key.from().getIdentifier());
			add(element, "to_identifier", key.to().getIdentifier());
			add(element, "on_delete", key.onDelete());
			add(element, "on_update", key.onUpdate());

			for (int i = 0; i < key.fromColumns().size(); i++) {
				Element column = XmlFiles.addChild(element, "column");
				XmlFiles.setAttribute(column, "from", key.fromColumns().get(i));
				XmlFiles.setAttribute(column, "to", key.toColumns().get(i));
			}
		}
	}

	private static void bounds(Element parent, ModelCard object) {
		// The card, not the shadow margin around it, so that models written before the cards had a shadow keep their positions.
		Rectangle card = object.cardBounds();
		Element bounds = XmlFiles.addChild(parent, "bounds");
		add(bounds, "x", card.x);
		add(bounds, "y", card.y);
		add(bounds, "w", card.width);
		add(bounds, "h", card.height);
	}

	private static void add(Element parent, String name, Object value) {
		XmlFiles.addChild(parent, name, value == null ? "" : String.valueOf(value));
	}

	// Reading

	static Model read(File file) {
		Element root = XmlFiles.read(file.toPath()).getDocumentElement();
		String version = text(root, "version", "");

		if (!READABLE.contains(version)) {
			throw new EsqlException("The model file has version '" + version + "', only 0.1, 0.2 and " + VERSION + " can be opened.");
		}

		Model model = new Model(text(root, "name", "Model"));
		int serverType = number(root, "servertype", -1);
		// An unknown number (a server this version does not know) is treated as no server: generation then asks for a connection of any kind.
		model.setServerType(nl.errorsoft.esql.connection.ServerType.isKnown(serverType) ? new nl.errorsoft.esql.connection.ServerType(serverType) : null);
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
			for (Element column : XmlFiles.children(foreignKeyElement, "column")) {
				fromColumns.add(XmlFiles.attribute(column, "from", ""));
				toColumns.add(XmlFiles.attribute(column, "to", ""));
			}

			model.addForeignKey(new ModelForeignKey(fromTable, fromColumns, toTable, toColumns, text(foreignKeyElement, "name", ""),
				text(foreignKeyElement, "on_delete", ""), text(foreignKeyElement, "on_update", "")));
		}
	}

	private static void place(ModelCard object, Element element) {
		Element bounds = XmlFiles.child(element, "bounds");
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
		Element container = XmlFiles.child(parent, group);
		return container == null ? List.of() : XmlFiles.children(container, name);
	}

	private static String text(Element parent, String name, String fallback) {
		return XmlFiles.childText(parent, name, fallback);
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

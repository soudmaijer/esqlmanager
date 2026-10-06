package nl.errorsoft.esql.designer.model;

import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.export.DiagramModel;
import nl.errorsoft.esql.table.ColumnDefinition;
import nl.errorsoft.esql.designer.ui.diagram.NoteCard;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.ModelCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;

import java.util.*;
import java.awt.event.*;
import java.io.*;

public class Model implements MouseListener, MouseMotionListener {
	private String name = "";
	private String comment = "";
	private String author = "";
	private final List<ModelCard> cards = new ArrayList<>();
	private final List<ModelForeignKey> foreignKeys = new ArrayList<>();
	private boolean locked = false;
	private int identifier = 1;
	private File file = null;
	/** The server the model is designed for, null when unknown (files written before 0.3): generation then adopts the connection's. */
	private nl.errorsoft.esql.connection.ServerType serverType;

	public Model(String name) {
		this.name = name;
	}

	public nl.errorsoft.esql.connection.ServerType getServerType() {
		return serverType;
	}

	/** Sets the server the model is designed for; its database cards take the server's word for a database. */
	public void setServerType(nl.errorsoft.esql.connection.ServerType serverType) {
		this.serverType = serverType;
		for (ModelCard card : cards) {
			if (card instanceof DatabaseCard database) {
				database.setTerm(term());
			}
		}
	}

	/** What a database card is on the model's server ({@code Dialect.databaseTerm()}), "database" while the server is unknown. */
	public String term() {
		return serverType == null ? "database" : serverType.getDialect().databaseTerm();
	}

	/** The database cards a table is linked to. */
	public List<DatabaseCard> databasesOf(TableCard table) {
		return getReferences(table).stream().filter(DatabaseCard.class::isInstance).map(DatabaseCard.class::cast).toList();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public DatabaseCard createDatabaseCard(String name) {
		DatabaseCard database = new DatabaseCard(name, identifier);
		database.setTerm(term());
		identifier++;
		database.addMouseListener(this);
		database.addMouseMotionListener(this);
		cards.add(database);
		return database;
	}

	public TableCard createTableCard(String name) {
		TableCard table = new TableCard(name, identifier);
		identifier++;
		table.addMouseListener(this);
		table.addMouseMotionListener(this);
		cards.add(table);
		return table;
	}

	public NoteCard createNoteCard(String comment) {
		NoteCard note = new NoteCard(comment, identifier);
		identifier++;
		note.addMouseListener(this);
		note.addMouseMotionListener(this);
		cards.add(note);
		return note;
	}

	public void addReference(ModelCard source, ModelCard target) {
		if (source == target || (source instanceof DatabaseCard && target instanceof DatabaseCard)
			|| (source instanceof TableCard && target instanceof TableCard)) {
			return;
		}

		List<ModelCard> references = source.getReferences();
		for (int i = 0; i < references.size(); i++) {
			ModelCard other = (ModelCard) references.get(i);
			if (other == target) {
				return;
			}
		}

		references = target.getReferences();
		for (int i = 0; i < references.size(); i++) {
			ModelCard other = (ModelCard) references.get(i);
			if (other == source) {
				return;
			}
		}

		source.addReference(target);
	}

	/** Adds a foreign key between two tables, a key with the same name on the same table is replaced. */
	public void addForeignKey(ModelForeignKey key) {
		foreignKeys.removeIf(existing -> existing.from() == key.from() && existing.name().equals(key.name()));
		foreignKeys.add(key);
	}

	public void removeForeignKey(ModelForeignKey key) {
		foreignKeys.remove(key);
	}

	public List<ModelForeignKey> getForeignKeys() {
		return Collections.unmodifiableList(foreignKeys);
	}

	/** The keys the table has on other tables and the keys other tables have on it. */
	public List<ModelForeignKey> foreignKeysOf(TableCard table) {
		List<ModelForeignKey> keys = new ArrayList<>();

		for (ModelForeignKey key : foreignKeys) {
			if (key.involves(table)) {
				keys.add(key);
			}
		}
		return keys;
	}

	/** Keeps the foreign keys in step when a field of a table is renamed. */
	public void fieldRenamed(TableCard table, String oldName, String newName) {
		foreignKeys.replaceAll(key -> key.withColumnsRenamed(table, Map.of(oldName, newName)));
	}

	/** A foreign key that used a removed field can't exist any more, it is removed too. */
	public void fieldRemoved(TableCard table, String name) {
		foreignKeys.removeIf(key -> key.usesColumn(table, name));
	}

	/**
	 * Updates the foreign keys after the fields of a table were edited.
	 * @param namesBefore every field the table had before the edit, with the name it had then.
	 */
	public void fieldsEdited(TableCard table, Map<DesignerColumn, String> namesBefore) {
		List<DesignerColumn> now = Arrays.asList(table.getFields());
		Map<String, String> renames = new HashMap<>();

		for (Map.Entry<DesignerColumn, String> before : namesBefore.entrySet()) {
			if (!now.contains(before.getKey())) {
				fieldRemoved(table, before.getValue());
			} else if (!before.getValue().equals(before.getKey().getName())) {
				renames.put(before.getValue(), before.getKey().getName());
			}
		}

		if (!renames.isEmpty()) {
			foreignKeys.replaceAll(key -> key.withColumnsRenamed(table, renames));
		}
	}

	private void removeForeignKeysOf(ModelCard object) {
		if (object instanceof TableCard table) {
			foreignKeys.removeIf(key -> key.involves(table));
		}
	}

	public List<ModelCard> getObjects() {
		return cards;
	}

	public List<ModelCard> getReferences(ModelCard card) {
		List<ModelCard> related = new ArrayList<>();
		for (int i = 0; i < cards.size(); i++) {
			ModelCard other = (ModelCard) cards.get(i);
			if (other.getReferences().contains(card) && !(other instanceof NoteCard)) {
				related.add(other);
			}
		}
		for (int i = 0; i < card.getReferences().size(); i++) {
			ModelCard other = (ModelCard) card.getReferences().get(i);
			if (!(other instanceof NoteCard)) {
				related.add(other);
			}
		}
		return related;
	}

	public List<ModelCard> removeSelectedObjects() {
		List<ModelCard> removed = new ArrayList<>();
		if (!locked) {
			List<ModelCard> selectedCards = getSelectedObjects();
			for (int i = 0; i < selectedCards.size(); i++) {
				ModelCard card = (ModelCard) selectedCards.get(i);
				if (card.isSelected()) {
					for (int j = 0; j < cards.size(); j++) {
						ModelCard other = (ModelCard) cards.get(j);
						if (card != other) {
							other.removeReference(card);
						}
					}
				}
				cards.remove(card);
				removeForeignKeysOf(card);
				removed.add(card);
			}
		}
		return removed;
	}

	public void removeObject(ModelCard card) {
		if (!locked) {
			for (int j = 0; j < cards.size(); j++) {
				ModelCard other = (ModelCard) cards.get(j);
				if (card != other) {
					other.removeReference(card);
				}
			}
			cards.remove(card);
			removeForeignKeysOf(card);
		}
	}

	public List<ModelCard> getSelectedObjects() {
		List<ModelCard> selected = new ArrayList<>();
		for (int i = 0; i < cards.size(); i++) {
			ModelCard card = (ModelCard) cards.get(i);
			if (card.isSelected()) {
				selected.add(card);
			}
		}
		return selected;
	}

	public void deselectAll() {
		for (int i = 0; i < cards.size(); i++) {
			((ModelCard) cards.get(i)).setSelected(false);
		}
	}

	public void selectAll() {
		for (int i = 0; i < cards.size(); i++) {
			((ModelCard) cards.get(i)).setSelected(true);
		}
	}

	public void lock() {
		locked = true;
	}

	public void unlock() {
		locked = false;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File modelFile) {
		this.file = modelFile;
	}

	public void mousePressed(MouseEvent e) {
		if (!this.locked) {
			ModelCard card = (ModelCard) e.getSource();
			// A right click (ctrl-click on macOS) selects the object for its context menu, it does not toggle it.
			if (e.isPopupTrigger()) {
				if (!card.isSelected()) {
					deselectAll();
					card.setSelected(true);
				}
			} else if (!e.isControlDown() && !card.isSelected()) {
				deselectAll();
				card.setSelected(true);
			} else if (e.isControlDown()) {
				card.setSelected(!card.isSelected());
			}
			card.xc = e.getX();
			card.yc = e.getY();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (!e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && !this.locked) {
			ModelCard dragged = (ModelCard) e.getSource();
			// A drag that starts on the icon of a field draws a foreign key, it does not move the table.
			if (dragged instanceof TableCard table && table.handleAt(table.xc, table.yc) >= 0) {
				return;
			}

			int xloc = dragged.getX() + (e.getX() - dragged.xc);
			int yloc = dragged.getY() + (e.getY() - dragged.yc);

			dragged.setLocation(xloc, yloc);

			int xadj = (e.getX() - dragged.xc);
			int yadj = (e.getY() - dragged.yc);
			for (int i = 0; i < cards.size(); i++) {
				ModelCard other = (ModelCard) cards.get(i);
				if (other.isSelected() && other != dragged) {
					xloc = other.getX() + xadj;
					yloc = other.getY() + yadj;

					other.setLocation(xloc, yloc);
				}
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (!this.locked) {
			int x = 0;
			int y = 0;
			for (int i = 0; i < cards.size(); i++) {
				ModelCard card = (ModelCard) cards.get(i);
				if (card.getX() < x) {
					x = card.getX();
				}
				if (card.getY() < y) {
					y = card.getY();
				}
			}
			if (x < 0 || y < 0) {
				for (int i = 0; i < cards.size(); i++) {
					ModelCard card = (ModelCard) cards.get(i);
					card.setLocation(card.getX() + (-x) + 5, card.getY() + (-y) + 5);
				}
			}
		}
	}

	public ModelCard getObjectByIdentifier(int identifier) {
		for (int i = 0; i < cards.size(); i++) {
			ModelCard card = (ModelCard) cards.get(i);
			if (card.getIdentifier() == identifier) {
				return card;
			}
		}
		return null;
	}

	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

	public int getIdentifier() {
		return identifier;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	/** The model as the XML of an .edm file. */
	public String getModelXML() {
		return ModelXml.write(this);
	}

	/** Writes the model as an .edm file; the old file stays as it was when this fails. */
	public void saveModel(File file) {
		ModelXml.save(this, file);
	}

	/** Reads a model file of version 0.1 or 0.2. */
	public Model loadModel(File xml) throws Exception {
		return ModelXml.read(xml);
	}

	/**
	 * A snapshot of the model as plain records, for the check and the generation; read it on the event thread, the records can then be used on any
	 * thread. A table linked to no database is listed apart.
	 */
	public DesignedModel toDesigned() {
		List<DesignedDatabase> databases = new ArrayList<>();
		List<TableCard> linked = new ArrayList<>();

		for (ModelCard object : getObjects()) {
			if (object instanceof DatabaseCard database) {
				List<DesignedTable> tables = new ArrayList<>();
				for (ModelCard reference : getReferences(database)) {
					if (reference instanceof TableCard table) {
						linked.add(table);
						tables.add(designed(table));
					}
				}
				databases.add(new DesignedDatabase(database.getName(), tables));
			}
		}

		List<DesignedTable> unlinked = new ArrayList<>();
		for (ModelCard object : getObjects()) {
			if (object instanceof TableCard table && !linked.contains(table)) {
				unlinked.add(designed(table));
			}
		}
		return new DesignedModel(databases, unlinked);
	}

	private DesignedTable designed(TableCard table) {
		List<ColumnDefinition> columns = new ArrayList<>();
		for (DesignerColumn field : table.getFields()) {
			columns.add(column(field));
		}

		// The keys the table has on other tables; the keys other tables have on it are generated with those tables.
		List<DesignedForeignKey> keys = new ArrayList<>();
		for (ModelForeignKey key : foreignKeysOf(table)) {
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

	/** What a text diagram (PlantUML, Mermaid) shows of this model. */
	public DiagramModel toDiagram() {
		List<String> databases = new ArrayList<>();
		List<DiagramModel.Table> tables = new ArrayList<>();
		List<DiagramModel.Relation> relations = new ArrayList<>();

		for (ModelForeignKey key : foreignKeys) {
			relations.add(new DiagramModel.Relation(key.from().getName(), key.fromColumns(), key.to().getName(), key.toColumns(), key.name()));
		}

		for (Object object : cards) {
			if (object instanceof DatabaseCard database) {
				databases.add(database.getName());
			} else if (object instanceof TableCard table) {
				Set<String> foreign = new HashSet<>();
				for (ModelForeignKey key : foreignKeysOf(table)) {
					if (key.from() == table) {
						foreign.addAll(key.fromColumns());
					}
				}

				List<DiagramModel.Column> columns = new ArrayList<>();
				for (DesignerColumn field : table.getFields()) {
					columns.add(new DiagramModel.Column(field.getName(), TableCard.typeText(field), field.primary, foreign.contains(field.getName())));
				}
				tables.add(new DiagramModel.Table(table.getName(), columns));
			}
		}
		return new DiagramModel(databases, tables, relations);
	}

	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
}

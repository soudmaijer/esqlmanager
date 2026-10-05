package nl.errorsoft.esql.designer.model;

import nl.errorsoft.esql.designer.export.DiagramModel;
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
	private final List<ModelCard> modelobjects = new ArrayList<>();
	private final List<ModelForeignKey> foreignKeys = new ArrayList<>();
	private boolean locked = false;
	private int identifier = 1;
	private File file = null;

	public Model(String name) {
		this.name = name;
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
		DatabaseCard db = new DatabaseCard(name, identifier);
		identifier++;
		db.addMouseListener(this);
		db.addMouseMotionListener(this);
		modelobjects.add(db);
		return db;
	}

	public TableCard createTableCard(String name) {
		TableCard tb = new TableCard(name, identifier);
		identifier++;
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		modelobjects.add(tb);
		return tb;
	}

	public NoteCard createNoteCard(String comment) {
		NoteCard cm = new NoteCard(comment, identifier);
		identifier++;
		cm.addMouseListener(this);
		cm.addMouseMotionListener(this);
		modelobjects.add(cm);
		return cm;
	}

	public void addReference(ModelCard src, ModelCard end) {
		if (src == end || (src instanceof DatabaseCard && end instanceof DatabaseCard) || (src instanceof TableCard && end instanceof TableCard)) {
			return;
		}

		List<ModelCard> v = src.getReferences();
		for (int i = 0; i < v.size(); i++) {
			ModelCard mo = (ModelCard) v.get(i);
			if (mo == end) {
				return;
			}
		}

		v = end.getReferences();
		for (int i = 0; i < v.size(); i++) {
			ModelCard mo = (ModelCard) v.get(i);
			if (mo == src) {
				return;
			}
		}

		src.addReference(end);
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
		return modelobjects;
	}

	public List<ModelCard> getReferences(ModelCard m) {
		List<ModelCard> refs = new ArrayList<>();
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelCard tmp = (ModelCard) modelobjects.get(i);
			if (tmp.getReferences().contains(m) && !(tmp instanceof NoteCard)) {
				refs.add(tmp);
			}
		}
		for (int i = 0; i < m.getReferences().size(); i++) {
			ModelCard tmp = (ModelCard) m.getReferences().get(i);
			if (!(tmp instanceof NoteCard)) {
				refs.add(tmp);
			}
		}
		return refs;
	}

	public List<ModelCard> removeSelectedObjects() {
		List<ModelCard> v = new ArrayList<>();
		if (!locked) {
			List<ModelCard> sel = getSelectedObjects();
			for (int i = 0; i < sel.size(); i++) {
				ModelCard tmp = (ModelCard) sel.get(i);
				if (tmp.isSelected()) {
					for (int j = 0; j < modelobjects.size(); j++) {
						ModelCard tmp2 = (ModelCard) modelobjects.get(j);
						if (tmp != tmp2) {
							tmp2.removeReference(tmp);
						}
					}
				}
				modelobjects.remove(tmp);
				removeForeignKeysOf(tmp);
				v.add(tmp);
			}
		}
		return v;
	}

	public void removeObject(ModelCard m) {
		if (!locked) {
			for (int j = 0; j < modelobjects.size(); j++) {
				ModelCard tmp2 = (ModelCard) modelobjects.get(j);
				if (m != tmp2) {
					tmp2.removeReference(m);
				}
			}
			modelobjects.remove(m);
			removeForeignKeysOf(m);
		}
	}

	public List<ModelCard> getSelectedObjects() {
		List<ModelCard> temp = new ArrayList<>();
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelCard tmp = (ModelCard) modelobjects.get(i);
			if (tmp.isSelected()) {
				temp.add(tmp);
			}
		}
		return temp;
	}

	public void deselectAll() {
		for (int i = 0; i < modelobjects.size(); i++) {
			((ModelCard) modelobjects.get(i)).setSelected(false);
		}
	}

	public void selectAll() {
		for (int i = 0; i < modelobjects.size(); i++) {
			((ModelCard) modelobjects.get(i)).setSelected(true);
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

	public void setFile(File f) {
		this.file = f;
	}

	public void mousePressed(MouseEvent e) {
		if (!this.locked) {
			ModelCard tmp = (ModelCard) e.getSource();
			// A right click (ctrl-click on macOS) selects the object for its context menu, it does not toggle it.
			if (e.isPopupTrigger()) {
				if (!tmp.isSelected()) {
					deselectAll();
					tmp.setSelected(true);
				}
			} else if (!e.isControlDown() && !tmp.isSelected()) {
				deselectAll();
				tmp.setSelected(true);
			} else if (e.isControlDown()) {
				tmp.setSelected(!tmp.isSelected());
			}
			tmp.xc = e.getX();
			tmp.yc = e.getY();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (!e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && !this.locked) {
			ModelCard tmp = (ModelCard) e.getSource();
			// A drag that starts on the icon of a field draws a foreign key, it does not move the table.
			if (tmp instanceof TableCard table && table.handleAt(table.xc, table.yc) >= 0) {
				return;
			}

			int xloc = tmp.getX() + (e.getX() - tmp.xc);
			int yloc = tmp.getY() + (e.getY() - tmp.yc);

			tmp.setLocation(xloc, yloc);

			int xadj = (e.getX() - tmp.xc);
			int yadj = (e.getY() - tmp.yc);
			for (int i = 0; i < modelobjects.size(); i++) {
				ModelCard mo = (ModelCard) modelobjects.get(i);
				if (mo.isSelected() && mo != tmp) {
					xloc = mo.getX() + xadj;
					yloc = mo.getY() + yadj;

					mo.setLocation(xloc, yloc);
				}
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (!this.locked) {
			int x = 0;
			int y = 0;
			for (int i = 0; i < modelobjects.size(); i++) {
				ModelCard mo = (ModelCard) modelobjects.get(i);
				if (mo.getX() < x) {
					x = mo.getX();
				}
				if (mo.getY() < y) {
					y = mo.getY();
				}
			}
			if (x < 0 || y < 0) {
				for (int i = 0; i < modelobjects.size(); i++) {
					ModelCard mo = (ModelCard) modelobjects.get(i);
					mo.setLocation(mo.getX() + (-x) + 5, mo.getY() + (-y) + 5);
				}
			}
		}
	}

	public ModelCard getObjectByIdentifier(int identifier) {
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelCard tmp = (ModelCard) modelobjects.get(i);
			if (tmp.getIdentifier() == identifier) {
				return tmp;
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

	/** Reads a model file of version 0.1 or 0.2. */
	public Model loadModel(File xml) throws Exception {
		return ModelXml.read(xml);
	}

	/** What a text diagram (PlantUML, Mermaid) shows of this model. */
	public DiagramModel toDiagram() {
		List<String> databases = new ArrayList<>();
		List<DiagramModel.Table> tables = new ArrayList<>();
		List<DiagramModel.Relation> relations = new ArrayList<>();

		for (ModelForeignKey key : foreignKeys) {
			relations.add(new DiagramModel.Relation(key.from().getName(), key.fromColumns(), key.to().getName(), key.toColumns(), key.name()));
		}

		for (Object object : modelobjects) {
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

package nl.errorsoft.esql.designer.model;

import nl.errorsoft.esql.designer.ui.CommentObject;
import nl.errorsoft.esql.designer.ui.DatabaseObject;
import nl.errorsoft.esql.designer.ui.Field;
import nl.errorsoft.esql.designer.ui.ModelObject;
import nl.errorsoft.esql.designer.ui.TableObject;

import java.util.*;
import java.awt.event.*;
import java.io.*;

public class Model implements MouseListener, MouseMotionListener {
	private String name = "";
	private String comment = "";
	private String author = "";
	private Vector modelobjects = new Vector();
	private final List<ForeignKey> foreignKeys = new ArrayList<>();
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

	public DatabaseObject createDatabaseObject(String name) {
		DatabaseObject db = new DatabaseObject(name, identifier);
		identifier++;
		db.addMouseListener(this);
		db.addMouseMotionListener(this);
		modelobjects.add(db);
		return db;
	}

	public TableObject createTableObject(String name) {
		TableObject tb = new TableObject(name, identifier);
		identifier++;
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		modelobjects.add(tb);
		return tb;
	}

	public CommentObject createCommentObject(String comment) {
		CommentObject cm = new CommentObject(comment, identifier);
		identifier++;
		cm.addMouseListener(this);
		cm.addMouseMotionListener(this);
		modelobjects.add(cm);
		return cm;
	}

	public void addReference(ModelObject src, ModelObject end) {
		if (src == end || (src instanceof DatabaseObject && end instanceof DatabaseObject) || (src instanceof TableObject && end instanceof TableObject)) {
			return;
		}

		Vector v = src.getReferences();
		for (int i = 0; i < v.size(); i++) {
			ModelObject mo = (ModelObject) v.get(i);
			if (mo == end) {
				return;
			}
		}

		v = end.getReferences();
		for (int i = 0; i < v.size(); i++) {
			ModelObject mo = (ModelObject) v.get(i);
			if (mo == src) {
				return;
			}
		}

		src.addReference(end);
	}

	/** Adds a foreign key between two tables, a key with the same name on the same table is replaced. */
	public void addForeignKey(ForeignKey key) {
		foreignKeys.removeIf(existing -> existing.from() == key.from() && existing.name().equals(key.name()));
		foreignKeys.add(key);
	}

	public void removeForeignKey(ForeignKey key) {
		foreignKeys.remove(key);
	}

	public List<ForeignKey> getForeignKeys() {
		return Collections.unmodifiableList(foreignKeys);
	}

	/** The keys the table has on other tables and the keys other tables have on it. */
	public List<ForeignKey> foreignKeysOf(TableObject table) {
		List<ForeignKey> keys = new ArrayList<>();

		for (ForeignKey key : foreignKeys) {
			if (key.involves(table)) {
				keys.add(key);
			}
		}
		return keys;
	}

	/** Keeps the foreign keys in step when a field of a table is renamed. */
	public void fieldRenamed(TableObject table, String oldName, String newName) {
		foreignKeys.replaceAll(key -> key.withColumnsRenamed(table, Map.of(oldName, newName)));
	}

	/** A foreign key that used a removed field can't exist any more, it is removed too. */
	public void fieldRemoved(TableObject table, String name) {
		foreignKeys.removeIf(key -> key.usesColumn(table, name));
	}

	/**
	 * Updates the foreign keys after the fields of a table were edited.
	 * @param namesBefore every field the table had before the edit, with the name it had then.
	 */
	public void fieldsEdited(TableObject table, Map<Field, String> namesBefore) {
		List<Field> now = Arrays.asList(table.getFields());
		Map<String, String> renames = new HashMap<>();

		for (Map.Entry<Field, String> before : namesBefore.entrySet()) {
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

	private void removeForeignKeysOf(ModelObject object) {
		if (object instanceof TableObject table) {
			foreignKeys.removeIf(key -> key.involves(table));
		}
	}

	public Vector getObjects() {
		return modelobjects;
	}

	public Vector getReferences(ModelObject m) {
		Vector refs = new Vector();
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelObject tmp = (ModelObject) modelobjects.get(i);
			if (tmp.getReferences().contains(m) && !(tmp instanceof CommentObject)) {
				refs.add(tmp);
			}
		}
		for (int i = 0; i < m.getReferences().size(); i++) {
			ModelObject tmp = (ModelObject) m.getReferences().get(i);
			if (!(tmp instanceof CommentObject)) {
				refs.add(tmp);
			}
		}
		return refs;
	}

	public Vector removeSelectedObjects() {
		Vector v = new Vector();
		if (!locked) {
			Vector sel = getSelectedObjects();
			for (int i = 0; i < sel.size(); i++) {
				ModelObject tmp = (ModelObject) sel.get(i);
				if (tmp.isSelected()) {
					for (int j = 0; j < modelobjects.size(); j++) {
						ModelObject tmp2 = (ModelObject) modelobjects.get(j);
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

	public void removeObject(ModelObject m) {
		if (!locked) {
			for (int j = 0; j < modelobjects.size(); j++) {
				ModelObject tmp2 = (ModelObject) modelobjects.get(j);
				if (m != tmp2) {
					tmp2.removeReference(m);
				}
			}
			modelobjects.remove(m);
			removeForeignKeysOf(m);
		}
	}

	public Vector getSelectedObjects() {
		Vector temp = new Vector();
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelObject tmp = (ModelObject) modelobjects.get(i);
			if (tmp.isSelected()) {
				temp.add(tmp);
			}
		}
		return temp;
	}

	public void deselectAll() {
		for (int i = 0; i < modelobjects.size(); i++) {
			((ModelObject) modelobjects.get(i)).setSelected(false);
		}
	}

	public void selectAll() {
		for (int i = 0; i < modelobjects.size(); i++) {
			((ModelObject) modelobjects.get(i)).setSelected(true);
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
			ModelObject tmp = (ModelObject) e.getSource();
			if (!e.isControlDown() && !tmp.isSelected()) {
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
			ModelObject tmp = (ModelObject) e.getSource();
			// A drag that starts on the icon of a field draws a foreign key, it does not move the table.
			if (tmp instanceof TableObject table && table.handleAt(table.xc, table.yc) >= 0) {
				return;
			}

			int xloc = tmp.getX() + (e.getX() - tmp.xc);
			int yloc = tmp.getY() + (e.getY() - tmp.yc);

			tmp.setLocation(xloc, yloc);

			int xadj = (e.getX() - tmp.xc);
			int yadj = (e.getY() - tmp.yc);
			for (int i = 0; i < modelobjects.size(); i++) {
				ModelObject mo = (ModelObject) modelobjects.get(i);
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
				ModelObject mo = (ModelObject) modelobjects.get(i);
				if (mo.getX() < x) {
					x = mo.getX();
				}
				if (mo.getY() < y) {
					y = mo.getY();
				}
			}
			if (x < 0 || y < 0) {
				for (int i = 0; i < modelobjects.size(); i++) {
					ModelObject mo = (ModelObject) modelobjects.get(i);
					mo.setLocation(mo.getX() + (-x) + 5, mo.getY() + (-y) + 5);
				}
			}
		}
	}

	public ModelObject getObjectByIdentifier(int identifier) {
		for (int i = 0; i < modelobjects.size(); i++) {
			ModelObject tmp = (ModelObject) modelobjects.get(i);
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

	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
}

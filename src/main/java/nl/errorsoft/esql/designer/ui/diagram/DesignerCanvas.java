package nl.errorsoft.esql.designer.ui.diagram;

import nl.errorsoft.esql.designer.ui.dialog.ForeignKeyDialog;
import nl.errorsoft.esql.designer.ui.dialog.GenerateDialog;
import nl.errorsoft.esql.designer.ui.dialog.DesignerPropertiesDialog;
import nl.errorsoft.esql.table.Table;

import nl.errorsoft.esql.database.Database;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import nl.errorsoft.esql.ui.util.ToolbarButtons;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import nl.errorsoft.esql.designer.control.DesignerCanvasController;
import nl.errorsoft.esql.designer.model.ForeignKey;
import nl.errorsoft.esql.designer.model.Model;

public class DesignerCanvas extends JLayeredPane implements MouseListener, MouseMotionListener, ActionListener, AWTEventListener { //	Model for this component
	private Model model;

	// DesignerCanvasController
	private DesignerCanvasController canvasController;

	// Variables for positioning
	private int srcx = 0;
	private int srcy = 0;

	private int xpos = 0;
	private int ypos = 0;

	private int w = 0;
	private int h = 0;

	private ModelObject src = null;
	private int refx = 0;
	private int refy = 0;

	private boolean placemode = false;
	private ModelObject place = null;

	private JMenu model_menu = new JMenu("Model");
	private JMenuItem create_database = new JMenuItem("Add New Database");
	private JMenuItem create_table = new JMenuItem("Add New Table");
	private JMenuItem create_comment = new JMenuItem("Add New Comment");
	private JMenuItem show_properties = new JMenuItem("Show Object Properties");
	private JMenuItem show_model_properties = new JMenuItem("Show Model Properties");

	private JMenuItem attach_table = new JMenuItem("Attach Table");
	private JMenuItem attach_comment = new JMenuItem("Attach Comment");

	private JToolBar toolbar = new JToolBar();
	private JButton btn_add_database = new JButton();
	private JButton btn_add_table = new JButton();
	private JButton btn_add_comment = new JButton();
	private JButton btn_properties = new JButton();

	private JButton btn_new = new JButton();
	private JButton btn_save = new JButton();
	private JButton btn_open = new JButton();

	private JButton btn_export = new JButton();

	// Whether the server has storage engines that tables show in their header
	private boolean showTableTypes = false;
	private boolean showGrid = true;

	// The foreign key connector that is selected or under the mouse, and the card under the mouse
	private ForeignKey selectedKey;
	private ForeignKey hoveredKey;
	private ModelObject hoveredObject;

	// A foreign key being dragged from a column row: the table, the column and the mouse in viewer coordinates
	private TableObject linkFrom;
	private String linkColumn;
	private Point linkPoint;

	/*
	 	DesignerCanvas default constructor
	 */
	public DesignerCanvas(DesignerCanvasController canvasController) {
		this.canvasController = canvasController;
		this.setLayout(null);
		this.setOpaque(true);
		model = new Model("New Model");

		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		model_menu.add(create_database);
		model_menu.add(create_table);
		model_menu.add(create_comment);
		model_menu.addSeparator();
		model_menu.add(attach_table);
		model_menu.add(attach_comment);
		model_menu.addSeparator();
		model_menu.add(show_properties);
		model_menu.add(show_model_properties);

		model_menu.setMnemonic('M');
		create_database.setMnemonic('D');
		create_table.setMnemonic('T');
		create_comment.setMnemonic('C');

		create_database.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		create_table.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		create_comment.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		attach_table.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		attach_comment.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		attach_table.setEnabled(false);
		attach_comment.setEnabled(false);
		show_properties.setEnabled(false);

		attach_table.addMouseListener(this);
		attach_comment.addMouseListener(this);
		create_database.addActionListener(this);
		create_table.addActionListener(this);
		create_comment.addActionListener(this);
		show_properties.addActionListener(this);
		show_model_properties.addActionListener(this);

		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);

		btn_add_database.setIcon(canvasController.getImageList().getIcon("add_database"));
		btn_add_table.setIcon(canvasController.getImageList().getIcon("add_table"));
		btn_add_comment.setIcon(canvasController.getImageList().getIcon("add_comment"));
		btn_properties.setIcon(canvasController.getImageList().getIcon("des_properties"));

		btn_new.setIcon(canvasController.getImageList().getIcon("des_new"));
		btn_save.setIcon(canvasController.getImageList().getIcon("des_save"));
		btn_open.setIcon(canvasController.getImageList().getIcon("des_open"));

		btn_export.setIcon(canvasController.getImageList().getIcon("des_check"));

		btn_add_database.setToolTipText("Add new database");
		btn_add_table.setToolTipText("Add new table");
		btn_add_comment.setToolTipText("Add new comment");

		btn_new.setToolTipText("New model");
		btn_save.setToolTipText("Save model");
		btn_open.setToolTipText("Open model");

		btn_add_database.addActionListener(this);
		btn_add_table.addActionListener(this);
		btn_add_comment.addActionListener(this);
		btn_properties.addActionListener(this);

		btn_new.addActionListener(this);
		btn_open.addActionListener(this);
		btn_save.addActionListener(this);

		btn_export.addActionListener(this);

		btn_export.setToolTipText("Generate model in database");
		btn_properties.setToolTipText("selected object/model properties");

		toolbar.add(btn_new);
		toolbar.add(btn_save);
		toolbar.add(btn_open);
		toolbar.addSeparator();
		toolbar.add(btn_add_database);
		toolbar.add(btn_add_table);
		toolbar.add(btn_add_comment);
		toolbar.addSeparator();
		toolbar.add(btn_properties);
		toolbar.addSeparator();
		toolbar.add(btn_export);
		ToolbarButtons.style(btn_new, btn_save, btn_open, btn_add_database, btn_add_table, btn_add_comment, btn_properties, btn_export);
	}

	/*
		Returns the model for this component
	*/
	public Model getModel() {
		return this.model;
	}

	/*
		Returns the toolbar for this component
	*/
	public JToolBar getToolbar() {
		return toolbar;
	}

	public boolean showsTableTypes() {
		return showTableTypes;
	}

	/** Tables show their storage engine only on servers that have them. */
	public void setShowTableTypes(boolean showTableTypes) {
		this.showTableTypes = showTableTypes;
		for (Component component : getComponents()) {
			if (component instanceof TableObject table) {
				table.reviewSize();
			}
		}
		repaint();
	}

	public boolean showsGrid() {
		return showGrid;
	}

	/** Fires the property "showGrid", so the View menu can follow a change made in the context menu. */
	public void setShowGrid(boolean showGrid) {
		boolean old = this.showGrid;
		this.showGrid = showGrid;
		firePropertyChange("showGrid", old, showGrid);
		repaint();
	}

	/** Places the tables with the automatic layout, referenced tables left of the tables that refer to them. */
	public void arrangeAutomatically() {
		ModelArranger.arrange(model);
		resize();
		repaint();
	}

	public void showModelProperties() {
		model.lock();
		canvasController.showModelPropertiesDialog(model);
		canvasController.updateTitle();
		model.unlock();
	}

	/*
		Function creates a new databaseobject
	*/
	public void createDatabaseObject(String name) {
		DatabaseObject db = model.createDatabaseObject(name);
		db.addMouseListener(this);
		db.addMouseMotionListener(this);
		this.enterPlaceMode(db);
	}

	/*
		Function creates a new tableobject
	*/
	public void createTableObject(String name) {
		TableObject tb = model.createTableObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		this.enterPlaceMode(tb);
	}

	/*
		Function creates a new tableobject, with reference from parent db
	*/
	public void createTableObject(String name, DatabaseObject db) {
		TableObject tb = model.createTableObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		model.addReference(db, tb);
		this.enterPlaceMode(tb);
	}

	/*
		Function creates a new commentobject, with reference to parentobject
	*/
	public void createCommentObject(String name, ModelObject obj) {
		CommentObject tb = model.createCommentObject(name);
		tb.addMouseListener(this);
		tb.addMouseMotionListener(this);
		model.addReference(tb, obj);
		this.enterPlaceMode(tb);
	}

	/*
		Function creates a new databaseobject
	*/
	public void createCommentObject(String name) {
		CommentObject cm = model.createCommentObject(name);
		cm.addMouseListener(this);
		cm.addMouseMotionListener(this);
		this.enterPlaceMode(cm);
	}

	public void enterPlaceMode(ModelObject mo) {
		this.place = mo;
		this.placemode = true;
		model.lock();
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.create_comment.setEnabled(false);
		this.create_table.setEnabled(false);
		this.create_database.setEnabled(false);

		this.btn_add_comment.setEnabled(false);
		this.btn_add_database.setEnabled(false);
		this.btn_add_table.setEnabled(false);
	}

	public void exitPlaceMode() {
		this.placemode = false;
		model.unlock();
		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		this.moveToFront(place);
		this.place = null;
		this.create_comment.setEnabled(true);
		this.create_table.setEnabled(true);
		this.create_database.setEnabled(true);

		this.btn_add_comment.setEnabled(true);
		this.btn_add_database.setEnabled(true);
		this.btn_add_table.setEnabled(true);
	}

	public void resetModel() {
		Model model = new Model("New Model");
		this.model = model;
		this.removeAll();
		this.repaint();
	}

	/*
		Function creates a new databaseobject
	*/
	public boolean isInPlaceMode() {
		return placemode;
	}

	/*
	 	Function to remove all selected objects from the model
	 */
	public void removeSelectedObjects() {
		List<ModelObject> objects = model.removeSelectedObjects();
		for (int i = 0; i < objects.size(); i++) {
			this.remove((ModelObject) objects.get(i));
		}
		this.repaint();
	}

	/*
	 	Function to get the menu for this model
	 */
	public JMenu getModelMenu() {
		return model_menu;
	}

	/*
	 	Paintcomponent method to paint selection rectangles
	 	and connections between components
	 */
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		paintCanvas(g2);
		markForeignKeyColumns();
		Graphics2D lines = (Graphics2D) g2.create();
		DesignerTheme.smooth(lines);

		for (Object object : model.getObjects()) {
			ModelObject from = (ModelObject) object;
			for (Object reference : from.getReferences()) {
				ModelObject to = (ModelObject) reference;
				if (!from.isHidden() && !to.isHidden()) {
					ConnectorRenderer.paintLink(lines, from, to);
				}
			}
		}

		for (ForeignKey key : model.getForeignKeys()) {
			if (!key.from().isHidden() && !key.to().isHidden()) {
				ConnectorRenderer.paint(lines, key, isHighlighted(key));
			}
		}

		lines.setColor(DesignerTheme.accent());
		lines.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5.0f, new float[]{3.0f}, 0.0f));
		if (w > 0 || h > 0) {
			lines.setColor(DesignerTheme.hover());
			lines.fillRect(xpos, ypos, w, h);
			lines.setColor(DesignerTheme.accent());
			lines.drawRect(xpos, ypos, w, h);
		}
		if (src != null) {
			lines.drawLine(src.getX() + (src.getWidth() / 2), src.getY() + (src.getHeight() / 2), refx, refy);
		}
		if (linkFrom != null && linkPoint != null) {
			ConnectorRenderer.paintGhost(lines, linkFrom, linkColumn, linkPoint);
		}
		lines.dispose();
	}

	/** A connector is drawn in the accent colour when it, or one of its tables, is selected or under the mouse. */
	private boolean isHighlighted(ForeignKey key) {
		return key == selectedKey || key == hoveredKey || key.from().isSelected() || key.to().isSelected() || key.from() == hoveredObject
			|| key.to() == hoveredObject;
	}

	/**
	 * Opens the foreign key dialog for a new key and adds the key to the model.
	 * @param to the referenced table, null to let the user choose (the dialog starts with the first other table).
	 */
	public void addForeignKey(TableObject from, String column, TableObject to, String toColumn) {
		TableObject parent = to != null ? to : firstOtherTable(from);
		String parentColumn = toColumn != null ? toColumn : ForeignKeyDialog.primaryColumn(parent);
		ForeignKey initial = new ForeignKey(from, column.isEmpty() ? List.of() : List.of(column), parent, column.isEmpty() ? List.of() : List.of(parentColumn),
			"",
			"", "");
		ForeignKey key = ForeignKeyDialog.edit(this, model, initial);
		if (key != null) {
			model.addForeignKey(key);
			selectedKey = key;
		}
		repaint();
	}

	public void editForeignKey(ForeignKey key) {
		if (key == null) {
			return;
		}
		ForeignKey edited = ForeignKeyDialog.edit(this, model, key);
		if (edited != null) {
			model.removeForeignKey(key);
			model.addForeignKey(edited);
			selectedKey = edited;
		}
		repaint();
	}

	public void removeForeignKey(ForeignKey key) {
		if (key != null) {
			model.removeForeignKey(key);
			if (selectedKey == key) {
				selectedKey = null;
			}
			hoveredKey = null;
			repaint();
		}
	}

	private TableObject firstOtherTable(TableObject table) {
		TableObject first = null;
		for (Object object : model.getObjects()) {
			if (object instanceof TableObject other) {
				if (other != table) {
					return other;
				}
				first = other;
			}
		}
		return first;
	}

	private static java.awt.Window windowOf(Component component) {
		return component instanceof java.awt.Window window ? window : SwingUtilities.getWindowAncestor(component);
	}

	private static String firstColumnOf(TableObject table) {
		Field[] fields = table.getFields();
		return fields.length == 0 ? "" : fields[0].getName();
	}

	/** Ends a foreign key drag: a drop on a table opens the dialog with the column under the mouse as the referenced column. */
	private void finishLink() {
		TableObject from = linkFrom;
		String column = linkColumn;
		Point point = linkPoint;
		linkFrom = null;
		linkColumn = null;
		linkPoint = null;
		repaint();

		if (point == null) {
			return;
		}
		for (Component component : getComponents()) {
			if (component instanceof TableObject table && table.isVisible() && table.cardBounds().contains(point)) {
				int row = table.rowAt(point.y - table.getY());
				String toColumn = row >= 0 ? table.getFields()[row].getName() : ForeignKeyDialog.primaryColumn(table);
				addForeignKey(from, column, table, toColumn);
				return;
			}
		}
	}

	/** The connector under a point of the viewer, the one painted last wins. */
	private ForeignKey connectorAt(Point point) {
		List<ForeignKey> keys = model.getForeignKeys();
		for (int i = keys.size() - 1; i >= 0; i--) {
			if (ConnectorRenderer.hit(keys.get(i), point)) {
				return keys.get(i);
			}
		}
		return null;
	}

	private void paintCanvas(Graphics2D g2) {
		Rectangle area = g2.getClipBounds() != null ? g2.getClipBounds() : new Rectangle(0, 0, getWidth(), getHeight());
		g2.setColor(DesignerTheme.canvas());
		g2.fill(area);

		if (showGrid) {
			int step = 20;
			g2.setColor(DesignerTheme.grid());
			for (int x = area.x - area.x % step; x < area.x + area.width; x += step) {
				for (int y = area.y - area.y % step; y < area.y + area.height; y += step) {
					g2.fillRect(x, y, 1, 1);
				}
			}
		}
	}

	/** Columns that are part of a foreign key get the link icon in their table. */
	private void markForeignKeyColumns() {
		java.util.Map<TableObject, java.util.Set<String>> columns = new java.util.HashMap<>();
		for (nl.errorsoft.esql.designer.model.ForeignKey key : model.getForeignKeys()) {
			columns.computeIfAbsent(key.from(), table -> new java.util.HashSet<>()).addAll(key.fromColumns());
		}
		for (Object object : model.getObjects()) {
			if (object instanceof TableObject table) {
				table.setForeignKeyColumns(columns.getOrDefault(table, java.util.Set.of()));
			}
		}
	}

	public void resize() {
		Component[] comps = this.getComponents();
		int w = 0;
		int h = 0;
		for (int i = 0; i < comps.length; i++) {
			if (comps[i].getX() + comps[i].getWidth() > w) {
				w = comps[i].getX() + comps[i].getWidth();
			}
			if (comps[i].getY() + comps[i].getHeight() > h) {
				h = comps[i].getY() + comps[i].getHeight();
			}
		}
		if (this.getWidth() != w || this.getHeight() != h) {
			this.setPreferredSize(new Dimension(w + 25, h + 25));
			this.revalidate();
		}
	}

	public void setModel(Model model) {
		this.removeAll();

		this.model = model;

		for (int i = 0; i < model.getObjects().size(); i++) {
			ModelObject mo = (ModelObject) model.getObjects().get(i);

			mo.addMouseListener(this);
			mo.addMouseMotionListener(this);

			if (mo instanceof DatabaseObject object) {
				this.add(object);
			}
			if (mo instanceof CommentObject object1) {
				this.add(object1);
			}
			if (mo instanceof TableObject object2) {
				this.add(object2);
			}
		}

		this.repaint();
	}

	public void automateMenus() {
		if (model.getSelectedObjects().size() == 1 && model.getSelectedObjects().get(0) instanceof DatabaseObject) {
			attach_table.setEnabled(true);
		} else {
			attach_table.setEnabled(false);
		}

		if (model.getSelectedObjects().size() == 1) {
			if (!(model.getSelectedObjects().get(0) instanceof CommentObject)) {
				show_properties.setEnabled(true);
			}

			attach_comment.setEnabled(true);
		} else {
			show_properties.setEnabled(false);
			attach_comment.setEnabled(false);
		}
	}

	public boolean showProperties() {
		model.lock();
		List<ModelObject> v = this.getModel().getSelectedObjects();
		if (v.size() == 1 && (v.get(0) instanceof DatabaseObject || v.get(0) instanceof TableObject)) {
			canvasController.showPropertiesDialog(model.getSelectedObjects());
			model.unlock();
			return true;
		} else {
			model.unlock();
			return false;
		}
	}

	/*
	 *
	 *	Start mouselisteners
	 *
	 */

	public void mousePressed(MouseEvent e) {
		if (!placemode) {
			this.moveToFront((Component) e.getSource());
			if (e.getSource() instanceof ModelObject) {
				selectedKey = null;
			}
			if (e.getSource() instanceof TableObject table && !e.isShiftDown() && !e.isMetaDown() && !e.isPopupTrigger()
				&& table.handleAt(e.getX(), e.getY()) >= 0) {
				linkFrom = table;
				linkColumn = table.getFields()[table.handleAt(e.getX(), e.getY())].getName();
				linkPoint = null;
			}
			if (e.getSource() instanceof DesignerCanvas) {
				model.deselectAll();
				src = null;
				selectedKey = connectorAt(e.getPoint());
				if (selectedKey != null) {
					requestFocusInWindow();
				}
			} else if (e.isShiftDown() && e.getSource() instanceof ModelObject) {
				src = (ModelObject) e.getSource();
				refx = src.getX() + e.getX();
				refy = src.getY() + e.getY();
			}

			this.srcx = e.getX();
			this.srcy = e.getY();

			this.repaint();
		}
		this.repaint();

		this.automateMenus();
		showContextMenu(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (linkFrom != null) {
			finishLink();
			this.resize();
			return;
		}
		if (!placemode) {
			if (e.getSource() instanceof JMenuItem) {
				JMenuItem tmp = (JMenuItem) e.getSource();
				if (tmp == attach_table && attach_table.isEnabled()) {
					List<ModelObject> v = this.getModel().getSelectedObjects();
					if (v.size() == 1 && v.get(0) instanceof DatabaseObject) {
						this.createTableObject("New Table", (DatabaseObject) v.get(0));
					}
				} else if (tmp == attach_comment && attach_comment.isEnabled()) {
					List<ModelObject> v = this.getModel().getSelectedObjects();
					if (v.size() == 1 && v.get(0) instanceof ModelObject) {
						this.createCommentObject("New Table", (ModelObject) v.get(0));
					}
				}
			}

			if (e.getSource() instanceof DesignerCanvas) {
				Rectangle rect = new Rectangle(xpos, ypos, w, h);
				Component[] cmps = this.getComponents();
				for (int i = 0; i < cmps.length; i++) {
					ModelObject tmp = (ModelObject) cmps[i];
					if (rect.contains(tmp.getLocation())) {
						tmp.setSelected(true);
					} else {
						tmp.setSelected(false);
					}
				}
				srcx = 0;
				srcy = 0;
				w = 0;
				h = 0;
			} else {
				if (e.isShiftDown()) {
					ModelObject end = (ModelObject) e.getSource();
					int xloc = end.getX() + e.getX();
					int yloc = end.getY() + e.getY();

					if (this.getComponentAt(xloc, yloc) instanceof ModelObject) {
						end = (ModelObject) this.getComponentAt(xloc, yloc);
					}

					if (src != null && end != null) {
						model.addReference(src, end);
					}
				}
			}

			src = null;
		} else if (placemode && e.getSource() == this) {
			place.setHidden(false);
			this.add(place);
			this.place.setCardLocation(e.getX(), e.getY());
			this.exitPlaceMode();
		}

		this.resize();
		this.repaint();

		this.automateMenus();
		if (!placemode) {
			showContextMenu(e);
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (linkFrom != null) {
			Component source = (Component) e.getSource();
			linkPoint = new Point(source.getX() + e.getX(), source.getY() + e.getY());
			repaint();
			return;
		}
		if (!placemode) {
			if (!e.isMetaDown()) {
				if (e.isShiftDown() && src != null) {
					refx = src.getX() + e.getX();
					refy = src.getY() + e.getY();
				}
				if (e.getSource() instanceof DesignerCanvas) {
					w = e.getX() - srcx;
					h = e.getY() - srcy;

					if (w < 0) {
						xpos = e.getX();
						w = srcx - xpos;
					} else {
						xpos = srcx;
					}
					if (h < 0) {
						ypos = e.getY();
						h = srcy - ypos;
					} else {
						ypos = srcy;
					}
				}
			}
		}
		this.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == create_database || e.getSource() == btn_add_database) {
			this.createDatabaseObject("New Database");
		} else if (e.getSource() == create_table || e.getSource() == btn_add_table) {
			this.createTableObject("New Table");
		} else if (e.getSource() == create_comment || e.getSource() == btn_add_comment) {
			this.createCommentObject("New Comment");
		} else if (e.getSource() == btn_open) {
			canvasController.openModel();
		} else if (e.getSource() == btn_save) {
			canvasController.saveModel();
		} else if (e.getSource() == btn_new) {
			canvasController.newModel();
		} else if (e.getSource() == btn_export) {
			canvasController.generate();
		} else if (e.getSource() == show_properties || e.getSource() == btn_properties) {
			boolean b = this.showProperties();
			if (e.getSource() == btn_properties && !b) {
				model.lock();
				canvasController.showModelPropertiesDialog(this.getModel());
				canvasController.updateTitle();
				model.unlock();
			}
		} else if (e.getSource() == show_model_properties) {
			showModelProperties();
		}
	}

	/*
	 *
	 *	Context menus, built for the object, connector or empty spot under the mouse with only the items that apply.
	 *
	 */

	/** The popup trigger comes on press on macOS (also ctrl-click) and Linux, on release on Windows. */
	private void showContextMenu(MouseEvent e) {
		if (!e.isPopupTrigger() || placemode) {
			return;
		}
		Component source = (Component) e.getSource();
		JPopupMenu menu;

		if (source instanceof ModelObject object) {
			if (!object.isSelected()) {
				model.deselectAll();
				object.setSelected(true);
			}
			menu = switch (object) {
				case TableObject table -> tableMenu(table);
				case DatabaseObject database -> databaseMenu(database);
				case CommentObject note -> noteMenu(note);
				default -> null;
			};
		} else if (source == this) {
			menu = selectedKey != null ? connectorMenu(selectedKey) : canvasMenu(e.getPoint());
		} else {
			return;
		}

		if (menu != null) {
			repaint();
			automateMenus();
			menu.show(source, e.getX(), e.getY());
		}
	}

	JPopupMenu canvasMenu(Point point) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Add Database", "add_database", () -> placeAt(model.createDatabaseObject("New Database"), point)));
		menu.add(item("Add Table", "add_table", () -> placeAt(model.createTableObject("New Table"), point)));
		menu.add(item("Add Note", "add_comment", () -> placeAt(model.createCommentObject("New Comment"), point)));
		menu.addSeparator();
		if (!model.getObjects().isEmpty()) {
			menu.add(item("Select All", null, () -> {
				model.selectAll();
				repaint();
			}));
		}
		if (model.getObjects().stream().anyMatch(object -> object instanceof TableObject)) {
			menu.add(item("Arrange Automatically", null, this::arrangeAutomatically));
		}
		JCheckBoxMenuItem grid = new JCheckBoxMenuItem("Show Grid", showGrid);
		grid.addActionListener(e -> setShowGrid(grid.isSelected()));
		menu.add(grid);
		menu.addSeparator();
		menu.add(item("Model Properties...", "des_properties", this::showModelProperties));
		return menu;
	}

	JPopupMenu tableMenu(TableObject table) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Properties...", "des_properties", this::showProperties));
		menu.add(item("Add Foreign Key...", "linkimg", () -> addForeignKey(table, firstColumnOf(table), null, null)));

		List<ModelObject> linked = linkedObjects(table);
		JMenu link = new JMenu("Link to Database");
		for (Object object : model.getObjects()) {
			if (object instanceof DatabaseObject database && !linked.contains(database)) {
				link.add(item(database.getName(), null, () -> {
					model.addReference(database, table);
					repaint();
				}));
			}
		}
		if (link.getItemCount() > 0) {
			menu.add(link);
		}
		addObjectItems(menu, table, linked);
		return menu;
	}

	JPopupMenu databaseMenu(DatabaseObject database) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Properties...", "des_properties", this::showProperties));
		menu.add(item("Add Table to This Database", "add_table", () -> {
			TableObject table = model.createTableObject("New Table");
			model.addReference(database, table);
			Rectangle card = database.cardBounds();
			placeAt(table, new Point(card.x, card.y + card.height + 40));
		}));
		addObjectItems(menu, database, linkedObjects(database));
		return menu;
	}

	JPopupMenu noteMenu(CommentObject note) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Edit", "des_properties", note::startEditing));
		addObjectItems(menu, note, linkedObjects(note));
		return menu;
	}

	JPopupMenu connectorMenu(ForeignKey key) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Edit Foreign Key...", "des_properties", () -> editForeignKey(key)));
		menu.add(item("Remove Foreign Key", null, () -> removeForeignKey(key)));
		return menu;
	}

	/** What every card has: a note attached to it, removing its links (when it has any), and delete. */
	private void addObjectItems(JPopupMenu menu, ModelObject object, List<ModelObject> linked) {
		if (!(object instanceof CommentObject)) {
			menu.add(item("Add Note", "add_comment", () -> {
				CommentObject note = model.createCommentObject("New Comment");
				model.addReference(note, object);
				Rectangle card = object.cardBounds();
				placeAt(note, new Point(card.x + card.width + 40, card.y));
			}));
		}
		if (!linked.isEmpty()) {
			JMenu unlink = new JMenu("Remove Link");
			for (ModelObject other : linked) {
				String name = other instanceof CommentObject ? "Note" : other.getName();
				unlink.add(item(name, null, () -> {
					other.removeReference(object);
					object.removeReference(other);
					repaint();
				}));
			}
			menu.add(unlink);
		}
		menu.addSeparator();
		int count = model.getSelectedObjects().size();
		menu.add(item(count > 1 ? "Delete " + count + " Objects" : "Delete", null, this::removeSelectedObjects));
	}

	/** The cards an object is linked to with a database or note link, in both directions. */
	private List<ModelObject> linkedObjects(ModelObject object) {
		List<ModelObject> linked = new java.util.ArrayList<>();
		for (Object other : model.getObjects()) {
			ModelObject candidate = (ModelObject) other;
			if (candidate != object && (candidate.getReferences().contains(object) || object.getReferences().contains(candidate))) {
				linked.add(candidate);
			}
		}
		return linked;
	}

	private JMenuItem item(String text, String icon, Runnable action) {
		JMenuItem item = new JMenuItem(text);
		if (icon != null) {
			item.setIcon(canvasController.getImageList().getIcon(icon));
		}
		item.addActionListener(e -> action.run());
		return item;
	}

	/** Adds a new card with its top left corner at a point of the viewer and selects it. */
	private void placeAt(ModelObject object, Point point) {
		object.addMouseListener(this);
		object.addMouseMotionListener(this);
		object.setHidden(false);
		add(object);
		object.setCardLocation(Math.max(0, point.x), Math.max(0, point.y));
		moveToFront(object);
		model.deselectAll();
		object.setSelected(true);
		resize();
		repaint();
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == this && e.getClickCount() == 2 && !e.isMetaDown()) {
			editForeignKey(connectorAt(e.getPoint()));
		}
	}
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() instanceof ModelObject object) {
			hoveredObject = object;
			hoveredKey = null;
			repaint();
		}
	}

	public void mouseExited(MouseEvent e) {
		if (e.getSource() == hoveredObject) {
			hoveredObject = null;
			repaint();
		}
	}

	public void mouseMoved(MouseEvent e) {
		if (e.getSource() == this) {
			ForeignKey key = connectorAt(e.getPoint());
			if (key != hoveredKey) {
				hoveredKey = key;
				setToolTipText(key == null ? null : key.name());
				repaint();
			}
		}
	}

	public void eventDispatched(AWTEvent event) {
		KeyEvent e = (KeyEvent) event;
		// Keys typed in another window (a dialog of the designer, the main window) are not meant for the model.
		if (e.getComponent() == null || windowOf(e.getComponent()) != windowOf(this)) {
			return;
		}
		if (e.getID() == 401) {
			if (e.isControlDown() && e.getKeyCode() == e.VK_A) {
				this.getModel().selectAll();
			}
			if (e.isControlDown() && e.getKeyCode() == e.VK_D) {
				this.getModel().deselectAll();
			}
			if (e.isControlDown() && e.getKeyCode() == e.VK_O) {
				canvasController.openModel();
			}
			if (e.getKeyCode() == e.VK_DELETE) {
				if (selectedKey != null) {
					removeForeignKey(selectedKey);
				} else {
					this.removeSelectedObjects();
				}
			}
			if (e.isControlDown() && e.getKeyCode() == e.VK_S) {
				canvasController.saveModel(true);
			}
			if (e.isControlDown() && e.getKeyCode() == e.VK_N) {
				canvasController.newModel();
			}
			if (e.getKeyCode() == e.VK_F4) {
				List<ModelObject> v = this.getModel().getSelectedObjects();
				if (v.size() == 1 && v.get(0) instanceof DatabaseObject) {
					this.createTableObject("New Table", (DatabaseObject) v.get(0));
				}
			}
			if (e.getKeyCode() == e.VK_F5) {
				List<ModelObject> v = this.getModel().getSelectedObjects();
				if (v.size() == 1 && v.get(0) instanceof ModelObject) {
					this.createCommentObject("New Table", (ModelObject) v.get(0));
				}
			}
		}
	}
}

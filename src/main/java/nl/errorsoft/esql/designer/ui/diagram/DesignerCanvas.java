package nl.errorsoft.esql.designer.ui.diagram;

import nl.errorsoft.esql.ui.util.MouseClicks;

import nl.errorsoft.esql.designer.ui.dialog.ForeignKeyDialog;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import nl.errorsoft.esql.ui.util.ToolbarButtons;
import javax.swing.KeyStroke;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.designer.control.DesignerCanvasController;
import nl.errorsoft.esql.designer.model.ModelForeignKey;
import nl.errorsoft.esql.designer.model.Model;

public class DesignerCanvas extends JLayeredPane implements MouseListener, MouseMotionListener, ActionListener { //	Model for this component
	private Model model;

	// DesignerCanvasController
	private final DesignerCanvasController canvasController;

	// Variables for positioning
	private int srcx = 0;
	private int srcy = 0;

	private int xpos = 0;
	private int ypos = 0;

	private int w = 0;
	private int h = 0;

	private ModelCard linkSource = null;
	private int refx = 0;
	private int refy = 0;

	private boolean placemode = false;
	private ModelCard place = null;

	private JMenu model_menu = new JMenu("Model");
	private JMenuItem create_database = new JMenuItem("Add database");
	private JMenuItem create_table = new JMenuItem("Add table");
	private JMenuItem create_comment = new JMenuItem("Add note");
	private JMenuItem show_properties = new JMenuItem("Show object properties");
	private JMenuItem show_model_properties = new JMenuItem("Show model properties");

	private JMenuItem attach_table = new JMenuItem("Attach table");
	private JMenuItem attach_comment = new JMenuItem("Attach note");

	private JToolBar toolbar = new JToolBar();
	private JButton addDatabaseButton = new JButton();
	private JButton addTableButton = new JButton();
	private JButton addCommentButton = new JButton();
	private JButton propertiesButton = new JButton();

	private JButton newButton = new JButton();
	private JButton saveButton = new JButton();
	private JButton openButton = new JButton();

	private JButton exportButton = new JButton();

	// Whether the server has storage engines that tables show in their header
	private boolean showTableTypes = false;
	private boolean showGrid = true;

	// The foreign key connector that is selected or under the mouse, and the card under the mouse
	private ModelForeignKey selectedKey;
	private ModelForeignKey hoveredKey;
	private ModelCard hoveredObject;

	// A foreign key being dragged from a column row: the table, the column and the mouse in viewer coordinates
	private TableCard linkFrom;
	private String linkColumn;
	private Point linkPoint;

	private final java.beans.PropertyChangeListener selectionListener = e -> automateMenus();

	/*
	 	DesignerCanvas default constructor
	 */
	public DesignerCanvas(DesignerCanvasController canvasController) {
		this.canvasController = canvasController;
		this.setLayout(null);
		this.setOpaque(true);
		model = new Model("New model");

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
		create_comment.setMnemonic('N');
		attach_table.setMnemonic('A');
		attach_comment.setMnemonic('C');
		show_properties.setMnemonic('S');
		show_model_properties.setMnemonic('M');

		create_database.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		create_table.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		create_comment.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		attach_table.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		attach_comment.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		attach_table.setEnabled(false);
		attach_comment.setEnabled(false);
		show_properties.setEnabled(false);

		attach_table.addActionListener(e -> attachTable());
		attach_comment.addActionListener(e -> attachNote());
		create_database.addActionListener(this);
		create_table.addActionListener(this);
		create_comment.addActionListener(this);
		show_properties.addActionListener(this);
		show_model_properties.addActionListener(this);

		addDatabaseButton.setIcon(ApplicationContext.get().imageLoader().getIcon("add_database"));
		addTableButton.setIcon(ApplicationContext.get().imageLoader().getIcon("add_table"));
		addCommentButton.setIcon(ApplicationContext.get().imageLoader().getIcon("add_comment"));
		propertiesButton.setIcon(ApplicationContext.get().imageLoader().getIcon("des_properties"));

		newButton.setIcon(ApplicationContext.get().imageLoader().getIcon("des_new"));
		saveButton.setIcon(ApplicationContext.get().imageLoader().getIcon("des_save"));
		openButton.setIcon(ApplicationContext.get().imageLoader().getIcon("des_open"));

		exportButton.setIcon(ApplicationContext.get().imageLoader().getIcon("des_check"));

		addDatabaseButton.setToolTipText("Add database");
		addTableButton.setToolTipText("Add table");
		addCommentButton.setToolTipText("Add note");

		newButton.setToolTipText("New model");
		saveButton.setToolTipText("Save model");
		openButton.setToolTipText("Open model");

		addDatabaseButton.addActionListener(this);
		addTableButton.addActionListener(this);
		addCommentButton.addActionListener(this);
		propertiesButton.addActionListener(this);

		newButton.addActionListener(this);
		openButton.addActionListener(this);
		saveButton.addActionListener(this);

		exportButton.addActionListener(this);

		exportButton.setToolTipText("Generate model in database");
		propertiesButton.setToolTipText("Properties of the selected object or the model");

		toolbar.add(newButton);
		toolbar.add(saveButton);
		toolbar.add(openButton);
		toolbar.addSeparator();
		toolbar.add(addDatabaseButton);
		toolbar.add(addTableButton);
		toolbar.add(addCommentButton);
		toolbar.addSeparator();
		toolbar.add(propertiesButton);
		toolbar.addSeparator();
		toolbar.add(exportButton);
		ToolbarButtons.style(newButton, saveButton, openButton, addDatabaseButton, addTableButton, addCommentButton, propertiesButton, exportButton);
	}

	/** Every card on the canvas keeps the Model menu (and its F4/F5 shortcuts) in step with the selection. */
	@Override
	protected void addImpl(Component component, Object constraints, int index) {
		super.addImpl(component, constraints, index);
		if (component instanceof ModelCard object) {
			object.removePropertyChangeListener("selected", selectionListener);
			object.addPropertyChangeListener("selected", selectionListener);
		}
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
			if (component instanceof TableCard table) {
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
		try {
			canvasController.showModelPropertiesDialog(model);
			canvasController.updateTitle();
		} finally {
			model.unlock();
		}
	}

	/*
		Function creates a new databaseobject
	*/
	public void createDatabaseCard(String name) {
		DatabaseCard db = model.createDatabaseCard(name);
		db.addMouseListener(this);
		db.addMouseMotionListener(this);
		this.enterPlaceMode(db);
	}

	/*
		Function creates a new tableobject
	*/
	public void createTableCard(String name) {
		TableCard table = model.createTableCard(name);
		table.addMouseListener(this);
		table.addMouseMotionListener(this);
		this.enterPlaceMode(table);
	}

	/*
		Function creates a new tableobject, with reference from parent db
	*/
	public void createTableCard(String name, DatabaseCard db) {
		TableCard table = model.createTableCard(name);
		table.addMouseListener(this);
		table.addMouseMotionListener(this);
		model.addReference(db, table);
		this.enterPlaceMode(table);
	}

	/*
		Function creates a new commentobject, with reference to parentobject
	*/
	public void createNoteCard(String name, ModelCard target) {
		NoteCard note = model.createNoteCard(name);
		note.addMouseListener(this);
		note.addMouseMotionListener(this);
		model.addReference(note, target);
		this.enterPlaceMode(note);
	}

	/*
		Function creates a new databaseobject
	*/
	public void createNoteCard(String name) {
		NoteCard note = model.createNoteCard(name);
		note.addMouseListener(this);
		note.addMouseMotionListener(this);
		this.enterPlaceMode(note);
	}

	public void enterPlaceMode(ModelCard card) {
		this.place = card;
		this.placemode = true;
		model.lock();
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.create_comment.setEnabled(false);
		this.create_table.setEnabled(false);
		this.create_database.setEnabled(false);

		this.addCommentButton.setEnabled(false);
		this.addDatabaseButton.setEnabled(false);
		this.addTableButton.setEnabled(false);
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

		this.addCommentButton.setEnabled(true);
		this.addDatabaseButton.setEnabled(true);
		this.addTableButton.setEnabled(true);
	}

	public void resetModel() {
		Model model = new Model("New model");
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
		List<ModelCard> selected = model.getSelectedObjects();
		if (selected.isEmpty() || !Dialogs.confirmDestructive(this, "Delete from model", deleteMessage(selected), "Delete")) {
			return;
		}
		List<ModelCard> objects = model.removeSelectedObjects();
		for (int i = 0; i < objects.size(); i++) {
			this.remove((ModelCard) objects.get(i));
		}
		this.repaint();
	}

	/** What the confirmation of a delete says: the object by name, and the foreign keys that go with a table. */
	private String deleteMessage(List<ModelCard> selected) {
		if (selected.size() > 1) {
			return "Delete " + selected.size() + " objects from the model?";
		}
		return switch (selected.get(0)) {
			case TableCard table when !model.foreignKeysOf(table).isEmpty() -> "Delete table '" + table.getName() + "' and its foreign keys from the model?";
			case TableCard table -> "Delete table '" + table.getName() + "' from the model?";
			case DatabaseCard database -> "Delete database '" + database.getName() + "' from the model?";
			default -> "Delete this note from the model?";
		};
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
			ModelCard from = (ModelCard) object;
			for (Object reference : from.getReferences()) {
				ModelCard to = (ModelCard) reference;
				if (!from.isHidden() && !to.isHidden()) {
					ConnectorPainter.paintLink(lines, from, to);
				}
			}
		}

		for (ModelForeignKey key : model.getForeignKeys()) {
			if (!key.from().isHidden() && !key.to().isHidden()) {
				ConnectorPainter.paint(lines, key, isHighlighted(key));
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
		if (linkSource != null) {
			lines.drawLine(linkSource.getX() + (linkSource.getWidth() / 2), linkSource.getY() + (linkSource.getHeight() / 2), refx, refy);
		}
		if (linkFrom != null && linkPoint != null) {
			ConnectorPainter.paintGhost(lines, linkFrom, linkColumn, linkPoint);
		}
		lines.dispose();
	}

	/** A connector is drawn in the accent colour when it, or one of its tables, is selected or under the mouse. */
	private boolean isHighlighted(ModelForeignKey key) {
		return key == selectedKey || key == hoveredKey || key.from().isSelected() || key.to().isSelected() || key.from() == hoveredObject
			|| key.to() == hoveredObject;
	}

	/**
	 * Opens the foreign key dialog for a new key and adds the key to the model.
	 * @param to the referenced table, null to let the user choose (the dialog starts with the first other table).
	 */
	public void addForeignKey(TableCard from, String column, TableCard to, String toColumn) {
		TableCard parent = to != null ? to : firstOtherTable(from);
		String parentColumn = toColumn != null ? toColumn : ForeignKeyDialog.primaryColumn(parent);
		ModelForeignKey initial = new ModelForeignKey(from, column.isEmpty() ? List.of() : List.of(column), parent,
			column.isEmpty() ? List.of() : List.of(parentColumn),
			"",
			"", "");
		ModelForeignKey key = ForeignKeyDialog.edit(this, model, initial);
		if (key != null) {
			model.addForeignKey(key);
			selectedKey = key;
		}
		repaint();
	}

	public void editForeignKey(ModelForeignKey key) {
		if (key == null) {
			return;
		}
		ModelForeignKey edited = ForeignKeyDialog.edit(this, model, key);
		if (edited != null) {
			model.removeForeignKey(key);
			model.addForeignKey(edited);
			selectedKey = edited;
		}
		repaint();
	}

	public void removeForeignKey(ModelForeignKey key) {
		if (key != null && Dialogs.confirmDestructive(this, "Remove foreign key", "Remove foreign key '" + key.name() + "' from the model?", "Remove")) {
			model.removeForeignKey(key);
			if (selectedKey == key) {
				selectedKey = null;
			}
			hoveredKey = null;
			repaint();
		}
	}

	private TableCard firstOtherTable(TableCard table) {
		TableCard first = null;
		for (Object object : model.getObjects()) {
			if (object instanceof TableCard other) {
				if (other != table) {
					return other;
				}
				first = other;
			}
		}
		return first;
	}

	private static String firstColumnOf(TableCard table) {
		DesignerColumn[] fields = table.getFields();
		return fields.length == 0 ? "" : fields[0].getName();
	}

	/** Ends a foreign key drag: a drop on a table opens the dialog with the column under the mouse as the referenced column. */
	private void finishLink() {
		TableCard from = linkFrom;
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
			if (component instanceof TableCard table && table.isVisible() && table.cardBounds().contains(point)) {
				int row = table.rowAt(point.y - table.getY());
				String toColumn = row >= 0 ? table.getFields()[row].getName() : ForeignKeyDialog.primaryColumn(table);
				addForeignKey(from, column, table, toColumn);
				return;
			}
		}
	}

	/** The connector under a point of the viewer, the one painted last wins. */
	private ModelForeignKey connectorAt(Point point) {
		List<ModelForeignKey> keys = model.getForeignKeys();
		for (int i = keys.size() - 1; i >= 0; i--) {
			if (ConnectorPainter.hit(keys.get(i), point)) {
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
		java.util.Map<TableCard, java.util.Set<String>> columns = new java.util.HashMap<>();
		for (ModelForeignKey key : model.getForeignKeys()) {
			columns.computeIfAbsent(key.from(), table -> new java.util.HashSet<>()).addAll(key.fromColumns());
		}
		for (Object object : model.getObjects()) {
			if (object instanceof TableCard table) {
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
			ModelCard card = (ModelCard) model.getObjects().get(i);

			card.addMouseListener(this);
			card.addMouseMotionListener(this);

			if (card instanceof DatabaseCard object) {
				this.add(object);
			}
			if (card instanceof NoteCard object1) {
				this.add(object1);
			}
			if (card instanceof TableCard object2) {
				this.add(object2);
			}
		}

		this.repaint();
	}

	/** Enables the items of the Model menu that apply to the selection: a single database can get a table, any single card a note. */
	public void automateMenus() {
		List<ModelCard> selected = model.getSelectedObjects();
		ModelCard single = selected.size() == 1 ? selected.get(0) : null;
		attach_table.setEnabled(single instanceof DatabaseCard);
		attach_comment.setEnabled(single != null);
		show_properties.setEnabled(single != null && !(single instanceof NoteCard));
	}

	public boolean showProperties() {
		model.lock();
		try {
			List<ModelCard> selected = this.getModel().getSelectedObjects();
			if (selected.size() == 1 && (selected.get(0) instanceof DatabaseCard || selected.get(0) instanceof TableCard)) {
				canvasController.showPropertiesDialog(model.getSelectedObjects());
				return true;
			}
			return false;
		} finally {
			model.unlock();
		}
	}

	/** Model > Attach table (F4): a new table linked to the selected database. */
	private void attachTable() {
		List<ModelCard> selected = this.getModel().getSelectedObjects();
		if (!placemode && selected.size() == 1 && selected.get(0) instanceof DatabaseCard database) {
			this.createTableCard("New table", database);
		}
	}

	/** Model > Attach note (F5): a new note linked to the selected object. */
	private void attachNote() {
		List<ModelCard> selected = this.getModel().getSelectedObjects();
		if (!placemode && selected.size() == 1) {
			this.createNoteCard("New note", selected.get(0));
		}
	}

	/** Edit > Delete selected (Delete): the selected connector, or else the selected cards. */
	public void deleteSelection() {
		if (selectedKey != null) {
			removeForeignKey(selectedKey);
		} else {
			removeSelectedObjects();
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
			if (e.getSource() instanceof ModelCard) {
				selectedKey = null;
			}
			if (e.getSource() instanceof TableCard table && !e.isShiftDown() && !e.isMetaDown() && !e.isPopupTrigger()
				&& table.handleAt(e.getX(), e.getY()) >= 0) {
				linkFrom = table;
				linkColumn = table.getFields()[table.handleAt(e.getX(), e.getY())].getName();
				linkPoint = null;
			}
			if (e.getSource() instanceof DesignerCanvas) {
				model.deselectAll();
				linkSource = null;
				selectedKey = connectorAt(e.getPoint());
				if (selectedKey != null) {
					requestFocusInWindow();
				}
			} else if (e.isShiftDown() && e.getSource() instanceof ModelCard) {
				linkSource = (ModelCard) e.getSource();
				refx = linkSource.getX() + e.getX();
				refy = linkSource.getY() + e.getY();
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
			if (e.getSource() instanceof DesignerCanvas) {
				Rectangle rect = new Rectangle(xpos, ypos, w, h);
				Component[] components = this.getComponents();
				for (int i = 0; i < components.length; i++) {
					ModelCard card = (ModelCard) components[i];
					if (rect.contains(card.getLocation())) {
						card.setSelected(true);
					} else {
						card.setSelected(false);
					}
				}
				srcx = 0;
				srcy = 0;
				w = 0;
				h = 0;
			} else {
				if (e.isShiftDown()) {
					ModelCard linkTarget = (ModelCard) e.getSource();
					int xloc = linkTarget.getX() + e.getX();
					int yloc = linkTarget.getY() + e.getY();

					if (this.getComponentAt(xloc, yloc) instanceof ModelCard) {
						linkTarget = (ModelCard) this.getComponentAt(xloc, yloc);
					}

					if (linkSource != null && linkTarget != null) {
						model.addReference(linkSource, linkTarget);
					}
				}
			}

			linkSource = null;
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
				if (e.isShiftDown() && linkSource != null) {
					refx = linkSource.getX() + e.getX();
					refy = linkSource.getY() + e.getY();
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
		if (e.getSource() == create_database || e.getSource() == addDatabaseButton) {
			this.createDatabaseCard("New database");
		} else if (e.getSource() == create_table || e.getSource() == addTableButton) {
			this.createTableCard("New table");
		} else if (e.getSource() == create_comment || e.getSource() == addCommentButton) {
			this.createNoteCard("New note");
		} else if (e.getSource() == openButton) {
			canvasController.openModel();
		} else if (e.getSource() == saveButton) {
			canvasController.saveModel();
		} else if (e.getSource() == newButton) {
			canvasController.newModel();
		} else if (e.getSource() == exportButton) {
			canvasController.generate();
		} else if (e.getSource() == show_properties || e.getSource() == propertiesButton) {
			boolean b = this.showProperties();
			if (e.getSource() == propertiesButton && !b) {
				showModelProperties();
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

		if (source instanceof ModelCard object) {
			if (!object.isSelected()) {
				model.deselectAll();
				object.setSelected(true);
			}
			menu = switch (object) {
				case TableCard table -> tableMenu(table);
				case DatabaseCard database -> databaseMenu(database);
				case NoteCard note -> noteMenu(note);
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
		menu.add(item("Add database", "add_database", () -> placeAt(model.createDatabaseCard("New database"), point)));
		menu.add(item("Add table", "add_table", () -> placeAt(model.createTableCard("New table"), point)));
		menu.add(item("Add note", "add_comment", () -> placeAt(model.createNoteCard("New note"), point)));
		menu.addSeparator();
		if (!model.getObjects().isEmpty()) {
			menu.add(item("Select all", null, () -> {
				model.selectAll();
				repaint();
			}));
		}
		if (model.getObjects().stream().anyMatch(object -> object instanceof TableCard)) {
			menu.add(item("Arrange automatically", null, this::arrangeAutomatically));
		}
		JCheckBoxMenuItem grid = new JCheckBoxMenuItem("Show grid", showGrid);
		grid.addActionListener(e -> setShowGrid(grid.isSelected()));
		menu.add(grid);
		menu.addSeparator();
		menu.add(item("Model properties...", "des_properties", this::showModelProperties));
		return menu;
	}

	JPopupMenu tableMenu(TableCard table) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Properties...", "des_properties", this::showProperties));
		menu.add(item("Add foreign key...", "linkimg", () -> addForeignKey(table, firstColumnOf(table), null, null)));

		List<ModelCard> linked = linkedObjects(table);
		JMenu link = new JMenu("Link to database");
		for (Object object : model.getObjects()) {
			if (object instanceof DatabaseCard database && !linked.contains(database)) {
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

	JPopupMenu databaseMenu(DatabaseCard database) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Properties...", "des_properties", this::showProperties));
		menu.add(item("Add table to this database", "add_table", () -> {
			TableCard table = model.createTableCard("New table");
			model.addReference(database, table);
			Rectangle card = database.cardBounds();
			placeAt(table, new Point(card.x, card.y + card.height + 40));
		}));
		addObjectItems(menu, database, linkedObjects(database));
		return menu;
	}

	JPopupMenu noteMenu(NoteCard note) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Edit", "des_properties", note::startEditing));
		addObjectItems(menu, note, linkedObjects(note));
		return menu;
	}

	JPopupMenu connectorMenu(ModelForeignKey key) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(item("Edit foreign key...", "des_properties", () -> editForeignKey(key)));
		menu.add(item("Remove foreign key", null, () -> removeForeignKey(key)));
		return menu;
	}

	/** What every card has: a note attached to it, removing its links (when it has any), and delete. */
	private void addObjectItems(JPopupMenu menu, ModelCard object, List<ModelCard> linked) {
		if (!(object instanceof NoteCard)) {
			menu.add(item("Add note", "add_comment", () -> {
				NoteCard note = model.createNoteCard("New note");
				model.addReference(note, object);
				Rectangle card = object.cardBounds();
				placeAt(note, new Point(card.x + card.width + 40, card.y));
			}));
		}
		if (!linked.isEmpty()) {
			JMenu unlink = new JMenu("Remove link");
			for (ModelCard other : linked) {
				String name = other instanceof NoteCard ? "Note" : other.getName();
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
		menu.add(item(count > 1 ? "Delete " + count + " objects" : "Delete", null, this::removeSelectedObjects));
	}

	/** The cards an object is linked to with a database or note link, in both directions. */
	private List<ModelCard> linkedObjects(ModelCard object) {
		List<ModelCard> linked = new java.util.ArrayList<>();
		for (Object other : model.getObjects()) {
			ModelCard candidate = (ModelCard) other;
			if (candidate != object && (candidate.getReferences().contains(object) || object.getReferences().contains(candidate))) {
				linked.add(candidate);
			}
		}
		return linked;
	}

	private JMenuItem item(String text, String icon, Runnable action) {
		JMenuItem item = new JMenuItem(text);
		if (icon != null) {
			item.setIcon(ApplicationContext.get().imageLoader().getIcon(icon));
		}
		item.addActionListener(e -> action.run());
		return item;
	}

	/** Adds a new card with its top left corner at a point of the viewer and selects it. */
	private void placeAt(ModelCard object, Point point) {
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
		if (e.getSource() == this && MouseClicks.isDoubleClick(e) && !e.isMetaDown()) {
			editForeignKey(connectorAt(e.getPoint()));
		}
	}
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() instanceof ModelCard object) {
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
			ModelForeignKey key = connectorAt(e.getPoint());
			if (key != hoveredKey) {
				hoveredKey = key;
				setToolTipText(key == null ? null : key.name());
				repaint();
			}
		}
	}
}

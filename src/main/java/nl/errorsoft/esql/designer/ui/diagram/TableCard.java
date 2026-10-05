package nl.errorsoft.esql.designer.ui.diagram;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import nl.errorsoft.esql.app.ApplicationContext;

/** A table of the model, drawn as a card: a header in the accent colour with the name and a row per field. */
public class TableCard extends ModelCard {
	static final int HEADER = 30;
	static final int ROW = 20;
	private static final int BOTTOM = 6;
	private static final int PAD = 10;
	/** The field icon at the start of a row is the handle to drag a foreign key from. */
	private static final int HANDLE = PAD + 18;

	private String name;
	private String description = "";
	private final List<DesignerColumn> fields = new ArrayList<>();
	private String type = "InnoDB";
	private String comment = "";

	private int hoveredRow = -1;
	private Set<String> foreignKeyColumns = new HashSet<>();

	public TableCard(String name, int identifier) {
		this.name = name;
		this.setOpaque(false);
		this.setIdentifier(identifier);
		this.reviewSize();

		MouseAdapter hover = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				setHoveredRow(rowAt(e.getY()));
				setCursor(handleAt(e.getX(), e.getY()) >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setHoveredRow(-1);
			}
		};
		addMouseListener(hover);
		addMouseMotionListener(hover);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		reviewSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		DesignerTheme.smooth(g2);

		Rectangle card = localCard();
		RoundRectangle2D shape = new RoundRectangle2D.Float(card.x, card.y, card.width - 1, card.height - 1, DesignerTheme.RADIUS * 2,
			DesignerTheme.RADIUS * 2);

		paintShadow(g2, shape);
		g2.setColor(DesignerTheme.card());
		g2.fill(shape);

		Area header = new Area(shape);
		header.intersect(new Area(new Rectangle(card.x, card.y, card.width, HEADER)));
		g2.setColor(DesignerTheme.accent());
		g2.fill(header);

		paintHeader(g2, card);
		paintRows(g2, card, shape);

		g2.setColor(isSelected() ? DesignerTheme.accent() : DesignerTheme.border());
		g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
		g2.draw(shape);
		g2.dispose();
	}

	private void paintHeader(Graphics2D g2, Rectangle card) {
		Icon icon = ApplicationContext.get().imageLoader().getIcon("tbimgsel");
		int iconY = card.y + (HEADER - 16) / 2;
		if (icon != null) {
			icon.paintIcon(this, g2, card.x + PAD, iconY);
		}

		g2.setFont(DesignerTheme.bold());
		FontMetrics metrics = g2.getFontMetrics();
		int baseline = card.y + (HEADER - metrics.getHeight()) / 2 + metrics.getAscent();
		g2.setColor(DesignerTheme.onAccent());
		g2.drawString(name, card.x + PAD + 22, baseline);

		String engine = shownType();
		if (!engine.isEmpty()) {
			g2.setFont(DesignerTheme.small());
			FontMetrics small = g2.getFontMetrics();
			g2.setColor(DesignerTheme.translucent(DesignerTheme.onAccent(), 170));
			g2.drawString(engine, card.x + card.width - PAD - small.stringWidth(engine), baseline);
		}
	}

	private void paintRows(Graphics2D g2, Rectangle card, Shape shape) {
		g2.setFont(DesignerTheme.font());
		FontMetrics metrics = g2.getFontMetrics();
		Shape clip = g2.getClip();
		g2.clip(shape);

		for (int i = 0; i < fields.size(); i++) {
			DesignerColumn field = (DesignerColumn) fields.get(i);
			int top = card.y + HEADER + i * ROW;

			if (i == hoveredRow) {
				g2.setColor(DesignerTheme.hover());
				g2.fillRect(card.x, top, card.width, ROW);
			}

			Icon icon = ApplicationContext.get().imageLoader().getIcon(iconName(field));
			if (icon != null) {
				icon.paintIcon(this, g2, card.x + PAD, top + (ROW - 16) / 2);
			}

			int baseline = top + (ROW - metrics.getHeight()) / 2 + metrics.getAscent();
			g2.setColor(DesignerTheme.text());
			g2.drawString(field.getName(), card.x + PAD + 22, baseline);

			String typeText = typeText(field);
			g2.setColor(DesignerTheme.muted());
			g2.drawString(typeText, card.x + card.width - PAD - metrics.stringWidth(typeText), baseline);
		}
		g2.setClip(clip);
	}

	private String iconName(DesignerColumn field) {
		if (field.primary) {
			return "keyimg";
		}
		return foreignKeyColumns.contains(field.getName()) ? "linkimg" : "fldimg";
	}

	/** The type as the card shows it, lower case with the length: varchar(100). */
	public static String typeText(DesignerColumn field) {
		String length = field.getLength() == null ? "" : field.getLength().trim();
		String typeName = field.getType() == null ? "" : field.getType().getName().toLowerCase();
		return length.isEmpty() ? typeName : typeName + "(" + length + ")";
	}

	/** The storage engine, only on servers that have them (a PostgreSQL table has no engine). */
	private String shownType() {
		boolean typesShown = getParent() instanceof DesignerCanvas viewer && viewer.showsTableTypes();
		return typesShown && type != null ? type : "";
	}

	public void addField(DesignerColumn column) {
		this.fields.add(column);
		this.reviewSize();
		this.repaint();
	}

	void reviewSize() {
		FontMetrics bold = this.getFontMetrics(DesignerTheme.bold());
		FontMetrics plain = this.getFontMetrics(DesignerTheme.font());
		FontMetrics small = this.getFontMetrics(DesignerTheme.small());

		String engine = shownType();
		int width = PAD + 22 + bold.stringWidth(name) + (engine.isEmpty() ? 0 : 16 + small.stringWidth(engine)) + PAD;

		for (int i = 0; i < fields.size(); i++) {
			DesignerColumn field = (DesignerColumn) fields.get(i);
			width = Math.max(width, PAD + 22 + plain.stringWidth(field.getName()) + 24 + plain.stringWidth(typeText(field)) + PAD);
		}

		setCardSize(Math.max(160, width), HEADER + fields.size() * ROW + BOTTOM);
	}

	/** The row of the field under a y coordinate of this component, -1 outside the rows. */
	public int rowAt(int y) {
		int row = Math.floorDiv(y - DesignerTheme.SHADOW - HEADER, ROW);
		return row >= 0 && row < fields.size() ? row : -1;
	}

	/** The row whose drag handle (the field icon) is under the point, -1 if there is none. */
	public int handleAt(int x, int y) {
		return x >= DesignerTheme.SHADOW && x < DesignerTheme.SHADOW + HANDLE ? rowAt(y) : -1;
	}

	/** The y coordinate, in the viewer, of the middle of the row of a column; the middle of the header when there is no such column. */
	public int rowAnchorY(String column) {
		Rectangle card = cardBounds();
		for (int i = 0; i < fields.size(); i++) {
			if (((DesignerColumn) fields.get(i)).getName().equals(column)) {
				return card.y + HEADER + i * ROW + ROW / 2;
			}
		}
		return card.y + HEADER / 2;
	}

	private void setHoveredRow(int row) {
		if (row != hoveredRow) {
			hoveredRow = row;
			repaint();
		}
	}

	/** The columns that are part of a foreign key of this table, they get the link icon. */
	void setForeignKeyColumns(Set<String> columns) {
		if (!columns.equals(foreignKeyColumns)) {
			foreignKeyColumns = columns;
			repaint();
		}
	}

	public DesignerColumn[] getFields() {
		DesignerColumn[] result = new DesignerColumn[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			result[i] = (DesignerColumn) fields.get(i);
		}
		return result;
	}

	public DesignerColumn getField(String name) {
		for (DesignerColumn field : getFields()) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		return null;
	}

	public void removeAllFields() {
		fields.clear();
		this.reviewSize();
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getComment() {
		return comment;
	}

	public String getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
		this.reviewSize();
		this.repaint();
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setType(String type) {
		this.type = type;
		this.reviewSize();
		this.repaint();
	}
}

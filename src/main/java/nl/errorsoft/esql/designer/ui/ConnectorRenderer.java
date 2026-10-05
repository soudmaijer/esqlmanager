package nl.errorsoft.esql.designer.ui;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import nl.errorsoft.esql.designer.model.ForeignKey;

/**
 * Draws the relations of the designer. A foreign key is an orthogonal connector from the row of its column in the child table to the row of the referenced
 * column in the parent, with a crow's foot at the child (many) and a double bar at the parent (exactly one). Links between a database or a note and another
 * object are dashed curves.
 */
public final class ConnectorRenderer {
	/** How far a connector leaves a card before it turns. */
	private static final int STUB = 24;
	private static final int CORNER = 8;
	private static final int FOOT = 12;
	private static final int SPREAD = 6;
	private static final float HIT_WIDTH = 8f;

	private ConnectorRenderer() {
	}

	/**
	 * The corner points of a connector, from the child to the parent. The first and last point are on a card edge.
	 * @param startOut +1 when the connector leaves the child to the right, -1 to the left.
	 * @param endOut +1 when the connector meets the parent on its right edge, -1 on its left edge.
	 */
	record Route(List<Point2D> points, int startOut, int endOut) {
	}

	static Route route(ForeignKey key) {
		Rectangle from = key.from().cardBounds();
		Rectangle to = key.to().cardBounds();
		int fromY = key.from().rowAnchorY(key.fromColumns().getFirst());
		int toY = key.to().rowAnchorY(key.toColumns().getFirst());
		return route(from, fromY, to, toY, key.from() == key.to());
	}

	static Route route(Rectangle from, int fromY, Rectangle to, int toY, boolean self) {
		List<Point2D> points = new ArrayList<>();

		if (!self && from.getMaxX() + 2 * STUB <= to.x) {
			double middle = (from.getMaxX() + to.x) / 2;
			points.add(new Point2D.Double(from.getMaxX(), fromY));
			points.add(new Point2D.Double(middle, fromY));
			points.add(new Point2D.Double(middle, toY));
			points.add(new Point2D.Double(to.x, toY));
			return new Route(points, 1, -1);
		}
		if (!self && to.getMaxX() + 2 * STUB <= from.x) {
			double middle = (to.getMaxX() + from.x) / 2;
			points.add(new Point2D.Double(from.x, fromY));
			points.add(new Point2D.Double(middle, fromY));
			points.add(new Point2D.Double(middle, toY));
			points.add(new Point2D.Double(to.getMaxX(), toY));
			return new Route(points, -1, 1);
		}

		// The cards are above each other (or it is the same card): go around them on the right.
		double outside = Math.max(from.getMaxX(), to.getMaxX()) + STUB + (self ? 8 : 0);
		if (fromY == toY) {
			// A column that refers to itself: let the loop come back a little lower so both ends can be seen.
			toY += 8;
		}
		points.add(new Point2D.Double(from.getMaxX(), fromY));
		points.add(new Point2D.Double(outside, fromY));
		points.add(new Point2D.Double(outside, toY));
		points.add(new Point2D.Double(to.getMaxX(), toY));
		return new Route(points, 1, 1);
	}

	/** The line of a route with rounded corners. */
	static Path2D path(Route route) {
		List<Point2D> points = route.points();
		Path2D path = new Path2D.Double();
		path.moveTo(points.getFirst().getX(), points.getFirst().getY());

		for (int i = 1; i < points.size() - 1; i++) {
			Point2D before = points.get(i - 1);
			Point2D corner = points.get(i);
			Point2D after = points.get(i + 1);
			double radius = Math.min(CORNER, Math.min(before.distance(corner), corner.distance(after)) / 2);

			Point2D in = towards(corner, before, radius);
			Point2D out = towards(corner, after, radius);
			path.lineTo(in.getX(), in.getY());
			path.quadTo(corner.getX(), corner.getY(), out.getX(), out.getY());
		}
		path.lineTo(points.getLast().getX(), points.getLast().getY());
		return path;
	}

	public static void paint(Graphics2D g2, ForeignKey key, boolean highlighted) {
		paint(g2, route(key), highlighted);
	}

	private static void paint(Graphics2D g2, Route route, boolean highlighted) {
		Stroke stroke = g2.getStroke();
		g2.setColor(highlighted ? DesignerTheme.accent() : DesignerTheme.muted());
		g2.setStroke(new BasicStroke(highlighted ? 2f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(path(route));

		Point2D start = route.points().getFirst();
		Point2D end = route.points().getLast();
		crowsFoot(g2, start, route.startOut());
		bar(g2, end, route.endOut(), 7);
		bar(g2, end, route.endOut(), 11);
		g2.setStroke(stroke);
	}

	/** The connector that is drawn while a foreign key is dragged from a column, it follows the mouse. */
	public static void paintGhost(Graphics2D g2, TableObject from, String column, Point mouse) {
		Rectangle card = from.cardBounds();
		int y = from.rowAnchorY(column);
		boolean right = mouse.x >= card.getCenterX();
		Rectangle target = new Rectangle(mouse.x, mouse.y, 0, 0);

		Route route = route(card, y, target, mouse.y, false);
		if (route.points().size() != 4 || (route.startOut() == 1) != right) {
			// The mouse is above or below the card: leave on the side the mouse is on.
			List<Point2D> points = List.of(new Point2D.Double(right ? card.getMaxX() : card.x, y), new Point2D.Double(mouse.x, y), new Point2D.Double(mouse.x,
				mouse.y));
			route = new Route(points, right ? 1 : -1, 0);
		}

		Stroke stroke = g2.getStroke();
		g2.setColor(DesignerTheme.accent());
		g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 4f}, 0f));
		g2.draw(path(route));
		g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		crowsFoot(g2, route.points().getFirst(), route.startOut());
		g2.setStroke(stroke);
	}

	/** Whether a point (in viewer coordinates) is on the connector or close to it. */
	public static boolean hit(ForeignKey key, Point point) {
		return new BasicStroke(HIT_WIDTH).createStrokedShape(path(route(key))).contains(point);
	}

	/** A dashed curve between a database or a note and the object it belongs to. Cards are painted on top, so it runs from centre to centre. */
	public static void paintLink(Graphics2D g2, ModelObject a, ModelObject b) {
		Stroke stroke = g2.getStroke();
		g2.setColor(DesignerTheme.translucent(DesignerTheme.muted(), 140));
		g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{4f, 4f}, 0f));
		g2.draw(link(a.cardBounds(), b.cardBounds()));
		g2.setStroke(stroke);
	}

	static Shape link(Rectangle a, Rectangle b) {
		double ax = a.getCenterX();
		double ay = a.getCenterY();
		double bx = b.getCenterX();
		double by = b.getCenterY();

		if (Math.abs(bx - ax) > Math.abs(by - ay)) {
			double middle = (ax + bx) / 2;
			return new CubicCurve2D.Double(ax, ay, middle, ay, middle, by, bx, by);
		}
		double middle = (ay + by) / 2;
		return new CubicCurve2D.Double(ax, ay, ax, middle, bx, middle, bx, by);
	}

	/** Three lines that spread from a point on the line to the card edge. */
	private static void crowsFoot(Graphics2D g2, Point2D edge, int out) {
		double x = edge.getX();
		double y = edge.getY();
		double tip = x + out * FOOT;
		g2.draw(new Line2D.Double(tip, y, x, y - SPREAD));
		g2.draw(new Line2D.Double(tip, y, x, y));
		g2.draw(new Line2D.Double(tip, y, x, y + SPREAD));
	}

	private static void bar(Graphics2D g2, Point2D edge, int out, int distance) {
		double x = edge.getX() + out * distance;
		g2.draw(new Line2D.Double(x, edge.getY() - SPREAD, x, edge.getY() + SPREAD));
	}

	private static Point2D towards(Point2D from, Point2D to, double distance) {
		double length = from.distance(to);
		if (length == 0) {
			return from;
		}
		return new Point2D.Double(from.getX() + (to.getX() - from.getX()) * distance / length, from.getY() + (to.getY() - from.getY()) * distance / length);
	}
}

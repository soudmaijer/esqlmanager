package nl.errorsoft.esql.designer.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.layout.AutoLayout.Edge;
import nl.errorsoft.esql.designer.layout.AutoLayout.Node;
import nl.errorsoft.esql.designer.layout.AutoLayout.Position;
import org.junit.jupiter.api.Test;

class AutoLayoutTest {
	private static final List<Node> SHOP = List.of(new Node("order_lines", 220, 130), new Node("orders", 200, 110), new Node("customers", 180, 90),
		new Node("products", 200, 110), new Node("settings", 160, 70), new Node("audit_log", 190, 90), new Node("notes", 160, 50));
	private static final List<Edge> KEYS = List.of(new Edge("orders", "customers"), new Edge("order_lines", "orders"), new Edge("order_lines", "products"),
		new Edge("orders", "orders"), new Edge("orders", "missing"));

	@Test
	void placesEveryTableWithoutOverlap() {
		Map<String, Position> positions = AutoLayout.arrange(SHOP, KEYS, 20, 30);

		assertEquals(SHOP.size(), positions.size());
		for (int i = 0; i < SHOP.size(); i++) {
			Rectangle a = bounds(SHOP.get(i), positions);
			assertTrue(a.x >= 20 && a.y >= 30, SHOP.get(i).id() + " starts before the origin");
			for (int j = i + 1; j < SHOP.size(); j++) {
				assertFalse(a.intersects(bounds(SHOP.get(j), positions)), SHOP.get(i).id() + " overlaps " + SHOP.get(j).id());
			}
		}
	}

	@Test
	void referencedTableComesBeforeTheTableThatRefersToIt() {
		Map<String, Position> positions = AutoLayout.arrange(SHOP, KEYS, 0, 0);

		assertTrue(right(SHOP.get(2), positions) < positions.get("orders").x());
		assertTrue(right(SHOP.get(1), positions) < positions.get("order_lines").x());
		assertTrue(right(SHOP.get(3), positions) < positions.get("order_lines").x());
	}

	@Test
	void tablesWithoutRelationsGoInAGridBelow() {
		Map<String, Position> positions = AutoLayout.arrange(SHOP, KEYS, 0, 0);
		int bottom = 0;
		for (Node node : SHOP.subList(0, 4)) {
			bottom = Math.max(bottom, bounds(node, positions).y + node.height());
		}

		for (Node node : SHOP.subList(4, 7)) {
			assertTrue(positions.get(node.id()).y() > bottom, node.id() + " is not below the related tables");
		}
		assertEquals(positions.get("settings").y(), positions.get("audit_log").y());
	}

	@Test
	void worksWithoutAnyRelation() {
		Map<String, Position> positions = AutoLayout.arrange(List.of(new Node("a", 100, 50)), List.of(), 5, 5);

		assertEquals(new Position(5, 5), positions.get("a"));
	}

	private static Rectangle bounds(Node node, Map<String, Position> positions) {
		Position position = positions.get(node.id());
		return new Rectangle(position.x(), position.y(), node.width(), node.height());
	}

	private static int right(Node node, Map<String, Position> positions) {
		return positions.get(node.id()).x() + node.width();
	}
}

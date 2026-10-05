package nl.errorsoft.esql.designer.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.elk.alg.layered.LayeredLayoutProvider;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

/**
 * Places the tables of a model automatically with the layered algorithm of the Eclipse Layout Kernel (ELK).
 * Tables that are connected by foreign keys are laid out from left to right, a referenced table before the tables that refer to it.
 * Tables without any relation are put in a grid below them. Pure computation: sizes and edges in, positions out.
 */
public final class AutoLayout {
	/** Space between the tables, and between the layers where the connectors run. */
	public static final int SPACING = 40;
	static final int LAYER_SPACING = 90;

	/** A table to place, with the size of its card. */
	public record Node(String id, int width, int height) {
	}

	/** A foreign key: table {@code from} refers to table {@code to}. */
	public record Edge(String from, String to) {
	}

	/** The top left corner of a card. */
	public record Position(int x, int y) {
	}

	static {
		// Outside Eclipse the options of ELK are only known once the meta data service has loaded them.
		LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
	}

	private AutoLayout() {
	}

	/**
	 * The position of every node, starting at the given origin.
	 * Edges between unknown nodes and edges of a table to itself are ignored.
	 */
	public static Map<String, Position> arrange(List<Node> nodes, List<Edge> edges, int originX, int originY) {
		Map<String, Node> byId = new LinkedHashMap<>();
		for (Node node : nodes) {
			byId.put(node.id(), node);
		}

		Set<String> connected = new HashSet<>();
		List<Edge> usable = new ArrayList<>();
		for (Edge edge : edges) {
			if (byId.containsKey(edge.from()) && byId.containsKey(edge.to()) && !edge.from().equals(edge.to())) {
				usable.add(edge);
				connected.add(edge.from());
				connected.add(edge.to());
			}
		}

		Map<String, Position> positions = new LinkedHashMap<>();
		int bottom = layered(byId, connected, usable, originX, originY, positions);
		int top = connected.isEmpty() ? originY : bottom + LAYER_SPACING;
		grid(nodes.stream().filter(node -> !connected.contains(node.id())).toList(), originX, top, positions);
		return positions;
	}

	/** Lays out the connected tables with ELK and returns the bottom of the result. */
	private static int layered(Map<String, Node> byId, Set<String> connected, List<Edge> edges, int originX, int originY, Map<String, Position> positions) {
		if (connected.isEmpty()) {
			return originY;
		}

		ElkNode graph = ElkGraphUtil.createGraph();
		graph.setProperty(LayeredOptions.DIRECTION, Direction.RIGHT);
		graph.setProperty(LayeredOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
		graph.setProperty(LayeredOptions.SPACING_NODE_NODE, (double) SPACING);
		graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, (double) LAYER_SPACING);
		graph.setProperty(LayeredOptions.SPACING_COMPONENT_COMPONENT, (double) SPACING);

		Map<String, ElkNode> elkNodes = new LinkedHashMap<>();
		for (Node node : byId.values()) {
			if (connected.contains(node.id())) {
				ElkNode elkNode = ElkGraphUtil.createNode(graph);
				elkNode.setDimensions(node.width(), node.height());
				elkNodes.put(node.id(), elkNode);
			}
		}
		// The edge runs from the referenced table to the referencing one, so the referenced table comes first.
		for (Edge edge : edges) {
			ElkGraphUtil.createSimpleEdge(elkNodes.get(edge.to()), elkNodes.get(edge.from()));
		}

		new LayeredLayoutProvider().layout(graph, new BasicProgressMonitor());

		int bottom = originY;
		for (Map.Entry<String, ElkNode> entry : elkNodes.entrySet()) {
			ElkNode elkNode = entry.getValue();
			int x = originX + (int) Math.round(elkNode.getX());
			int y = originY + (int) Math.round(elkNode.getY());
			positions.put(entry.getKey(), new Position(x, y));
			bottom = Math.max(bottom, y + byId.get(entry.getKey()).height());
		}
		return bottom;
	}

	/** Puts the tables in rows, about as many per row as there are rows, each row as high as its highest card. */
	private static void grid(List<Node> nodes, int originX, int top, Map<String, Position> positions) {
		int perRow = Math.max(1, (int) Math.ceil(Math.sqrt(nodes.size())));
		int y = top;

		for (int start = 0; start < nodes.size(); start += perRow) {
			int x = originX;
			int rowHeight = 0;
			for (Node node : nodes.subList(start, Math.min(nodes.size(), start + perRow))) {
				positions.put(node.id(), new Position(x, y));
				x += node.width() + SPACING;
				rowHeight = Math.max(rowHeight, node.height());
			}
			y += rowHeight + SPACING;
		}
	}
}

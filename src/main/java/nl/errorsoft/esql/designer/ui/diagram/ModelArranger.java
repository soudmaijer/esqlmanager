package nl.errorsoft.esql.designer.ui.diagram;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.layout.AutoLayout;
import nl.errorsoft.esql.designer.model.ForeignKey;
import nl.errorsoft.esql.designer.model.Model;

/** Moves the cards of a model to the places {@link AutoLayout} gives them: the databases in a row at the top, the tables below. */
public final class ModelArranger {
	private static final int MARGIN = 20;

	private ModelArranger() {
	}

	public static void arrange(Model model) {
		List<AutoLayout.Node> tables = new ArrayList<>();
		int x = MARGIN;
		int databasesBottom = MARGIN;

		for (Object object : model.getObjects()) {
			if (object instanceof DatabaseObject database) {
				database.setCardLocation(x, MARGIN);
				Rectangle card = database.cardBounds();
				x += card.width + AutoLayout.SPACING;
				databasesBottom = Math.max(databasesBottom, card.y + card.height + AutoLayout.SPACING);
			} else if (object instanceof TableObject table) {
				// The card may have been made before the table knew its viewer, its size is brought up to date first.
				table.reviewSize();
				Rectangle card = table.cardBounds();
				tables.add(new AutoLayout.Node(String.valueOf(table.getIdentifier()), card.width, card.height));
			}
		}

		List<AutoLayout.Edge> edges = new ArrayList<>();
		for (ForeignKey key : model.getForeignKeys()) {
			edges.add(new AutoLayout.Edge(String.valueOf(key.from().getIdentifier()), String.valueOf(key.to().getIdentifier())));
		}

		Map<String, AutoLayout.Position> positions = AutoLayout.arrange(tables, edges, MARGIN, databasesBottom);
		for (Object object : model.getObjects()) {
			if (object instanceof TableObject table) {
				AutoLayout.Position position = positions.get(String.valueOf(table.getIdentifier()));
				table.setCardLocation(position.x(), position.y());
			}
		}
	}
}

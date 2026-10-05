package nl.errorsoft.esql.designer.ui.diagram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.designer.control.ModelViewerControl;
import nl.errorsoft.esql.designer.model.Model;

/** The context menus of the designer only offer what applies to the object. */
class DesignerCanvasMenuTest {
	private final DesignerCanvas viewer = new DesignerCanvas(new ModelViewerControl(null));

	@Test
	void emptyCanvasOffersOnlyAddingAndTheModel() {
		assertEquals(List.of("Add Database", "Add Table", "Add Note", "-", "Show Grid", "-", "Model Properties..."),
			texts(viewer.canvasMenu(new Point(10, 10))));
	}

	@Test
	void canvasWithTablesCanBeSelectedAndArranged() {
		viewer.getModel().createTableObject("orders");
		assertEquals(List.of("Add Database", "Add Table", "Add Note", "-", "Select All", "Arrange Automatically", "Show Grid", "-", "Model Properties..."),
			texts(viewer.canvasMenu(new Point(10, 10))));
	}

	@Test
	void tableMenuLinksOnlyToDatabasesItIsNotLinkedTo() {
		Model model = viewer.getModel();
		TableObject table = model.createTableObject("orders");
		assertEquals(List.of("Properties...", "Add Foreign Key...", "Add Note", "-", "Delete"), texts(viewer.tableMenu(table)));

		DatabaseObject shop = model.createDatabaseObject("shop");
		assertEquals(List.of("Properties...", "Add Foreign Key...", "Link to Database", "Add Note", "-", "Delete"), texts(viewer.tableMenu(table)));

		model.addReference(shop, table);
		assertEquals(List.of("Properties...", "Add Foreign Key...", "Add Note", "Remove Link", "-", "Delete"), texts(viewer.tableMenu(table)));
	}

	@Test
	void databaseAndNoteMenus() {
		Model model = viewer.getModel();
		assertEquals(List.of("Properties...", "Add Table to This Database", "Add Note", "-", "Delete"),
			texts(viewer.databaseMenu(model.createDatabaseObject("shop"))));
		assertEquals(List.of("Edit", "-", "Delete"), texts(viewer.noteMenu(model.createCommentObject("remember"))));
	}

	private static List<String> texts(JPopupMenu menu) {
		List<String> texts = new ArrayList<>();
		for (var component : menu.getComponents()) {
			texts.add(component instanceof JSeparator ? "-" : ((JMenuItem) component).getText());
		}
		return texts;
	}
}

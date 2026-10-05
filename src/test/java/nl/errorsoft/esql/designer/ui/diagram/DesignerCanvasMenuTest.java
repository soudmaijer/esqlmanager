package nl.errorsoft.esql.designer.ui.diagram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.designer.control.DesignerCanvasController;
import nl.errorsoft.esql.designer.model.Model;

/** The context menus of the designer only offer what applies to the object. */
class DesignerCanvasMenuTest {
	private final DesignerCanvas viewer = new DesignerCanvas(new DesignerCanvasController(null));

	@Test
	void emptyCanvasOffersOnlyAddingAndTheModel() {
		assertEquals(List.of("Add database", "Add table", "Add note", "-", "Show grid", "-", "Model properties..."),
			texts(viewer.canvasMenu(new Point(10, 10))));
	}

	@Test
	void canvasWithTablesCanBeSelectedAndArranged() {
		viewer.getModel().createTableCard("orders");
		assertEquals(List.of("Add database", "Add table", "Add note", "-", "Select all", "Arrange automatically", "Show grid", "-", "Model properties..."),
			texts(viewer.canvasMenu(new Point(10, 10))));
	}

	@Test
	void tableMenuLinksOnlyToDatabasesItIsNotLinkedTo() {
		Model model = viewer.getModel();
		TableCard table = model.createTableCard("orders");
		assertEquals(List.of("Properties...", "Add foreign key...", "Add note", "-", "Delete"), texts(viewer.tableMenu(table)));

		DatabaseCard shop = model.createDatabaseCard("shop");
		assertEquals(List.of("Properties...", "Add foreign key...", "Link to database", "Add note", "-", "Delete"), texts(viewer.tableMenu(table)));

		model.addReference(shop, table);
		assertEquals(List.of("Properties...", "Add foreign key...", "Add note", "Remove link", "-", "Delete"), texts(viewer.tableMenu(table)));
	}

	@Test
	void databaseAndNoteMenus() {
		Model model = viewer.getModel();
		assertEquals(List.of("Properties...", "Add table to this database", "Add note", "-", "Delete"),
			texts(viewer.databaseMenu(model.createDatabaseCard("shop"))));
		assertEquals(List.of("Edit", "-", "Delete"), texts(viewer.noteMenu(model.createNoteCard("remember"))));
	}

	private static List<String> texts(JPopupMenu menu) {
		List<String> texts = new ArrayList<>();
		for (var component : menu.getComponents()) {
			texts.add(component instanceof JSeparator ? "-" : ((JMenuItem) component).getText());
		}
		return texts;
	}
}

package nl.errorsoft.esql.designer.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;

class ObjectNamesTest {
	private final Model model = new Model("shop");

	@Test
	void namesCannotBeEmpty() {
		TableCard table = model.createTableCard("orders");
		assertEquals("The model needs a name.", ObjectNames.modelProblem("  "));
		assertNull(ObjectNames.modelProblem("shop"));
		assertEquals("The table needs a name.", ObjectNames.tableProblem(model, table, ""));
		assertEquals("The database needs a name.", ObjectNames.databaseProblem(model, model.createDatabaseCard("a"), " "));
	}

	@Test
	void aDatabaseNameIsUniqueInTheModel() {
		DatabaseCard shop = model.createDatabaseCard("shop");
		DatabaseCard other = model.createDatabaseCard("archive");

		assertNull(ObjectNames.databaseProblem(model, shop, "SHOP"));
		assertNotNull(ObjectNames.databaseProblem(model, other, "Shop"));
		assertNull(ObjectNames.databaseProblem(model, other, "archive2"));
	}

	@Test
	void aTableNameIsUniqueWithinItsDatabase() {
		DatabaseCard shop = model.createDatabaseCard("shop");
		DatabaseCard archive = model.createDatabaseCard("archive");
		TableCard orders = model.createTableCard("orders");
		TableCard copy = model.createTableCard("orders_copy");
		model.addReference(shop, orders);
		model.addReference(shop, copy);

		assertNotNull(ObjectNames.tableProblem(model, copy, "Orders"));
		assertNull(ObjectNames.tableProblem(model, orders, "orders"));

		// The same name in another database is fine.
		shop.removeReference(copy);
		model.addReference(archive, copy);
		assertNull(ObjectNames.tableProblem(model, copy, "orders"));
	}

	@Test
	void tablesWithoutADatabaseShareOneGroup() {
		TableCard first = model.createTableCard("a");
		TableCard second = model.createTableCard("b");
		assertNotNull(ObjectNames.tableProblem(model, second, "a"));
		assertNull(ObjectNames.tableProblem(model, first, "a"));
	}
}

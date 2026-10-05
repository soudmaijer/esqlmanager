package nl.errorsoft.esql.designer.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

class ObjectNamesTest {
	private final Model model = new Model("shop");

	@Test
	void namesCannotBeEmpty() {
		TableObject table = model.createTableObject("orders");
		assertEquals("The model needs a name.", ObjectNames.modelProblem("  "));
		assertNull(ObjectNames.modelProblem("shop"));
		assertEquals("The table needs a name.", ObjectNames.tableProblem(model, table, ""));
		assertEquals("The database needs a name.", ObjectNames.databaseProblem(model, model.createDatabaseObject("a"), " "));
	}

	@Test
	void aDatabaseNameIsUniqueInTheModel() {
		DatabaseObject shop = model.createDatabaseObject("shop");
		DatabaseObject other = model.createDatabaseObject("archive");

		assertNull(ObjectNames.databaseProblem(model, shop, "SHOP"));
		assertNotNull(ObjectNames.databaseProblem(model, other, "Shop"));
		assertNull(ObjectNames.databaseProblem(model, other, "archive2"));
	}

	@Test
	void aTableNameIsUniqueWithinItsDatabase() {
		DatabaseObject shop = model.createDatabaseObject("shop");
		DatabaseObject archive = model.createDatabaseObject("archive");
		TableObject orders = model.createTableObject("orders");
		TableObject copy = model.createTableObject("orders_copy");
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
		TableObject first = model.createTableObject("a");
		TableObject second = model.createTableObject("b");
		assertNotNull(ObjectNames.tableProblem(model, second, "a"));
		assertNull(ObjectNames.tableProblem(model, first, "a"));
	}
}

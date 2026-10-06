package nl.errorsoft.esql.designer.ui.diagram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.designer.model.ModelForeignKey;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.table.DataType;
import org.junit.jupiter.api.Test;

/** The snapshot the check and the generation work on, taken from the canvas model. */
class ModelFactoryTest {
	@Test
	void snapshotHasTheTablesOfEachDatabaseTheirColumnsAndOutgoingKeys() {
		Model model = new Model("test");
		DatabaseCard shop = model.createDatabaseCard("shop");
		TableCard customers = model.createTableCard("customers");
		TableCard orders = model.createTableCard("orders");
		TableCard loose = model.createTableCard("loose");
		NoteCard note = model.createNoteCard("a note");
		DesignerColumn id = new DesignerColumn("id", DataType.named("integer"), "", "", "");
		id.primary = true;
		customers.addField(id);
		orders.addField(new DesignerColumn("customer_id", DataType.named("integer"), "", "", ""));
		model.addReference(shop, customers);
		model.addReference(shop, orders);
		// A note linked to a database is not a table.
		model.addReference(shop, note);
		model.addForeignKey(new ModelForeignKey(orders, List.of("customer_id"), customers, List.of("id"), "fk_customer", "CASCADE", ""));

		DesignedModel snapshot = model.toDesigned();

		assertEquals(1, snapshot.databases().size());
		List<DesignedTable> tables = snapshot.databases().get(0).tables();
		assertEquals(List.of("customers", "orders"), tables.stream().map(DesignedTable::name).toList());
		assertTrue(tables.get(0).columns().get(0).primary);
		assertTrue(tables.get(0).foreignKeys().isEmpty());
		assertEquals("customers", tables.get(1).foreignKeys().get(0).referencedTable());
		assertEquals(List.of("loose"), snapshot.unlinkedTables().stream().map(DesignedTable::name).toList());
		assertTrue(loose.getFields().length == 0);
	}
}

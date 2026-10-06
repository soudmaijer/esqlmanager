package nl.errorsoft.esql.designer.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import nl.errorsoft.esql.designer.ui.diagram.DesignerColumn;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import org.junit.jupiter.api.Test;

class ModelForeignKeyTest {
	private final Model model = new Model("shop");
	private final TableCard customers = table("customers", "id", "INT", "name", "VARCHAR");
	private final TableCard orders = table("orders", "id", "INT", "customer_id", "BIGINT");

	@Test
	void keyBetweenWholeNumberColumnsIsValid() {
		assertDoesNotThrow(() -> key(List.of("customer_id"), List.of("id"), "CASCADE").validate());
	}

	@Test
	void columnThatDoesNotExistIsRefused() {
		EsqlException e = assertThrows(EsqlException.class, () -> key(List.of("missing"), List.of("id"), "").validate());
		assertTrue(e.getMessage().contains("missing"));
	}

	@Test
	void typesOfADifferentKindAreRefused() {
		assertThrows(EsqlException.class, () -> key(List.of("customer_id"), List.of("name"), "").validate());
	}

	@Test
	void unknownActionIsRefused() {
		assertThrows(EsqlException.class, () -> key(List.of("customer_id"), List.of("id"), "DROP TABLE").validate());
	}

	@Test
	void columnPairsMustMatch() {
		assertThrows(EsqlException.class, () -> key(List.of("customer_id", "id"), List.of("id"), "").validate());
	}

	@Test
	void tablesOfDifferentDatabasesCannotBeLinked() {
		model.addReference(model.createDatabaseCard("shop"), orders);
		model.addReference(model.createDatabaseCard("crm"), customers);

		EsqlException e = assertThrows(EsqlException.class, () -> key(List.of("customer_id"), List.of("id"), "").validate(model));
		assertEquals("A foreign key can only link tables of the same database.", e.getMessage());
	}

	@Test
	void tablesOfTheSameDatabaseOrOfNoneCanBeLinked() {
		assertDoesNotThrow(() -> key(List.of("customer_id"), List.of("id"), "").validate(model));
		var shop = model.createDatabaseCard("shop");
		model.addReference(shop, orders);
		model.addReference(shop, customers);
		assertDoesNotThrow(() -> key(List.of("customer_id"), List.of("id"), "").validate(model));
	}

	@Test
	void theModelTakesTheWordOfItsServer() {
		model.setServerType(new nl.errorsoft.esql.connection.ServerType(nl.errorsoft.esql.connection.ServerType.POSTGRES));
		assertEquals("database", model.term());
	}

	@Test
	void defaultNameIsTableAndColumn() {
		assertEquals("fk_orders_customer_id", ModelForeignKey.defaultName(orders, "customer_id"));
	}

	private ModelForeignKey key(List<String> from, List<String> to, String onDelete) {
		return new ModelForeignKey(orders, from, customers, to, "fk_orders_customer_id", onDelete, "");
	}

	private TableCard table(String name, String... columns) {
		TableCard table = model.createTableCard(name);
		for (int i = 0; i < columns.length; i += 2) {
			DesignerColumn field = new DesignerColumn(columns[i], new DataType(columns[i + 1], java.util.EnumSet.of(DataType.Option.PRIMARY,
				DataType.Option.INDEX, DataType.Option.UNIQUE, DataType.Option.NOT_NULL, DataType.Option.UNSIGNED, DataType.Option.AUTO_INCREMENT)), "", "",
				"");
			field.primary = i == 0;
			table.addField(field);
		}
		return table;
	}
}

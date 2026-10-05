package nl.errorsoft.esql.designer.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import nl.errorsoft.esql.designer.ui.diagram.Field;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.error.EsqlException;
import org.junit.jupiter.api.Test;

class ForeignKeyTest {
	private final Model model = new Model("shop");
	private final TableObject customers = table("customers", "id", "INT", "name", "VARCHAR");
	private final TableObject orders = table("orders", "id", "INT", "customer_id", "BIGINT");

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
	void defaultNameIsTableAndColumn() {
		assertEquals("fk_orders_customer_id", ForeignKey.defaultName(orders, "customer_id"));
	}

	private ForeignKey key(List<String> from, List<String> to, String onDelete) {
		return new ForeignKey(orders, from, customers, to, "fk_orders_customer_id", onDelete, "");
	}

	private TableObject table(String name, String... columns) {
		TableObject table = model.createTableObject(name);
		for (int i = 0; i < columns.length; i += 2) {
			Field field = new Field(columns[i], new DataType(columns[i + 1], true, true, true, false, true, true, true, false), "", "", "");
			field.primary = i == 0;
			table.addField(field);
		}
		return table;
	}
}

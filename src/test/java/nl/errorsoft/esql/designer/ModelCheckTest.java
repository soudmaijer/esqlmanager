package nl.errorsoft.esql.designer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import nl.errorsoft.esql.table.CreateColumn;
import org.junit.jupiter.api.Test;

class ModelCheckTest {
	private static DesignedTable table(String name, String... columns) {
		return new DesignedTable(name, "", "", java.util.Arrays.stream(columns).map(CreateColumn::new).toList(), List.of());
	}

	@Test
	void aSoundModelPassesEveryStep() {
		DesignedModel model = new DesignedModel(List.of(new DesignedDatabase("shop", List.of(table("orders", "id"), table("customers", "id")))), List.of());
		for (ModelCheck step : ModelCheck.values()) {
			assertTrue(step.problems(model).isEmpty(), step.label());
		}
	}

	@Test
	void eachStepNamesItsProblems() {
		DesignedModel model = new DesignedModel(List.of(new DesignedDatabase("shop", List.of(table("orders", "id", "ID"), table("Orders", "id"))),
			new DesignedDatabase("SHOP", List.of(table("empty")))), List.of(table("loose", "id")));

		assertEquals(List.of("The model has two databases named 'SHOP'."), ModelCheck.DATABASES.problems(model));
		assertEquals(List.of("Database 'shop' has two tables named 'Orders'."), ModelCheck.TABLES.problems(model));
		assertEquals(List.of("Table 'orders' has two columns named 'ID'.", "Table 'empty' has no columns."), ModelCheck.COLUMNS.problems(model));
		assertEquals(List.of("Table 'loose' is not linked to a database."), ModelCheck.RELATIONS.problems(model));
		assertTrue(ModelCheck.MODEL.problems(model).isEmpty());
		assertEquals(List.of("The model has no database."), ModelCheck.MODEL.problems(new DesignedModel(List.of(), List.of())));
	}

	@Test
	void generationStepsCountDatabasesTablesAndColumns() {
		DesignedModel model = new DesignedModel(List.of(new DesignedDatabase("shop", List.of(table("orders", "id", "total")))), List.of());
		assertEquals(1 + 1 + 2, model.generationSteps());
	}
}

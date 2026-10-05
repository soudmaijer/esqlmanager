package nl.errorsoft.esql.designer.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import nl.errorsoft.esql.designer.export.DiagramModel.Column;
import nl.errorsoft.esql.designer.export.DiagramModel.Relation;
import nl.errorsoft.esql.designer.export.DiagramModel.Table;
import org.junit.jupiter.api.Test;

class DiagramExporterTest {
	private final DiagramModel shop = new DiagramModel(List.of("shop"),
		List.of(new Table("customers", List.of(new Column("id", "int", true, false), new Column("name", "varchar(100)", false, false))),
			new Table("orders", List.of(new Column("id", "int", true, false), new Column("customer_id", "int", false, true),
				new Column("total", "numeric(10,2)", false, false)))),
		List.of(new Relation("orders", List.of("customer_id"), "customers", List.of("id"), "fk_orders_customer_id")));

	@Test
	void plantUmlHasEntitiesWithKeysAndCrowsFootRelations() {
		assertEquals("""
			@startuml
			' database: shop
			hide circle
			skinparam linetype ortho

			entity "customers" as customers {
			  * id : int <<PK>>
			  --
			  name : varchar(100)
			}

			entity "orders" as orders {
			  * id : int <<PK>>
			  --
			  customer_id : int <<FK>>
			  total : numeric(10,2)
			}

			orders }o--|| customers : fk_orders_customer_id
			@enduml
			""", DiagramExporter.plantUml(shop));
	}

	@Test
	void mermaidHasEntitiesWithTypesAndRelations() {
		assertEquals("""
			erDiagram
			    %% database: shop
			    customers {
			        int id PK
			        varchar(100) name
			    }
			    orders {
			        int id PK
			        int customer_id FK
			        numeric total
			    }
			    customers ||--o{ orders : "fk_orders_customer_id"
			""", DiagramExporter.mermaid(shop));
	}

	@Test
	void namesWithOtherCharactersBecomeIdentifiers() {
		DiagramModel model = new DiagramModel(List.of(), List.of(new Table("order lines", List.of(new Column("id", "int", true, true)))), List.of());

		assertEquals("""
			erDiagram
			    order_lines {
			        int id PK, FK
			    }
			""", DiagramExporter.mermaid(model));
	}
}

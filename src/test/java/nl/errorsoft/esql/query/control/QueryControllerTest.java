package nl.errorsoft.esql.query.control;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import nl.errorsoft.esql.database.Database;
import org.junit.jupiter.api.Test;

class QueryControllerTest {
	private final Database postgres = new Database("postgres");
	private final Database shop = new Database("shop");
	private final List<Database> databases = List.of(postgres, shop);

	@Test
	void startsOnTheDatabaseSelectedInTheTree() {
		assertSame(shop, QueryController.startDatabase(databases, new Database("shop"), "postgres"));
	}

	@Test
	void withoutASelectionStartsOnTheConnectedDatabase() {
		assertSame(shop, QueryController.startDatabase(databases, null, "shop"));
	}

	@Test
	void otherwiseStartsOnTheFirstDatabaseOfTheList() {
		assertSame(postgres, QueryController.startDatabase(databases, null, null));
		assertSame(postgres, QueryController.startDatabase(databases, null, "hidden_by_the_filter"));
	}

	@Test
	void anEmptyListHasNoDatabase() {
		assertNull(QueryController.startDatabase(List.of(), null, "shop"));
	}
}

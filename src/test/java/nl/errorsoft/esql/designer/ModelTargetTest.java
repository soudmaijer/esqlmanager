package nl.errorsoft.esql.designer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.connection.ServerType;

class ModelTargetTest {
	private static final ServerType MYSQL = new ServerType(ServerType.MY_SQL);
	private static final ServerType POSTGRES = new ServerType(ServerType.POSTGRES);

	@Test
	void aModelIsGeneratedOnItsOwnKindOfServerOnly() {
		assertNull(ModelTarget.refusal(POSTGRES, POSTGRES));
		assertNotNull(ModelTarget.refusal(POSTGRES, MYSQL));
	}

	@Test
	void aModelWithoutServerFitsAny() {
		assertNull(ModelTarget.refusal(null, MYSQL));
	}
}

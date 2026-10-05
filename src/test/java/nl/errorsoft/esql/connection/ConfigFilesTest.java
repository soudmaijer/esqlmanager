package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shipped conf/ files still load with the XML reading of {@code XmlFiles}. */
class ConfigFilesTest {

	@Test
	void everyServerTypeHasDataTypes() {
		for (ServerType type : ServerType.getServerTypes()) {
			assertTrue(type.getDataTypes().length > 0, "no datatypes for " + type);
		}
	}

	@Test
	void allDriversOfDriverXmlLoad() {
		assertEquals(4, new DatabaseDriver().getDatabaseDrivers().length);
	}
}

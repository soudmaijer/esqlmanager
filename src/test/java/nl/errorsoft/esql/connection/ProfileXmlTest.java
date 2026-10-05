package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.Test;

class ProfileXmlTest {

	private static Element parse(String xml) throws Exception {
		return new SAXBuilder().build(new StringReader(xml)).getRootElement();
	}

	@Test
	void anOldFileWithOnlyDatabasesStillLoads() throws Exception {
		Element element = parse("<profile><name>Local</name><host>localhost</host><port>5432</port><username>postgres</username><password>x</password>"
			+ "<serverType>2</serverType><databases>shop,crm</databases><autoConnect>true</autoConnect><lastUsed>false</lastUsed></profile>");

		ConnectionProfile profile = ProfileXml.read(element);

		assertEquals("Local", profile.getName());
		assertEquals("shop,crm", profile.getDatabases());
		assertEquals(List.of("shop", "crm"), profile.getSelection().databases());
		assertFalse(profile.getSelection().hasSchemaFilter());
		assertTrue(profile.isAutoConnect());
		assertTrue(profile.isSavePassword());
	}

	@Test
	void schemasAreReadPerDatabase() throws Exception {
		Element element = parse("<profile><name>Local</name><serverType>2</serverType><databases>shop,crm</databases>"
			+ "<schemas><database name=\"shop\"><schema>public</schema><schema>archive</schema></database><database name=\"crm\" /></schemas></profile>");

		DatabaseSelection selection = ProfileXml.read(element).getSelection();

		assertEquals(Set.of("public", "archive"), selection.schemasOf("shop"));
		assertEquals(Set.of(), selection.schemasOf("crm"));
		assertTrue(selection.showsSchema("crm", "anything"));
	}

	@Test
	void schemasOfADatabaseThatIsNotSelectedAreDropped() throws Exception {
		Element element = parse("<profile><name>Local</name><serverType>2</serverType><databases>shop</databases>"
			+ "<schemas><database name=\"other\"><schema>public</schema></database></schemas></profile>");

		DatabaseSelection selection = ProfileXml.read(element).getSelection();

		assertEquals(List.of("shop"), selection.databases());
		assertFalse(selection.hasSchemaFilter());
	}

	@Test
	void missingElementsGetDefaults() throws Exception {
		ConnectionProfile profile = ProfileXml.read(parse("<profile><name>Bare</name></profile>"));

		assertEquals("Bare", profile.getName());
		assertEquals("", profile.getHost());
		assertTrue(profile.getSelection().isEmpty());
		assertFalse(profile.isAutoConnect());
	}

	@Test
	void writingAndReadingKeepsTheSelection() throws Exception {
		ConnectionProfile profile = ProfileXml.read(parse("<profile><name>Local</name><host>h</host><port>1</port><serverType>2</serverType></profile>"));
		profile.setSelection(DatabaseSelection.parse("shop,crm").withSchema("shop", "public", true));
		Element element = new Element("profile");

		ProfileXml.write(element, profile);

		assertEquals("shop,crm", element.getChildText("databases"));
		assertEquals("public", element.getChild("schemas").getChild("database").getChildText("schema"));
		assertEquals(profile.getSelection(), ProfileXml.read(element).getSelection());
		assertEquals("false", element.getChildText("lastUsed"));
	}

	@Test
	void theSchemasElementIsRemovedWhenNoSchemaIsChosen() throws Exception {
		Element element = parse("<profile><name>Local</name><serverType>2</serverType><databases>shop</databases>"
			+ "<schemas><database name=\"shop\"><schema>public</schema></database></schemas></profile>");
		ConnectionProfile profile = ProfileXml.read(element);
		profile.setSelection(DatabaseSelection.NONE);

		ProfileXml.write(element, profile);

		assertNull(element.getChild("schemas"));
		assertNotNull(element.getChild("databases"));
		assertEquals("", element.getChildText("databases"));
	}
}

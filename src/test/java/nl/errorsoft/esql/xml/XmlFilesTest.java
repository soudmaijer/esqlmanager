package nl.errorsoft.esql.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import nl.errorsoft.esql.error.EsqlException;

class XmlFilesTest {
	private static final String HOSTILE = "a < b && c > d \"quoted\" 'single' é中😀 €\nsecond line\r\nthird\ttab  two spaces";

	@Test
	void hostileTextSurvivesAFileRoundTrip(@TempDir Path dir) {
		Document document = XmlFiles.newDocument("config");
		Element root = document.getDocumentElement();
		XmlFiles.addChild(root, "value", HOSTILE);
		XmlFiles.setAttribute(XmlFiles.addChild(root, "item"), "name", HOSTILE);
		Path file = dir.resolve("a.xml");

		XmlFiles.write(file, document);

		Element read = XmlFiles.read(file).getDocumentElement();
		assertEquals(HOSTILE, XmlFiles.childText(read, "value"));
		assertEquals(HOSTILE, XmlFiles.child(read, "item").getAttribute("name"));
	}

	@Test
	void theFileIsUtf8AndIndentedWithTabs(@TempDir Path dir) throws Exception {
		Document document = XmlFiles.newDocument("config");
		Element group = XmlFiles.addChild(document.getDocumentElement(), "group");
		XmlFiles.addChild(group, "name", "café");
		XmlFiles.addChild(group, "empty");
		Path file = dir.resolve("a.xml");

		XmlFiles.write(file, document);

		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config>\n\t<group>\n\t\t<name>café</name>\n\t\t<empty/>\n\t</group>\n</config>\n",
			Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	void writingAReadDocumentAgainDoesNotPileUpWhitespace(@TempDir Path dir) {
		Path file = dir.resolve("a.xml");
		XmlFiles.write(file, XmlFiles.parse("<a>\n  <b>x</b>\n  <c>  keep  </c>\n</a>"));
		String first = read(file);

		XmlFiles.write(file, XmlFiles.read(file));

		assertEquals(first, read(file));
		assertEquals("  keep  ", XmlFiles.childText(XmlFiles.read(file).getDocumentElement(), "c"));
	}

	@Test
	void aFailedWriteKeepsTheOldFile(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("a.xml");
		Files.writeString(file, "<old/>");
		Document document = XmlFiles.newDocument("config");

		// A directory with this name makes the final move impossible.
		Path blocked = dir.resolve("blocked.xml");
		Files.createDirectory(blocked);
		Files.writeString(blocked.resolve("inside"), "x");

		assertThrows(EsqlException.class, () -> XmlFiles.write(blocked, document));
		assertThrows(EsqlException.class, () -> XmlFiles.write(dir.resolve("missing").resolve("a.xml"), document));
		assertEquals("<old/>", Files.readString(file));
		try (Stream<Path> files = Files.list(dir)) {
			assertTrue(files.noneMatch(f -> f.getFileName().toString().endsWith(".tmp")));
		}
	}

	@Test
	void aSuccessfulWriteReplacesTheOldFileAndLeavesNoTemporaryFile(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("a.xml");
		Files.writeString(file, "<old/>");

		XmlFiles.write(file, XmlFiles.newDocument("new"));

		assertTrue(read(file).contains("<new/>"));
		try (Stream<Path> files = Files.list(dir)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	void aDoctypeIsRefused() {
		String xxe = "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";

		EsqlException refused = assertThrows(EsqlException.class, () -> XmlFiles.parse(xxe));

		assertTrue(refused.getMessage().contains("DOCTYPE"), refused.getMessage());
		assertThrows(EsqlException.class, () -> XmlFiles.parse("<!DOCTYPE a><a/>"));
	}

	@Test
	void malformedAndMissingFilesNameTheFile(@TempDir Path dir) throws Exception {
		Path broken = dir.resolve("broken.xml");
		Files.writeString(broken, "<config><a></config>");

		assertTrue(assertThrows(EsqlException.class, () -> XmlFiles.read(broken)).getMessage().startsWith("broken.xml cannot be read"));
		assertTrue(assertThrows(EsqlException.class, () -> XmlFiles.read(dir.resolve("none.xml"))).getMessage().startsWith("none.xml cannot be read"));
	}

	@Test
	void textXmlCannotStoreIsRefusedNotChanged() {
		Element root = XmlFiles.newDocument("a").getDocumentElement();

		assertThrows(EsqlException.class, () -> XmlFiles.addChild(root, "x", "bad\u0000text"));
		assertThrows(EsqlException.class, () -> XmlFiles.setChildText(root, "x", "bad\u000btext"));
		assertThrows(EsqlException.class, () -> XmlFiles.setAttribute(root, "x", "lone\ud800"));
		assertEquals("", XmlFiles.addChild(root, "n", null).getTextContent());
	}

	@Test
	void backupCopiesTheFileNextToIt(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("a.xml");
		Files.writeString(file, "<broken");

		Path backup = XmlFiles.backup(file);

		assertEquals("a.xml.bak", backup.getFileName().toString());
		assertEquals("<broken", Files.readString(backup));
	}

	@Test
	void childHelpers() {
		Element root = XmlFiles.parse("<a><b>1</b><b>2</b><c/></a>").getDocumentElement();

		assertEquals(List.of("1", "2"), XmlFiles.children(root, "b").stream().map(Element::getTextContent).toList());
		assertNull(XmlFiles.childText(root, "none"));
		assertEquals("x", XmlFiles.childText(root, "none", "x"));
		assertEquals("", XmlFiles.childText(root, "c"));
		assertEquals("y", XmlFiles.attribute(root, "id", "y"));

		XmlFiles.setChildText(root, "b", "9");
		XmlFiles.setChildText(root, "d", "new");
		XmlFiles.removeChildren(root, "b");
		assertNull(XmlFiles.child(root, "b"));
		assertEquals("new", XmlFiles.childText(root, "d"));
	}

	private static String read(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}
}

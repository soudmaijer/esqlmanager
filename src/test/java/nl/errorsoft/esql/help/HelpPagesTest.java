package nl.errorsoft.esql.help;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class HelpPagesTest {

	@Test
	void rendersHeadingsTablesAndCode() {
		String html = HelpPages.toHtml("""
			# Title

			| Key | Action |
			|---|---|
			| `Cmd+Enter` | run |
			""");

		assertTrue(html.contains("<h1>Title</h1>"), html);
		assertTrue(html.contains("<table border=\"1\" cellspacing=\"0\">"), html);
		assertTrue(html.contains("<code>Cmd+Enter</code>"), html);
		assertTrue(html.startsWith("<html><head><style"), html);
	}

	@Test
	void scalesWideImagesFromTheDocsFolder() {
		String html = HelpPages.toHtml("![shot](screenshot.png)");

		assertTrue(html.contains("width=\"" + HelpPages.MAX_IMAGE_WIDTH + "\""), html);
		assertTrue(html.contains("height=\""), html);
	}

	@Test
	void resolvesLinks() {
		assertEquals(new HelpPages.Link.Page("query.md"), HelpPages.resolve("query.md"));
		assertEquals(new HelpPages.Link.Page("designer.md"), HelpPages.resolve("designer.md#foreign-keys"));
		assertEquals(new HelpPages.Link.Page("index.md"), HelpPages.resolve("./index.md"));
		assertEquals(new HelpPages.Link.Web(URI.create("https://lucide.dev")), HelpPages.resolve("https://lucide.dev"));
		assertInstanceOf(HelpPages.Link.Ignored.class, HelpPages.resolve("#top"));
		assertInstanceOf(HelpPages.Link.Ignored.class, HelpPages.resolve("../README.md"));
		assertInstanceOf(HelpPages.Link.Ignored.class, HelpPages.resolve("mailto:someone@example.com"));
	}

	@Test
	void everyPageRenders() {
		for (String page : new String[]{"index.md", "getting-started.md", "tables-and-columns.md", "query.md", "designer.md", "settings.md"}) {
			assertNotNull(HelpPages.resource(page), page);
			assertTrue(HelpPages.render(page).contains("<h1>"), page);
		}
	}
}

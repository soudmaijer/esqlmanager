package nl.errorsoft.esql.help;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Image;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * The user documentation: Markdown pages in docs/ (on the classpath under /docs), rendered to HTML that Swing's HTML 3.2 renderer can show.
 */
public final class HelpPages {
	public static final String FOLDER = "/docs/";
	public static final String INDEX = "index.md";

	/** Wider images are scaled down, Swing does not fit them to the page. */
	static final int MAX_IMAGE_WIDTH = 760;

	private static final String STYLE = """
		body { font-family: sans-serif; font-size: 13pt; color: #000000; background-color: #ffffff; margin: 12px; }
		h1 { font-size: 20pt; margin-top: 4px; margin-bottom: 8px; }
		h2 { font-size: 16pt; margin-top: 14px; margin-bottom: 6px; }
		h3 { font-size: 14pt; margin-top: 10px; margin-bottom: 4px; }
		p { margin-top: 4px; margin-bottom: 6px; }
		code { font-family: monospace; font-size: 12pt; color: #24292f; }
		pre { font-family: monospace; font-size: 12pt; background-color: #f3f4f6; padding: 6px; }
		table { border-collapse: collapse; }
		th { background-color: #eef0f3; text-align: left; padding: 3px; }
		td { padding: 3px; }
		a { color: #0b57d0; }
		""";

	private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
	private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

	private HelpPages() {
	}

	/** Where a link in a help page leads. */
	public sealed interface Link {
		/** Another help page, by its file name in docs/. */
		record Page(String name) implements Link {
		}

		/** A web page, opened in the browser. */
		record Web(URI uri) implements Link {
		}

		/** Anything else (an anchor, a mail address, a file outside docs/): not followed. */
		record Ignored(String href) implements Link {
		}
	}

	/** Decides where a link leads: http(s) to the browser, a .md file (with or without an anchor) to that help page. */
	public static Link resolve(String href) {
		if (href == null || href.isBlank()) {
			return new Link.Ignored(String.valueOf(href));
		}
		String target = href.trim();
		if (target.startsWith("http://") || target.startsWith("https://")) {
			return new Link.Web(URI.create(target));
		}
		int hash = target.indexOf('#');
		String path = hash >= 0 ? target.substring(0, hash) : target;
		if (path.endsWith(".md")) {
			String name = path.substring(path.lastIndexOf('/') + 1);
			if (!path.contains("..") && !path.contains(":")) {
				return new Link.Page(name);
			}
		}
		return new Link.Ignored(target);
	}

	/** The URL of a page or image in docs/ on the classpath, null when it is not there. */
	public static URL resource(String name) {
		return HelpPages.class.getResource(FOLDER + name);
	}

	/** Reads a page from the classpath and renders it. */
	public static String render(String name) {
		URL url = resource(name);
		if (url == null) {
			throw new IllegalArgumentException("No help page " + name);
		}
		try (InputStream in = url.openStream()) {
			return toHtml(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Renders Markdown to a complete HTML page with the help stylesheet. */
	public static String toHtml(String markdown) {
		HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS).attributeProviderFactory(context -> (node, tagName, attributes) -> {
			if (node instanceof Image) {
				scale(attributes);
			} else if ("table".equals(tagName)) {
				// Swing ignores CSS borders on tables, the attributes still work.
				attributes.put("border", "1");
				attributes.put("cellspacing", "0");
			}
		}).build();
		String body = renderer.render(PARSER.parse(markdown));
		return "<html><head><style type=\"text/css\">" + STYLE + "</style></head><body>" + body + "</body></html>";
	}

	/** Gives a local image a width and height that fit the page. */
	private static void scale(java.util.Map<String, String> attributes) {
		String src = attributes.get("src");
		if (src == null || src.contains(":") || src.contains("..")) {
			return;
		}
		int[] size = imageSize(src);
		if (size == null) {
			return;
		}
		int width = Math.min(size[0], MAX_IMAGE_WIDTH);
		int height = size[1] * width / size[0];
		attributes.put("width", Integer.toString(width));
		attributes.put("height", Integer.toString(height));
	}

	/** Width and height of an image in docs/, read from its header; null when it is missing. */
	private static int[] imageSize(String name) {
		URL url = resource(name);
		if (url == null) {
			return null;
		}
		try (InputStream in = url.openStream(); ImageInputStream stream = ImageIO.createImageInputStream(in)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(stream);
				return new int[]{reader.getWidth(0), reader.getHeight(0)};
			} finally {
				reader.dispose();
			}
		} catch (IOException e) {
			// The page is still shown, the image at its own size.
			return null;
		}
	}
}

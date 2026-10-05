package nl.errorsoft.esql.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import nl.errorsoft.esql.error.EsqlException;

/**
 * The one way the application reads and writes its XML files (settings, profiles, drivers, datatypes, designer models): the DOM and Transformer of the JDK
 * with a few helpers so that call sites stay short.
 * <ul>
 * <li>Reading is secure: a DOCTYPE is refused (so no external entities or entity expansion), secure processing is on, and no parser message goes to the
 * console.</li>
 * <li>Writing produces UTF-8 with one tab per level of nesting, and replaces the file atomically: the text goes to a temporary file in the same directory,
 * which then takes the place of the old file, so a failed write leaves the old file as it was.</li>
 * <li>Text and attribute values are rejected with an {@link EsqlException} when they hold a character XML cannot store (a control character other than tab,
 * line feed and carriage return, or an unpaired surrogate). Rejecting instead of removing the character means a value is never changed silently.</li>
 * <li>Every failure is an {@link EsqlException} that names the file.</li>
 * </ul>
 */
public final class XmlFiles {
	private static final ErrorHandler STRICT = new ErrorHandler() {
		@Override
		public void warning(SAXParseException e) {
			// A warning is no reason to refuse a file.
		}

		@Override
		public void error(SAXParseException e) throws SAXParseException {
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXParseException {
			throw e;
		}
	};

	private XmlFiles() {
	}

	// Reading

	/** The document in the file; a missing, unreadable or malformed file is an {@link EsqlException}. */
	public static Document read(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			return parse(new InputSource(in));
		} catch (IOException | EsqlException e) {
			throw new EsqlException(file.getFileName() + " cannot be read: " + e.getMessage(), e);
		}
	}

	/** The document in the text; malformed text is an {@link EsqlException}. */
	public static Document parse(String xml) {
		return parse(new InputSource(new StringReader(xml)));
	}

	private static Document parse(InputSource source) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(STRICT);
			return builder.parse(source);
		} catch (Exception e) {
			throw new EsqlException("not valid XML: " + e.getMessage(), e);
		}
	}

	/** An empty document with a root element of the given name. */
	public static Document newDocument(String rootName) {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			document.appendChild(document.createElement(rootName));
			return document;
		} catch (Exception e) {
			throw new EsqlException("An XML document cannot be created: " + e.getMessage(), e);
		}
	}

	// Writing

	/** The document as text with an XML declaration, tab indented. */
	public static String toString(Document document) {
		try {
			Document copy = (Document) document.cloneNode(true);
			indent(copy.getDocumentElement(), 1);

			Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			StringWriter out = new StringWriter();
			out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			transformer.transform(new DOMSource(copy), new StreamResult(out));
			out.write("\n");
			return out.toString();
		} catch (Exception e) {
			throw new EsqlException("The XML cannot be written: " + e.getMessage(), e);
		}
	}

	/** Writes the document to the file in one step: the old file stays as it was when this fails. */
	public static void write(Path file, Document document) {
		String text = toString(document);
		Path directory = file.toAbsolutePath().getParent();
		Path temporary = null;
		try {
			temporary = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
			Files.writeString(temporary, text, StandardCharsets.UTF_8);
			try {
				Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				} catch (IOException ignored) {
					// The write failed already, a temporary file that stays behind is not worth a second message.
				}
			}
			throw new EsqlException(file.getFileName() + " cannot be written: " + e.getMessage(), e);
		}
	}

	/** Copies a file that cannot be read to {@code <name>.bak} next to it, replacing an older backup, and returns the backup. */
	public static Path backup(Path file) throws IOException {
		Path backup = file.resolveSibling(file.getFileName() + ".bak");
		Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
		return backup;
	}

	/** Puts a line break and the indent before every child of an element that holds only elements, and before its end tag. Text is left alone. */
	private static void indent(Element element, int depth) {
		List<Element> children = elements(element);
		if (children.isEmpty() || !onlyElementsAndBlankText(element)) {
			return;
		}
		for (Node child = element.getFirstChild(); child != null;) {
			Node next = child.getNextSibling();
			if (child.getNodeType() == Node.TEXT_NODE) {
				element.removeChild(child);
			}
			child = next;
		}
		Document document = element.getOwnerDocument();
		for (Element child : children) {
			element.insertBefore(document.createTextNode("\n" + "\t".repeat(depth)), child);
			indent(child, depth + 1);
		}
		element.appendChild(document.createTextNode("\n" + "\t".repeat(depth - 1)));
	}

	private static boolean onlyElementsAndBlankText(Element element) {
		for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.TEXT_NODE && !child.getTextContent().isBlank() || child.getNodeType() == Node.CDATA_SECTION_NODE) {
				return false;
			}
		}
		return true;
	}

	// Elements

	/** The first child element with the name, null when there is none. */
	public static Element child(Element parent, String name) {
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && element.getTagName().equals(name)) {
				return element;
			}
		}
		return null;
	}

	/** The child elements with the name, in document order. */
	public static List<Element> children(Element parent, String name) {
		List<Element> found = new ArrayList<>();
		for (Element element : elements(parent)) {
			if (element.getTagName().equals(name)) {
				found.add(element);
			}
		}
		return found;
	}

	private static List<Element> elements(Element parent) {
		List<Element> found = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element) {
				found.add(element);
			}
		}
		return found;
	}

	/** The text of a child element, null when there is no such child. */
	public static String childText(Element parent, String name) {
		Element child = child(parent, name);
		return child == null ? null : child.getTextContent();
	}

	/** The text of a child element, the fallback when there is no such child. */
	public static String childText(Element parent, String name, String fallback) {
		String text = childText(parent, name);
		return text == null ? fallback : text;
	}

	/** The value of an attribute, the fallback when it is not there. */
	public static String attribute(Element element, String name, String fallback) {
		return element.hasAttribute(name) ? element.getAttribute(name) : fallback;
	}

	/** Sets an attribute; a value with a character XML cannot store is an {@link EsqlException}. */
	public static void setAttribute(Element element, String name, String value) {
		element.setAttribute(name, checked(value));
	}

	/** Adds an empty child element at the end. */
	public static Element addChild(Element parent, String name) {
		Element child = parent.getOwnerDocument().createElement(name);
		parent.appendChild(child);
		return child;
	}

	/** Adds a child element with the text (null is empty) at the end. */
	public static Element addChild(Element parent, String name, String text) {
		Element child = addChild(parent, name);
		child.setTextContent(checked(text));
		return child;
	}

	/** Sets the text of the child element with the name, adding the child when it is missing. */
	public static Element setChildText(Element parent, String name, String text) {
		Element child = child(parent, name);
		if (child == null) {
			child = addChild(parent, name);
		}
		child.setTextContent(checked(text));
		return child;
	}

	/** Removes every child element with the name. */
	public static void removeChildren(Element parent, String name) {
		for (Element child : children(parent, name)) {
			parent.removeChild(child);
		}
	}

	private static String checked(String text) {
		if (text == null) {
			return "";
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isHighSurrogate(c) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
				i++;
			} else if (!isXmlChar(c)) {
				throw new EsqlException(String.format("The text contains a character that XML cannot store (U+%04X)", (int) c));
			}
		}
		return text;
	}

	private static boolean isXmlChar(char c) {
		return c == '\t' || c == '\n' || c == '\r' || c >= 0x20 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD;
	}
}

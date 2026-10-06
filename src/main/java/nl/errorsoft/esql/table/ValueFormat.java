package nl.errorsoft.esql.table;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import nl.errorsoft.esql.error.EsqlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** What kind of text a cell holds, guessed from the text, and pretty printing for JSON and XML. Pure, no Swing. */
public enum ValueFormat {
	JSON, XML, SQL, PLAIN;

	private static final Pattern SQL_START = Pattern.compile("^(select|insert|update|delete|create|alter|drop|with|grant|revoke)\\b.*",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final String INDENT = "  ";

	/** The kind of a text: an object or array is JSON, an element is XML, a statement is SQL, anything else plain. */
	public static ValueFormat detect(String text) {
		String trimmed = text == null ? "" : text.strip();

		if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
			return JSON;
		} else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
			return XML;
		} else if (SQL_START.matcher(trimmed).matches()) {
			return SQL;
		}
		return PLAIN;
	}

	/** Whether {@link #format} can pretty print this kind. */
	public boolean canFormat() {
		return this == JSON || this == XML;
	}

	/** The text pretty printed; an {@link EsqlException} when it is not valid JSON or XML. */
	public String format(String text) throws EsqlException {
		return switch (this) {
			case JSON -> formatJson(text.strip());
			case XML -> formatXml(text.strip());
			default -> text;
		};
	}

	/** Indents JSON by the brackets outside strings; checks that brackets and strings are balanced. */
	private static String formatJson(String text) throws EsqlException {
		StringBuilder out = new StringBuilder();
		StringBuilder open = new StringBuilder();
		boolean inString = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (inString) {
				out.append(c);
				if (c == '\\' && i + 1 < text.length()) {
					out.append(text.charAt(++i));
				} else if (c == '"') {
					inString = false;
				}
				continue;
			}
			switch (c) {
				case '"' -> {
					inString = true;
					out.append(c);
				}
				case '{', '[' -> {
					open.append(c);
					out.append(c);
					char close = c == '{' ? '}' : ']';
					int next = nextSignificant(text, i + 1);

					if (next < text.length() && text.charAt(next) == close) {
						out.append(close);
						open.setLength(open.length() - 1);
						i = next;
					} else {
						newLine(out, open.length());
					}
				}
				case '}', ']' -> {
					char expected = c == '}' ? '{' : '[';

					if (open.isEmpty() || open.charAt(open.length() - 1) != expected) {
						throw new EsqlException("Not valid JSON: unexpected '" + c + "'.");
					}
					open.setLength(open.length() - 1);
					newLine(out, open.length());
					out.append(c);
				}
				case ',' -> {
					out.append(c);
					newLine(out, open.length());
				}
				case ':' -> out.append(": ");
				case ' ', '\t', '\n', '\r' -> {
					// Whitespace between tokens is replaced by the indentation.
				}
				default -> out.append(c);
			}
		}
		if (inString || !open.isEmpty()) {
			throw new EsqlException("Not valid JSON: a string or bracket is not closed.");
		}
		return out.toString();
	}

	private static int nextSignificant(String text, int from) {
		int i = from;

		while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
			i++;
		}
		return i;
	}

	private static void newLine(StringBuilder out, int depth) {
		out.append('\n').append(INDENT.repeat(depth));
	}

	/** Indents XML by two spaces; a document type is refused, so nothing is fetched from outside. */
	private static String formatXml(String text) throws EsqlException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(text)));

			// The whitespace between elements would otherwise come back as extra empty lines.
			NodeList blanks = (NodeList) XPathFactory.newInstance().newXPath().evaluate("//text()[normalize-space()='']", document, XPathConstants.NODESET);
			for (int i = 0; i < blanks.getLength(); i++) {
				Node blank = blanks.item(i);
				blank.getParentNode().removeChild(blank);
			}
			var transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, text.toLowerCase(Locale.ROOT).startsWith("<?xml") ? "no" : "yes");
			StringWriter out = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(out));
			return out.toString().strip();
		} catch (Exception e) {
			throw new EsqlException("Not valid XML: " + e.getMessage());
		}
	}
}

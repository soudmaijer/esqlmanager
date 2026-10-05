package nl.errorsoft.esql.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.text.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class SyntaxDocument extends DefaultStyledDocument {
	private static final Logger log = LogManager.getLogger(SyntaxDocument.class);

	Vector keywords = new Vector();

	public SyntaxDocument() {
		try {
			SAXBuilder saxbuilder = new SAXBuilder();
			Document driverData = saxbuilder.build(new File("conf/syntax.xml"));

			List list = null;

			if (driverData.hasRootElement()) {
				list = driverData.getRootElement().getChildren("keyword");
			}

			for (int i = 0; i < list.size(); i++) {
				keywords.add(((Element) list.get(i)).getText());
			}
		} catch (Exception exception) {
			log.warn("Warning: syntax.xml could not be loaded, no syntax highlighting will be available!");
		}
	}

	/** Appends the text as it is, quoted text in green and keywords in blue. */
	public void append(String text) {
		StringTokenizer st = new StringTokenizer(text, " '\"`", true);
		Color quoted = new Color(71, 134, 41);
		String quote = null;

		while (st.hasMoreTokens()) {
			String t = st.nextToken();

			if (quote != null) {
				this.append(t, quoted, false);

				if (t.equals(quote)) {
					quote = null;
				}
			} else if (t.equals("'") || t.equals("\"") || t.equals("`")) {
				this.append(t, quoted, false);
				quote = t;
			} else {
				this.appendKeyword(t);
			}
		}
	}

	public void appendKeyword(String text) {
		for (int i = 0; i < keywords.size(); i++) {
			if (keywords.get(i).toString().equalsIgnoreCase(text.trim())) {
				this.append(text, Color.blue, true);
				return;
			}
		}
		this.append(text, UIManager.getColor("TextPane.foreground"), false);
	}

	private void append(String text, Color c, boolean bold) {
		SimpleAttributeSet sas = new SimpleAttributeSet();
		StyleConstants.setBold(sas, bold);
		StyleConstants.setForeground(sas, c);
		try {
			this.insertString(this.getLength(), text, sas);
		} catch (Exception e) {
		}
	}
}

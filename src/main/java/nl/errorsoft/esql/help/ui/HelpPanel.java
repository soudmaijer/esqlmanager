package nl.errorsoft.esql.help.ui;

import nl.errorsoft.esql.help.HelpPages;
import nl.errorsoft.esql.ui.util.DesktopUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;

/** Shows the Markdown help pages from docs/. Links to other pages stay in the pane, web links open in the browser. */
public class HelpPanel extends JEditorPane {
	private static final Logger log = LogManager.getLogger(HelpPanel.class);
	private String page;

	public HelpPanel() {
		setEditable(false);
		setContentType("text/html");
		addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				follow(e.getDescription());
			}
		});
		show(HelpPages.INDEX);
	}

	/** Shows the page again in the colours of the new look and feel. */
	@Override
	public void updateUI() {
		super.updateUI();
		if (page != null) {
			show(page);
		}
	}

	/** Shows a page of docs/ by its file name, for example "query.md". */
	public void show(String name) {
		page = name;
		boolean dark = FlatLaf.isLafDark();
		// FlatLaf paints a read-only pane grey, the page has the background of its stylesheet.
		setBackground(dark ? new Color(0x1e1f22) : Color.white);
		try {
			HTMLEditorKit kit = (HTMLEditorKit) getEditorKit();
			HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
			// Images are relative to the page.
			document.setBase(HelpPages.resource(name));
			kit.read(new StringReader(HelpPages.render(name, dark)), document, 0);
			setDocument(document);
			setCaretPosition(0);
		} catch (IOException | javax.swing.text.BadLocationException | RuntimeException e) {
			log.warn("The help page {} could not be shown: {}", name, e.getMessage());
		}
	}

	private void follow(String href) {
		switch (HelpPages.resolve(href)) {
			case HelpPages.Link.Page page -> show(page.name());
			case HelpPages.Link.Web web -> DesktopUtils.openInBrowser(web.uri());
			case HelpPages.Link.Ignored ignored -> log.debug("Link not followed: {}", ignored.href());
		}
	}
}

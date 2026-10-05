package nl.errorsoft.esql.ui.util;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class HyperLinkListener implements javax.swing.event.HyperlinkListener {
	private static final Logger log = LogManager.getLogger(HyperLinkListener.class);

	ConnectionWindowCC cwcc;

	public HyperLinkListener(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
		if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
			JEditorPane pane = (JEditorPane) e.getSource();

			try {
				if (e.getURL().getFile().indexOf("USERMANAGER") != -1) {
					//UserPriviliges up = new UserPriviliges(jm, cw);
				} else {
					pane.setPage(e.getURL());
				}
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}
}

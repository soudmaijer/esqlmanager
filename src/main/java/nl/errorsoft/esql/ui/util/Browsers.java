package nl.errorsoft.esql.ui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Opens links in the system browser. */
public final class Browsers {
	private static final Logger log = LogManager.getLogger(Browsers.class);

	private Browsers() {
	}

	/** Opens a web page in the system browser. */
	public static void open(URI uri) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			log.warn("No browser available to open {}", uri);
			return;
		}
		try {
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
			log.warn("Could not open {}: {}", uri, e.getMessage());
		}
	}
}

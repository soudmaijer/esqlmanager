package nl.errorsoft.esql.ui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import nl.errorsoft.esql.error.EsqlException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Opens links in the system browser and folders in the file manager. */
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

	/** Opens a folder in the file manager (Finder, Explorer), creating it when it does not exist yet. */
	public static void openFolder(Path folder) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			throw new EsqlException("This system cannot open folders. The folder is " + folder);
		}
		try {
			Files.createDirectories(folder);
			Desktop.getDesktop().open(folder.toFile());
		} catch (IOException e) {
			throw new EsqlException("Cannot open " + folder + ": " + e.getMessage(), e);
		}
	}
}

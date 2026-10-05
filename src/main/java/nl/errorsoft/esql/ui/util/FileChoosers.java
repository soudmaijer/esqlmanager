package nl.errorsoft.esql.ui.util;

import java.io.File;
import javax.swing.JFileChooser;

import nl.errorsoft.esql.app.ApplicationContext;

/** File choosers of the windows that move files (export, import, upload and download), which start in the default folder of the preferences. */
public final class FileChoosers {
	private FileChoosers() {
	}

	/** A chooser that starts in the default folder when the preferences name one that exists. */
	public static JFileChooser create() {
		String folder = ApplicationContext.get().settings().getDefaultFolder();
		File directory = folder.isEmpty() ? null : new File(folder);
		return new JFileChooser(directory != null && directory.isDirectory() ? directory : null);
	}
}

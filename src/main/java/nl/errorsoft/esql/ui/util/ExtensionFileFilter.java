package nl.errorsoft.esql.ui.util;

import java.io.File;
import javax.swing.filechooser.*;

public class ExtensionFileFilter extends FileFilter {

	String[] extensions;
	String description;

	public ExtensionFileFilter(String description, String[] extensions) {
		this.extensions = extensions != null ? extensions : new String[]{};
		this.description = description == null ? extList() : description + " (" + extList() + ")";
	}

	private String extList() {
		int count = extensions.length;
		if (count > 0) {
			String joined = extensions[0];

			for (int j = 1; j < count; joined += ", " + extensions[j++]) {
			}
			return joined;
		} else {
			return "";
		}
	}

	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}

		if (file.isFile()) {
			String fileName = file.getName().toLowerCase();

			for (int j = extensions.length; j-- > 0;) {
				if (fileName.endsWith(extensions[j])) {
					return true;
				}
			}
		}
		return false;
	}
	public String getDescription() {
		return description;
	}
}

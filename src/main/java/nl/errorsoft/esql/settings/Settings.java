package nl.errorsoft.esql.settings;

import nl.errorsoft.esql.app.DataDirectory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * The preferences of the user, kept in {@code conf/settings.xml}: the appearance, the font size of the editors, the folder the export, import and
 * file transfer windows start in, and the default file encoding. A missing file, a missing element or a value that makes no sense gives the default.
 */
public class Settings {
	public static final int MIN_FONT_SIZE = 8;
	public static final int MAX_FONT_SIZE = 32;
	public static final int DEFAULT_FONT_SIZE = 13;

	private static final Logger log = LogManager.getLogger(Settings.class);

	private final File file;
	private Appearance appearance = Appearance.SYSTEM;
	private int editorFontSize = DEFAULT_FONT_SIZE;
	private String defaultFolder = "";
	private Charset defaultEncoding = StandardCharsets.UTF_8;

	public Settings() {
		this(DataDirectory.file("conf/settings.xml"));
	}

	/** Settings kept in the given file, read now. */
	public Settings(File file) {
		this.file = file;
		loadSettings();
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	public int getEditorFontSize() {
		return editorFontSize;
	}

	/** The size is kept between {@value #MIN_FONT_SIZE} and {@value #MAX_FONT_SIZE}. */
	public void setEditorFontSize(int size) {
		this.editorFontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
	}

	/** The folder the export, import and file transfer windows start in, empty when there is none. */
	public String getDefaultFolder() {
		return defaultFolder;
	}

	public void setDefaultFolder(String folder) {
		this.defaultFolder = folder == null ? "" : folder.trim();
	}

	/** The encoding the export and import windows start with. */
	public Charset getDefaultEncoding() {
		return defaultEncoding;
	}

	public void setDefaultEncoding(Charset encoding) {
		this.defaultEncoding = encoding == null ? StandardCharsets.UTF_8 : encoding;
	}

	public void loadSettings() {
		if (!file.isFile()) {
			log.info("{} does not exist, using the default settings", file);
			return;
		}

		try {
			Element root = new SAXBuilder().build(file).getRootElement();
			appearance = Appearance.of(root.getChildText("appearance"));
			setEditorFontSize(parseInt(root.getChildText("editorFontSize"), DEFAULT_FONT_SIZE));
			setDefaultFolder(root.getChildText("defaultFolder"));
			setDefaultEncoding(parseCharset(root.getChildText("defaultEncoding")));
		} catch (Exception e) {
			log.error("Could not read {}: {}", file, e.getMessage(), e);
		}
	}

	public boolean saveSettings() {
		Element root = new Element("config");
		root.addContent(new Element("appearance").setText(appearance.name()));
		root.addContent(new Element("editorFontSize").setText(String.valueOf(editorFontSize)));
		root.addContent(new Element("defaultFolder").setText(defaultFolder));
		root.addContent(new Element("defaultEncoding").setText(defaultEncoding.name()));

		try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			new XMLOutputter(Format.getPrettyFormat().setIndent("\t")).output(new Document(root), out);
			return true;
		} catch (IOException e) {
			log.error("Could not write {}: {}", file, e.getMessage());
			return false;
		}
	}

	private static int parseInt(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static Charset parseCharset(String name) {
		try {
			return Charset.forName(name.trim());
		} catch (RuntimeException e) {
			return StandardCharsets.UTF_8;
		}
	}
}

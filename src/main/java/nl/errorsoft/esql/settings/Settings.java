package nl.errorsoft.esql.settings;

import nl.errorsoft.esql.app.DataDirectory;
import nl.errorsoft.esql.error.EsqlException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
	/** Why the file could not be read, until the user has been told once; null when it was read or did not exist. */
	private EsqlException loadProblem;

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

	/**
	 * Reads the file. A missing file gives the defaults. A file that cannot be read also gives the defaults, but is first copied to a .bak file next to
	 * it so that the next save does not destroy it, and {@link #takeLoadProblem} tells the user once.
	 */
	public void loadSettings() {
		if (!file.isFile()) {
			// A first start: there is nothing to read and nothing to tell.
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
			loadProblem = new EsqlException(keepCorruptFile(e), e);
		}
	}

	/** Copies the unreadable file to a backup and returns the message for the user. */
	private String keepCorruptFile(Exception cause) {
		File backup = backupFile();
		try {
			Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return file.getName() + " cannot be read (" + cause.getMessage() + "). The default settings are used; the file was kept as " + backup.getName()
				+ ".";
		} catch (IOException copyFailed) {
			return file.getName() + " cannot be read (" + cause.getMessage() + ") and could not be kept as " + backup.getName() + " ("
				+ copyFailed.getMessage() + "). The default settings are used.";
		}
	}

	/** Where an unreadable settings file is kept: settings.xml.bak next to it. */
	public File backupFile() {
		return new File(file.getPath() + ".bak");
	}

	/** Why the settings file could not be read, once: the next call returns null, so the user is told only once. */
	public synchronized EsqlException takeLoadProblem() {
		EsqlException problem = loadProblem;
		loadProblem = null;
		return problem;
	}

	/** Writes the file; a failure is thrown for the dialog that saves to report. */
	public void saveSettings() throws IOException {
		Element root = new Element("config");
		root.addContent(new Element("appearance").setText(appearance.name()));
		root.addContent(new Element("editorFontSize").setText(String.valueOf(editorFontSize)));
		root.addContent(new Element("defaultFolder").setText(defaultFolder));
		root.addContent(new Element("defaultEncoding").setText(defaultEncoding.name()));

		try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			new XMLOutputter(Format.getPrettyFormat().setIndent("\t")).output(new Document(root), out);
		}
	}

	private static int parseInt(String text, int fallback) {
		try {
			return Integer.parseInt(text.trim());
		} catch (RuntimeException e) {
			// A value that is not a number gives the default, as the class comment says.
			return fallback;
		}
	}

	private static Charset parseCharset(String name) {
		try {
			return Charset.forName(name.trim());
		} catch (RuntimeException e) {
			// An unknown encoding gives the default, as the class comment says.
			return StandardCharsets.UTF_8;
		}
	}
}

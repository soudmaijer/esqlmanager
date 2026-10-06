package nl.errorsoft.esql.settings;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.swing.UIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The look and feel the user prefers. FlatLaf paints much faster than the native macOS look and feel, especially while resizing. */
public enum Appearance {
	SYSTEM("Follow the system"), LIGHT("Light"), DARK("Dark"), NATIVE("Native (operating system)");

	private static final Logger log = LogManager.getLogger(Appearance.class);
	private static final boolean MAC = System.getProperty("os.name").toLowerCase().contains("mac");

	private final String label;

	Appearance(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}

	/** Reads the stored name, an unknown or missing name gives {@link #SYSTEM}. */
	public static Appearance of(String name) {
		try {
			return valueOf(name.trim().toUpperCase());
		} catch (RuntimeException e) {
			// A missing or unknown name in settings.xml is not a problem, the system appearance is the default.
			return SYSTEM;
		}
	}

	/** Properties that macOS only reads before the first window exists. */
	public static void prepareDesktop() {
		if (MAC) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.application.appearance", "system");
			System.setProperty("apple.awt.application.name", "eSQLManager");
		}
	}

	/** Installs this look and feel. Open windows are updated when there are any. */
	public void apply() {
		FlatLaf.setGlobalExtraDefaults(FLAT_DEFAULTS);
		boolean dark = this == DARK || this == SYSTEM && isSystemDark();
		boolean installed = switch (this) {
			case NATIVE -> installNative();
			default -> MAC ? setup(dark ? new FlatMacDarkLaf() : new FlatMacLightLaf()) : setup(dark ? new FlatDarkLaf() : new FlatLightLaf());
		};

		if (!installed) {
			installNative();
		}
		FlatLaf.updateUI();
	}

	/**
	 * FlatLaf defaults of eSQLManager, in FlatLaf properties syntax so that the colours are derived from the theme. Split pane dividers are thin and flat, without grip dots or arrows. The selected tab has a darker background and the accent underline, also when the focus is elsewhere.
	 */
	private static final Map<String, String> FLAT_DEFAULTS = Map.ofEntries(Map.entry("SplitPane.dividerSize", "5"),
		Map.entry("SplitPaneDivider.style", "plain"),
		Map.entry("SplitPaneDivider.gripDotCount", "0"), Map.entry("SplitPaneDivider.draggingColor", "$Component.focusColor"),
		Map.entry("TabbedPane.selectedBackground", "darken(@background,6%)"), Map.entry("TabbedPane.inactiveUnderlineColor", "$TabbedPane.underlineColor"));

	private static boolean setup(FlatLaf laf) {
		return FlatLaf.setup(laf);
	}

	private static boolean installNative() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			return true;
		} catch (Exception e) {
			log.warn("Could not set the look and feel", e);
			return false;
		}
	}

	/** Only macOS tells us, through the global interface style. Other systems get the light theme. */
	private static boolean isSystemDark() {
		if (!MAC) {
			return false;
		}

		try {
			Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").redirectErrorStream(true).start();
			try (InputStream output = process.getInputStream()) {
				return new String(output.readAllBytes(), StandardCharsets.UTF_8).trim().equalsIgnoreCase("Dark");
			}
		} catch (IOException e) {
			// No defaults command or no answer: the light theme is the fallback.
			return false;
		}
	}
}

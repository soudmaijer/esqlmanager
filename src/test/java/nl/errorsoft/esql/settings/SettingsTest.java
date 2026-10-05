package nl.errorsoft.esql.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
	@Test
	void aMissingFileGivesTheDefaults(@TempDir Path dir) {
		Settings settings = new Settings(dir.resolve("none.xml").toFile());

		assertEquals(Appearance.SYSTEM, settings.getAppearance());
		assertEquals(Settings.DEFAULT_FONT_SIZE, settings.getEditorFontSize());
		assertEquals("", settings.getDefaultFolder());
		assertEquals(StandardCharsets.UTF_8, settings.getDefaultEncoding());
	}

	@Test
	void whatIsSavedIsReadBack(@TempDir Path dir) {
		File file = dir.resolve("settings.xml").toFile();
		Settings settings = new Settings(file);
		settings.setAppearance(Appearance.DARK);
		settings.setEditorFontSize(18);
		settings.setDefaultFolder("/data/R&D <exports>");
		settings.setDefaultEncoding(Charset.forName("windows-1252"));
		assertTrue(settings.saveSettings());

		Settings read = new Settings(file);
		assertEquals(Appearance.DARK, read.getAppearance());
		assertEquals(18, read.getEditorFontSize());
		assertEquals("/data/R&D <exports>", read.getDefaultFolder());
		assertEquals(Charset.forName("windows-1252"), read.getDefaultEncoding());
	}

	@Test
	void anOldFileWithOnlyTheAppearanceKeepsTheDefaultsForTheRest(@TempDir Path dir) throws Exception {
		File file = dir.resolve("settings.xml").toFile();
		Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config>\n\t<appearance>LIGHT</appearance>\n</config>\n");

		Settings settings = new Settings(file);
		assertEquals(Appearance.LIGHT, settings.getAppearance());
		assertEquals(Settings.DEFAULT_FONT_SIZE, settings.getEditorFontSize());
		assertEquals(StandardCharsets.UTF_8, settings.getDefaultEncoding());
	}

	@Test
	void valuesThatMakeNoSenseFallBack(@TempDir Path dir) throws Exception {
		File file = dir.resolve("settings.xml").toFile();
		Files.writeString(file.toPath(), "<config><appearance>NOPE</appearance><editorFontSize>huge</editorFontSize>"
			+ "<defaultEncoding>no-such-charset</defaultEncoding></config>");

		Settings settings = new Settings(file);
		assertEquals(Appearance.SYSTEM, settings.getAppearance());
		assertEquals(Settings.DEFAULT_FONT_SIZE, settings.getEditorFontSize());
		assertEquals(StandardCharsets.UTF_8, settings.getDefaultEncoding());

		settings.setEditorFontSize(500);
		assertEquals(Settings.MAX_FONT_SIZE, settings.getEditorFontSize());
		settings.setEditorFontSize(1);
		assertEquals(Settings.MIN_FONT_SIZE, settings.getEditorFontSize());
	}
}

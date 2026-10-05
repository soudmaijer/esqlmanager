package nl.errorsoft.esql.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	void whatIsSavedIsReadBack(@TempDir Path dir) throws Exception {
		File file = dir.resolve("settings.xml").toFile();
		Settings settings = new Settings(file);
		settings.setAppearance(Appearance.DARK);
		settings.setEditorFontSize(18);
		settings.setDefaultFolder("/data/R&D <exports>");
		settings.setDefaultEncoding(Charset.forName("windows-1252"));
		settings.saveSettings();

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
	void aCorruptFileGivesTheDefaultsIsKeptAndIsReportedOnce(@TempDir Path dir) throws Exception {
		File file = dir.resolve("settings.xml").toFile();
		Files.writeString(file.toPath(), "<config><appearance>DARK</appear");

		Settings settings = new Settings(file);
		assertEquals(Appearance.SYSTEM, settings.getAppearance());
		assertEquals("<config><appearance>DARK</appear", Files.readString(settings.backupFile().toPath()));
		var problem = settings.takeLoadProblem();
		assertTrue(problem.getMessage().startsWith("settings.xml cannot be read"), problem.getMessage());
		assertTrue(problem.getMessage().endsWith("kept as settings.xml.bak."), problem.getMessage());
		assertNull(settings.takeLoadProblem());
	}

	@Test
	void savingWhereNothingCanBeWrittenFails(@TempDir Path dir) {
		Settings settings = new Settings(dir.resolve("missing").resolve("settings.xml").toFile());
		assertThrows(java.io.IOException.class, settings::saveSettings);
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

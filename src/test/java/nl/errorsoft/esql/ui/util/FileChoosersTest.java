package nl.errorsoft.esql.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

class FileChoosersTest {
	@Test
	void addsTheExtensionOnlyToANameWithoutOne() {
		assertEquals(new File("/tmp/notes.txt"), FileChoosers.withExtension(new File("/tmp/notes"), ".txt"));
		assertEquals(new File("/tmp/notes.txt"), FileChoosers.withExtension(new File("/tmp/notes.txt"), ".txt"));
		assertEquals(new File("/tmp/notes.md"), FileChoosers.withExtension(new File("/tmp/notes.md"), ".txt"));
		// A dot only at the start is a hidden file, not an extension.
		assertEquals(new File("/tmp/.notes.txt"), FileChoosers.withExtension(new File("/tmp/.notes"), ".txt"));
	}
}

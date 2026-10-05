package nl.errorsoft.esql.ui.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.JComboBox;

/** The file encodings offered in the export and import windows. */
public final class Encodings {
	/** The character sets people use for SQL scripts, UTF-8 first. */
	public static final List<Charset> COMMON = List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.ISO_8859_1,
		Charset.forName("windows-1252"), StandardCharsets.US_ASCII);

	private Encodings() {
	}

	/** A combo box of {@link #COMMON} with the given encoding selected; an encoding that is not in the list is added. */
	public static JComboBox<Charset> combo(Charset selected) {
		JComboBox<Charset> combo = new JComboBox<>(COMMON.toArray(new Charset[0]));
		if (!COMMON.contains(selected)) {
			combo.addItem(selected);
		}
		combo.setSelectedItem(selected);
		return combo;
	}
}

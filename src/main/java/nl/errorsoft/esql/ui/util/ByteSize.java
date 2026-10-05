package nl.errorsoft.esql.ui.util;

import java.util.Locale;

/** Bytes as people read them: 1536 is "1.5 KB". */
public final class ByteSize {
	private static final String[] UNITS = {"bytes", "KB", "MB", "GB", "TB"};

	private ByteSize() {
	}

	public static String format(long bytes) {
		double value = bytes;
		int unit = 0;

		while (value >= 1024 && unit < UNITS.length - 1) {
			value /= 1024;
			unit++;
		}
		return unit == 0 ? bytes + " bytes" : String.format(Locale.ROOT, "%.1f %s", value, UNITS[unit]);
	}
}

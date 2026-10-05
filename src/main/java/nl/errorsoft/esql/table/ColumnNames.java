package nl.errorsoft.esql.table;

import java.util.List;
import java.util.Locale;

/** Default names for new columns. */
public final class ColumnNames {
	private ColumnNames() {
	}

	/** The first of column_1, column_2, ... that is not in use (names are compared ignoring case). */
	public static String next(List<String> existing) {
		int n = 1;
		while (isUsed("column_" + n, existing)) {
			n++;
		}
		return "column_" + n;
	}

	/** True when the name is in use, ignoring case and surrounding spaces. */
	public static boolean isUsed(String name, List<String> existing) {
		String wanted = name.trim().toLowerCase(Locale.ROOT);
		return existing.stream().anyMatch(other -> other.trim().toLowerCase(Locale.ROOT).equals(wanted));
	}
}

package nl.errorsoft.esql.designer.ui.dialog;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** The checks on the fields of a table in the designer, without Swing. */
public final class FieldRules {
	private static final Pattern LENGTH = Pattern.compile("\\d+(\\s*,\\s*\\d+)?");

	private FieldRules() {
	}

	/** The problem with a length, null when it is fine. A length is a number ("255") or precision and scale ("10,2"); ENUM and SET list their values. */
	public static String lengthProblem(String typeName, String length) {
		if (length == null || length.isBlank()) {
			return null;
		}
		if ("enum".equalsIgnoreCase(typeName) || "set".equalsIgnoreCase(typeName)) {
			return null;
		}
		return LENGTH.matcher(length.trim()).matches() ? null : "The length must be a number, such as 255 or 10,2.";
	}

	/** The first problem in a list of field names (empty or used twice), null when they are fine. Names are compared ignoring case. */
	public static String nameProblem(List<String> names) {
		Set<String> seen = new HashSet<>();
		for (String name : names) {
			if (name == null || name.isBlank()) {
				return "A field has no name.";
			}
			if (!seen.add(name.trim().toLowerCase(Locale.ROOT))) {
				return "The field name '" + name.trim() + "' is used twice.";
			}
		}
		return null;
	}

	/** A name like base_2 that is not in use yet, base itself when it is free. */
	public static String uniqueName(String base, List<String> existing) {
		Set<String> used = new HashSet<>();
		for (String name : existing) {
			used.add(name.toLowerCase(Locale.ROOT));
		}
		if (!used.contains(base.toLowerCase(Locale.ROOT))) {
			return base;
		}
		int n = 2;
		while (used.contains((base + "_" + n).toLowerCase(Locale.ROOT))) {
			n++;
		}
		return base + "_" + n;
	}
}

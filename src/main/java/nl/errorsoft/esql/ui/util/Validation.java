package nl.errorsoft.esql.ui.util;

import java.util.Arrays;

/** Checks for the input of a dialog. Each returns the message to show, or null when the input is fine. */
public final class Validation {
	private Validation() {
	}

	/** "Enter a name." for a blank value; {@code what} is the object of the sentence, for example "a name". */
	public static String required(String what, String value) {
		return value == null || value.isBlank() ? "Enter " + what + "." : null;
	}

	/** The message when the two passwords differ. */
	public static String same(char[] first, char[] second, String message) {
		return Arrays.equals(first, second) ? null : message;
	}

	/** The first message that is not null, so checks can be listed in the order they should be reported. */
	public static String first(String... messages) {
		for (String message : messages) {
			if (message != null) {
				return message;
			}
		}
		return null;
	}
}

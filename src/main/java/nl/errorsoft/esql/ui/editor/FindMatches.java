package nl.errorsoft.esql.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Where a text occurs in a document and which occurrence is the current one, for the "2 of 5" of the find bar. Without Swing, so it can be tested. */
public final class FindMatches {
	private FindMatches() {
	}

	/** The start of every occurrence of {@code query} in {@code text}, overlapping ones too; empty for an empty query. */
	public static List<Integer> positions(String text, String query, boolean matchCase) {
		List<Integer> positions = new ArrayList<>();
		if (query.isEmpty()) {
			return positions;
		}
		String haystack = matchCase ? text : text.toLowerCase(Locale.ROOT);
		String needle = matchCase ? query : query.toLowerCase(Locale.ROOT);
		for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
			positions.add(at);
		}
		return positions;
	}

	/** "2 of 5" when the match at {@code selectionStart} is the second of five, "5 matches" when none is selected, "No results" without any. */
	public static String describe(List<Integer> positions, int selectionStart) {
		if (positions.isEmpty()) {
			return "No results";
		}
		int index = positions.indexOf(selectionStart);
		return index < 0 ? positions.size() + (positions.size() == 1 ? " match" : " matches") : (index + 1) + " of " + positions.size();
	}
}

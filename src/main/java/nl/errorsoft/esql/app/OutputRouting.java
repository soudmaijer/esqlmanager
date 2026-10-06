package nl.errorsoft.esql.app;

import java.util.Set;

/**
 * Which tab of the output panel a log line goes to. A line written for a connection (log context {@code connection}, see
 * {@code ConnectionWindowController.logContext}) goes to the tab of that connection without a prefix; a line without a connection, or for a connection
 * whose tab is gone, goes to the Application tab, with the connection's name in front when it has one. Without Swing, so it can be tested.
 */
public final class OutputRouting {
	private OutputRouting() {
	}

	/**
	 * @param tab the connection whose tab shows the line, null for the Application tab
	 * @param text the line as it is shown there
	 */
	public record Routed(String tab, String text) {
	}

	/**
	 * @param connection the profile name from the log context, null or empty for a line of the application
	 * @param line the formatted line, without the connection
	 * @param connectionTabs the names of the connections that have a tab
	 */
	public static Routed route(String connection, String line, Set<String> connectionTabs) {
		if (connection == null || connection.isEmpty()) {
			return new Routed(null, line);
		}
		if (connectionTabs.contains(connection)) {
			return new Routed(connection, line);
		}
		return new Routed(null, withPrefix(connection, line));
	}

	/** "13:37:08 INFO  [shop] text": the name goes after the time and level, where it stood when there was one log for everything. */
	private static String withPrefix(String connection, String line) {
		int afterLevel = line.length() > 15 && line.charAt(8) == ' ' ? 15 : 0;
		return line.substring(0, afterLevel) + "[" + connection + "] " + line.substring(afterLevel);
	}
}

package nl.errorsoft.esql.app;

import java.util.function.Function;

/**
 * What the status bar of the application describes: the window in front or the node selected in the explorer, whichever the user touched last. Pure, so
 * the decision is tested without windows.
 */
public final class StatusContext {
	/** Where the user can be working. */
	public enum Source {
		WORK, EXPLORER
	}

	private Source last = Source.WORK;

	/** The user brought a window to the front, or selected or clicked a node in the explorer. */
	public void touched(Source source) {
		this.last = source;
	}

	/**
	 * What the last touched source describes; when that has nothing (no window open, nothing selected) the other source, else null.
	 *
	 * @param lookup what a source describes now, null for nothing
	 */
	public <T> T pick(Function<Source, T> lookup) {
		Source other = last == Source.WORK ? Source.EXPLORER : Source.WORK;
		T chosen = lookup.apply(last);
		return chosen != null ? chosen : lookup.apply(other);
	}

	/** The database with its schema ("shop.public"), the database alone, or empty when there is none. */
	public static String where(String database, String schema) {
		if (database == null || database.isEmpty()) {
			return "";
		}
		return schema == null || schema.isEmpty() ? database : database + "." + schema;
	}

	/** The right hand side of the status bar: the server, the account and, when a database is in play, where. */
	public static String info(String server, String account, String where) {
		return where.isEmpty() ? server + "  |  " + account : server + "  |  " + account + "  |  " + where;
	}
}

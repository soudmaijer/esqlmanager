package nl.errorsoft.esql.app;

import java.util.ArrayList;
import java.util.List;

/**
 * What leaving the application means: whether to ask first, and the words for it. Quitting is a question, not a warning: nothing is lost that was not
 * saved, so there is no dialog when nothing is open. macOS says "Quit", other systems "Exit". Pure, so the wording is tested.
 */
public record ExitPlan(int connections, int designers, boolean mac) {

	/** Whether anything is open that closes with the application. */
	public boolean needsConfirmation() {
		return connections > 0 || designers > 0;
	}

	/** The verb on the button and in the menu: "Quit" or "Exit". */
	public String verb() {
		return mac ? "Quit" : "Exit";
	}

	/** The title of the question and the menu item, with the name of the application. */
	public String title(String application) {
		return verb() + " " + application;
	}

	/** What happens: "Close 2 connections and quit?", "Close 2 connections and 1 designer, then quit?". */
	public String message() {
		List<String> parts = new ArrayList<>();

		if (connections > 0) {
			parts.add(connections + (connections == 1 ? " connection" : " connections"));
		}
		if (designers > 0) {
			parts.add(designers + (designers == 1 ? " designer" : " designers"));
		}
		String what = String.join(" and ", parts);
		return "Close " + what + (parts.size() > 1 ? ", then " : " and ") + verb().toLowerCase() + "?";
	}
}

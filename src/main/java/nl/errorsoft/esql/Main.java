package nl.errorsoft.esql;

import nl.errorsoft.esql.app.control.ESQLManagerCC;

/** Starts eSQLManager. Run from the runtime directory, which holds the configuration. */
public class Main {
	public static void main(String[] args) {
		runApplication(args);
	}

	static void runApplication(String[] args) {
		new ESQLManagerCC();
	}
}

package nl.errorsoft.esql;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.control.ESQLManagerCC;

/** Starts eSQLManager. Run from the runtime directory, which holds the configuration. */
public class Main {
	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> ApplicationContext.get().errors().report("Unexpected error", error));
		new ESQLManagerCC();
	}
}

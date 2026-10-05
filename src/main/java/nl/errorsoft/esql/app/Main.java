package nl.errorsoft.esql.app;

import nl.errorsoft.esql.app.control.ESQLManagerCC;

public class Main {
	// The JDBC drivers in dist/lib are put on the classpath by run.sh,
	// since modern Java no longer allows adding jars to the system classloader at runtime.
	public Main() throws Exception {
		new ESQLManagerCC();
	}

	public static void main(String args[]) throws Exception {
		new Main();
	}
}

package nl.errorsoft.esql.app;

/** What the application calls itself. */
public class ESQLManager {
	private static final String NAME = "eSQLManager";
	private static final String VERSION = "1.0";
	private static final int BUILD = 64;

	public String getAppName() {
		return NAME;
	}

	public String getAppVersion() {
		return VERSION;
	}

	public int getAppBuild() {
		return BUILD;
	}
}

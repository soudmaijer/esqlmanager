package nl.errorsoft.esql.driver;

/** Whether the JDBC driver of a server type can be used, and where it comes from. */
public enum DriverStatus {
	BUNDLED("Bundled"), DOWNLOADED("Downloaded"), OWN_JAR("Own jar"), NOT_INSTALLED("Not installed");

	private final String label;

	DriverStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}

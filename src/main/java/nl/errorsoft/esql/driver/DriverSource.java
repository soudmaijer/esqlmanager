package nl.errorsoft.esql.driver;

/**
 * Where the JDBC driver of a server type comes from.
 * @param className the driver class from driver.xml
 * @param ownJar a jar the user chose, empty when none
 * @param artifact the driver to download when it is not bundled, null when it is bundled
 */
public record DriverSource(String className, String ownJar, DriverArtifact artifact) {
	public DriverSource {
		ownJar = ownJar == null ? "" : ownJar.trim();
	}

	public boolean hasOwnJar() {
		return !ownJar.isEmpty();
	}
}

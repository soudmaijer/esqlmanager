package nl.errorsoft.esql.driver;

import java.util.Locale;

/**
 * A JDBC driver that is not bundled because of its licence and is downloaded from Maven Central when it is first needed. The version and the SHA-256 of the
 * jar are pinned here: they are the trust anchor, a downloaded file that does not match is deleted.
 */
public record DriverArtifact(String name, String groupId, String artifactId, String version, String sha256, long size, String licence) {

	public static final DriverArtifact MYSQL = new DriverArtifact("MySQL", "com.mysql", "mysql-connector-j", "26.7.0",
		"69084713593a4aa8d07c383619b9639276f08bccf8faf1c562178147d389b1e1", 2_613_206, "GPLv2 with the Universal FOSS Exception");

	public static final DriverArtifact ORACLE = new DriverArtifact("Oracle", "com.oracle.database.jdbc", "ojdbc11", "23.26.3.0.0",
		"764cc3f88454d1be117e155d01ed617467b984c9c68f13090dd6083799d2bb8c", 7_779_716, "Oracle Free Use Terms and Conditions (FUTC)");

	/** The name of the jar, such as {@code mysql-connector-j-26.7.0.jar}. */
	public String fileName() {
		return artifactId + "-" + version + ".jar";
	}

	/** The path of the jar in a Maven repository. */
	public String repositoryPath() {
		return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
	}

	/** The size for people, such as "2.6 MB". */
	public String sizeText() {
		return String.format(Locale.ROOT, "%.1f MB", size / 1_000_000.0);
	}
}

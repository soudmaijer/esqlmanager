package nl.errorsoft.esql.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Build information of the application: its name, the version from the pom and the commit this build was made from. */
public class BuildInfo {
	private static final Logger log = LogManager.getLogger(BuildInfo.class);
	private static final String NAME = "eSQLManager";

	private final String version;
	private final String commit;

	public BuildInfo() {
		Properties build = new Properties();

		try (InputStream in = BuildInfo.class.getResourceAsStream("/build.properties")) {
			if (in != null) {
				build.load(in);
			}
		} catch (IOException e) {
			log.warn("Could not read the build information: {}", e.getMessage());
		}

		version = value(build, "version", "unknown");
		// Without git the placeholder stays in the file, a build with uncommitted changes gets a marker.
		String hash = value(build, "commit", "unknown");
		commit = Boolean.parseBoolean(build.getProperty("dirty")) ? hash + "-dirty" : hash;
	}

	private static String value(Properties properties, String key, String fallback) {
		String value = properties.getProperty(key);
		return value == null || value.isBlank() || value.startsWith("${") ? fallback : value;
	}

	public String getAppName() {
		return NAME;
	}

	public String getAppVersion() {
		return version;
	}

	public String getAppCommit() {
		return commit;
	}
}

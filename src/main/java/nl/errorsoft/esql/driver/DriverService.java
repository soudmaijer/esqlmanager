package nl.errorsoft.esql.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.job.ProgressListener;

import org.apache.logging.log4j.LogManager;

/**
 * Finds and loads the JDBC driver of a server type: a jar the user chose, a driver bundled with the application, or a driver downloaded from a Maven
 * repository into the drivers directory. A downloaded jar is checked against the SHA-256 pinned in its {@link DriverArtifact} when it is downloaded and
 * before it is loaded; the class loader reads a private copy of the checked bytes, so a file changed after the check is never loaded. A jar is loaded
 * once per run, again when the file changed (another jar chosen at the same path). Connections are made through the driver {@link #load} returns,
 * not through {@code DriverManager}, which would hand out whichever registered driver accepts the URL first.
 */
public class DriverService {
	public static final URI MAVEN_CENTRAL = URI.create("https://repo1.maven.org/maven2/");

	private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(DriverService.class);
	private static final int TIMEOUT_MILLIS = 30_000;

	private final Path directory;
	private final URI repository;
	/** The driver loaded from each jar, so a jar gets one class loader per run (a new one when the file changed). */
	private final Map<JarFile, Driver> loaded = new ConcurrentHashMap<>();

	/**
	 * @param directory where downloaded drivers are kept
	 * @param repository the base of the Maven repository, ending with a slash
	 */
	public DriverService(Path directory, URI repository) {
		this.directory = directory;
		this.repository = repository;
	}

	public DriverStatus status(DriverSource source) {
		if (source.hasOwnJar()) {
			return DriverStatus.OWN_JAR;
		}
		if (isBundled(source.className())) {
			return DriverStatus.BUNDLED;
		}
		if (source.artifact() != null && Files.isRegularFile(jarOf(source.artifact()))) {
			return DriverStatus.DOWNLOADED;
		}
		return DriverStatus.NOT_INSTALLED;
	}

	/** Whether the driver has to be downloaded before it can be used. */
	public boolean needsDownload(DriverSource source) {
		return source.artifact() != null && status(source) == DriverStatus.NOT_INSTALLED;
	}

	/** The file a downloaded driver is kept in. */
	public Path jarOf(DriverArtifact artifact) {
		return directory.resolve(artifact.fileName());
	}

	/** Loads the driver. A driver that is not installed is an {@link EsqlException}. */
	public Driver load(DriverSource source) {
		switch (status(source)) {
			case OWN_JAR -> {
				Path jar = Path.of(source.ownJar());
				if (!Files.isRegularFile(jar)) {
					throw new EsqlException("The driver jar " + jar + " does not exist. Choose another one in Settings > JDBC driver settings.");
				}
				return loadFromJar(jar, source.className(), path -> path);
			}
			case BUNDLED -> {
				return newDriver(source.className(), DriverService.class.getClassLoader());
			}
			case DOWNLOADED -> {
				return loadFromJar(jarOf(source.artifact()), source.className(), path -> verifiedCopy(path, source.artifact()));
			}
			default -> {
				String name = source.artifact() != null ? "The " + source.artifact().name() + " driver" : "The driver " + source.className();
				throw new EsqlException(name + " is not installed. Download it in Settings > JDBC driver settings.");
			}
		}
	}

	/** Connects with the driver of the source, the one {@link #load} gives; a driver that does not accept the URL is an {@link EsqlException}. */
	public Connection connect(DriverSource source, String url, Properties properties) throws SQLException {
		Driver driver = load(source);
		Connection connection = driver.connect(url, properties);

		if (connection == null) {
			throw new EsqlException("The driver " + source.className() + " does not accept the URL " + url + ".");
		}
		return connection;
	}

	/**
	 * Downloads the driver into the drivers directory and checks its SHA-256. A file that does not match is deleted and is an {@link EsqlException}. Runs on
	 * the calling thread, which should not be the event thread.
	 */
	public Path download(DriverArtifact artifact, ProgressListener progress) throws IOException {
		Files.createDirectories(directory);
		URL url = repository.resolve(artifact.repositoryPath()).toURL();
		Path target = jarOf(artifact);
		Path part = Files.createTempFile(directory, artifact.artifactId(), ".part");
		log.info("Downloading the {} driver from {}", artifact.name(), url);
		try {
			String actual = copy(url, part, artifact.size(), progress);
			if (!actual.equalsIgnoreCase(artifact.sha256())) {
				throw new EsqlException("The downloaded " + artifact.name() + " driver does not have the expected SHA-256 checksum and was deleted (expected "
					+ artifact.sha256() + ", got " + actual + ").");
			}
			Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			log.info("The {} driver {} was saved as {}", artifact.name(), artifact.version(), target);
			return target;
		} finally {
			Files.deleteIfExists(part);
		}
	}

	private static String copy(URL url, Path to, long expectedSize, ProgressListener progress) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(TIMEOUT_MILLIS);
		connection.setReadTimeout(TIMEOUT_MILLIS);
		long size = connection.getContentLengthLong() > 0 ? connection.getContentLengthLong() : expectedSize;
		MessageDigest digest = sha256();
		try (InputStream in = new DigestInputStream(connection.getInputStream(), digest); OutputStream out = Files.newOutputStream(to)) {
			byte[] buffer = new byte[64 * 1024];
			long done = 0;
			int percent = -1;
			for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
				out.write(buffer, 0, read);
				done += read;
				int now = (int) Math.min(100, done * 100 / Math.max(1, size));
				if (now != percent) {
					percent = now;
					progress.progressed(percent);
				}
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	/**
	 * Checks a downloaded jar before it is loaded and returns a private copy of the bytes that were checked, for the class loader. A jar that was changed is
	 * deleted.
	 */
	private Path verifiedCopy(Path jar, DriverArtifact artifact) {
		Path copy = null;
		try {
			copy = Files.createTempFile("esql-" + artifact.artifactId() + "-", ".jar");
			copy.toFile().deleteOnExit();
			String actual;
			try (DigestInputStream in = new DigestInputStream(Files.newInputStream(jar), sha256()); OutputStream out = Files.newOutputStream(copy)) {
				in.transferTo(out);
				actual = HexFormat.of().formatHex(in.getMessageDigest().digest());
			}
			if (!actual.equalsIgnoreCase(artifact.sha256())) {
				Files.deleteIfExists(copy);
				Files.deleteIfExists(jar);
				throw new EsqlException("The " + artifact.name() + " driver in " + jar
					+ " does not have the expected SHA-256 checksum and was deleted. Download it again in Settings > JDBC driver settings.");
			}
			return copy;
		} catch (IOException e) {
			throw new EsqlException("The " + artifact.name() + " driver in " + jar + " cannot be read: " + e.getMessage(), e);
		}
	}

	/** A jar as it is now: the same path with another time or size is another jar. */
	private record JarFile(Path path, FileTime modified, long size) {
		static JarFile of(Path jar) {
			try {
				Path path = jar.toAbsolutePath();
				return new JarFile(path, Files.getLastModifiedTime(path), Files.size(path));
			} catch (IOException e) {
				throw new EsqlException("The driver jar " + jar + " cannot be read: " + e.getMessage(), e);
			}
		}
	}

	/** The jar to give the class loader for a jar file: the file itself, or a checked copy of it. */
	private interface Prepare {
		Path jarToLoad(Path jar);
	}

	private Driver loadFromJar(Path jar, String className, Prepare prepare) {
		return loaded.computeIfAbsent(JarFile.of(jar), file -> {
			try {
				// The class loader stays open as long as the application runs: the driver's classes are loaded from it lazily.
				URLClassLoader loader = new URLClassLoader(new URL[]{prepare.jarToLoad(file.path()).toUri().toURL()}, DriverService.class.getClassLoader());
				Driver driver = newDriver(className, loader);
				log.info("Loaded the driver {} from {}", className, file.path());
				return driver;
			} catch (IOException e) {
				throw new EsqlException("The driver " + className + " cannot be loaded from " + file.path() + ": " + e.getMessage(), e);
			}
		});
	}

	private static Driver newDriver(String className, ClassLoader loader) {
		try {
			return (Driver) Class.forName(className, true, loader).getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw new EsqlException("The driver class " + className + " was not found.", e);
		} catch (ReflectiveOperationException | ClassCastException e) {
			throw new EsqlException("The driver class " + className + " cannot be used: " + e.getMessage(), e);
		}
	}

	private static boolean isBundled(String className) {
		try {
			Class.forName(className, false, DriverService.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			// Not on the class path: the driver is downloaded or chosen.
			return false;
		}
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java runtime has SHA-256", e);
		}
	}
}

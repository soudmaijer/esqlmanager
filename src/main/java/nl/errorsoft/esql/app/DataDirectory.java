package nl.errorsoft.esql.app;

import nl.errorsoft.esql.error.EsqlException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Where conf/ and credits.txt live. Started from the runtime directory (development) they are relative to the working directory. An installed application
 * cannot rely on its working directory and may not be writable, so it is started with {@value #SEED_PROPERTY} pointing at the bundled copy of runtime/: the
 * files are then kept in {@code ~/.esqlmanager}, where missing ones are copied from the bundled copy on first use.
 */
public final class DataDirectory {

	public static final String SEED_PROPERTY = "esql.seed";

	public static final String DRIVERS_PROPERTY = "esql.drivers";

	private static File root;

	private DataDirectory() {
	}

	/** The file for a path relative to the runtime directory, such as {@code conf/profiles.xml}. */
	public static File file(String relativePath) {
		return new File(root(), relativePath);
	}

	/**
	 * Where downloaded JDBC drivers are kept: {@code ~/.esqlmanager/drivers}, also in development, so a driver is downloaded once per user. The system
	 * property {@value #DRIVERS_PROPERTY} points elsewhere.
	 */
	public static Path drivers() {
		String drivers = System.getProperty(DRIVERS_PROPERTY);
		return drivers != null ? Path.of(drivers) : Path.of(System.getProperty("user.home"), ".esqlmanager", "drivers");
	}

	/** Resolved on first use, so that a folder that cannot be prepared is an {@link EsqlException} of the action that needs it, not a failed class load. */
	private static synchronized File root() {
		if (root == null) {
			root = locate();
		}
		return root;
	}

	private static File locate() {
		String seed = System.getProperty(SEED_PROPERTY);
		if (seed == null) {
			return new File(".");
		}
		Path home = Path.of(System.getProperty("user.home"), ".esqlmanager");
		copyMissing(Path.of(seed), home);
		return home.toFile();
	}

	private static void copyMissing(Path from, Path to) {
		try (Stream<Path> files = Files.walk(from)) {
			for (Path source : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(source).toString());
				if (Files.isDirectory(source)) {
					Files.createDirectories(target);
				} else if (Files.notExists(target)) {
					Files.copy(source, target);
				}
			}
		} catch (IOException e) {
			throw new EsqlException("Cannot prepare the data folder " + to + ": " + e.getMessage(), e);
		}
	}
}

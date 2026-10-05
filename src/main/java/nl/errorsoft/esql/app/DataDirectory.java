package nl.errorsoft.esql.app;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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

	private static final File ROOT = locate();

	private DataDirectory() {
	}

	/** The file for a path relative to the runtime directory, such as {@code conf/profiles.xml}. */
	public static File file(String relativePath) {
		return new File(ROOT, relativePath);
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
			throw new UncheckedIOException("Cannot prepare " + to, e);
		}
	}
}

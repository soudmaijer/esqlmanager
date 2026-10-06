package nl.errorsoft.esql.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.sun.net.httpserver.HttpServer;

import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.job.ProgressListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DriverServiceTest {
	private static final String DRIVER_CLASS = "esqltest.FakeDriver";
	private static final String DRIVER_SOURCE = """
		package esqltest;

		public class FakeDriver implements java.sql.Driver {
			public java.sql.Connection connect(String url, java.util.Properties info) throws java.sql.SQLException {
				if (!acceptsURL(url)) {
					return null;
				}
				throw new java.sql.SQLException("FakeDriver connects to " + url);
			}
			public boolean acceptsURL(String url) { return url.startsWith("jdbc:esqltest:"); }
			public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) { return new java.sql.DriverPropertyInfo[0]; }
			public int getMajorVersion() { return 1; }
			public int getMinorVersion() { return 0; }
			public boolean jdbcCompliant() { return false; }
			public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getGlobal(); }
		}
		""";

	@TempDir
	Path temp;

	private Path repository;
	private Path drivers;
	private byte[] jar;

	@BeforeEach
	void createRepository() throws Exception {
		repository = Files.createDirectories(temp.resolve("repository"));
		drivers = temp.resolve("drivers");
		jar = driverJar();
		Path published = repository.resolve(artifact("0".repeat(64)).repositoryPath());
		Files.createDirectories(published.getParent());
		Files.write(published, jar);
	}

	@Test
	void downloadKeepsAJarWithThePinnedChecksum() throws Exception {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverArtifact artifact = artifact(sha256(jar));
		List<Integer> progress = new ArrayList<>();

		assertEquals(DriverStatus.NOT_INSTALLED, service.status(source(artifact)));
		Path saved = service.download(artifact, listener(progress));

		assertEquals(service.jarOf(artifact), saved);
		assertTrue(Files.isRegularFile(saved));
		assertEquals(100, progress.getLast());
		assertEquals(DriverStatus.DOWNLOADED, service.status(source(artifact)));
		assertFalse(service.needsDownload(source(artifact)));
	}

	@Test
	void aJarWithAnotherChecksumIsRefusedAndDeleted() throws Exception {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverArtifact artifact = artifact("ab".repeat(32));

		EsqlException e = assertThrows(EsqlException.class, () -> service.download(artifact, ProgressListener.NONE));

		assertTrue(e.getMessage().contains("checksum"), e.getMessage());
		try (Stream<Path> files = Files.list(drivers)) {
			assertEquals(0, files.count(), "neither the jar nor the partial download is kept");
		}
		assertEquals(DriverStatus.NOT_INSTALLED, service.status(source(artifact)));
	}

	@Test
	void downloadsOverHttp() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/maven2/", exchange -> {
			Path file = repository.resolve(exchange.getRequestURI().getPath().substring("/maven2/".length()));
			byte[] body = Files.readAllBytes(file);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		try {
			URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/maven2/");
			DriverArtifact artifact = artifact(sha256(jar));

			Path saved = new DriverService(drivers, base).download(artifact, ProgressListener.NONE);

			assertEquals(sha256(jar), sha256(Files.readAllBytes(saved)));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void aDownloadedDriverIsLoadedOnceAndConnectsItself() throws Exception {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverArtifact artifact = artifact(sha256(jar));
		service.download(artifact, ProgressListener.NONE);

		Driver loaded = service.load(source(artifact));

		assertEquals(DRIVER_CLASS, loaded.getClass().getName());
		assertSame(loaded, service.load(source(artifact)), "a jar is loaded once");
		// The connection is made by the loaded driver, whatever DriverManager has registered.
		SQLException e = assertThrows(SQLException.class, () -> service.connect(source(artifact), "jdbc:esqltest:shop", new Properties()));
		assertEquals("FakeDriver connects to jdbc:esqltest:shop", e.getMessage());
		assertThrows(EsqlException.class, () -> service.connect(source(artifact), "jdbc:postgresql://localhost/shop", new Properties()));
	}

	@Test
	void anOwnJarReplacedAtTheSamePathIsLoadedAgain() throws Exception {
		Path own = Files.write(temp.resolve("own.jar"), jar);
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverSource source = new DriverSource(DRIVER_CLASS, own.toString(), null);
		Driver first = service.load(source);

		Files.write(own, jar);
		Files.setLastModifiedTime(own, java.nio.file.attribute.FileTime.fromMillis(Files.getLastModifiedTime(own).toMillis() + 5000));

		assertNotSame(first, service.load(source));
	}

	@Test
	void aChangedJarIsDeletedInsteadOfLoaded() throws Exception {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverArtifact artifact = artifact(sha256(jar));
		Files.createDirectories(drivers);
		Files.write(service.jarOf(artifact), "not the driver".getBytes());

		assertThrows(EsqlException.class, () -> service.load(source(artifact)));

		assertFalse(Files.exists(service.jarOf(artifact)));
	}

	@Test
	void aDriverThatIsNotInstalledIsAProblemTheUserCanFix() throws Exception {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverArtifact artifact = artifact(sha256(jar));

		assertTrue(service.needsDownload(source(artifact)));
		EsqlException e = assertThrows(EsqlException.class, () -> service.load(source(artifact)));
		assertTrue(e.getMessage().contains("not installed"), e.getMessage());
	}

	@Test
	void anOwnJarIsLoadedWithoutDownloading() throws Exception {
		Path own = Files.write(temp.resolve("own.jar"), jar);
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverSource source = new DriverSource(DRIVER_CLASS, own.toString(), artifact("0".repeat(64)));

		assertEquals(DriverStatus.OWN_JAR, service.status(source));
		assertFalse(service.needsDownload(source));
		assertEquals(DRIVER_CLASS, service.load(source).getClass().getName());
	}

	@Test
	void aDriverOnTheClassPathIsBundled() {
		DriverService service = new DriverService(drivers, repository.toUri());
		DriverSource postgres = new DriverSource("org.postgresql.Driver", "", null);

		assertEquals(DriverStatus.BUNDLED, service.status(postgres));
		assertFalse(service.needsDownload(postgres));
		assertEquals("org.postgresql.Driver", service.load(postgres).getClass().getName());
	}

	@Test
	void thePinnedArtifactsPointAtMavenCentralPaths() {
		assertEquals("com/mysql/mysql-connector-j/26.7.0/mysql-connector-j-26.7.0.jar", DriverArtifact.MYSQL.repositoryPath());
		assertEquals(64, DriverArtifact.ORACLE.sha256().length());
		assertEquals("2.6 MB", DriverArtifact.MYSQL.sizeText());
	}

	private static DriverArtifact artifact(String sha256) {
		return new DriverArtifact("Test", "nl.errorsoft.test", "fake-driver", "1.0", sha256, 1000, "Test licence");
	}

	private static DriverSource source(DriverArtifact artifact) {
		return new DriverSource(DRIVER_CLASS, "", artifact);
	}

	private static ProgressListener listener(List<Integer> progress) {
		return new ProgressListener() {
			@Override
			public void progressed(int percent) {
				progress.add(percent);
			}

			@Override
			public void failed(Exception error) {
			}
		};
	}

	/** A jar with a driver class that is not on the class path of the tests. */
	private byte[] driverJar() throws IOException {
		Path sources = Files.createDirectories(temp.resolve("src/esqltest"));
		Path source = Files.writeString(sources.resolve("FakeDriver.java"), DRIVER_SOURCE);
		Path classes = Files.createDirectories(temp.resolve("classes"));
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(), source.toString()));
		Path jarFile = temp.resolve("fake-driver.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile))) {
			out.putNextEntry(new JarEntry("esqltest/FakeDriver.class"));
			out.write(Files.readAllBytes(classes.resolve("esqltest/FakeDriver.class")));
			out.closeEntry();
		}
		return Files.readAllBytes(jarFile);
	}

	private static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}
}

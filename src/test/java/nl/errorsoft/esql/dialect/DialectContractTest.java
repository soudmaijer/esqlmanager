package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.server.ServerProcess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseLister;
import nl.errorsoft.esql.database.DatabaseRepository;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.connection.DatabaseSelection;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.exporter.ExportOptions;
import nl.errorsoft.esql.connection.ConnectionContext;
import nl.errorsoft.esql.user.GrantTarget;
import nl.errorsoft.esql.user.UserService;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableName;
import nl.errorsoft.esql.database.Schema;
import org.junit.jupiter.api.Assumptions;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableIndex;
import nl.errorsoft.esql.table.TableForeignKey;
import nl.errorsoft.esql.designer.DesignedDatabase;
import nl.errorsoft.esql.designer.DesignedForeignKey;
import nl.errorsoft.esql.designer.DesignedTable;
import nl.errorsoft.esql.error.EsqlException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;
import nl.errorsoft.esql.job.ProgressListener;
import nl.errorsoft.esql.exporter.ExportService;
import nl.errorsoft.esql.importer.ImportOptions;
import nl.errorsoft.esql.importer.ImportService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What every dialect must be able to do, checked against a real server.
 * A subclass supplies the server, the tests run unchanged against each of them.
 */
abstract class DialectContractTest {
	protected static final String DATABASE = "shop";

	private static final DataType INTEGER = new DataType("integer", true, true, true, false, true, false, true, false);
	private static final DataType VARCHAR = new DataType("varchar", true, true, true, false, true, false, false, false);
	private static final DataType BIGINT = new DataType("bigint", true, true, true, false, true, false, false, false);

	private static DatabaseConnection connection;

	private Dialect dialect;
	private Database database;

	/** Connects to the server under test, once for all tests of the subclass. */
	protected abstract ConnectionProfile profile();

	@BeforeEach
	void connect() throws Exception {
		if (connection == null) {
			connection = new DatabaseConnection();
			connection.connect(profile(), "");
		}
		dialect = connection.getConnectionProfile().getServerType().getDialect();
		database = new Database(DATABASE);
		connection.useDatabase(DATABASE);
	}

	@AfterAll
	static void disconnect() throws Exception {
		if (connection != null) {
			connection.close();
		}

		connection = null;
	}

	@Test
	void createsTableAndChangesItsColumnsAndIndexes() throws Exception {
		String name = "ddl_" + System.nanoTime();
		createTable(name, "Customer's note");
		Table table = table(name);

		assertEquals(Arrays.asList("id", "name", "note"), columnNames(table));
		assertTrue(service().loadIndexes(table)[0].isPrimary() || indexNames(table).contains("PRIMARY"));

		service().addColumn(table, "age", "", "0", INTEGER, false, false, false, true);
		assertTrue(columnNames(table).contains("age"));

		service().editColumn(table.getTableColumn("age"), "years", "", "5", BIGINT, false, false, false, false);
		assertTrue(columnNames(table).contains("years"));
		assertFalse(columnNames(table).contains("age"));

		TableIndex index = new TableIndex(table);
		index.setName("u_years");
		service().addIndex(table, index, new TableColumn[]{table.getTableColumn("years")}, "UNIQUE");
		assertTrue(table.getTableIndex("u_years").isUnique());

		service().modifyIndex(table, table.getTableIndex("u_years"), new TableColumn[]{table.getTableColumn("years"), table.getTableColumn("name")},
			"INDEX");
		assertFalse(table.getTableIndex("u_years").isUnique());
		assertEquals(2, table.getTableIndex("u_years").getTableColumns().length);

		service().dropIndex(table, table.getTableIndex("u_years"));
		assertFalse(indexNames(table).contains("u_years"));

		service().modifyTable(table, name + "_renamed", null, "new comment");
		assertEquals(name + "_renamed", table.getName());
		service().dropTable(table);
	}

	@Test
	void exportedTableCanBeImportedAgain(@TempDir Path dir) throws Exception {
		String name = "dump_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		insert(table, "first", "plain");
		insert(table, "second", "it's \\ tricky\nwith a second line");
		insert(table, "third", null);

		File file = dir.resolve("dump.sql").toFile();
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{table}, file.getAbsolutePath(),
			new ExportOptions(true, true, false, true, true));
		runSynchronously(export::setListener, export);
		ImportService imported = new ConnectionContext(connection).newImport(database, file.getAbsolutePath());
		runSynchronously(imported::setListener, imported);

		TableData[][] rows = service().loadPage(table, 0, 100);
		assertEquals(3, rows.length);
		assertEquals("it's \\ tricky\nwith a second line", rows[1][2].getData());
		assertTrue(rows[2][2].isNull());

		// Auto numbering continues after the highest id that was loaded.
		insert(table, "fourth", "after import");
		assertEquals(4, service().loadPage(table, 0, 100).length);
		service().dropTable(table);
	}

	@Test
	void valuesAreEscapedWhenEditingData() throws Exception {
		String name = "data_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		insert(table, "a", "x");
		insert(table, "b", "y");
		String hostile = "O'Brien \\' ; DROP TABLE " + name + "; -- \\";

		TableData[][] rows = service().loadPage(table, 0, 100);
		assertEquals(1, service().changeCell(table, rows[0], rows[0][2], hostile));
		assertEquals(hostile, service().loadPage(table, 0, 100)[0][2].getData());

		service().deleteRow(table, service().loadPage(table, 0, 100)[1]);
		assertEquals(1, service().loadPage(table, 0, 100).length);
		service().dropTable(table);
	}

	@Test
	void managesUsersAndTheirPrivileges() throws Exception {
		String name = "usr_" + System.nanoTime();
		String tableName = "grants_" + System.nanoTime();
		createTable(tableName, "");
		UserService admin = new ConnectionContext(connection).users();
		DatabaseUser user = new DatabaseUser(name, admin.usesHost() ? "%" : null);

		admin.createUser(user, "pass'word");
		assertTrue(admin.listUsers().stream().anyMatch(u -> u.name().equals(name)));
		admin.changePassword(user, "other");

		GrantTarget onTable = GrantTarget.table(DATABASE, tableName);
		admin.setGrants(user, onTable, new LinkedHashSet<>(Arrays.asList("SELECT", "INSERT")));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("SELECT", "INSERT")), admin.getGrants(user, onTable));

		admin.setGrants(user, onTable, new LinkedHashSet<>(Arrays.asList("SELECT")));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("SELECT")), admin.getGrants(user, onTable));

		GrantTarget onDatabase = GrantTarget.database(DATABASE);
		String databasePrivilege = admin.getPrivileges(GrantTarget.Scope.DATABASE).get(0);
		admin.setGrants(user, onDatabase, new LinkedHashSet<>(Arrays.asList(databasePrivilege)));
		assertTrue(admin.getGrants(user, onDatabase).contains(databasePrivilege));

		admin.setGrants(user, onTable, new LinkedHashSet<>());
		admin.setGrants(user, onDatabase, new LinkedHashSet<>());
		admin.dropUser(user);
		assertFalse(admin.listUsers().stream().anyMatch(u -> u.name().equals(name)));
		service().dropTable(table(tableName));
	}

	@Test
	void testsAConnectionWithoutKeepingIt() throws Exception {
		var profiles = new nl.errorsoft.esql.connection.control.ConnectionProfileController(null);
		AtomicReference<nl.errorsoft.esql.connection.control.ConnectionProfileController.TestResult> result = new AtomicReference<>();
		java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
		profiles.testConnection(profile(), outcome -> {
			result.set(outcome);
			done.countDown();
		});
		assertTrue(done.await(30, java.util.concurrent.TimeUnit.SECONDS));
		assertTrue(result.get().success(), result.get().message());
		assertTrue(result.get().message().startsWith("Connected to "));

		ConnectionProfile wrong = profile();
		wrong.setPassword("not the password");
		wrong.setPort("1");
		java.util.concurrent.CountDownLatch failed = new java.util.concurrent.CountDownLatch(1);
		profiles.testConnection(wrong, outcome -> {
			result.set(outcome);
			failed.countDown();
		});
		assertTrue(failed.await(30, java.util.concurrent.TimeUnit.SECONDS));
		assertFalse(result.get().success());
	}

	@Test
	void exportOptionsShapeTheScript(@TempDir Path dir) throws Exception {
		String name = "opts_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		insert(table, "first", "caf\u00e9");
		insert(table, "second", "b");
		insert(table, "third", null);

		File file = dir.resolve("dump.sql.gz").toFile();
		ExportOptions options = new ExportOptions(true, true, false, true, false, false, true, java.nio.charset.StandardCharsets.ISO_8859_1, 2, false, true,
			dialect.disableForeignKeyChecksSql() != null);
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{table}, file.getAbsolutePath(), options);
		runSynchronously(export::setListener, export);

		String script;
		try (var in = new java.util.zip.GZIPInputStream(java.nio.file.Files.newInputStream(file.toPath()))) {
			script = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
		assertTrue(script.contains("DROP TABLE " + dialect.quote(name) + ";"), script);
		assertFalse(script.contains("IF EXISTS"), script);
		assertTrue(script.contains("CREATE TABLE IF NOT EXISTS"), script);
		assertTrue(script.contains("caf\u00e9"), script);
		// Three rows, two per statement.
		assertEquals(2, script.split("INSERT INTO", -1).length - 1, script);
		assertEquals(dialect.beginTransactionSql() != null, script.contains(dialect.beginTransactionSql() + ";"), script);
		assertEquals(dialect.disableForeignKeyChecksSql() != null, script.contains("FOREIGN_KEY_CHECKS=0"), script);

		// The script drops the table (without IF EXISTS) and creates it again.
		ImportService imported = new ConnectionContext(connection).newImport(database, file.getAbsolutePath(),
			new ImportOptions(true, false, java.nio.charset.StandardCharsets.ISO_8859_1));
		runSynchronously(imported::setListener, imported);
		TableData[][] rows = service().loadPage(table, 0, 100);
		assertEquals(3, rows.length);
		assertEquals("caf\u00e9", rows[0][2].getData());
		service().dropTable(table);
	}

	@Test
	void exportCanLeaveOutTheDropAndWriteViews(@TempDir Path dir) throws Exception {
		Assumptions.assumeTrue(dialect.showCreateViewSql(TableName.of("v")) != null, "The server cannot give view definitions");
		var databases = new ConnectionContext(connection).databases();
		Database other = databases.createDatabase("views_" + System.nanoTime());
		connection.useDatabase(other.getName());
		createTable("base", "");
		connection.executeUpdate("CREATE VIEW " + dialect.quote("base_names") + " AS SELECT " + dialect.quote("name") + " FROM " + dialect.quote("base"));

		File file = dir.resolve("views.sql").toFile();
		ExportOptions options = new ExportOptions(true, false, false, true, false, true, false, null, 1, true, false, false);
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{other}, file.getAbsolutePath(), options);
		runSynchronously(export::setListener, export);
		String script = java.nio.file.Files.readString(file.toPath());
		assertTrue(script.contains("DROP VIEW IF EXISTS"), script);
		assertTrue(script.contains("CREATE "), script);
		assertTrue(script.indexOf("base_names") > script.indexOf("CREATE TABLE"), script);

		// Without the option the view is not in the script.
		ExportService without = new ConnectionContext(connection).newExport(new Object[]{other}, file.getAbsolutePath(),
			new ExportOptions(true, false, false, true, false));
		runSynchronously(without::setListener, without);
		assertFalse(java.nio.file.Files.readString(file.toPath()).contains("base_names"));

		// The script brings the view back.
		export = new ConnectionContext(connection).newExport(new Object[]{other}, file.getAbsolutePath(), options);
		runSynchronously(export::setListener, export);
		connection.executeUpdate("DROP VIEW " + dialect.quote("base_names"));
		ImportService imported = new ConnectionContext(connection).newImport(other, file.getAbsolutePath());
		runSynchronously(imported::setListener, imported);
		try (ResultSet rs = connection.executeQuery("SELECT count(*) FROM " + dialect.quote("base_names"))) {
			assertTrue(rs.next());
		}

		connection.useDatabase(DATABASE);
		databases.dropDatabase(other);
	}

	@Test
	void cancelledExportDeletesThePartialFile(@TempDir Path dir) throws Exception {
		String name = "cancel_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		insert(table, "first", "a");

		File file = dir.resolve("cancelled.sql").toFile();
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{table}, file.getAbsolutePath(),
			new ExportOptions(true, true, false, true, true));
		Recording recording = new Recording();
		recording.onStatus = status -> export.cancel();
		export.setListener(recording);
		export.run();

		assertTrue(recording.cancelled.startsWith("Cancelled after 0 of 1 table(s)"), String.valueOf(recording.cancelled));
		assertFalse(file.exists());
		service().dropTable(table);
	}

	@Test
	void importContinuesAfterErrorsOrStopsAtTheFirst(@TempDir Path dir) throws Exception {
		String name = "imp_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		String q = dialect.quote(name);
		Path script = dir.resolve("errors.sql");
		java.nio.file.Files.writeString(script,
			"INSERT INTO " + q + " (" + dialect.quote("name") + ") VALUES('one');\nINSERT INTO no_such_table_here VALUES(1);\n"
				+ "INSERT INTO " + q + " (" + dialect.quote("name") + ") VALUES('two');\n");

		Recording stopped = new Recording();
		ImportService stopping = new ConnectionContext(connection).newImport(database, script.toString());
		stopping.setListener(stopped);
		stopping.run();
		assertNotNull(stopped.failure);
		assertTrue(stopped.failure.getMessage().startsWith("Statement 2 ("), stopped.failure.getMessage());
		assertEquals(1, service().loadPage(table, 0, 100).length);

		Recording continued = new Recording();
		ImportService continuing = new ConnectionContext(connection).newImport(database, script.toString(),
			new ImportOptions(false, false, java.nio.charset.StandardCharsets.UTF_8));
		continuing.setListener(continued);
		continuing.run();
		assertEquals(null, continued.failure);
		assertEquals(1, continued.details.size());
		assertTrue(continued.finished.contains("1 failed"), continued.finished);
		assertEquals(3, service().loadPage(table, 0, 100).length);
		service().dropTable(table);
	}

	@Test
	void importInASingleTransactionRollsBackOnErrorAndCancel(@TempDir Path dir) throws Exception {
		String name = "tx_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		String insert = "INSERT INTO " + dialect.quote(name) + " (" + dialect.quote("name") + ") VALUES('x');\n";
		Path failing = dir.resolve("failing.sql");
		java.nio.file.Files.writeString(failing, insert + insert + "INSERT INTO no_such_table_here VALUES(1);\n");
		ImportOptions transaction = new ImportOptions(true, true, java.nio.charset.StandardCharsets.UTF_8);

		Recording failed = new Recording();
		ImportService failing1 = new ConnectionContext(connection).newImport(database, failing.toString(), transaction);
		failing1.setListener(failed);
		failing1.run();
		assertNotNull(failed.failure);
		assertEquals(0, service().loadPage(table, 0, 100).length);

		// Cancelled after the first statement: the one that ran is rolled back.
		Path three = dir.resolve("three.sql");
		java.nio.file.Files.writeString(three, insert + insert + insert);
		ImportService cancelling = new ConnectionContext(connection).newImport(database, three.toString(), transaction);
		Recording cancelled = new Recording();
		cancelled.onStatus = status -> cancelling.cancel();
		cancelling.setListener(cancelled);
		cancelling.run();
		assertTrue(cancelled.cancelled.contains("rolled back"), String.valueOf(cancelled.cancelled));
		assertEquals(0, service().loadPage(table, 0, 100).length);

		// Without a transaction the statement that ran stays.
		ImportService plain = new ConnectionContext(connection).newImport(database, three.toString());
		Recording plainCancelled = new Recording();
		plainCancelled.onStatus = status -> plain.cancel();
		plain.setListener(plainCancelled);
		plain.run();
		assertTrue(plainCancelled.cancelled.contains("not undone"), String.valueOf(plainCancelled.cancelled));
		assertEquals(1, service().loadPage(table, 0, 100).length);
		service().dropTable(table);
	}

	@Test
	void createsADatabaseWithOptionsAndDescribesIt() throws Exception {
		var databases = new ConnectionContext(connection).databases();
		var choices = databases.createDatabaseChoices();
		assertEquals(dialect.createDatabaseOptions().stream().map(o -> o.key()).toList(), List.copyOf(choices.keySet()));
		choices.values().forEach(values -> assertFalse(values.isEmpty()));

		java.util.Map<String, String> preferred = java.util.Map.of("charset", "latin1", "collation", "latin1_swedish_ci", "owner",
			profile().getUsername(), "encoding", "UTF8");
		java.util.Map<String, String> chosen = new java.util.LinkedHashMap<>();
		choices.forEach((key, values) -> {
			assertTrue(values.contains(preferred.get(key)), key + " " + values);
			chosen.put(key, preferred.get(key));
		});

		String name = "opt_" + System.nanoTime();
		Database created = databases.createDatabase(name, chosen);
		var properties = databases.properties(created);
		assertEquals(name, properties.name());
		assertEquals(0, properties.tableCount());
		chosen.values().forEach(value -> assertTrue(properties.details().containsValue(value), value + " in " + properties.details()));

		assertThrows(EsqlException.class, () -> databases.createDatabase(name));
		assertThrows(EsqlException.class, () -> databases.createDatabase("  "));
		connection.useDatabase(DATABASE);
		databases.dropDatabase(created);
	}

	@Test
	void renamesASchema() throws Exception {
		Assumptions.assumeTrue(dialect.supports(Dialect.Feature.SCHEMAS), "The server has no schemas");
		var databases = new ConnectionContext(connection).databases();
		String name = "before_" + System.nanoTime();
		Schema schema = databases.createSchema(database, name);
		service().createTable(schema, "kept", List.of(column("id", INTEGER, "", true)), null, "");

		Schema renamed = databases.renameSchema(schema, name + "_after");
		assertEquals(name + "_after", renamed.getName());
		var names = databases.getSchemas(database).stream().map(Schema::getName).toList();
		assertTrue(names.contains(name + "_after"));
		assertFalse(names.contains(name));
		assertEquals(List.of("kept"), databases.getTables(renamed).stream().map(Table::getName).toList());
		databases.dropSchema(renamed);
	}

	@Test
	void renamesAndDuplicatesTablesAndDescribesThem() throws Exception {
		String name = "orig_" + System.nanoTime();
		createTable(name, "the original");
		Table table = table(name);
		insert(table, "first", "a");
		insert(table, "second", "b");

		service().renameTable(table, name + "_r");
		assertEquals(name + "_r", table.getName());
		assertTrue(service().exists(database, name + "_r"));
		assertFalse(service().exists(database, name));
		assertThrows(EsqlException.class, () -> service().renameTable(table, " "));

		Table empty = service().duplicateTable(table, name + "_empty", false);
		assertEquals(List.of("id", "name", "note"), columnNames(empty));
		assertEquals(0, service().loadPage(empty, 0, 100).length);

		Table full = service().duplicateTable(table, name + "_full", true);
		assertEquals(2, service().loadPage(full, 0, 100).length);
		// Auto numbering of the copy continues after the copied rows.
		insert(full, "third", "c");
		assertEquals(3, service().loadPage(full, 0, 100).length);
		assertThrows(EsqlException.class, () -> service().duplicateTable(table, name + "_full", false));
		assertEquals("A table named '" + name + "_full' exists already.", service().newNameProblem(table, name + "_full"));

		var info = service().describe(table);
		assertEquals(name + "_r", info.name());
		assertEquals(3, info.columnCount());
		assertEquals(2, info.rowCount());
		assertTrue(info.sizeBytes() != null && info.sizeBytes() > 0, String.valueOf(info.sizeBytes()));

		service().dropTable(table);
		service().dropTable(empty);
		service().dropTable(full);
	}

	@Test
	void columnsCarryCommentsAndStatementsCanBePreviewed() throws Exception {
		Assumptions.assumeTrue(dialect.supportsColumnComments(), "The server has no column comments");
		String name = "cc_" + System.nanoTime();
		CreateColumn id = column("id", INTEGER, "", true);
		id.comment = "the key, it's unique";
		CreateColumn label = column("label", VARCHAR, "20", false);
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;

		// The preview is what Save runs.
		List<String> preview = service().createStatements(null, name, List.of(id, label), engine, "");
		assertEquals(dialect.createTableSql(TableName.of(name), List.of(id, label), engine, ""), preview);
		service().createTable(database, name, List.of(id, label), engine, "");
		Table table = table(name);
		assertEquals("the key, it's unique", service().loadColumns(table)[0].getComment());
		assertEquals("", service().loadColumns(table)[1].getComment());

		CreateColumn added = column("extra", INTEGER, "", false);
		added.comment = "added later";
		service().addColumn(table, added);
		assertEquals("added later", service().loadColumns(table)[2].getComment());

		CreateColumn changed = column("extra", INTEGER, "", false);
		changed.comment = "";
		service().editColumn(table.getTableColumn("extra"), changed);
		assertEquals("", service().loadColumns(table)[2].getComment());

		assertTrue(service().modifyStatements(table, name, table.getType(), "").isEmpty());
		assertEquals(1, service().modifyStatements(table, name + "_x", table.getType(), "").size());
		service().dropTable(table);
	}

	@Test
	void createsAndSwitchesDatabases() throws Exception {
		String name = "db_" + System.nanoTime();
		var databases = new ConnectionContext(connection).databases();
		assertFalse(databases.exists(new Database(name)));

		databases.createDatabase(name);
		assertTrue(databases.exists(new Database(name)));

		// The connection is using the database that is dropped.
		connection.useDatabase(name);
		databases.dropDatabase(new Database(name));
		assertFalse(databases.exists(new Database(name)));
		connection.useDatabase(DATABASE);
	}

	@Test
	void showsServerStatusVariablesProcessesAndRunsMaintenance() throws Exception {
		for (String query : Arrays.asList(dialect.getStatusQuery(), dialect.getVariablesQuery())) {
			try (java.sql.ResultSet rs = connection.executeQuery(query)) {
				assertTrue(rs.next(), query);
			}
		}

		// Another connection is active, and can be ended.
		try (DatabaseConnection other = new DatabaseConnection()) {
			other.connect(profile(), "");
			List<ServerProcess> processes = new ConnectionContext(connection).servers().getProcesses();
			assertFalse(processes.isEmpty());
			// The second connection waits for its client, the list can hide it.
			assertTrue(processes.stream().anyMatch(ServerProcess::idle), processes.toString());
		}

		String name = "maint_" + System.nanoTime();
		createTable(name, "");
		Table table = table(name);
		// Every command the dialect offers (and so the tree menu shows) runs.
		for (Dialect.Maintenance command : dialect.maintenanceCommands()) {
			String message = switch (command) {
				case OPTIMIZE -> service().optimizeTable(table);
				case ANALYZE -> service().analyseTable(table);
				case CHECK -> service().checkTable(table);
				case REPAIR -> service().repairTable(table);
			};
			assertNotNull(message, command.name());
		}
		service().dropTable(table);
	}

	@Test
	void addsAndDropsForeignKeys() throws Exception {
		assertTrue(dialect.supports(Dialect.Feature.FOREIGN_KEYS));
		String parent = "fk_parent_" + System.nanoTime();
		String child = "fk_child_" + System.nanoTime();
		createTable(parent, "");
		CreateColumn id = new CreateColumn("id");
		id.type = INTEGER;
		id.primary = true;
		id.notnull = true;
		CreateColumn parentId = new CreateColumn("parent_id");
		parentId.type = INTEGER;
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;
		service().createTable(database, child, Arrays.asList(id, parentId), engine, "");
		Table childTable = table(child);

		service().addForeignKey(childTable, new TableForeignKey("fk_child_parent", List.of("parent_id"), parent, List.of("id"), "cascade", "NO ACTION"));

		try (ResultSet rs = connection.getConnection().getMetaData().getImportedKeys(connection.getConnection().getCatalog(), connection.getSchema(),
			child)) {
			assertTrue(rs.next());
			assertEquals(parent, rs.getString("PKTABLE_NAME"));
			assertEquals("parent_id", rs.getString("FKCOLUMN_NAME"));
			assertEquals("id", rs.getString("PKCOLUMN_NAME"));
		}
		assertEquals(List.of("fk_child_parent"), service().foreignKeyNames(childTable));
		assertThrows(SQLException.class, () -> connection.executeUpdate("INSERT INTO " + dialect.quote(child) + " VALUES (1, 999)"));

		service().dropForeignKey(childTable, "fk_child_parent");
		assertTrue(service().foreignKeyNames(childTable).isEmpty());
		connection.executeUpdate("INSERT INTO " + dialect.quote(child) + " VALUES (1, 999)");

		service().dropTable(childTable);
		service().dropTable(table(parent));
	}

	@Test
	void designerGeneratesForeignKeysOnceAllTablesExist() throws Exception {
		String parent = "gen_parent_" + System.nanoTime();
		String child = "gen_child_" + System.nanoTime();
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;
		CreateColumn id = new CreateColumn("id");
		id.type = INTEGER;
		id.primary = true;
		id.notnull = true;
		CreateColumn parentId = new CreateColumn("parent_id");
		parentId.type = INTEGER;
		DesignedForeignKey key = new DesignedForeignKey("fk_gen", List.of("parent_id"), parent, List.of("id"), "SET NULL", "");
		// The child comes first, so the key can only be added after every table was created.
		List<DesignedDatabase> model = List.of(new DesignedDatabase(DATABASE,
			List.of(new DesignedTable(child, engine, "", List.of(id, parentId), List.of(key)), new DesignedTable(parent, engine, "",
				List.of(id), List.of()))));

		new ConnectionContext(connection).designer().generate(model, () -> {
		});
		new ConnectionContext(connection).designer().generate(model, () -> {
		});
		connection.useDatabase(DATABASE);
		assertEquals(List.of("fk_gen"), service().foreignKeyNames(table(child)));

		DesignedForeignKey broken = new DesignedForeignKey("fk_broken", List.of("missing"), parent, List.of("id"), "", "");
		List<DesignedDatabase> brokenModel = List.of(new DesignedDatabase(DATABASE, List.of(new DesignedTable(child, engine, "", List.of(id), List.of(
			broken)))));
		assertThrows(EsqlException.class, () -> new ConnectionContext(connection).designer().generate(brokenModel, () -> {
		}));

		connection.useDatabase(DATABASE);
		service().dropTable(table(child));
		service().dropTable(table(parent));
	}

	@Test
	void designerReadsAnExistingDatabaseBack() throws Exception {
		String name = "re_" + System.nanoTime();
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;
		CreateColumn id = column("id", INTEGER, "", true);
		id.autoincrement = true;
		CreateColumn customerName = column("name", VARCHAR, "50", false);
		customerName.notnull = true;
		customerName.defaultval = "it's";
		DesignedTable customers = new DesignedTable("customers", engine, "", List.of(id, customerName), List.of());
		DesignedTable products = new DesignedTable("products", engine, "", List.of(column("code", VARCHAR, "20", true), column("version", INTEGER, "", true)),
			List.of());
		DesignedTable lines = new DesignedTable("order_lines", engine, "",
			List.of(column("id", INTEGER, "", true), column("customer_id", INTEGER, "", false), column("product_code", VARCHAR, "20", false),
				column("product_version", INTEGER, "", false)),
			List.of(new DesignedForeignKey("fk_line_customer", List.of("customer_id"), "customers", List.of("id"), "CASCADE", ""),
				new DesignedForeignKey("fk_line_product", List.of("product_code", "product_version"), "products", List.of("code", "version"), "", "")));
		DesignedTable notes = new DesignedTable("notes", engine, "", List.of(column("id", INTEGER, "", true)), List.of());
		ConnectionContext context = new ConnectionContext(connection);
		context.designer().generate(List.of(new DesignedDatabase(name, List.of(customers, products, lines, notes))), () -> {
		});
		connection.useDatabase(name);
		connection.executeUpdate("CREATE VIEW " + dialect.quote("customer_names") + " AS SELECT " + dialect.quote("name") + " FROM " + dialect.quote(
			"customers"));

		DesignedDatabase read = context.designer().reverseEngineer(new Database(name));

		assertEquals(name, read.name());
		assertEquals(List.of("customers", "notes", "order_lines", "products"), read.tables().stream().map(DesignedTable::name).sorted().toList());
		DesignedTable readCustomers = designed(read, "customers");
		assertEquals(List.of("id", "name"), readCustomers.columns().stream().map(c -> c.name).toList());
		CreateColumn readId = readCustomers.columns().get(0);
		assertTrue(readId.primary && readId.autoincrement && readId.notnull);
		assertTrue(readId.type.getName().toLowerCase().startsWith("int"), readId.type.getName());
		CreateColumn readName = readCustomers.columns().get(1);
		assertEquals("varchar", readName.type.getName().toLowerCase());
		assertEquals("50", readName.length);
		assertEquals("it's", readName.defaultval);
		assertTrue(readName.notnull && !readName.primary);

		assertEquals(List.of("code", "version"), designed(read, "products").columns().stream().filter(c -> c.primary).map(c -> c.name).toList());
		assertTrue(designed(read, "notes").foreignKeys().isEmpty());

		List<DesignedForeignKey> keys = designed(read, "order_lines").foreignKeys().stream().sorted(java.util.Comparator.comparing(DesignedForeignKey::name))
			.toList();
		assertEquals(2, keys.size());
		// The update rule is left out: MySQL reports a key without one as RESTRICT, which is what NO ACTION does there.
		DesignedForeignKey customerKey = keys.get(0);
		assertEquals(new DesignedForeignKey("fk_line_customer", List.of("customer_id"), "customers", List.of("id"), "CASCADE", customerKey.onUpdate()),
			customerKey);
		assertEquals("products", keys.get(1).referencedTable());
		assertEquals(List.of("product_code", "product_version"), keys.get(1).columns());
		assertEquals(List.of("code", "version"), keys.get(1).referencedColumns());

		// Generating the model that was read leaves the existing tables and keys alone.
		context.designer().generate(List.of(read), () -> {
		});
		connection.useDatabase(DATABASE);
		context.databases().dropDatabase(new Database(name));
		connection.useDatabase(DATABASE);
	}

	@Test
	void tablesLiveInTheirOwnSchema(@TempDir Path dir) throws Exception {
		Assumptions.assumeTrue(dialect.supports(Dialect.Feature.SCHEMAS), "The server has no schemas");
		String name = "sales_" + System.nanoTime();
		var databases = new ConnectionContext(connection).databases();
		Schema schema = databases.createSchema(database, name);
		assertTrue(databases.getSchemas(database).stream().anyMatch(s -> s.getName().equals(name)));
		assertTrue(databases.getSchemas(database).stream().noneMatch(s -> s.getName().startsWith("pg_") || s.getName().equals("information_schema")));

		// A table with the same name as one in the current schema, so only a qualified statement reaches it.
		createTable("orders", "");
		service().createTable(schema, "orders", List.of(column("id", INTEGER, "", true), column("note", VARCHAR, "20", false)), null, "");
		assertTrue(service().exists(schema, "orders"));
		List<Table> tables = databases.getTables(schema);
		assertEquals(List.of("orders"), tables.stream().map(Table::getName).toList());
		Table orders = tables.get(0);
		assertEquals(name, orders.getSchema().getName());

		TableData[] row = new TableData[2];
		TableColumn[] columns = service().loadColumns(orders);
		assertEquals(List.of("id", "note"), Arrays.stream(columns).map(TableColumn::getName).toList());
		for (int i = 0; i < 2; i++) {
			row[i] = new TableData();
			row[i].setTableColumn(columns[i]);
			row[i].setData(i == 0 ? "7" : "in schema");
		}
		service().insertRow(orders, row);
		TableData[][] rows = service().loadPage(orders, 0, 10);
		assertEquals(1, rows.length);
		assertEquals("in schema", rows[0][1].getData());
		assertEquals(0, service().loadPage(table("orders"), 0, 10).length);
		assertEquals(1, databases.getTables(schema).get(0).getRowCount());

		// The script names the schema, so importing it replaces the table in that schema.
		File file = dir.resolve("schema.sql").toFile();
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{schema}, file.getAbsolutePath(),
			new ExportOptions(true, true, false, true, true));
		runSynchronously(export::setListener, export);
		ImportService imported = new ConnectionContext(connection).newImport(schema, file.getAbsolutePath());
		runSynchronously(imported::setListener, imported);
		assertEquals(1, service().loadPage(orders, 0, 10).length);

		service().addColumn(orders, "extra", "", "", INTEGER, false, false, false, true);
		TableIndex index = new TableIndex(orders);
		index.setName("orders_note_idx");
		service().addIndex(orders, index, new TableColumn[]{orders.getTableColumn("note")}, "INDEX");
		service().dropIndex(orders, orders.getTableIndex("orders_note_idx"));
		assertNotNull(service().analyseTable(orders));

		var designed = new ConnectionContext(connection).designer().reverseEngineer(schema);
		assertEquals(List.of("orders"), designed.tables().stream().map(DesignedTable::name).toList());
		assertEquals(3, designed.tables().get(0).columns().size());

		databases.dropSchema(schema);
		assertTrue(databases.getSchemas(database).stream().noneMatch(s -> s.getName().equals(name)));
		service().dropTable(table("orders"));
	}

	@Test
	void profileSelectionLimitsTheDatabasesAndSchemasShown() throws Exception {
		var all = new ConnectionContext(connection).databases();
		Database first = all.createDatabase("sel_a_" + System.nanoTime());
		Database second = all.createDatabase("sel_b_" + System.nanoTime());
		boolean schemas = dialect.supports(Dialect.Feature.SCHEMAS);
		if (schemas) {
			for (Database db : List.of(first, second)) {
				all.createSchema(db, "one");
				all.createSchema(db, "two");
			}
		}

		var unfiltered = new DatabaseService(new DatabaseRepository(connection), DatabaseSelection.NONE);
		assertTrue(names(unfiltered.getDatabases()).containsAll(List.of(first.getName(), second.getName(), DATABASE)));

		DatabaseSelection onlyFirst = DatabaseSelection.parse(first.getName());
		var filtered = new DatabaseService(new DatabaseRepository(connection), onlyFirst);
		assertEquals(List.of(first.getName()), names(filtered.getDatabases()));
		if (schemas) {
			assertEquals(List.of("one", "public", "two"), schemaNames(filtered.getSchemas(first)));
			assertEquals(List.of(), schemaNames(filtered.getSchemas(second)));
		}

		if (schemas) {
			DatabaseSelection oneSchema = onlyFirst.withSchema(first.getName(), "two", true).withSchema(second.getName(), "one", true);
			var picked = new DatabaseService(new DatabaseRepository(connection), oneSchema);
			assertEquals(List.of(first.getName(), second.getName()), names(picked.getDatabases()));
			assertEquals(List.of("two"), schemaNames(picked.getSchemas(first)));
			assertEquals(List.of("one"), schemaNames(picked.getSchemas(second)));
		}

		unfiltered.dropDatabase(first);
		unfiltered.dropDatabase(second);
	}

	@Test
	void catalogListsEveryDatabaseAndSchemaOfTheServer() throws Exception {
		var all = new ConnectionContext(connection).databases();
		Database extra = all.createDatabase("cat_" + System.nanoTime());
		boolean schemas = dialect.supports(Dialect.Feature.SCHEMAS);
		if (schemas) {
			all.createSchema(extra, "sales");
		}

		ConnectionProfile typed = profile();
		// A selection that names a database which is gone must not stop the catalog from connecting.
		typed.setDatabases("no_such_database");
		try (DatabaseLister catalog = new DatabaseLister(typed)) {
			catalog.connect();
			assertFalse(catalog.serverDescription().isEmpty());
			assertTrue(catalog.databases().containsAll(List.of(DATABASE, extra.getName())));
			if (schemas) {
				assertTrue(catalog.schemas(extra.getName()).containsAll(List.of("public", "sales")));
				assertFalse(catalog.schemas(extra.getName()).contains("pg_catalog"));
			} else {
				assertEquals(List.of(), catalog.schemas(extra.getName()));
			}
		}

		all.dropDatabase(extra);
	}

	private static List<String> names(List<Database> databases) {
		return databases.stream().map(Database::getName).toList();
	}

	private static List<String> schemaNames(List<Schema> schemas) {
		return schemas.stream().map(Schema::getName).toList();
	}

	@Test
	void exportOfADatabaseRestoresEverySchema(@TempDir Path dir) throws Exception {
		Assumptions.assumeTrue(dialect.supports(Dialect.Feature.SCHEMAS), "The server has no schemas");
		var databases = new ConnectionContext(connection).databases();
		Database other = databases.createDatabase("multi_" + System.nanoTime());
		connection.useDatabase(other.getName());
		Schema schema = databases.createSchema(other, "archive");
		service().createTable(schema, "items", List.of(column("id", INTEGER, "", true), column("note", VARCHAR, "20", false)), null, "");
		connection.executeUpdate("INSERT INTO " + dialect.quote(new TableName("archive", "items")) + " VALUES (1, 'kept')");

		File file = dir.resolve("database.sql").toFile();
		ExportService export = new ConnectionContext(connection).newExport(new Object[]{other}, file.getAbsolutePath(),
			new ExportOptions(true, true, false, true, true));
		runSynchronously(export::setListener, export);
		databases.dropSchema(schema);

		// The script creates the schema again and puts the table back into it, not into the current schema.
		ImportService imported = new ConnectionContext(connection).newImport(other, file.getAbsolutePath());
		runSynchronously(imported::setListener, imported);
		List<Table> tables = databases.getTables(schema);
		assertEquals(List.of("items"), tables.stream().map(Table::getName).toList());
		assertEquals("kept", service().loadPage(tables.get(0), 0, 10)[0][1].getData());
		assertFalse(service().exists(new Schema(other, connection.getSchema()), "items"));

		databases.dropDatabase(other);
	}

	@Test
	void importIntoASchemaPutsUnqualifiedTablesThere(@TempDir Path dir) throws Exception {
		Assumptions.assumeTrue(dialect.supports(Dialect.Feature.SCHEMAS), "The server has no schemas");
		var databases = new ConnectionContext(connection).databases();
		Schema schema = databases.createSchema(database, "loose_" + System.nanoTime());
		String current = connection.getSchema();
		Path script = dir.resolve("plain.sql");
		java.nio.file.Files.writeString(script, "CREATE TABLE plain_rows (id integer);\nINSERT INTO plain_rows VALUES(1);\n");

		ImportService imported = new ConnectionContext(connection).newImport(schema, script.toString());
		runSynchronously(imported::setListener, imported);
		assertTrue(service().exists(schema, "plain_rows"));
		assertFalse(service().exists(new Schema(database, current), "plain_rows"));
		assertEquals(current, connection.getSchema());

		// The query tab chooses a schema the same way.
		new ConnectionContext(connection).queries().useSchema(DATABASE, schema.getName());
		assertEquals(schema.getName(), connection.getSchema());
		assertEquals(1, new ConnectionContext(connection).queries().update("UPDATE plain_rows SET id = 2"));
		connection.useSchema(current);

		databases.dropSchema(schema);
	}

	private static CreateColumn column(String name, DataType type, String length, boolean primary) {
		CreateColumn column = new CreateColumn(name);
		column.type = type;
		column.length = length;
		column.primary = primary;
		column.notnull = primary;
		return column;
	}

	private static DesignedTable designed(DesignedDatabase database, String table) {
		return database.tables().stream().filter(t -> t.name().equals(table)).findFirst().orElseThrow();
	}

	@Test
	void refusesUnknownForeignKeyActions() {
		assertThrows(EsqlException.class,
			() -> dialect.addForeignKeySql(TableName.of("a"), "fk", List.of("b"), "c", List.of("d"), "CASCADE; DROP TABLE a", ""));
		assertFalse(dialect.addForeignKeySql(TableName.of("a"), "fk", List.of("b"), "c", List.of("d"), "", null).get(0).contains("ON "));
	}

	private void createTable(String name, String comment) throws Exception {
		CreateColumn id = new CreateColumn("id");
		id.type = INTEGER;
		id.primary = true;
		id.notnull = true;
		id.autoincrement = true;
		CreateColumn title = new CreateColumn("name");
		title.type = VARCHAR;
		title.length = "50";
		title.notnull = true;
		title.defaultval = "it's";
		CreateColumn note = new CreateColumn("note");
		note.type = new DataType("text", false, false, false, false, false, false, false, false);
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;

		for (String statement : dialect.createTableSql(TableName.of(name), Arrays.asList(id, title, note), engine, comment)) {
			connection.executeUpdate(statement);
		}
	}

	private TableService service() {
		return new ConnectionContext(connection).tables();
	}

	private Table table(String name) {
		Table table = new Table(database);
		table.setName(name);
		table.setType("TABLE");
		table.setComment("");
		return table;
	}

	private void insert(Table table, String name, String note) throws Exception {
		TableColumn[] columns = service().loadColumns(table);
		table.setColumns(columns);
		TableData[] row = new TableData[2];

		for (int i = 0; i < 2; i++) {
			row[i] = new TableData();
			row[i].setTableColumn(columns[i + 1]);
			row[i].setData(i == 0 ? name : note);
		}
		service().insertRow(table, row);
	}

	private List<String> columnNames(Table table) throws Exception {
		TableColumn[] columns = service().loadColumns(table);
		table.setColumns(columns);
		return Arrays.stream(columns).map(TableColumn::getName).collect(Collectors.toList());
	}

	private List<String> indexNames(Table table) throws Exception {
		return Arrays.stream(service().loadIndexes(table)).map(TableIndex::getName).collect(Collectors.toList());
	}

	/** Export and Import are written to run on a thread, a test wants them finished. */
	private void runSynchronously(Consumer<ProgressListener> attach, Runnable work) {
		AtomicReference<Exception> failure = new AtomicReference<>();
		attach.accept(new ProgressListener() {
			@Override
			public void progressed(int percent) {
			}

			@Override
			public void failed(Exception error) {
				failure.set(error);
			}
		});
		work.run();
		assertEquals(null, failure.get());
	}

	/** Keeps what a job told its listener, and can react to a status line (to cancel the job from inside). */
	private static final class Recording implements ProgressListener {
		Exception failure;
		String finished;
		String cancelled;
		List<String> details = List.of();
		Consumer<String> onStatus = status -> {
		};

		@Override
		public void progressed(int percent) {
		}

		@Override
		public void failed(Exception error) {
			failure = error;
		}

		@Override
		public void status(String text) {
			onStatus.accept(text);
		}

		@Override
		public void finished(String summary, List<String> lines) {
			finished = summary;
			details = lines;
		}

		@Override
		public void cancelled(String summary) {
			cancelled = summary;
		}
	}
}

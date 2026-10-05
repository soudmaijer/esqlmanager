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
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.export.ExportOptions;
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
import nl.errorsoft.esql.export.ExportService;
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
}

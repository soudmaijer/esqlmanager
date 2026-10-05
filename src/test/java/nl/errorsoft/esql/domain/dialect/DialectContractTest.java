package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.connection.ServerProcess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.domain.CreateColumn;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.domain.DataType;
import nl.errorsoft.esql.importexport.ExportService;
import nl.errorsoft.esql.user.GrantTarget;
import nl.errorsoft.esql.importexport.ImportService;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableIndex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What every dialect must be able to do, checked against a real server.
 * A subclass supplies the server, the tests run unchanged against each of them.
 */
abstract class DialectContractTest
{
	protected static final String DATABASE = "shop";

	private static final DataType INTEGER = new DataType( "integer", true, true, true, false, true, false, true, false );
	private static final DataType VARCHAR = new DataType( "varchar", true, true, true, false, true, false, false, false );
	private static final DataType BIGINT = new DataType( "bigint", true, true, true, false, true, false, false, false );

	private static DatabaseConnection connection;

	private Dialect dialect;
	private Database database;

	/** Connects to the server under test, once for all tests of the subclass. */
	protected abstract ConnectionProfile profile();

	@BeforeEach
	void connect() throws Exception
	{
		if( connection == null )
		{
			connection = new DatabaseConnection();
			connection.connect( profile(), "" );
		}
		dialect = connection.getConnectionProfile().getServerType().getDialect();
		database = new Database( DATABASE );
		connection.useDatabase( DATABASE );
	}

	@AfterAll
	static void disconnect() throws Exception
	{
		if( connection != null )
			connection.close();

		connection = null;
	}

	@Test
	void createsTableAndChangesItsColumnsAndIndexes() throws Exception
	{
		String name = "ddl_" + System.nanoTime();
		createTable( name, "Customer's note" );
		Table table = table( name );

		assertEquals( Arrays.asList( "id", "name", "note" ), columnNames( table ) );
		assertTrue( service().loadIndexes( table )[0].isPrimary() || indexNames( table ).contains( "PRIMARY" ) );

		service().addColumn( table, "age", "", "0", INTEGER, false, false, false, true );
		assertTrue( columnNames( table ).contains( "age" ) );

		service().editColumn( table.getTableColumn( "age" ), "years", "", "5", BIGINT, false, false, false, false );
		assertTrue( columnNames( table ).contains( "years" ) );
		assertFalse( columnNames( table ).contains( "age" ) );

		TableIndex index = new TableIndex( table );
		index.setName( "u_years" );
		service().addIndex( table, index, new TableColumn[] { table.getTableColumn( "years" ) }, "UNIQUE" );
		assertTrue( table.getTableIndex( "u_years" ).isUnique() );

		service().modifyIndex( table, table.getTableIndex( "u_years" ), new TableColumn[] { table.getTableColumn( "years" ), table.getTableColumn( "name" ) }, "INDEX" );
		assertFalse( table.getTableIndex( "u_years" ).isUnique() );
		assertEquals( 2, table.getTableIndex( "u_years" ).getTableColumns().length );

		service().dropIndex( table, table.getTableIndex( "u_years" ) );
		assertFalse( indexNames( table ).contains( "u_years" ) );

		service().modifyTable( table, name + "_renamed", null, "new comment" );
		assertEquals( name + "_renamed", table.getName() );
		service().dropTable( table );
	}

	@Test
	void exportedTableCanBeImportedAgain( @TempDir Path dir ) throws Exception
	{
		String name = "dump_" + System.nanoTime();
		createTable( name, "" );
		Table table = table( name );
		insert( table, "first", "plain" );
		insert( table, "second", "it's \\ tricky\nwith a second line" );
		insert( table, "third", null );

		File file = dir.resolve( "dump.sql" ).toFile();
		runSynchronously( new ExportService( connection, new Object[] { table }, file.getAbsolutePath(), true, true, false, true, true ) );
		runSynchronously( new ImportService( connection, database, file.getAbsolutePath() ) );

		TableData[][] rows = service().loadPage( table, 0, 100 );
		assertEquals( 3, rows.length );
		assertEquals( "it's \\ tricky\nwith a second line", rows[1][2].getData() );
		assertTrue( rows[2][2].isNull() );

		// Auto numbering continues after the highest id that was loaded.
		insert( table, "fourth", "after import" );
		assertEquals( 4, service().loadPage( table, 0, 100 ).length );
		service().dropTable( table );
	}

	@Test
	void valuesAreEscapedWhenEditingData() throws Exception
	{
		String name = "data_" + System.nanoTime();
		createTable( name, "" );
		Table table = table( name );
		insert( table, "a", "x" );
		insert( table, "b", "y" );
		String hostile = "O'Brien \\' ; DROP TABLE " + name + "; -- \\";

		TableData[][] rows = service().loadPage( table, 0, 100 );
		assertEquals( 1, service().changeCell( table, rows[0], rows[0][2], hostile ) );
		assertEquals( hostile, service().loadPage( table, 0, 100 )[0][2].getData() );

		service().deleteRow( table, service().loadPage( table, 0, 100 )[1] );
		assertEquals( 1, service().loadPage( table, 0, 100 ).length );
		service().dropTable( table );
	}

	@Test
	void managesUsersAndTheirPrivileges() throws Exception
	{
		String name = "usr_" + System.nanoTime();
		String tableName = "grants_" + System.nanoTime();
		createTable( tableName, "" );
		UserAdmin admin = dialect.getUserAdmin();
		DatabaseUser user = new DatabaseUser( name, admin.usesHost() ? "%" : null );

		admin.createUser( connection, user, "pass'word" );
		assertTrue( admin.listUsers( connection ).stream().anyMatch( u -> u.getName().equals( name ) ) );
		admin.changePassword( connection, user, "other" );

		GrantTarget onTable = GrantTarget.table( DATABASE, tableName );
		admin.setGrants( connection, user, onTable, new LinkedHashSet<String>( Arrays.asList( "SELECT", "INSERT" ) ) );
		assertEquals( new LinkedHashSet<String>( Arrays.asList( "SELECT", "INSERT" ) ), admin.getGrants( connection, user, onTable ) );

		admin.setGrants( connection, user, onTable, new LinkedHashSet<String>( Arrays.asList( "SELECT" ) ) );
		assertEquals( new LinkedHashSet<String>( Arrays.asList( "SELECT" ) ), admin.getGrants( connection, user, onTable ) );

		GrantTarget onDatabase = GrantTarget.database( DATABASE );
		String databasePrivilege = admin.getPrivileges( GrantTarget.Scope.DATABASE ).get( 0 );
		admin.setGrants( connection, user, onDatabase, new LinkedHashSet<String>( Arrays.asList( databasePrivilege ) ) );
		assertTrue( admin.getGrants( connection, user, onDatabase ).contains( databasePrivilege ) );

		admin.setGrants( connection, user, onTable, new LinkedHashSet<String>() );
		admin.setGrants( connection, user, onDatabase, new LinkedHashSet<String>() );
		admin.dropUser( connection, user );
		assertFalse( admin.listUsers( connection ).stream().anyMatch( u -> u.getName().equals( name ) ) );
		service().dropTable( table( tableName ) );
	}

	@Test
	void createsAndSwitchesDatabases() throws Exception
	{
		String name = "db_" + System.nanoTime();
		assertFalse( dialect.listDatabases( connection ).contains( name ) );

		new DatabaseService( connection ).createDatabase( name );
		assertTrue( dialect.listDatabases( connection ).contains( name ) );

		// The connection is using the database that is dropped.
		connection.useDatabase( name );
		new DatabaseService( connection ).dropDatabase( new Database( name ) );
		assertFalse( dialect.listDatabases( connection ).contains( name ) );
		connection.useDatabase( DATABASE );
	}

	@Test
	void showsServerStatusVariablesProcessesAndRunsMaintenance() throws Exception
	{
		for( String query : Arrays.asList( dialect.getStatusQuery(), dialect.getVariablesQuery() ) )
		{
			java.sql.ResultSet rs = connection.executeQuery( query );
			assertTrue( rs.next(), query );
			rs.close();
		}

		// Another connection is active, and can be ended.
		DatabaseConnection other = new DatabaseConnection();
		other.connect( profile(), "" );
		List<nl.errorsoft.esql.connection.ServerProcess> processes = dialect.listProcesses( connection );
		assertFalse( processes.isEmpty() );
		other.close();

		String name = "maint_" + System.nanoTime();
		createTable( name, "" );
		Table table = table( name );
		assertNotNull( service().optimizeTable( table ) );
		assertNotNull( service().analyseTable( table ) );
		service().dropTable( table );
	}

	private void createTable( String name, String comment ) throws Exception
	{
		CreateColumn id = new CreateColumn( "id" );
		id.type = INTEGER;
		id.primary = true;
		id.notnull = true;
		id.autoincrement = true;
		CreateColumn title = new CreateColumn( "name" );
		title.type = VARCHAR;
		title.length = "50";
		title.notnull = true;
		title.defaultval = "it's";
		CreateColumn note = new CreateColumn( "note" );
		note.type = new DataType( "text", false, false, false, false, false, false, false, false );
		String engine = dialect.getTableTypes().length > 0 ? dialect.getTableTypes()[0] : null;

		for( String statement : dialect.createTableSql( name, Arrays.asList( id, title, note ), engine, comment ) )
			connection.executeUpdate( statement );
	}

	private TableService service()
	{
		return new TableService( connection );
	}

	private Table table( String name )
	{
		Table table = new Table( database );
		table.setName( name );
		table.setType( "TABLE" );
		table.setComment( "" );
		return table;
	}

	private void insert( Table table, String name, String note ) throws Exception
	{
		TableColumn[] columns = service().loadColumns( table );
		table.setColumns( columns );
		TableData[] row = new TableData[2];

		for( int i = 0; i < 2; i++ )
		{
			row[i] = new TableData();
			row[i].setTableColumn( columns[i + 1] );
			row[i].setData( i == 0 ? name : note );
		}
		service().insertRow( table, row );
	}

	private List<String> columnNames( Table table ) throws Exception
	{
		TableColumn[] columns = service().loadColumns( table );
		table.setColumns( columns );
		return Arrays.stream( columns ).map( TableColumn::getName ).collect( Collectors.toList() );
	}

	private List<String> indexNames( Table table ) throws Exception
	{
		return Arrays.stream( service().loadIndexes( table ) ).map( TableIndex::getName ).collect( Collectors.toList() );
	}

	/** Export and Import are written to run on a thread, a test wants them finished. */
	private void runSynchronously( Runnable work )
	{
		java.util.concurrent.atomic.AtomicReference<Object> failure = new java.util.concurrent.atomic.AtomicReference<Object>();
		( (java.util.Observable)work ).addObserver( ( o, arg ) -> { if( arg instanceof Exception ) failure.set( arg ); } );
		work.run();
		assertEquals( null, failure.get() );
	}
}

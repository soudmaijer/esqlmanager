package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.blob.BlobRepository;
import nl.errorsoft.esql.blob.BlobService;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.DatabaseRepository;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.designer.DesignerService;
import nl.errorsoft.esql.importexport.ExportOptions;
import nl.errorsoft.esql.importexport.ExportRepository;
import nl.errorsoft.esql.importexport.ExportService;
import nl.errorsoft.esql.importexport.ImportRepository;
import nl.errorsoft.esql.importexport.ImportService;
import nl.errorsoft.esql.query.QueryRepository;
import nl.errorsoft.esql.query.QueryService;
import nl.errorsoft.esql.table.TableRepository;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.user.UserRepository;
import nl.errorsoft.esql.user.UserService;

/**
 * The composition root of one connection: creates the repositories and services of every feature once and wires them together.
 * Controllers ask the context for the service they need and never create services or repositories themselves.
 * Services that report progress to observers (export, import, blob transfer) are jobs, so a new one is made for every run.
 */
public class ConnectionContext {
	private final DatabaseConnection connection;
	private final TableService tables;
	private final DatabaseService databases;
	private final UserService users;
	private final ServerService servers;
	private final QueryService queries;
	private final DesignerService designer;

	public ConnectionContext(DatabaseConnection connection) {
		this.connection = connection;
		this.tables = new TableService(new TableRepository(connection));
		this.databases = new DatabaseService(new DatabaseRepository(connection), connection.getConnectionProfile().getDatabases());
		this.users = new UserService(new UserRepository(connection), databases);
		this.servers = new ServerService(new ServerRepository(connection), tables);
		this.queries = new QueryService(new QueryRepository(connection));
		this.designer = new DesignerService(databases, tables);
	}

	public TableService tables() {
		return tables;
	}

	public DatabaseService databases() {
		return databases;
	}

	public UserService users() {
		return users;
	}

	public ServerService servers() {
		return servers;
	}

	public QueryService queries() {
		return queries;
	}

	public DesignerService designer() {
		return designer;
	}

	public ExportService newExport(Object[] objects, String file, ExportOptions options) {
		return new ExportService(new ExportRepository(connection), objects, file, options);
	}

	public ImportService newImport(Object target, String file) {
		return new ImportService(new ImportRepository(connection), target, file);
	}

	public BlobService newBlobTransfer() {
		return new BlobService(new BlobRepository(connection), tables);
	}
}

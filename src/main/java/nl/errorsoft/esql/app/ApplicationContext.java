package nl.errorsoft.esql.app;

import nl.errorsoft.esql.driver.DriverService;
import nl.errorsoft.esql.error.ErrorHandler;
import nl.errorsoft.esql.settings.Settings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.errorsoft.esql.connection.ConnectionContext;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.ui.icon.ImageLoader;

/**
 * The one place components are resolved from. It holds what exists once per application (images, settings) and the
 * {@link ConnectionContext} of every open connection, which holds the services that work on that connection.
 */
public final class ApplicationContext {
	private static final ApplicationContext INSTANCE = new ApplicationContext();

	private final Map<DatabaseConnection, ConnectionContext> connections = new ConcurrentHashMap<>();
	private final ErrorHandler errors = new ErrorHandler();
	private ImageLoader images;
	private Settings settings;
	private DriverService drivers;

	private ApplicationContext() {
	}

	public static ApplicationContext get() {
		return INSTANCE;
	}

	public synchronized ImageLoader imageLoader() {
		if (images == null) {
			images = new ImageLoader("icons/");
			images.addIcon("greenLight", "check-good", 16, false);
			images.addIcon("redLight", "check-error", 16, false);
			images.addImage("../images/splash.gif", "esql");
			images.addIcon("imgConnect", "connect", 20, false);
			images.addIcon("imgDisconnect", "disconnect", 20, false);
			images.addIcon("imgHelp", "circle-help", 20, false);
			images.addIcon("imgHelpTab", "circle-help", 16, false);
			images.addIcon("imgPreferences", "ellipsis", 20, false);
			images.addIcon("pc", "server", 16, false);
			images.addBrandIcon("serverPostgres", "brand-postgresql", 18);
			images.addBrandIcon("serverMySql", "brand-mysql", 18);
			images.addBrandIcon("serverOracle", "brand-oracle", 18);
			images.addBrandIcon("serverSqlServer", "brand-microsoftsqlserver", 18);
			images.addIcon("imgCreateTable", "table-add", 16, false);
			images.addIcon("imgDropTable", "table-drop", 16, false);
			images.addIcon("imgUserManager", "users", 16, false);
			images.addIcon("imgRunQuery", "run-query", 16, false);
			images.addIcon("imgRunSelection", "run", 16, false);
			images.addIcon("imgRunAll", "run-all", 16, false);
			images.addIcon("imgOpen", "folder-open", 16, false);
			images.addIcon("imgNewRow", "row-add", 16, false);
			images.addIcon("imgUpdateRow", "update-row", 16, false);
			images.addIcon("imgDeleteRow", "row-drop", 16, false);
			images.addIcon("imgAddField", "field-add", 16, false);
			images.addIcon("imgDeleteField", "field-drop", 16, false);
			images.addIcon("imgSave", "save", 16, false);
			images.addIcon("imgClose", "window-close", 16, false);
			images.addIcon("profileNew", "plus", 16, false);
			images.addIcon("profileRemove", "minus", 16, false);
			images.addIcon("profileDuplicate", "copy", 16, false);
			images.addIcon("tbnew", "table-add", 16, false);
			images.addIcon("tbrem", "table-drop", 16, false);
			images.addIcon("tbedit", "update-row", 16, false);
			images.addIcon("imgFirst", "first", 16, false);
			images.addIcon("imgPrev", "prev", 16, false);
			images.addIcon("imgRun", "refresh", 16, false);
			images.addIcon("imgNext", "next", 16, false);
			images.addIcon("imgLast", "last", 16, false);
			images.addIcon("add_database", "database-add", 16, false);
			images.addIcon("imgDropDatabase", "database-drop", 16, false);
			images.addIcon("add_table", "table-add", 16, false);
			images.addIcon("add_comment", "comment", 16, false);
			images.addIcon("des_properties", "properties", 16, false);
			images.addIcon("des_open", "folder-open", 16, false);
			images.addIcon("des_new", "new-file", 16, false);
			images.addIcon("des_save", "save", 16, false);
			images.addIcon("des_check", "check", 16, false);
			images.addIcon("imgDesigner", "workflow", 16, false);
			images.addIcon("check_off", "check-off", 16, false);
			images.addIcon("check_pending", "check-pending", 16, false);
			images.addIcon("check_good", "check-good", 16, false);
			images.addIcon("check_error", "check-error", 16, false);
			images.addIcon("dbimg", "database", 16, false);
			images.addIcon("dbimgsel", "database", 16, true);
			images.addIcon("schemaimg", "folder-tree", 16, false);
			images.addIcon("schemaimgsel", "folder-tree", 16, true);
			images.addIcon("sc_select_20x20", "folder-tree", 20, false);
			images.addIcon("tbimg", "table", 16, false);
			images.addIcon("tbimgsel", "table", 16, true);
			images.addIcon("fldimg", "field", 16, false);
			images.addIcon("fldimgsel", "field", 16, true);
			images.addIcon("keyimg", "key", 16, false);
			images.addIcon("keyimgsel", "key", 16, true);
			images.addIcon("linkimg", "link", 16, false);
			images.addIcon("fkimg", "foreign-key", 16, false);
			images.addIcon("fkimgsel", "foreign-key", 16, true);
			images.addIcon("imgHasIndex", "index", 16, false);
			images.addIcon("imgHasIndexSel", "index", 16, true);
			images.addIcon("rt_select_20x20", "server", 20, false);
			images.addIcon("db_select_20x20", "database", 20, false);
			images.addIcon("tb_select_20x20", "table", 20, false);
			images.addIcon("fd_select_20x20", "field", 20, false);
			images.addIcon("sortup", "sort-up", 12, false);
			images.addIcon("sortdown", "sort-down", 12, false);
		}
		return images;
	}

	public ErrorHandler errors() {
		return errors;
	}

	public synchronized Settings settings() {
		if (settings == null) {
			settings = new Settings();
		}
		return settings;
	}

	/** Finds, loads and downloads the JDBC drivers. */
	public synchronized DriverService drivers() {
		if (drivers == null) {
			drivers = new DriverService(DataDirectory.drivers(), DriverService.MAVEN_CENTRAL);
		}
		return drivers;
	}

	/** The services for a connection, created the first time the connection is asked for. */
	public ConnectionContext connection(DatabaseConnection connection) {
		return connections.computeIfAbsent(connection, ConnectionContext::new);
	}

	/** Forgets the services of a connection that has been closed. */
	public void release(DatabaseConnection connection) {
		if (connection != null) {
			connections.remove(connection);
		}
	}
}
